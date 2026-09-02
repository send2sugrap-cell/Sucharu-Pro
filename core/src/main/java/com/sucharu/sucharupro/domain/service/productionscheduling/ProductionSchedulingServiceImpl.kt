package com.sucharu.sucharupro.domain.service.productionscheduling

import com.sucharu.sucharupro.domain.model.productionexecution.ProductionExecutionEventType
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecutionStatus
import com.sucharu.sucharupro.domain.model.productionscheduling.*
import com.sucharu.sucharupro.domain.repository.OrderRepository
import com.sucharu.sucharupro.domain.repository.productionexecution.ProductionExecutionRepository
import com.sucharu.sucharupro.domain.repository.productionplanning.ProductionPlanningRepository
import com.sucharu.sucharupro.domain.repository.productionscheduling.ProductionSchedulingRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class ProductionSchedulingServiceImpl(
    private val schedulingRepository: ProductionSchedulingRepository,
    private val executionRepository: ProductionExecutionRepository,
    private val orderRepository: OrderRepository,
    private val planningRepository: ProductionPlanningRepository
) : ProductionSchedulingService {

    override suspend fun createScheduleForJob(
        tenantId: String,
        executionJobId: String,
        baseStartTime: Long?,
        requestedDueDate: Long?,
        actor: String,
        idempotencyKey: String?
    ): ProductionSchedule {
        // Idempotency check
        if (idempotencyKey != null) {
            val existing = schedulingRepository.getScheduleByIdempotencyKey(tenantId, idempotencyKey)
            if (existing != null) return existing
        }

        val job = executionRepository.getJobExecutionById(tenantId, executionJobId)
            ?: throw IllegalArgumentException("Production job execution '$executionJobId' not found in tenant '$tenantId'.")

        val startTime = baseStartTime ?: System.currentTimeMillis()
        val schedule = ProductionSchedulingEngine.generateSchedule(
            job = job,
            baseStartTime = startTime,
            requestedDueDate = requestedDueDate,
            createdBy = actor
        )

        val saved = schedulingRepository.saveSchedule(schedule, idempotencyKey)

        // Record Audit Event
        schedulingRepository.saveScheduleEvent(
            ProductionScheduleEvent(
                eventId = "EVT-${UUID.randomUUID()}",
                scheduleId = saved.scheduleId,
                tenantId = tenantId,
                eventType = ProductionSchedulingEventType.SCHEDULE_CREATED,
                fromStatus = null,
                toStatus = saved.status,
                payload = "Schedule V1 generated with ${saved.slots.size} slots.",
                performedBy = actor,
                performedAt = System.currentTimeMillis()
            )
        )

        return saved
    }

    override suspend fun getScheduleById(tenantId: String, scheduleId: String): ProductionSchedule? {
        return schedulingRepository.getScheduleById(tenantId, scheduleId)
    }

    override suspend fun listSchedulesForJob(tenantId: String, executionJobId: String): List<ProductionSchedule> {
        return schedulingRepository.listSchedulesByJob(tenantId, executionJobId)
    }

    override suspend fun listSchedules(tenantId: String, limit: Int): List<ProductionSchedule> {
        return schedulingRepository.listSchedules(tenantId, limit)
    }

    override suspend fun approveSchedule(tenantId: String, scheduleId: String, actor: String): ProductionSchedule {
        val schedule = schedulingRepository.getScheduleById(tenantId, scheduleId)
            ?: throw IllegalArgumentException("Schedule '$scheduleId' not found.")

        if (schedule.hasBlockingConflicts) {
            val blocking = schedule.conflicts.filter { it.isBlocking }.joinToString("; ") { it.message }
            throw IllegalStateException("Cannot approve schedule with blocking conflicts: $blocking")
        }

        val job = executionRepository.getJobExecutionById(tenantId, schedule.executionJobId)
            ?: throw IllegalArgumentException("Underlying job '${schedule.executionJobId}' not found.")

        val now = System.currentTimeMillis()
        val approvedSchedule = schedule.copy(
            status = ScheduleStatus.APPROVED,
            approvedAt = now,
            approvedBy = actor,
            updatedAt = now,
            updatedBy = actor
        )

        val savedSchedule = schedulingRepository.saveSchedule(approvedSchedule)

        // Build and save dispatch queue items
        val queueItems = ProductionSchedulingEngine.buildDispatchQueueItems(savedSchedule, job)
        schedulingRepository.saveDispatchQueueItems(queueItems)

        // Update underlying execution job status to SCHEDULED if currently READY/RELEASED
        if (job.status == ProductionJobExecutionStatus.READY || job.status == ProductionJobExecutionStatus.RELEASED) {
            val updatedJob = job.copy(
                status = ProductionJobExecutionStatus.SCHEDULED,
                updatedAt = now,
                updatedBy = actor
            )
            executionRepository.saveJobExecution(updatedJob)
        }

        // Audit event
        schedulingRepository.saveScheduleEvent(
            ProductionScheduleEvent(
                eventId = "EVT-${UUID.randomUUID()}",
                scheduleId = scheduleId,
                tenantId = tenantId,
                eventType = ProductionSchedulingEventType.SCHEDULE_APPROVED,
                fromStatus = schedule.status,
                toStatus = ScheduleStatus.APPROVED,
                payload = "Schedule approved and ${queueItems.size} items queued for dispatch.",
                performedBy = actor,
                performedAt = now
            )
        )

        return savedSchedule
    }

    override suspend fun supersedeSchedule(
        tenantId: String,
        scheduleId: String,
        reason: String,
        newStartTime: Long?,
        requestedDueDate: Long?,
        actor: String
    ): ProductionSchedule {
        val existing = schedulingRepository.getScheduleById(tenantId, scheduleId)
            ?: throw IllegalArgumentException("Schedule '$scheduleId' not found.")

        val job = executionRepository.getJobExecutionById(tenantId, existing.executionJobId)
            ?: throw IllegalArgumentException("Underlying job '${existing.executionJobId}' not found.")

        val startTime = newStartTime ?: System.currentTimeMillis()
        val (supersededOld, newSchedule) = ProductionSchedulingEngine.supersedeSchedule(
            existing = existing,
            job = job,
            reason = reason,
            actor = actor,
            newStartTime = startTime,
            requestedDueDate = requestedDueDate
        )

        schedulingRepository.saveSchedule(supersededOld)
        val savedNew = schedulingRepository.saveSchedule(newSchedule)

        // Audit events
        schedulingRepository.saveScheduleEvent(
            ProductionScheduleEvent(
                eventId = "EVT-${UUID.randomUUID()}",
                scheduleId = existing.scheduleId,
                tenantId = tenantId,
                eventType = ProductionSchedulingEventType.SCHEDULE_SUPERSEDED,
                fromStatus = existing.status,
                toStatus = ScheduleStatus.SUPERSEDED,
                payload = "Superseded by ${savedNew.scheduleId}. Reason: $reason",
                performedBy = actor,
                performedAt = System.currentTimeMillis()
            )
        )

        schedulingRepository.saveScheduleEvent(
            ProductionScheduleEvent(
                eventId = "EVT-${UUID.randomUUID()}",
                scheduleId = savedNew.scheduleId,
                tenantId = tenantId,
                eventType = ProductionSchedulingEventType.SCHEDULE_CREATED,
                fromStatus = null,
                toStatus = savedNew.status,
                payload = "Schedule V${savedNew.version} created via superseding of ${existing.scheduleId}.",
                performedBy = actor,
                performedAt = System.currentTimeMillis()
            )
        )

        return savedNew
    }

    override suspend fun dispatchQueueItem(tenantId: String, queueItemId: String, actor: String): ProductionDispatchQueueItem {
        val item = schedulingRepository.getDispatchQueueItemById(tenantId, queueItemId)
            ?: throw IllegalArgumentException("Dispatch queue item '$queueItemId' not found.")

        val now = System.currentTimeMillis()
        val updated = item.copy(
            dispatchStatus = DispatchStatus.DISPATCHED,
            dispatchedAt = now,
            notes = "Dispatched by $actor"
        )

        val saved = schedulingRepository.updateDispatchQueueItem(updated)

        // Audit event
        schedulingRepository.saveScheduleEvent(
            ProductionScheduleEvent(
                eventId = "EVT-${UUID.randomUUID()}",
                scheduleId = item.scheduleId,
                tenantId = tenantId,
                eventType = ProductionSchedulingEventType.WORK_ORDER_DISPATCHED,
                fromStatus = null,
                toStatus = null,
                payload = "Work order ${item.workOrderId} (${item.operationCode}) dispatched to machine ${item.machineName}.",
                performedBy = actor,
                performedAt = now
            )
        )

        return saved
    }

    override suspend fun acknowledgeQueueItem(tenantId: String, queueItemId: String, actor: String): ProductionDispatchQueueItem {
        val item = schedulingRepository.getDispatchQueueItemById(tenantId, queueItemId)
            ?: throw IllegalArgumentException("Dispatch queue item '$queueItemId' not found.")

        val now = System.currentTimeMillis()
        val updated = item.copy(
            dispatchStatus = DispatchStatus.ACKNOWLEDGED,
            acknowledgedAt = now,
            notes = "Acknowledged by operator: $actor"
        )

        val saved = schedulingRepository.updateDispatchQueueItem(updated)

        schedulingRepository.saveScheduleEvent(
            ProductionScheduleEvent(
                eventId = "EVT-${UUID.randomUUID()}",
                scheduleId = item.scheduleId,
                tenantId = tenantId,
                eventType = ProductionSchedulingEventType.WORK_ORDER_ACKNOWLEDGED,
                fromStatus = null,
                toStatus = null,
                payload = "Work order ${item.workOrderId} acknowledged on shop floor.",
                performedBy = actor,
                performedAt = now
            )
        )

        return saved
    }

    override suspend fun listDispatchQueue(
        tenantId: String,
        scheduleId: String?,
        limit: Int
    ): List<ProductionDispatchQueueItem> {
        return schedulingRepository.listDispatchQueue(tenantId, scheduleId, limit)
    }

    override suspend fun listCapacityWindows(
        tenantId: String,
        machineId: String?,
        shiftDate: String?
    ): List<ProductionCapacityWindow> {
        return schedulingRepository.listCapacityWindows(tenantId, machineId, shiftDate)
    }

    override suspend fun getScheduleConflicts(tenantId: String, scheduleId: String): List<ProductionScheduleConflict> {
        val schedule = schedulingRepository.getScheduleById(tenantId, scheduleId)
            ?: throw IllegalArgumentException("Schedule '$scheduleId' not found.")
        return schedule.conflicts
    }

    override suspend fun reconcileSchedule(tenantId: String, scheduleId: String): ProductionScheduleReconciliationResult {
        val schedule = schedulingRepository.getScheduleById(tenantId, scheduleId)
            ?: throw IllegalArgumentException("Schedule '$scheduleId' not found.")

        val job = executionRepository.getJobExecutionById(tenantId, schedule.executionJobId)
            ?: throw IllegalArgumentException("Job '${schedule.executionJobId}' not found.")

        val dispatchQueue = schedulingRepository.listDispatchQueue(tenantId, scheduleId)

        val result = ProductionSchedulingReconciliationService.reconcile(schedule, job, dispatchQueue)

        schedulingRepository.saveScheduleEvent(
            ProductionScheduleEvent(
                eventId = "EVT-${UUID.randomUUID()}",
                scheduleId = scheduleId,
                tenantId = tenantId,
                eventType = ProductionSchedulingEventType.RECONCILIATION_PERFORMED,
                fromStatus = null,
                toStatus = null,
                payload = "Reconciliation result: isFullyReconciled=${result.isFullyReconciled}, discrepancies=${result.discrepancies.size}",
                performedBy = "SYSTEM_RECONCILER",
                performedAt = System.currentTimeMillis()
            )
        )

        return result
    }

    override suspend fun getAiHandoffContract(
        tenantId: String,
        scheduleId: String
    ): Module17Step06ProductionSchedulingHandoffContract {
        val schedule = schedulingRepository.getScheduleById(tenantId, scheduleId)
            ?: throw IllegalArgumentException("Schedule '$scheduleId' not found.")

        val dispatchQueue = schedulingRepository.listDispatchQueue(tenantId, scheduleId)

        val machineSummary = schedule.slots.map { "${it.stageType.name} -> ${it.machineName} (${it.totalEstimatedMinutes}m)" }
        val operatorSummary = schedule.slots.mapNotNull { it.operatorName }

        val avgUtil = if (schedule.capacityWindows.isNotEmpty()) {
            schedule.capacityWindows.map { it.utilizationRate }.reduce { a, b -> a.add(b) }
                .divide(BigDecimal.valueOf(schedule.capacityWindows.size.toLong()), 4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        }

        val blockingIssues = schedule.conflicts.filter { it.isBlocking }.map { it.message }
        val warnings = schedule.conflicts.filter { !it.isBlocking }.map { it.message }

        val dispatchStatusMap = dispatchQueue.groupingBy { it.dispatchStatus.name }.eachCount()

        val recon = reconcileSchedule(tenantId, scheduleId)

        return Module17Step06ProductionSchedulingHandoffContract(
            contractVersion = "1.0.0",
            scheduleId = schedule.scheduleId,
            scheduleVersion = schedule.version,
            tenantId = schedule.tenantId,
            projectId = schedule.projectId,
            executionJobId = schedule.executionJobId,
            orderId = schedule.orderId,
            orderNumber = schedule.orderNumber,
            status = schedule.status.name,
            plannedStartAt = schedule.plannedStartAt,
            plannedEndAt = schedule.plannedEndAt,
            totalEstimatedDurationMinutes = schedule.totalEstimatedMinutes,
            slotsCount = schedule.slots.size,
            machineAssignmentsSummary = machineSummary,
            operatorAssignmentsSummary = operatorSummary,
            capacityUtilizationAvg = avgUtil,
            activeConflictsCount = schedule.conflicts.size,
            blockingIssues = blockingIssues,
            warnings = warnings,
            dispatchStatusSummary = dispatchStatusMap,
            isFullyReconciled = recon.isFullyReconciled,
            integrityHash = schedule.integrityHash,
            generatedAt = System.currentTimeMillis()
        )
    }
}

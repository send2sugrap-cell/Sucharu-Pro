package com.sucharu.sucharupro.domain.service.productionscheduling

import com.sucharu.sucharupro.domain.model.productionexecution.ProductionJobExecution
import com.sucharu.sucharupro.domain.model.productionexecution.ProductionWorkOrder
import com.sucharu.sucharupro.domain.model.productionscheduling.*
import java.util.UUID

object ProductionSchedulingEngine {

    const val CHANGEOVER_BUFFER_MINUTES = 10

    /**
     * Generates a deterministic [ProductionSchedule] from canonical [ProductionJobExecution].
     */
    fun generateSchedule(
        job: ProductionJobExecution,
        baseStartTime: Long = System.currentTimeMillis(),
        requestedDueDate: Long? = null,
        createdBy: String
    ): ProductionSchedule {
        val scheduleId = "SCHED-${job.executionJobId}-V1"
        val slots = deriveSlots(scheduleId, job, baseStartTime, requestedDueDate)

        val totalSetup = slots.sumOf { it.setupMinutes }
        val totalRun = slots.sumOf { it.runMinutes }
        val plannedStart = slots.firstOrNull()?.scheduledStartTimestamp ?: baseStartTime
        val plannedEnd = slots.lastOrNull()?.scheduledEndTimestamp ?: (baseStartTime + (totalSetup + totalRun) * 60000L)

        val capacityWindows = ProductionCapacityPlanner.computeCapacityWindows(
            tenantId = job.tenantId,
            startTime = baseStartTime,
            slots = slots
        )

        val conflicts = ProductionConflictDetector.detectConflicts(
            scheduleId = scheduleId,
            job = job,
            slots = slots,
            capacityWindows = capacityWindows,
            requestedDueDate = requestedDueDate
        )

        val fingerprint = ProductionSchedulingMathUtils.generateFingerprint(
            tenantId = job.tenantId,
            executionJobId = job.executionJobId,
            orderId = job.orderId,
            version = 1,
            totalEstimatedMinutes = totalSetup + totalRun,
            slotCount = slots.size
        )

        val now = System.currentTimeMillis()
        val integrityHash = ProductionSchedulingMathUtils.sha256(
            "$scheduleId|${job.tenantId}|${job.executionJobId}|${job.orderId}|1|$fingerprint|$now"
        )

        val initialStatus = if (conflicts.any { it.isBlocking }) ScheduleStatus.PROPOSED else ScheduleStatus.PROPOSED

        return ProductionSchedule(
            scheduleId = scheduleId,
            tenantId = job.tenantId,
            projectId = job.projectId,
            executionJobId = job.executionJobId,
            orderId = job.orderId,
            orderNumber = job.orderNumber,
            version = 1,
            isCurrent = true,
            status = initialStatus,
            plannedStartAt = plannedStart,
            plannedEndAt = plannedEnd,
            totalSetupMinutes = totalSetup,
            totalRunMinutes = totalRun,
            slots = slots,
            capacityWindows = capacityWindows,
            conflicts = conflicts,
            scheduleFingerprint = fingerprint,
            integrityHash = integrityHash,
            supersededByScheduleId = null,
            supersedingReason = null,
            approvedAt = null,
            approvedBy = null,
            createdAt = now,
            createdBy = createdBy,
            updatedAt = now,
            updatedBy = createdBy
        )
    }

    /**
     * Supersedes an existing schedule and produces a new immutable version.
     */
    fun supersedeSchedule(
        existing: ProductionSchedule,
        job: ProductionJobExecution,
        reason: String,
        actor: String,
        newStartTime: Long = System.currentTimeMillis(),
        requestedDueDate: Long? = null
    ): Pair<ProductionSchedule, ProductionSchedule> {
        val nextVersion = existing.version + 1
        val newScheduleId = "SCHED-${job.executionJobId}-V$nextVersion"
        val slots = deriveSlots(newScheduleId, job, newStartTime, requestedDueDate)

        val totalSetup = slots.sumOf { it.setupMinutes }
        val totalRun = slots.sumOf { it.runMinutes }
        val plannedStart = slots.firstOrNull()?.scheduledStartTimestamp ?: newStartTime
        val plannedEnd = slots.lastOrNull()?.scheduledEndTimestamp ?: (newStartTime + (totalSetup + totalRun) * 60000L)

        val capacityWindows = ProductionCapacityPlanner.computeCapacityWindows(
            tenantId = job.tenantId,
            startTime = newStartTime,
            slots = slots
        )

        val conflicts = ProductionConflictDetector.detectConflicts(
            scheduleId = newScheduleId,
            job = job,
            slots = slots,
            capacityWindows = capacityWindows,
            requestedDueDate = requestedDueDate
        )

        val fingerprint = ProductionSchedulingMathUtils.generateFingerprint(
            tenantId = job.tenantId,
            executionJobId = job.executionJobId,
            orderId = job.orderId,
            version = nextVersion,
            totalEstimatedMinutes = totalSetup + totalRun,
            slotCount = slots.size
        )

        val now = System.currentTimeMillis()
        val integrityHash = ProductionSchedulingMathUtils.sha256(
            "$newScheduleId|${job.tenantId}|${job.executionJobId}|${job.orderId}|$nextVersion|$fingerprint|$now"
        )

        val supersededOld = existing.copy(
            isCurrent = false,
            status = ScheduleStatus.SUPERSEDED,
            supersededByScheduleId = newScheduleId,
            supersedingReason = reason,
            updatedAt = now,
            updatedBy = actor
        )

        val newSchedule = ProductionSchedule(
            scheduleId = newScheduleId,
            tenantId = job.tenantId,
            projectId = job.projectId,
            executionJobId = job.executionJobId,
            orderId = job.orderId,
            orderNumber = job.orderNumber,
            version = nextVersion,
            isCurrent = true,
            status = ScheduleStatus.PROPOSED,
            plannedStartAt = plannedStart,
            plannedEndAt = plannedEnd,
            totalSetupMinutes = totalSetup,
            totalRunMinutes = totalRun,
            slots = slots,
            capacityWindows = capacityWindows,
            conflicts = conflicts,
            scheduleFingerprint = fingerprint,
            integrityHash = integrityHash,
            supersededByScheduleId = null,
            supersedingReason = null,
            approvedAt = null,
            approvedBy = null,
            createdAt = now,
            createdBy = actor,
            updatedAt = now,
            updatedBy = actor
        )

        return Pair(supersededOld, newSchedule)
    }

    /**
     * Converts approved schedule slots into active dispatch queue items.
     */
    fun buildDispatchQueueItems(
        schedule: ProductionSchedule,
        job: ProductionJobExecution
    ): List<ProductionDispatchQueueItem> {
        val now = System.currentTimeMillis()
        return schedule.slots.mapIndexed { idx, slot ->
            val initialDispatchStatus = if (idx == 0) DispatchStatus.READY else DispatchStatus.QUEUED
            ProductionDispatchQueueItem(
                queueItemId = "QUEUE-${slot.slotId}",
                tenantId = schedule.tenantId,
                scheduleId = schedule.scheduleId,
                scheduleVersion = schedule.version,
                workOrderId = slot.workOrderId,
                executionJobId = job.executionJobId,
                orderId = job.orderId,
                orderNumber = job.orderNumber,
                sequenceNumber = slot.sequenceNumber,
                stageType = slot.stageType,
                operationCode = slot.operationCode,
                operationName = slot.operationName,
                targetWorkCenter = slot.machineName,
                machineId = slot.machineId,
                machineName = slot.machineName,
                operatorId = slot.operatorId,
                operatorName = slot.operatorName,
                dispatchStatus = initialDispatchStatus,
                priorityScore = slot.priorityScore,
                plannedQuantity = job.plannedQuantity,
                estimatedSetupMinutes = slot.setupMinutes,
                estimatedRunMinutes = slot.runMinutes,
                scheduledStartTimestamp = slot.scheduledStartTimestamp,
                scheduledEndTimestamp = slot.scheduledEndTimestamp,
                queuedAt = now,
                readyAt = if (initialDispatchStatus == DispatchStatus.READY) now else null,
                dispatchedAt = null,
                acknowledgedAt = null,
                completedAt = null,
                notes = slot.notes
            )
        }
    }

    private fun deriveSlots(
        scheduleId: String,
        job: ProductionJobExecution,
        baseStartTime: Long,
        requestedDueDate: Long?
    ): List<ProductionScheduleSlot> {
        val slots = mutableListOf<ProductionScheduleSlot>()
        var cursorTime = baseStartTime

        val sortedWorkOrders = job.workOrders.sortedBy { it.sequenceNumber }

        sortedWorkOrders.forEach { wo ->
            val compatibleMachine = ProductionCapacityPlanner.resolveCompatibleMachine(
                stageType = wo.stageType,
                printingMethod = job.specification.printingMethod
            )

            val operator = ProductionCapacityPlanner.resolveQualifiedOperator(stageType = wo.stageType)

            val setupMinutes = if (wo.estimatedSetupMinutes > 0) wo.estimatedSetupMinutes else 15
            val runMinutes = if (wo.estimatedRunMinutes > 0) wo.estimatedRunMinutes else 30
            val totalMinutes = setupMinutes + runMinutes

            val slotStart = cursorTime
            val slotEnd = slotStart + (totalMinutes * 60000L)

            val priority = ProductionSchedulingPriorityCalculator.calculateSlotPriority(
                job = job,
                workOrder = wo,
                targetStartTime = slotStart,
                dueDateTime = requestedDueDate
            )

            val slot = ProductionScheduleSlot(
                slotId = "SLOT-$scheduleId-${wo.sequenceNumber}",
                scheduleId = scheduleId,
                workOrderId = wo.workOrderId,
                executionJobId = job.executionJobId,
                sequenceNumber = wo.sequenceNumber,
                stageType = wo.stageType,
                operationCode = wo.operationCode,
                operationName = wo.operationName,
                machineId = compatibleMachine.machineId,
                machineName = compatibleMachine.machineName,
                operatorId = operator?.operatorId,
                operatorName = operator?.operatorName,
                scheduledStartTimestamp = slotStart,
                scheduledEndTimestamp = slotEnd,
                setupMinutes = setupMinutes,
                runMinutes = runMinutes,
                totalEstimatedMinutes = totalMinutes,
                priorityScore = priority,
                status = DispatchStatus.QUEUED,
                notes = "Scheduled for ${compatibleMachine.machineName}"
            )

            slots.add(slot)
            cursorTime = slotEnd + (CHANGEOVER_BUFFER_MINUTES * 60000L) // Add 10-minute changeover buffer
        }

        return slots
    }
}

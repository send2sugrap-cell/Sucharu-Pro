package com.sucharu.sucharupro.domain.service.productionexecution

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.productionexecution.*
import com.sucharu.sucharupro.domain.service.productionexecution.p4
import com.sucharu.sucharupro.domain.repository.OrderRepository
import com.sucharu.sucharupro.domain.repository.commercialcommitment.CommercialCommitmentRepository
import com.sucharu.sucharupro.domain.repository.printingquote.PrintingQuoteRepository
import com.sucharu.sucharupro.domain.repository.productionexecution.ProductionExecutionRepository
import com.sucharu.sucharupro.domain.repository.productionplanning.ProductionPlanningRepository
import com.sucharu.sucharupro.domain.validation.productionexecution.ProductionExecutionValidator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.util.UUID

class ProductionExecutionServiceImpl(
    private val executionRepository: ProductionExecutionRepository,
    private val orderRepository: OrderRepository,
    private val planningRepository: ProductionPlanningRepository,
    private val commitmentRepository: CommercialCommitmentRepository,
    private val quoteRepository: PrintingQuoteRepository
) : ProductionExecutionService {

    private val mutex = Mutex()

    private suspend fun resolveOrder(orderId: String): Order {
        return when (val res = orderRepository.findOrderById(orderId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> throw NoSuchElementException(res.message)
            DomainResult.Loading -> throw IllegalStateException("Unexpected loading state")
        }
    }

    override suspend fun evaluateJobEligibility(
        tenantId: String,
        orderId: String
    ): DomainResult<List<ProductionExecutionDiagnostic>> {
        return try {
            val order = resolveOrder(orderId)
            val planning = planningRepository.getLatestPlanningSnapshotByOrder(tenantId, orderId)
                ?: return DomainResult.Success(
                    listOf(
                        ProductionExecutionDiagnostic(
                            code = "PLANNING_SNAPSHOT_MISSING",
                            message = "No production planning snapshot found for order '$orderId'. Generate plan in Step 04 first.",
                            isBlocking = true,
                            recommendedAction = "Execute manufacturing readiness evaluation and generate plan."
                        )
                    )
                )

            val diagnostics = ProductionExecutionValidator.validateJobCreationEligibility(order, planning)
            DomainResult.Success(diagnostics)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to evaluate job eligibility")
        }
    }

    override suspend fun createJobExecution(
        tenantId: String,
        orderId: String,
        requestedBy: String,
        idempotencyKey: String?
    ): DomainResult<ProductionJobExecution> = mutex.withLock {
        try {
            // Idempotency check
            if (idempotencyKey != null) {
                val existing = executionRepository.getJobExecutionByIdempotencyKey(tenantId, idempotencyKey)
                if (existing != null) {
                    return@withLock DomainResult.Success(existing)
                }
            }

            val order = resolveOrder(orderId)
            val planning = planningRepository.getLatestPlanningSnapshotByOrder(tenantId, orderId)
                ?: throw IllegalStateException("No production planning snapshot found for order '$orderId'.")

            val diagnostics = ProductionExecutionValidator.validateJobCreationEligibility(order, planning)
            val blockers = diagnostics.filter { it.isBlocking }
            if (blockers.isNotEmpty()) {
                throw IllegalStateException("Cannot create production job: ${blockers.joinToString { it.message }}")
            }

            val job = ProductionJobEngine.createJobExecution(order, planning, requestedBy)
            val saved = executionRepository.saveJobExecution(job, idempotencyKey)

            // Audit
            executionRepository.saveExecutionEvent(
                ProductionExecutionEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    executionJobId = saved.executionJobId,
                    tenantId = tenantId,
                    eventType = ProductionExecutionEventType.JOB_CREATED,
                    fromStatus = null,
                    toStatus = saved.status.name,
                    payload = "Created production job '${saved.executionJobId}' with ${saved.workOrders.size} work orders.",
                    performedBy = requestedBy,
                    performedAt = System.currentTimeMillis()
                )
            )

            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to create production job execution")
        }
    }

    override suspend fun getJobExecution(
        tenantId: String,
        executionJobId: String
    ): DomainResult<ProductionJobExecution?> {
        return try {
            val job = executionRepository.getJobExecutionById(tenantId, executionJobId)
            DomainResult.Success(job)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to get job execution")
        }
    }

    override suspend fun listJobExecutionsByOrder(
        tenantId: String,
        orderId: String
    ): DomainResult<List<ProductionJobExecution>> {
        return try {
            val list = executionRepository.listJobExecutionsByOrder(tenantId, orderId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to list job executions by order")
        }
    }

    override suspend fun listJobExecutions(
        tenantId: String,
        limit: Int
    ): DomainResult<List<ProductionJobExecution>> {
        return try {
            val list = executionRepository.listJobExecutions(tenantId, limit)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to list job executions")
        }
    }

    override suspend fun releaseJob(
        tenantId: String,
        executionJobId: String,
        releasedBy: String
    ): DomainResult<ProductionJobExecution> = mutex.withLock {
        try {
            val job = executionRepository.getJobExecutionById(tenantId, executionJobId)
                ?: throw NoSuchElementException("Job '$executionJobId' not found.")

            require(job.status.canTransitionTo(ProductionJobExecutionStatus.RELEASED)) {
                "Cannot release job in status '${job.status}'."
            }

            val updated = job.copy(
                status = ProductionJobExecutionStatus.RELEASED,
                updatedAt = System.currentTimeMillis(),
                updatedBy = releasedBy
            )
            val saved = executionRepository.saveJobExecution(updated)

            executionRepository.saveExecutionEvent(
                ProductionExecutionEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    executionJobId = saved.executionJobId,
                    tenantId = tenantId,
                    eventType = ProductionExecutionEventType.JOB_RELEASED,
                    fromStatus = job.status.name,
                    toStatus = ProductionJobExecutionStatus.RELEASED.name,
                    payload = "Job released to shop-floor.",
                    performedBy = releasedBy,
                    performedAt = System.currentTimeMillis()
                )
            )

            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to release job")
        }
    }

    override suspend fun scheduleJob(
        tenantId: String,
        executionJobId: String,
        scheduledBy: String
    ): DomainResult<ProductionJobExecution> = mutex.withLock {
        try {
            val job = executionRepository.getJobExecutionById(tenantId, executionJobId)
                ?: throw NoSuchElementException("Job '$executionJobId' not found.")

            val updated = job.copy(
                status = ProductionJobExecutionStatus.SCHEDULED,
                updatedAt = System.currentTimeMillis(),
                updatedBy = scheduledBy
            )
            val saved = executionRepository.saveJobExecution(updated)

            executionRepository.saveExecutionEvent(
                ProductionExecutionEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    executionJobId = saved.executionJobId,
                    tenantId = tenantId,
                    eventType = ProductionExecutionEventType.JOB_SCHEDULED,
                    fromStatus = job.status.name,
                    toStatus = ProductionJobExecutionStatus.SCHEDULED.name,
                    payload = "Job scheduled for execution.",
                    performedBy = scheduledBy,
                    performedAt = System.currentTimeMillis()
                )
            )

            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to schedule job")
        }
    }

    override suspend fun startStage(
        tenantId: String,
        executionJobId: String,
        workOrderId: String,
        operatorId: String?,
        machineId: String?,
        startedBy: String
    ): DomainResult<ProductionJobExecution> = mutex.withLock {
        try {
            val job = executionRepository.getJobExecutionById(tenantId, executionJobId)
                ?: throw NoSuchElementException("Job '$executionJobId' not found.")

            val wo = job.workOrders.find { it.workOrderId == workOrderId }
                ?: throw NoSuchElementException("Work order '$workOrderId' not found in job '$executionJobId'.")

            // Validate predecessor dependencies
            val diag = ProductionExecutionValidator.validateWorkOrderStart(wo, job.workOrders)
            val blockers = diag.filter { it.isBlocking }
            if (blockers.isNotEmpty()) {
                throw IllegalStateException("Cannot start stage: ${blockers.joinToString { it.message }}")
            }

            val now = System.currentTimeMillis()
            val updatedWo = wo.copy(
                status = WorkOrderStatus.IN_PROGRESS,
                assignedOperatorId = operatorId ?: wo.assignedOperatorId,
                assignedMachineId = machineId ?: wo.assignedMachineId,
                startedAt = wo.startedAt ?: now
            )
            executionRepository.updateWorkOrder(updatedWo)

            val newJobStatus = if (job.status == ProductionJobExecutionStatus.READY || job.status == ProductionJobExecutionStatus.RELEASED || job.status == ProductionJobExecutionStatus.SCHEDULED) {
                ProductionJobExecutionStatus.IN_PROGRESS
            } else {
                job.status
            }

            val updatedJob = job.copy(
                status = newJobStatus,
                currentStageType = wo.stageType,
                workOrders = job.workOrders.map { if (it.workOrderId == workOrderId) updatedWo else it },
                startedQuantity = if (job.startedQuantity.compareTo(BigDecimal.ZERO) == 0) job.plannedQuantity else job.startedQuantity,
                updatedAt = now,
                updatedBy = startedBy
            )
            val saved = executionRepository.saveJobExecution(updatedJob)

            executionRepository.saveExecutionEvent(
                ProductionExecutionEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    executionJobId = saved.executionJobId,
                    workOrderId = workOrderId,
                    tenantId = tenantId,
                    eventType = ProductionExecutionEventType.STAGE_STARTED,
                    fromStatus = wo.status.name,
                    toStatus = WorkOrderStatus.IN_PROGRESS.name,
                    payload = "Started stage '${wo.operationName}' (Stage: ${wo.stageType.name}).",
                    performedBy = startedBy,
                    performedAt = now
                )
            )

            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to start stage")
        }
    }

    override suspend fun pauseStage(
        tenantId: String,
        executionJobId: String,
        workOrderId: String,
        reason: String?,
        pausedBy: String
    ): DomainResult<ProductionJobExecution> = mutex.withLock {
        try {
            val job = executionRepository.getJobExecutionById(tenantId, executionJobId)
                ?: throw NoSuchElementException("Job '$executionJobId' not found.")

            val wo = job.workOrders.find { it.workOrderId == workOrderId }
                ?: throw NoSuchElementException("Work order '$workOrderId' not found.")

            val now = System.currentTimeMillis()
            val updatedWo = wo.copy(status = WorkOrderStatus.PAUSED, pausedAt = now)
            executionRepository.updateWorkOrder(updatedWo)

            executionRepository.saveExecutionEvent(
                ProductionExecutionEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    executionJobId = job.executionJobId,
                    workOrderId = workOrderId,
                    tenantId = tenantId,
                    eventType = ProductionExecutionEventType.STAGE_PAUSED,
                    fromStatus = wo.status.name,
                    toStatus = WorkOrderStatus.PAUSED.name,
                    payload = "Paused stage '${wo.operationName}'. Reason: ${reason ?: "N/A"}",
                    performedBy = pausedBy,
                    performedAt = now
                )
            )

            val fresh = executionRepository.getJobExecutionById(tenantId, executionJobId)!!
            DomainResult.Success(fresh)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to pause stage")
        }
    }

    override suspend fun resumeStage(
        tenantId: String,
        executionJobId: String,
        workOrderId: String,
        resumedBy: String
    ): DomainResult<ProductionJobExecution> = mutex.withLock {
        try {
            val job = executionRepository.getJobExecutionById(tenantId, executionJobId)
                ?: throw NoSuchElementException("Job '$executionJobId' not found.")

            val wo = job.workOrders.find { it.workOrderId == workOrderId }
                ?: throw NoSuchElementException("Work order '$workOrderId' not found.")

            val now = System.currentTimeMillis()
            val updatedWo = wo.copy(status = WorkOrderStatus.IN_PROGRESS, pausedAt = null)
            executionRepository.updateWorkOrder(updatedWo)

            executionRepository.saveExecutionEvent(
                ProductionExecutionEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    executionJobId = job.executionJobId,
                    workOrderId = workOrderId,
                    tenantId = tenantId,
                    eventType = ProductionExecutionEventType.STAGE_RESUMED,
                    fromStatus = wo.status.name,
                    toStatus = WorkOrderStatus.IN_PROGRESS.name,
                    payload = "Resumed stage '${wo.operationName}'.",
                    performedBy = resumedBy,
                    performedAt = now
                )
            )

            val fresh = executionRepository.getJobExecutionById(tenantId, executionJobId)!!
            DomainResult.Success(fresh)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to resume stage")
        }
    }

    override suspend fun completeStage(
        tenantId: String,
        executionJobId: String,
        workOrderId: String,
        goodQuantity: BigDecimal,
        scrapQuantity: BigDecimal,
        notes: String?,
        completedBy: String
    ): DomainResult<ProductionJobExecution> = mutex.withLock {
        try {
            val job = executionRepository.getJobExecutionById(tenantId, executionJobId)
                ?: throw NoSuchElementException("Job '$executionJobId' not found.")

            val wo = job.workOrders.find { it.workOrderId == workOrderId }
                ?: throw NoSuchElementException("Work order '$workOrderId' not found.")

            val now = System.currentTimeMillis()
            val durationSec = if (wo.startedAt != null) (now - wo.startedAt) / 1000 else 0L

            val updatedWo = wo.copy(
                status = WorkOrderStatus.COMPLETED,
                completedQuantity = goodQuantity.p4(),
                rejectedQuantity = scrapQuantity.p4(),
                completedAt = now,
                notes = notes ?: wo.notes
            )
            executionRepository.updateWorkOrder(updatedWo)

            // Record actuals
            executionRepository.saveActual(
                ProductionExecutionActual(
                    actualId = "ACT-${UUID.randomUUID()}",
                    executionJobId = executionJobId,
                    workOrderId = workOrderId,
                    tenantId = tenantId,
                    stageType = wo.stageType,
                    machineId = wo.assignedMachineId,
                    operatorId = wo.assignedOperatorId,
                    startedAt = wo.startedAt ?: now,
                    completedAt = now,
                    durationSeconds = durationSec,
                    goodQuantity = goodQuantity.p4(),
                    scrapQuantity = scrapQuantity.p4(),
                    reworkQuantity = BigDecimal.ZERO.p4(),
                    remarks = notes
                )
            )

            // Advance work orders
            val allWos = executionRepository.listWorkOrders(tenantId, executionJobId)
            val advancedWos = ProductionJobEngine.advanceWorkOrders(allWos)
            advancedWos.forEach { executionRepository.updateWorkOrder(it) }

            // Recalculate job overall quantities
            val isFinalStage = wo.stageType == ProductionStageType.PACKAGING || wo.stageType == ProductionStageType.FINAL_QC
            val newCompletedQty = if (isFinalStage) goodQuantity.p4() else job.completedQuantity
            val newRejectedQty = job.rejectedQuantity.add(scrapQuantity).p4()
            val newRemainingQty = job.plannedQuantity.subtract(newCompletedQty).subtract(newRejectedQty).subtract(job.wastageQuantity).coerceAtLeast(BigDecimal.ZERO).p4()

            val allCompleted = advancedWos.filter { it.isMandatory }.all { it.status == WorkOrderStatus.COMPLETED || it.status == WorkOrderStatus.SKIPPED }
            val nextJobStatus = if (allCompleted) ProductionJobExecutionStatus.COMPLETING else ProductionJobExecutionStatus.IN_PROGRESS

            val updatedJob = job.copy(
                status = nextJobStatus,
                workOrders = advancedWos,
                completedQuantity = newCompletedQty,
                rejectedQuantity = newRejectedQty,
                remainingQuantity = newRemainingQty,
                updatedAt = now,
                updatedBy = completedBy
            )
            val saved = executionRepository.saveJobExecution(updatedJob)

            executionRepository.saveExecutionEvent(
                ProductionExecutionEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    executionJobId = saved.executionJobId,
                    workOrderId = workOrderId,
                    tenantId = tenantId,
                    eventType = ProductionExecutionEventType.STAGE_COMPLETED,
                    fromStatus = wo.status.name,
                    toStatus = WorkOrderStatus.COMPLETED.name,
                    payload = "Completed stage '${wo.operationName}' with goodQty=${goodQuantity.p4()}, scrapQty=${scrapQuantity.p4()}.",
                    performedBy = completedBy,
                    performedAt = now
                )
            )

            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to complete stage")
        }
    }

    override suspend fun assignMachine(
        tenantId: String,
        executionJobId: String,
        workOrderId: String,
        machineId: String,
        machineName: String,
        assignedBy: String
    ): DomainResult<ProductionJobExecution> = mutex.withLock {
        try {
            val job = executionRepository.getJobExecutionById(tenantId, executionJobId)
                ?: throw NoSuchElementException("Job '$executionJobId' not found.")

            val wo = job.workOrders.find { it.workOrderId == workOrderId }
                ?: throw NoSuchElementException("Work order '$workOrderId' not found.")

            val updatedWo = wo.copy(
                assignedMachineId = machineId,
                assignedMachineName = machineName
            )
            executionRepository.updateWorkOrder(updatedWo)

            executionRepository.saveExecutionEvent(
                ProductionExecutionEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    executionJobId = job.executionJobId,
                    workOrderId = workOrderId,
                    tenantId = tenantId,
                    eventType = ProductionExecutionEventType.MACHINE_ASSIGNED,
                    payload = "Assigned machine '$machineName' ($machineId) to stage '${wo.operationName}'.",
                    performedBy = assignedBy,
                    performedAt = System.currentTimeMillis()
                )
            )

            val fresh = executionRepository.getJobExecutionById(tenantId, executionJobId)!!
            DomainResult.Success(fresh)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to assign machine")
        }
    }

    override suspend fun assignOperator(
        tenantId: String,
        executionJobId: String,
        workOrderId: String,
        operatorId: String,
        operatorName: String,
        assignedBy: String
    ): DomainResult<ProductionJobExecution> = mutex.withLock {
        try {
            val job = executionRepository.getJobExecutionById(tenantId, executionJobId)
                ?: throw NoSuchElementException("Job '$executionJobId' not found.")

            val wo = job.workOrders.find { it.workOrderId == workOrderId }
                ?: throw NoSuchElementException("Work order '$workOrderId' not found.")

            val updatedWo = wo.copy(
                assignedOperatorId = operatorId,
                assignedOperatorName = operatorName
            )
            executionRepository.updateWorkOrder(updatedWo)

            executionRepository.saveExecutionEvent(
                ProductionExecutionEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    executionJobId = job.executionJobId,
                    workOrderId = workOrderId,
                    tenantId = tenantId,
                    eventType = ProductionExecutionEventType.OPERATOR_ASSIGNED,
                    payload = "Assigned operator '$operatorName' ($operatorId) to stage '${wo.operationName}'.",
                    performedBy = assignedBy,
                    performedAt = System.currentTimeMillis()
                )
            )

            val fresh = executionRepository.getJobExecutionById(tenantId, executionJobId)!!
            DomainResult.Success(fresh)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to assign operator")
        }
    }

    override suspend fun holdJob(
        tenantId: String,
        executionJobId: String,
        workOrderId: String?,
        category: HoldCategory,
        reason: String,
        heldBy: String
    ): DomainResult<ProductionJobExecution> = mutex.withLock {
        try {
            val diag = ProductionExecutionValidator.validateHoldReason(reason)
            if (diag.any { it.isBlocking }) {
                throw IllegalArgumentException(diag.first().message)
            }

            val job = executionRepository.getJobExecutionById(tenantId, executionJobId)
                ?: throw NoSuchElementException("Job '$executionJobId' not found.")

            val now = System.currentTimeMillis()
            val hold = ProductionHold(
                holdId = "HOLD-${UUID.randomUUID()}",
                executionJobId = executionJobId,
                workOrderId = workOrderId,
                tenantId = tenantId,
                category = category,
                reason = reason,
                heldAt = now,
                heldBy = heldBy
            )
            executionRepository.saveHold(hold)

            val updatedJob = job.copy(
                status = ProductionJobExecutionStatus.ON_HOLD,
                currentHold = hold,
                updatedAt = now,
                updatedBy = heldBy
            )
            val saved = executionRepository.saveJobExecution(updatedJob)

            executionRepository.saveExecutionEvent(
                ProductionExecutionEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    executionJobId = saved.executionJobId,
                    workOrderId = workOrderId,
                    tenantId = tenantId,
                    eventType = ProductionExecutionEventType.STAGE_HELD,
                    fromStatus = job.status.name,
                    toStatus = ProductionJobExecutionStatus.ON_HOLD.name,
                    payload = "Job placed ON HOLD [Category: ${category.name}]. Reason: $reason",
                    performedBy = heldBy,
                    performedAt = now
                )
            )

            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to hold job")
        }
    }

    override suspend fun releaseHold(
        tenantId: String,
        executionJobId: String,
        resolutionNotes: String?,
        releasedBy: String
    ): DomainResult<ProductionJobExecution> = mutex.withLock {
        try {
            val job = executionRepository.getJobExecutionById(tenantId, executionJobId)
                ?: throw NoSuchElementException("Job '$executionJobId' not found.")

            val now = System.currentTimeMillis()
            val currentHold = job.currentHold
            if (currentHold != null) {
                val resolvedHold = currentHold.copy(
                    isResolved = true,
                    resolvedAt = now,
                    resolvedBy = releasedBy,
                    resolutionNotes = resolutionNotes
                )
                executionRepository.saveHold(resolvedHold)
            }

            val updatedJob = job.copy(
                status = ProductionJobExecutionStatus.IN_PROGRESS,
                currentHold = null,
                updatedAt = now,
                updatedBy = releasedBy
            )
            val saved = executionRepository.saveJobExecution(updatedJob)

            executionRepository.saveExecutionEvent(
                ProductionExecutionEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    executionJobId = saved.executionJobId,
                    tenantId = tenantId,
                    eventType = ProductionExecutionEventType.STAGE_HOLD_RELEASED,
                    fromStatus = ProductionJobExecutionStatus.ON_HOLD.name,
                    toStatus = ProductionJobExecutionStatus.IN_PROGRESS.name,
                    payload = "Released hold. Notes: ${resolutionNotes ?: "Resolved"}",
                    performedBy = releasedBy,
                    performedAt = now
                )
            )

            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to release hold")
        }
    }

    override suspend fun recordWastage(
        tenantId: String,
        executionJobId: String,
        workOrderId: String,
        materialCode: String,
        quantity: BigDecimal,
        unitOfMeasure: String,
        reason: String,
        recordedBy: String
    ): DomainResult<ProductionWastageRecord> = mutex.withLock {
        try {
            val job = executionRepository.getJobExecutionById(tenantId, executionJobId)
                ?: throw NoSuchElementException("Job '$executionJobId' not found.")

            val wo = job.workOrders.find { it.workOrderId == workOrderId }
                ?: throw NoSuchElementException("Work order '$workOrderId' not found.")

            val now = System.currentTimeMillis()
            val wastage = ProductionWastageRecord(
                wastageId = "WST-${UUID.randomUUID()}",
                executionJobId = executionJobId,
                workOrderId = workOrderId,
                tenantId = tenantId,
                materialCode = materialCode,
                quantity = quantity.p4(),
                unitOfMeasure = unitOfMeasure,
                reason = reason,
                stageType = wo.stageType,
                recordedBy = recordedBy,
                recordedAt = now
            )
            val saved = executionRepository.saveWastage(wastage)

            // Update job wastage total
            val newWastage = job.wastageQuantity.add(quantity).p4()
            val newRemaining = job.remainingQuantity.subtract(quantity).coerceAtLeast(BigDecimal.ZERO).p4()
            executionRepository.saveJobExecution(
                job.copy(
                    wastageQuantity = newWastage,
                    remainingQuantity = newRemaining,
                    updatedAt = now,
                    updatedBy = recordedBy
                )
            )

            executionRepository.saveExecutionEvent(
                ProductionExecutionEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    executionJobId = executionJobId,
                    workOrderId = workOrderId,
                    tenantId = tenantId,
                    eventType = ProductionExecutionEventType.WASTAGE_RECORDED,
                    payload = "Recorded wastage of ${quantity.p4()} $unitOfMeasure ($materialCode). Reason: $reason",
                    performedBy = recordedBy,
                    performedAt = now
                )
            )

            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to record wastage")
        }
    }

    override suspend fun createRework(
        tenantId: String,
        executionJobId: String,
        sourceWorkOrderId: String,
        targetWorkOrderId: String,
        quantity: BigDecimal,
        defectCode: String?,
        reason: String,
        requestedBy: String
    ): DomainResult<ProductionReworkRecord> = mutex.withLock {
        try {
            val job = executionRepository.getJobExecutionById(tenantId, executionJobId)
                ?: throw NoSuchElementException("Job '$executionJobId' not found.")

            val now = System.currentTimeMillis()
            val rework = ProductionReworkRecord(
                reworkId = "RWK-${UUID.randomUUID()}",
                executionJobId = executionJobId,
                sourceWorkOrderId = sourceWorkOrderId,
                targetWorkOrderId = targetWorkOrderId,
                tenantId = tenantId,
                quantity = quantity.p4(),
                defectCode = defectCode,
                reason = reason,
                status = "ACTIVE",
                requestedBy = requestedBy,
                requestedAt = now
            )
            val saved = executionRepository.saveRework(rework)

            // Reset target work order to IN_PROGRESS / REWORK_REQUIRED
            val targetWo = job.workOrders.find { it.workOrderId == targetWorkOrderId }
            if (targetWo != null) {
                executionRepository.updateWorkOrder(targetWo.copy(status = WorkOrderStatus.REWORK_REQUIRED))
            }

            executionRepository.saveJobExecution(
                job.copy(
                    status = ProductionJobExecutionStatus.REWORK_REQUIRED,
                    reworkQuantity = job.reworkQuantity.add(quantity).p4(),
                    updatedAt = now,
                    updatedBy = requestedBy
                )
            )

            executionRepository.saveExecutionEvent(
                ProductionExecutionEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    executionJobId = executionJobId,
                    workOrderId = targetWorkOrderId,
                    tenantId = tenantId,
                    eventType = ProductionExecutionEventType.REWORK_CREATED,
                    fromStatus = job.status.name,
                    toStatus = ProductionJobExecutionStatus.REWORK_REQUIRED.name,
                    payload = "Created rework for ${quantity.p4()} pcs from $sourceWorkOrderId -> $targetWorkOrderId. Reason: $reason",
                    performedBy = requestedBy,
                    performedAt = now
                )
            )

            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to create rework")
        }
    }

    override suspend fun requestQc(
        tenantId: String,
        executionJobId: String,
        workOrderId: String,
        requestedBy: String
    ): DomainResult<ProductionJobExecution> = mutex.withLock {
        try {
            val job = executionRepository.getJobExecutionById(tenantId, executionJobId)
                ?: throw NoSuchElementException("Job '$executionJobId' not found.")

            val wo = job.workOrders.find { it.workOrderId == workOrderId }
                ?: throw NoSuchElementException("Work order '$workOrderId' not found.")

            val updatedWo = wo.copy(status = WorkOrderStatus.QC_PENDING)
            executionRepository.updateWorkOrder(updatedWo)

            val updatedJob = job.copy(
                status = ProductionJobExecutionStatus.QC_PENDING,
                updatedAt = System.currentTimeMillis(),
                updatedBy = requestedBy
            )
            val saved = executionRepository.saveJobExecution(updatedJob)

            executionRepository.saveExecutionEvent(
                ProductionExecutionEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    executionJobId = executionJobId,
                    workOrderId = workOrderId,
                    tenantId = tenantId,
                    eventType = ProductionExecutionEventType.QC_REQUESTED,
                    fromStatus = wo.status.name,
                    toStatus = WorkOrderStatus.QC_PENDING.name,
                    payload = "QC Inspection requested for stage '${wo.operationName}'.",
                    performedBy = requestedBy,
                    performedAt = System.currentTimeMillis()
                )
            )

            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to request QC")
        }
    }

    override suspend fun completeJob(
        tenantId: String,
        executionJobId: String,
        summary: String?,
        completedBy: String
    ): DomainResult<ProductionJobExecution> = mutex.withLock {
        try {
            val job = executionRepository.getJobExecutionById(tenantId, executionJobId)
                ?: throw NoSuchElementException("Job '$executionJobId' not found.")

            // Verify work orders
            val incompleteMandatory = job.workOrders.filter { it.isMandatory && it.status != WorkOrderStatus.COMPLETED && it.status != WorkOrderStatus.SKIPPED }
            if (incompleteMandatory.isNotEmpty()) {
                throw IllegalStateException("Cannot complete job: mandatory work orders incomplete: ${incompleteMandatory.joinToString { it.operationName }}")
            }

            val now = System.currentTimeMillis()
            val updated = job.copy(
                status = ProductionJobExecutionStatus.COMPLETED,
                isCompleted = true,
                completedAt = now,
                completionSummary = summary ?: "Manufacturing completed successfully.",
                remainingQuantity = BigDecimal.ZERO.p4(),
                updatedAt = now,
                updatedBy = completedBy
            )
            val saved = executionRepository.saveJobExecution(updated)

            executionRepository.saveExecutionEvent(
                ProductionExecutionEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    executionJobId = saved.executionJobId,
                    tenantId = tenantId,
                    eventType = ProductionExecutionEventType.JOB_COMPLETED,
                    fromStatus = job.status.name,
                    toStatus = ProductionJobExecutionStatus.COMPLETED.name,
                    payload = "Production job marked COMPLETED. Summary: ${updated.completionSummary}",
                    performedBy = completedBy,
                    performedAt = now
                )
            )

            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to complete job")
        }
    }

    override suspend fun cancelJob(
        tenantId: String,
        executionJobId: String,
        reason: String,
        cancelledBy: String
    ): DomainResult<ProductionJobExecution> = mutex.withLock {
        try {
            require(reason.isNotBlank()) { "Cancellation reason is mandatory." }

            val job = executionRepository.getJobExecutionById(tenantId, executionJobId)
                ?: throw NoSuchElementException("Job '$executionJobId' not found.")

            val now = System.currentTimeMillis()
            val updated = job.copy(
                status = ProductionJobExecutionStatus.CANCELLED,
                completionSummary = "Cancelled: $reason",
                updatedAt = now,
                updatedBy = cancelledBy
            )
            val saved = executionRepository.saveJobExecution(updated)

            executionRepository.saveExecutionEvent(
                ProductionExecutionEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    executionJobId = saved.executionJobId,
                    tenantId = tenantId,
                    eventType = ProductionExecutionEventType.JOB_CANCELLED,
                    fromStatus = job.status.name,
                    toStatus = ProductionJobExecutionStatus.CANCELLED.name,
                    payload = "Job CANCELLED. Reason: $reason",
                    performedBy = cancelledBy,
                    performedAt = now
                )
            )

            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to cancel job")
        }
    }

    override suspend fun reconcileJob(
        tenantId: String,
        executionJobId: String
    ): DomainResult<ProductionExecutionReconciliationResult> {
        return try {
            val job = executionRepository.getJobExecutionById(tenantId, executionJobId)
                ?: throw NoSuchElementException("Job '$executionJobId' not found.")

            val order = resolveOrder(job.orderId)
            val planning = planningRepository.getPlanningSnapshotById(tenantId, job.planningId)
                ?: throw NoSuchElementException("Planning snapshot '${job.planningId}' not found.")

            val commitment = job.commercialCommitmentId?.let {
                when (val res = commitmentRepository.findCommitmentById(tenantId, it)) {
                    is DomainResult.Success -> res.data
                    else -> null
                }
            }

            val quote = job.quotationId?.let {
                when (val res = quoteRepository.findQuoteById(tenantId, it)) {
                    is DomainResult.Success -> res.data
                    else -> null
                }
            }

            val version = if (quote != null && job.quotationVersionNumber != null) {
                when (val res = quoteRepository.listVersionsByQuoteId(tenantId, quote.quoteId)) {
                    is DomainResult.Success -> res.data.find { it.versionNumber == job.quotationVersionNumber }
                    else -> null
                }
            } else null

            val result = ProductionExecutionReconciliationService.reconcile(
                job = job,
                order = order,
                commitment = commitment,
                quote = quote,
                version = version,
                planningSnapshot = planning
            )

            executionRepository.saveExecutionEvent(
                ProductionExecutionEvent(
                    eventId = "EVT-${UUID.randomUUID()}",
                    executionJobId = executionJobId,
                    tenantId = tenantId,
                    eventType = ProductionExecutionEventType.RECONCILIATION_PERFORMED,
                    payload = "7-way reconciliation completed. isFullyReconciled=${result.isFullyReconciled}, discrepancies=${result.discrepancies.size}",
                    performedBy = "system",
                    performedAt = System.currentTimeMillis()
                )
            )

            DomainResult.Success(result)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to reconcile job")
        }
    }

    override suspend fun exportHandoffContract(
        tenantId: String,
        executionJobId: String
    ): DomainResult<Module17Step05ProductionExecutionHandoffContract> {
        return try {
            val job = executionRepository.getJobExecutionById(tenantId, executionJobId)
                ?: throw NoSuchElementException("Job '$executionJobId' not found.")

            val reconRes = reconcileJob(tenantId, executionJobId)
            val isReconciled = reconRes is DomainResult.Success && reconRes.data.isFullyReconciled

            val contract = Module17Step05ProductionExecutionHandoffContract(
                contractVersion = "1.0.0",
                executionJobId = job.executionJobId,
                tenantId = job.tenantId,
                projectId = job.projectId,
                orderId = job.orderId,
                orderNumber = job.orderNumber,
                customerId = job.customerId,
                status = job.status.name,
                currentStage = job.currentStageType?.name,
                progressFraction = job.progressFraction,
                plannedQuantity = job.plannedQuantity,
                completedQuantity = job.completedQuantity,
                wastageQuantity = job.wastageQuantity,
                reworkQuantity = job.reworkQuantity,
                completedWorkOrdersCount = job.workOrders.count { it.status == WorkOrderStatus.COMPLETED || it.status == WorkOrderStatus.SKIPPED },
                totalWorkOrdersCount = job.workOrders.size,
                hasActiveHold = job.currentHold != null,
                holdReason = job.currentHold?.reason,
                isFullyReconciled = isReconciled,
                isCompleted = job.isCompleted,
                integrityHash = job.integrityHash,
                generatedAt = System.currentTimeMillis()
            )

            DomainResult.Success(contract)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to export handoff contract")
        }
    }

    override suspend fun listExecutionEvents(
        tenantId: String,
        executionJobId: String
    ): DomainResult<List<ProductionExecutionEvent>> {
        return try {
            val list = executionRepository.listExecutionEvents(tenantId, executionJobId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to list execution events")
        }
    }
}

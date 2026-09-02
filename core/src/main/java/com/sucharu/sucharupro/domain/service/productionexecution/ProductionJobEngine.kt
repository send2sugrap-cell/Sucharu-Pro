package com.sucharu.sucharupro.domain.service.productionexecution

import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.productionexecution.*
import com.sucharu.sucharupro.domain.model.productionplanning.ProductionPlanningSnapshot
import java.math.BigDecimal
import java.util.UUID

object ProductionJobEngine {

    /**
     * Creates a new [ProductionJobExecution] and derives its sequential [ProductionWorkOrder]s from [ProductionPlanningSnapshot].
     */
    fun createJobExecution(
        order: Order,
        planningSnapshot: ProductionPlanningSnapshot,
        createdBy: String
    ): ProductionJobExecution {
        val now = System.currentTimeMillis()
        val executionJobId = "JOB-${order.orderId}-${planningSnapshot.orderItemId}-V${planningSnapshot.version}"
        val plannedQty = BigDecimal.valueOf(planningSnapshot.specification.plannedQuantity).p4()

        val workOrders = deriveWorkOrders(executionJobId, planningSnapshot)

        val fingerprint = ProductionExecutionMathUtils.generateFingerprint(
            tenantId = planningSnapshot.tenantId,
            orderId = order.orderId,
            orderItemId = planningSnapshot.orderItemId,
            planningId = planningSnapshot.planningId,
            plannedQuantity = plannedQty,
            status = ProductionJobExecutionStatus.READY.name
        )

        val integrityHash = ProductionExecutionMathUtils.sha256(
            "$executionJobId|${planningSnapshot.tenantId}|${order.orderId}|$plannedQty|$fingerprint|$now"
        )

        return ProductionJobExecution(
            executionJobId = executionJobId,
            tenantId = planningSnapshot.tenantId,
            projectId = planningSnapshot.projectId,
            orderId = order.orderId,
            orderNumber = order.orderNumber,
            orderItemId = planningSnapshot.orderItemId,
            customerId = order.customerId,
            quotationId = planningSnapshot.quotationId,
            quotationVersionNumber = planningSnapshot.quotationVersionNumber,
            commercialCommitmentId = planningSnapshot.commercialCommitmentId,
            planningId = planningSnapshot.planningId,
            planningVersion = planningSnapshot.version,
            title = planningSnapshot.specification.jobTitle,
            priority = OrderPriority.NORMAL,
            status = ProductionJobExecutionStatus.READY,
            specification = planningSnapshot.specification,
            plannedQuantity = plannedQty,
            startedQuantity = BigDecimal.ZERO.p4(),
            completedQuantity = BigDecimal.ZERO.p4(),
            rejectedQuantity = BigDecimal.ZERO.p4(),
            wastageQuantity = BigDecimal.ZERO.p4(),
            reworkQuantity = BigDecimal.ZERO.p4(),
            remainingQuantity = plannedQty,
            workOrders = workOrders,
            currentHold = null,
            currentStageType = workOrders.firstOrNull()?.stageType,
            isCompleted = false,
            completedAt = null,
            completionSummary = null,
            jobFingerprint = fingerprint,
            integrityHash = integrityHash,
            version = 1,
            createdAt = now,
            createdBy = createdBy,
            updatedAt = now,
            updatedBy = createdBy
        )
    }

    /**
     * Derives sequential [ProductionWorkOrder]s from planning operations with predecessor linkages.
     */
    fun deriveWorkOrders(
        executionJobId: String,
        planningSnapshot: ProductionPlanningSnapshot
    ): List<ProductionWorkOrder> {
        val workOrders = mutableListOf<ProductionWorkOrder>()
        val plannedQty = BigDecimal.valueOf(planningSnapshot.specification.plannedQuantity).p4()

        var previousWoId: String? = null

        planningSnapshot.operations.sortedBy { it.sequenceNumber }.forEach { op ->
            val woId = "WO-${executionJobId}-${op.sequenceNumber}"
            val predecessors = if (previousWoId != null) listOf(previousWoId!!) else emptyList()

            val initialStatus = if (workOrders.isEmpty()) WorkOrderStatus.READY else WorkOrderStatus.PENDING

            val wo = ProductionWorkOrder(
                workOrderId = woId,
                executionJobId = executionJobId,
                tenantId = planningSnapshot.tenantId,
                sequenceNumber = op.sequenceNumber,
                stageType = op.stageType,
                operationCode = op.operationCode,
                operationName = op.operationName,
                targetWorkCenter = op.targetWorkCenter,
                status = initialStatus,
                assignedMachineId = null,
                assignedMachineName = null,
                assignedOperatorId = null,
                assignedOperatorName = null,
                estimatedSetupMinutes = op.estimatedSetupMinutes,
                estimatedRunMinutes = op.estimatedRunMinutes,
                actualSetupMinutes = 0,
                actualRunMinutes = 0,
                plannedQuantity = plannedQty,
                completedQuantity = BigDecimal.ZERO.p4(),
                rejectedQuantity = BigDecimal.ZERO.p4(),
                wastageQuantity = BigDecimal.ZERO.p4(),
                isMandatory = op.isMandatory,
                isQcCheckpoint = op.isQcCheckpoint,
                predecessorWorkOrderIds = predecessors,
                startedAt = null,
                pausedAt = null,
                completedAt = null,
                notes = null
            )
            workOrders.add(wo)
            previousWoId = woId
        }

        return workOrders
    }

    /**
     * Advances work order states after a stage completes: sets next work order to READY.
     */
    fun advanceWorkOrders(workOrders: List<ProductionWorkOrder>): List<ProductionWorkOrder> {
        val updated = workOrders.toMutableList()
        for (i in updated.indices) {
            val current = updated[i]
            if (current.status == WorkOrderStatus.PENDING) {
                val predecessorsDone = current.predecessorWorkOrderIds.all { predId ->
                    updated.any { it.workOrderId == predId && (it.status == WorkOrderStatus.COMPLETED || it.status == WorkOrderStatus.SKIPPED) }
                }
                if (predecessorsDone) {
                    updated[i] = current.copy(status = WorkOrderStatus.READY)
                }
            }
        }
        return updated
    }
}

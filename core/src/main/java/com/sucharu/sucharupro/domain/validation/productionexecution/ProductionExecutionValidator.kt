package com.sucharu.sucharupro.domain.validation.productionexecution

import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.productionexecution.*
import com.sucharu.sucharupro.domain.model.productionplanning.PlanningStatus
import com.sucharu.sucharupro.domain.model.productionplanning.ProductionPlanningSnapshot
import java.math.BigDecimal

object ProductionExecutionValidator {

    /**
     * Validates if a [ProductionPlanningSnapshot] and its [Order] are eligible to create a [ProductionJobExecution].
     */
    fun validateJobCreationEligibility(
        order: Order,
        planningSnapshot: ProductionPlanningSnapshot
    ): List<ProductionExecutionDiagnostic> {
        val diagnostics = mutableListOf<ProductionExecutionDiagnostic>()

        // 1. Order Status Check
        if (order.status == OrderStatusType.CANCELLED) {
            diagnostics.add(
                ProductionExecutionDiagnostic(
                    code = "ORDER_CANCELLED",
                    message = "Order '${order.orderNumber}' is CANCELLED. Cannot create production job.",
                    isBlocking = true,
                    recommendedAction = "Verify order state with sales administration."
                )
            )
        }

        // 2. Planning Status Check
        if (planningSnapshot.status != PlanningStatus.READY && planningSnapshot.status != PlanningStatus.HANDED_OFF) {
            diagnostics.add(
                ProductionExecutionDiagnostic(
                    code = "PLANNING_NOT_READY",
                    message = "Planning snapshot '${planningSnapshot.planningId}' is in state '${planningSnapshot.status}'. Must be READY or HANDED_OFF.",
                    isBlocking = true,
                    recommendedAction = "Resolve planning readiness before releasing to production."
                )
            )
        }

        // 3. Readiness Score Check
        if (planningSnapshot.readinessScore < BigDecimal("80.0000")) {
            diagnostics.add(
                ProductionExecutionDiagnostic(
                    code = "INSUFFICIENT_READINESS_SCORE",
                    message = "Manufacturing readiness score is ${planningSnapshot.readinessScore}% (minimum required: 80.0000%).",
                    isBlocking = true,
                    recommendedAction = "Complete missing specifications or commercial commitments in Step 04."
                )
            )
        }

        // 4. Critical Blockers Check
        val blockers = planningSnapshot.diagnostics.filter { it.isBlocking }
        if (blockers.isNotEmpty()) {
            diagnostics.add(
                ProductionExecutionDiagnostic(
                    code = "ACTIVE_PLANNING_BLOCKERS",
                    message = "Planning snapshot contains ${blockers.size} unresolved critical blockers: ${blockers.joinToString { it.code }}",
                    isBlocking = true,
                    recommendedAction = "Resolve all critical blockers before creating production job."
                )
            )
        }

        // 5. Quantity Check
        if (planningSnapshot.specification.plannedQuantity <= 0) {
            diagnostics.add(
                ProductionExecutionDiagnostic(
                    code = "INVALID_PLANNED_QUANTITY",
                    message = "Planned quantity is ${planningSnapshot.specification.plannedQuantity} (must be > 0).",
                    isBlocking = true,
                    recommendedAction = "Verify quantity specification in production plan."
                )
            )
        }

        return diagnostics
    }

    /**
     * Validates if a work order can start based on its predecessor dependencies.
     */
    fun validateWorkOrderStart(
        workOrder: ProductionWorkOrder,
        allWorkOrders: List<ProductionWorkOrder>
    ): List<ProductionExecutionDiagnostic> {
        val diagnostics = mutableListOf<ProductionExecutionDiagnostic>()

        val incompletePredecessors = allWorkOrders.filter {
            it.workOrderId in workOrder.predecessorWorkOrderIds &&
            it.status != WorkOrderStatus.COMPLETED &&
            it.status != WorkOrderStatus.SKIPPED
        }

        if (incompletePredecessors.isNotEmpty()) {
            diagnostics.add(
                ProductionExecutionDiagnostic(
                    code = "PREDECESSORS_INCOMPLETE",
                    message = "Work order '${workOrder.operationName}' cannot start because predecessor stages are not complete: ${incompletePredecessors.joinToString { it.operationName }}",
                    isBlocking = true,
                    stageType = workOrder.stageType,
                    recommendedAction = "Complete prerequisite operations first."
                )
            )
        }

        return diagnostics
    }

    /**
     * Validates hold placement reason.
     */
    fun validateHoldReason(reason: String): List<ProductionExecutionDiagnostic> {
        val diagnostics = mutableListOf<ProductionExecutionDiagnostic>()
        if (reason.isBlank() || reason.length < 5) {
            diagnostics.add(
                ProductionExecutionDiagnostic(
                    code = "INVALID_HOLD_REASON",
                    message = "A detailed hold reason (minimum 5 characters) is mandatory.",
                    isBlocking = true,
                    recommendedAction = "Provide an explanatory reason for placing production on hold."
                )
            )
        }
        return diagnostics
    }
}

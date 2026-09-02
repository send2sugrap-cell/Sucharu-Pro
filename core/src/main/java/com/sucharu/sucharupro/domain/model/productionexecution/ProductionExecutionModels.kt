package com.sucharu.sucharupro.domain.model.productionexecution

import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.productionplanning.ProductionJobSpecification
import java.math.BigDecimal

// ============================================================
// ENUMS (Module 17 Step 05)
// ============================================================

enum class ProductionJobExecutionStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    READY("Ready for Release"),
    RELEASED("Released to Floor"),
    SCHEDULED("Scheduled"),
    IN_PROGRESS("In Progress"),
    ON_HOLD("On Hold"),
    QC_PENDING("QC Pending"),
    REWORK_REQUIRED("Rework Required"),
    COMPLETING("Completing"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    BLOCKED("Blocked");

    val isTerminal: Boolean get() = this == COMPLETED || this == CANCELLED
    val isActionable: Boolean get() = !isTerminal && this != BLOCKED

    fun canTransitionTo(target: ProductionJobExecutionStatus): Boolean {
        if (this == target) return false
        if (this.isTerminal) return false

        return when (this) {
            DRAFT -> target == READY || target == CANCELLED || target == BLOCKED
            READY -> target == RELEASED || target == SCHEDULED || target == ON_HOLD || target == CANCELLED || target == BLOCKED
            RELEASED -> target == SCHEDULED || target == IN_PROGRESS || target == ON_HOLD || target == CANCELLED
            SCHEDULED -> target == IN_PROGRESS || target == ON_HOLD || target == CANCELLED
            IN_PROGRESS -> target == ON_HOLD || target == QC_PENDING || target == COMPLETING || target == COMPLETED || target == CANCELLED
            ON_HOLD -> target == IN_PROGRESS || target == RELEASED || target == SCHEDULED || target == CANCELLED
            QC_PENDING -> target == IN_PROGRESS || target == REWORK_REQUIRED || target == COMPLETING || target == COMPLETED || target == ON_HOLD
            REWORK_REQUIRED -> target == IN_PROGRESS || target == ON_HOLD || target == CANCELLED
            COMPLETING -> target == COMPLETED || target == IN_PROGRESS || target == ON_HOLD
            COMPLETED -> false
            CANCELLED -> false
            BLOCKED -> target == DRAFT || target == READY || target == CANCELLED
        }
    }
}

enum class WorkOrderStatus(val defaultLabel: String) {
    PENDING("Pending"),
    READY("Ready"),
    IN_PROGRESS("In Progress"),
    PAUSED("Paused"),
    QC_PENDING("QC Pending"),
    REWORK_REQUIRED("Rework Required"),
    COMPLETED("Completed"),
    SKIPPED("Skipped"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean get() = this == COMPLETED || this == SKIPPED || this == CANCELLED
}

enum class HoldCategory {
    MATERIAL_SHORTAGE,
    MACHINE_BREAKDOWN,
    CUSTOMER_CHANGE,
    QC_FAILURE,
    OPERATOR_UNAVAILABLE,
    TECHNICAL_SPEC_CLARIFICATION,
    OTHER
}

enum class ProductionExecutionEventType {
    JOB_CREATED,
    JOB_RELEASED,
    JOB_SCHEDULED,
    WORK_ORDER_CREATED,
    MACHINE_ASSIGNED,
    OPERATOR_ASSIGNED,
    STAGE_STARTED,
    STAGE_PAUSED,
    STAGE_RESUMED,
    STAGE_HELD,
    STAGE_HOLD_RELEASED,
    STAGE_COMPLETED,
    STAGE_SKIPPED,
    WASTAGE_RECORDED,
    REWORK_CREATED,
    QC_REQUESTED,
    QC_RESULT_RECEIVED,
    JOB_COMPLETED,
    JOB_BLOCKED,
    JOB_CANCELLED,
    RECONCILIATION_PERFORMED
}

// ============================================================
// DOMAIN ENTITIES
// ============================================================

/**
 * Authoritative shop-floor production job execution instance.
 */
data class ProductionJobExecution(
    val executionJobId: String,
    val tenantId: String,
    val projectId: String,
    val orderId: String,
    val orderNumber: String,
    val orderItemId: String,
    val customerId: String,
    val quotationId: String?,
    val quotationVersionNumber: Int?,
    val commercialCommitmentId: String?,
    val planningId: String,
    val planningVersion: Int,
    val title: String,
    val priority: OrderPriority = OrderPriority.NORMAL,
    val status: ProductionJobExecutionStatus = ProductionJobExecutionStatus.READY,
    val specification: ProductionJobSpecification,
    val plannedQuantity: BigDecimal,
    val startedQuantity: BigDecimal = BigDecimal.ZERO,
    val completedQuantity: BigDecimal = BigDecimal.ZERO,
    val rejectedQuantity: BigDecimal = BigDecimal.ZERO,
    val wastageQuantity: BigDecimal = BigDecimal.ZERO,
    val reworkQuantity: BigDecimal = BigDecimal.ZERO,
    val remainingQuantity: BigDecimal = plannedQuantity,
    val workOrders: List<ProductionWorkOrder> = emptyList(),
    val currentHold: ProductionHold? = null,
    val currentStageType: ProductionStageType? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val completionSummary: String? = null,
    val jobFingerprint: String,
    val integrityHash: String,
    val version: Int = 1,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String? = null
) {
    val progressFraction: Float
        get() {
            if (workOrders.isEmpty()) return 0f
            val completed = workOrders.count { it.status == WorkOrderStatus.COMPLETED || it.status == WorkOrderStatus.SKIPPED }
            return (completed.toFloat() / workOrders.size.toFloat()).coerceIn(0f, 1f)
        }
}

/**
 * Discrete shop-floor work order for a single routing stage.
 */
data class ProductionWorkOrder(
    val workOrderId: String,
    val executionJobId: String,
    val tenantId: String,
    val sequenceNumber: Int,
    val stageType: ProductionStageType,
    val operationCode: String,
    val operationName: String,
    val targetWorkCenter: String,
    val status: WorkOrderStatus = WorkOrderStatus.PENDING,
    val assignedMachineId: String? = null,
    val assignedMachineName: String? = null,
    val assignedOperatorId: String? = null,
    val assignedOperatorName: String? = null,
    val estimatedSetupMinutes: Int = 0,
    val estimatedRunMinutes: Int = 0,
    val actualSetupMinutes: Int = 0,
    val actualRunMinutes: Int = 0,
    val plannedQuantity: BigDecimal,
    val completedQuantity: BigDecimal = BigDecimal.ZERO,
    val rejectedQuantity: BigDecimal = BigDecimal.ZERO,
    val wastageQuantity: BigDecimal = BigDecimal.ZERO,
    val isMandatory: Boolean = true,
    val isQcCheckpoint: Boolean = false,
    val predecessorWorkOrderIds: List<String> = emptyList(),
    val startedAt: Long? = null,
    val pausedAt: Long? = null,
    val completedAt: Long? = null,
    val notes: String? = null
)

/**
 * Detailed execution actuals log.
 */
data class ProductionExecutionActual(
    val actualId: String,
    val executionJobId: String,
    val workOrderId: String,
    val tenantId: String,
    val stageType: ProductionStageType,
    val machineId: String?,
    val operatorId: String?,
    val startedAt: Long,
    val completedAt: Long?,
    val durationSeconds: Long?,
    val goodQuantity: BigDecimal,
    val scrapQuantity: BigDecimal,
    val reworkQuantity: BigDecimal,
    val remarks: String?
)

/**
 * Structured Hold record for paused/blocked executions.
 */
data class ProductionHold(
    val holdId: String,
    val executionJobId: String,
    val workOrderId: String?,
    val tenantId: String,
    val category: HoldCategory,
    val reason: String,
    val heldAt: Long,
    val heldBy: String,
    val isResolved: Boolean = false,
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null,
    val resolutionNotes: String? = null
)

/**
 * Structured Wastage record.
 */
data class ProductionWastageRecord(
    val wastageId: String,
    val executionJobId: String,
    val workOrderId: String,
    val tenantId: String,
    val materialCode: String,
    val quantity: BigDecimal,
    val unitOfMeasure: String,
    val reason: String,
    val stageType: ProductionStageType,
    val recordedBy: String,
    val recordedAt: Long
)

/**
 * Structured Rework record.
 */
data class ProductionReworkRecord(
    val reworkId: String,
    val executionJobId: String,
    val sourceWorkOrderId: String,
    val targetWorkOrderId: String,
    val tenantId: String,
    val quantity: BigDecimal,
    val defectCode: String?,
    val reason: String,
    val status: String = "ACTIVE",
    val requestedBy: String,
    val requestedAt: Long,
    val resolvedAt: Long? = null
)

/**
 * Structured Actionable Diagnostic.
 */
data class ProductionExecutionDiagnostic(
    val code: String,
    val message: String,
    val isBlocking: Boolean,
    val stageType: ProductionStageType? = null,
    val recommendedAction: String? = null
)

/**
 * 7-Way Multi-Tier Reconciliation Result.
 */
data class ProductionExecutionReconciliationResult(
    val executionJobId: String,
    val orderId: String,
    val isFullyReconciled: Boolean,
    val quotationMatch: Boolean,
    val commitmentMatch: Boolean,
    val orderMatch: Boolean,
    val planningMatch: Boolean,
    val workOrdersComplete: Boolean,
    val quantityBalanced: Boolean,
    val qcCheckpointsPassed: Boolean,
    val discrepancies: List<String> = emptyList(),
    val reconciledAt: Long
)

/**
 * Read-only AI Agent Handoff Contract for Module 17 Step 05.
 */
data class Module17Step05ProductionExecutionHandoffContract(
    val contractVersion: String = "1.0.0",
    val executionJobId: String,
    val tenantId: String,
    val projectId: String,
    val orderId: String,
    val orderNumber: String,
    val customerId: String,
    val status: String,
    val currentStage: String?,
    val progressFraction: Float,
    val plannedQuantity: BigDecimal,
    val completedQuantity: BigDecimal,
    val wastageQuantity: BigDecimal,
    val reworkQuantity: BigDecimal,
    val completedWorkOrdersCount: Int,
    val totalWorkOrdersCount: Int,
    val hasActiveHold: Boolean,
    val holdReason: String?,
    val isFullyReconciled: Boolean,
    val isCompleted: Boolean,
    val integrityHash: String,
    val generatedAt: Long
)

/**
 * Append-only lifecycle event.
 */
data class ProductionExecutionEvent(
    val eventId: String,
    val executionJobId: String,
    val workOrderId: String? = null,
    val tenantId: String,
    val eventType: ProductionExecutionEventType,
    val fromStatus: String? = null,
    val toStatus: String? = null,
    val payload: String? = null,
    val performedBy: String,
    val performedAt: Long
)

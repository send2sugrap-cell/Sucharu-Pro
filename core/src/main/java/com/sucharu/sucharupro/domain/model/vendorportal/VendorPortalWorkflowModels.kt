package com.sucharu.sucharupro.domain.model.vendorportal

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Lifecycle stages for End-to-End Vendor Workflow Orchestration (Module 13 Step 11).
 */
enum class VendorWorkflowStage {
    RFQ_RECEIVED,
    QUOTATION_DRAFTED,
    QUOTATION_SUBMITTED,
    QUOTATION_EVALUATED,
    AWARDED,
    PO_ACKNOWLEDGED,
    WORK_ORDER_ACKNOWLEDGED,
    PRODUCTION_IN_PROGRESS,
    READY_FOR_DISPATCH,
    DELIVERY_NOTICE_SUBMITTED,
    RECEIVED,
    QUALITY_INSPECTION,
    ACCEPTED,
    REJECTED,
    CAPA_REQUIRED,
    CAPA_RESPONDED,
    INVOICED,
    MATCHED,
    PAYMENT_PROCESSING,
    PAID,
    SETTLEMENT,
    RECONCILED,
    PERFORMANCE_EVALUATED,
    COMPLIANCE_VERIFIED,
    COMPLETED,
    TERMINATED,
    CANCELLED
}

/**
 * Overall status of a workflow instance.
 */
enum class VendorWorkflowStatus {
    ACTIVE,
    BLOCKED,
    PENDING_ACTION,
    EXCEPTION,
    COMPLETED,
    CANCELLED
}

/**
 * SLA classification status for milestone due-dates.
 */
enum class VendorWorkflowSlaStatus {
    ON_TRACK,
    DUE_SOON,
    OVERDUE,
    BLOCKED,
    COMPLETED,
    NOT_APPLICABLE
}

/**
 * Operational exception status.
 */
enum class VendorWorkflowExceptionStatus {
    OPEN,
    ACKNOWLEDGED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    ESCALATED
}

/**
 * Action types executable by authorized vendor users or ERP operators.
 */
enum class VendorWorkflowActionType {
    SUBMIT_QUOTATION,
    ACKNOWLEDGE_PO,
    ACKNOWLEDGE_WO,
    UPDATE_PRODUCTION_PROGRESS,
    SUBMIT_ASN,
    RESPOND_REJECTION,
    SUBMIT_CAPA,
    SUBMIT_INVOICE,
    RESOLVE_DISPUTE,
    REVIEW_SETTLEMENT,
    UPLOAD_COMPLIANCE,
    RESPOND_EVALUATION
}

/**
 * Priority levels for actions and exceptions.
 */
enum class VendorWorkflowPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT,
    CRITICAL
}

/**
 * Master aggregate representing an End-to-End Vendor Commercial Workflow Instance.
 */
data class VendorWorkflowItem(
    val workflowId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val correlationId: String,
    val workflowTitle: String,
    val currentStage: VendorWorkflowStage,
    val status: VendorWorkflowStatus = VendorWorkflowStatus.ACTIVE,
    val slaStatus: VendorWorkflowSlaStatus = VendorWorkflowSlaStatus.ON_TRACK,
    val rfqId: String? = null,
    val quotationId: String? = null,
    val purchaseOrderId: String? = null,
    val workOrderId: String? = null,
    val deliveryNoticeId: String? = null,
    val invoiceId: String? = null,
    val qualityCaseId: String? = null,
    val settlementId: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val targetDeliveryAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Immutable timeline event tracking cross-module lifecycle progression.
 */
data class VendorWorkflowTimelineEvent(
    val eventId: String,
    val workflowId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val correlationId: String,
    val causationId: String? = null,
    val stage: VendorWorkflowStage,
    val eventType: String,
    val title: String,
    val description: String? = null,
    val sourceModule: String,
    val actorId: String,
    val actorType: String = "VENDOR",
    val occurredAt: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Exception / Blocker record captured during workflow execution.
 */
data class VendorWorkflowException(
    val exceptionId: String,
    val workflowId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val category: String,
    val severity: VendorWorkflowPriority = VendorWorkflowPriority.MEDIUM,
    val status: VendorWorkflowExceptionStatus = VendorWorkflowExceptionStatus.OPEN,
    val title: String,
    val description: String,
    val detectedAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null,
    val resolutionNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

/**
 * Deterministic Next-Step / Action recommendation for a workflow.
 */
data class VendorWorkflowNextAction(
    val actionId: String,
    val workflowId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val actionType: VendorWorkflowActionType,
    val title: String,
    val description: String,
    val requiredRole: String,
    val priority: VendorWorkflowPriority = VendorWorkflowPriority.MEDIUM,
    val dueAt: Long? = null,
    val deepLinkTarget: String? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val completedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * SLA / Due-Date Milestone Projection for a workflow.
 */
data class VendorWorkflowSlaProjection(
    val workflowId: String,
    val milestoneTitle: String,
    val deadline: Long,
    val slaStatus: VendorWorkflowSlaStatus,
    val timeRemainingMs: Long,
    val isBreached: Boolean
) {
    companion object {
        fun calculate(
            workflowId: String,
            milestoneTitle: String,
            deadline: Long,
            now: Long = System.currentTimeMillis()
        ): VendorWorkflowSlaProjection {
            val remaining = deadline - now
            val isBreached = remaining < 0
            val status = when {
                isBreached -> VendorWorkflowSlaStatus.OVERDUE
                remaining <= 86400000L * 2 -> VendorWorkflowSlaStatus.DUE_SOON // 48 hours
                else -> VendorWorkflowSlaStatus.ON_TRACK
            }
            return VendorWorkflowSlaProjection(
                workflowId = workflowId,
                milestoneTitle = milestoneTitle,
                deadline = deadline,
                slaStatus = status,
                timeRemainingMs = remaining,
                isBreached = isBreached
            )
        }
    }
}

/**
 * Aggregated summary for the Vendor Portal Workflow Command Center.
 */
data class VendorWorkflowHubSummary(
    val vendorId: String,
    val tenantId: String,
    val projectId: String,
    val totalActiveWorkflows: Int,
    val completedWorkflows: Int,
    val blockedWorkflows: Int,
    val overdueWorkflows: Int,
    val averageCycleTimeDays: Double,
    val stageBreakdown: Map<String, Int>,
    val recentWorkflows: List<VendorWorkflowItem>,
    val urgentActions: List<VendorWorkflowNextAction>
)

/**
 * Audit log entry for workflow orchestration changes.
 */
data class VendorWorkflowAuditEntry(
    val auditId: String,
    val workflowId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val correlationId: String? = null,
    val reason: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
)

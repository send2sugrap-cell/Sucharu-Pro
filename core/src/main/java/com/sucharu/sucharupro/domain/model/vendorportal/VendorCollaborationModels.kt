package com.sucharu.sucharupro.domain.model.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import java.math.BigDecimal

/**
 * PO acknowledgement outcome options from the vendor portal.
 */
enum class VendorPoAcknowledgementType {
    ACKNOWLEDGED,
    ACKNOWLEDGED_WITH_EXCEPTION,
    DECLINED
}

/**
 * Work Order acknowledgement outcome options from the vendor portal.
 */
enum class VendorWoAcknowledgementType {
    ACKNOWLEDGED,
    ACKNOWLEDGED_WITH_EXCEPTION,
    DECLINED
}

/**
 * Categorization of execution blockers reported by vendors.
 */
enum class VendorBlockerCategory {
    MATERIAL_UNAVAILABLE,
    SPECIFICATION_UNCLEAR,
    CAPACITY_ISSUE,
    MACHINE_ISSUE,
    DELIVERY_DEPENDENCY,
    APPROVAL_PENDING,
    QUALITY_CONCERN,
    TECHNICAL_CLARIFICATION,
    OTHER
}

/**
 * Severity level of an execution blocker.
 */
enum class VendorBlockerSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * State machine for vendor execution blockers.
 */
enum class VendorBlockerStatus {
    OPEN,
    ACKNOWLEDGED,
    IN_PROGRESS,
    RESOLVED,
    CANCELLED;

    val isActive: Boolean get() = this in setOf(OPEN, ACKNOWLEDGED, IN_PROGRESS)
    val isTerminal: Boolean get() = this in setOf(RESOLVED, CANCELLED)

    fun canTransitionTo(target: VendorBlockerStatus): Boolean {
        if (this == target) return true
        return when (this) {
            OPEN -> target in setOf(ACKNOWLEDGED, IN_PROGRESS, RESOLVED, CANCELLED)
            ACKNOWLEDGED -> target in setOf(IN_PROGRESS, RESOLVED, CANCELLED)
            IN_PROGRESS -> target in setOf(RESOLVED, CANCELLED)
            RESOLVED -> false // Terminal
            CANCELLED -> false // Terminal
        }
    }
}

/**
 * Target resource type for collaboration threads.
 */
enum class VendorThreadResourceType {
    PURCHASE_ORDER,
    PURCHASE_ORDER_ITEM,
    WORK_ORDER,
    WORK_ORDER_TASK,
    BLOCKER,
    CLARIFICATION
}

/**
 * Visibility boundary for collaboration messages.
 */
enum class VendorMessageVisibility {
    VENDOR_VISIBLE,
    INTERNAL_ONLY,
    SYSTEM_EVENT
}

/**
 * State machine for vendor completion requests.
 */
enum class VendorCompletionStatus {
    NOT_REQUESTED,
    REQUESTED,
    UNDER_REVIEW,
    APPROVED,
    RETURNED_FOR_CORRECTION;

    val isPending: Boolean get() = this in setOf(REQUESTED, UNDER_REVIEW)

    fun canTransitionTo(target: VendorCompletionStatus): Boolean {
        if (this == target) return true
        return when (this) {
            NOT_REQUESTED -> target == REQUESTED
            REQUESTED -> target in setOf(UNDER_REVIEW, APPROVED, RETURNED_FOR_CORRECTION)
            UNDER_REVIEW -> target in setOf(APPROVED, RETURNED_FOR_CORRECTION)
            APPROVED -> false // Terminal
            RETURNED_FOR_CORRECTION -> target == REQUESTED
        }
    }
}

/**
 * Audit event types for vendor PO, WO, and job collaboration.
 */
enum class VendorCollaborationAuditEventType {
    PO_VIEWED,
    PO_ACKNOWLEDGED,
    PO_ACKNOWLEDGED_WITH_EXCEPTION,
    PO_DECLINED,
    WO_VIEWED,
    WO_ACKNOWLEDGED,
    WO_ACKNOWLEDGED_WITH_EXCEPTION,
    WO_DECLINED,
    PROGRESS_SUBMITTED,
    BLOCKER_CREATED,
    BLOCKER_ACKNOWLEDGED,
    BLOCKER_RESOLVED,
    BLOCKER_CANCELLED,
    CLARIFICATION_CREATED,
    CLARIFICATION_ANSWERED,
    COLLABORATION_THREAD_CREATED,
    COLLABORATION_MESSAGE_CREATED,
    EVIDENCE_REGISTERED,
    COMPLETION_REQUESTED,
    COMPLETION_APPROVED,
    COMPLETION_RETURNED
}

/**
 * Model representing a vendor's formal acknowledgement of a Purchase Order.
 */
data class VendorPoAcknowledgement(
    val acknowledgementId: String,
    val purchaseOrderId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val actorId: String,
    val acknowledgementType: VendorPoAcknowledgementType,
    val exceptionDetails: String? = null,
    val declineReason: String? = null,
    val promisedDeliveryDate: Long? = null,
    val comment: String? = null,
    val acknowledgedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

/**
 * Model representing a vendor's formal acknowledgement of a Work Order.
 */
data class VendorWoAcknowledgement(
    val acknowledgementId: String,
    val workOrderId: String,
    val purchaseOrderId: String? = null,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val actorId: String,
    val acknowledgementType: VendorWoAcknowledgementType,
    val exceptionDetails: String? = null,
    val declineReason: String? = null,
    val promisedStartDate: Long? = null,
    val promisedCompletionDate: Long? = null,
    val comment: String? = null,
    val acknowledgedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

/**
 * Append-only progress update submitted by a vendor on an active Work Order.
 */
data class VendorProgressUpdate(
    val progressUpdateId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val workOrderId: String,
    val progressPercentage: Double? = null,
    val completedQuantity: BigDecimal,
    val remainingQuantity: BigDecimal,
    val authorizedQuantity: BigDecimal,
    val statusSummary: String,
    val notes: String? = null,
    val expectedCompletionDate: Long? = null,
    val blockerReferenceId: String? = null,
    val submittedBy: String,
    val submittedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

/**
 * Blocker or execution issue reported by a vendor.
 */
data class VendorBlocker(
    val blockerId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val workOrderId: String,
    val purchaseOrderId: String? = null,
    val category: VendorBlockerCategory,
    val severity: VendorBlockerSeverity,
    val status: VendorBlockerStatus = VendorBlockerStatus.OPEN,
    val title: String,
    val description: String,
    val resolutionNotes: String? = null,
    val reportedBy: String,
    val reportedAt: Long = System.currentTimeMillis(),
    val acknowledgedBy: String? = null,
    val acknowledgedAt: Long? = null,
    val resolvedBy: String? = null,
    val resolvedAt: Long? = null,
    val version: Long = 1L
)

/**
 * Collaboration thread attached to a PO, WO, or related execution resource.
 */
data class VendorCollaborationThread(
    val threadId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val resourceType: VendorThreadResourceType,
    val resourceId: String,
    val title: String,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isClosed: Boolean = false,
    val version: Long = 1L
)

/**
 * Append-only message within a collaboration thread.
 */
data class VendorCollaborationMessage(
    val messageId: String,
    val threadId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val authorId: String,
    val authorName: String? = null,
    val isInternalAuthor: Boolean = false,
    val message: String,
    val visibility: VendorMessageVisibility = VendorMessageVisibility.VENDOR_VISIBLE,
    val attachmentMetadataJson: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Metadata record for evidence/document uploaded or attached during collaboration.
 */
data class VendorCollaborationEvidence(
    val evidenceId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val resourceType: VendorThreadResourceType,
    val resourceId: String,
    val fileReference: String,
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val checksum: String? = null,
    val description: String? = null,
    val visibility: VendorMessageVisibility = VendorMessageVisibility.VENDOR_VISIBLE,
    val uploadedBy: String,
    val uploadedAt: Long = System.currentTimeMillis()
)

/**
 * Vendor-initiated request to mark a Work Order complete.
 */
data class VendorCompletionRequest(
    val completionRequestId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val workOrderId: String,
    val status: VendorCompletionStatus = VendorCompletionStatus.REQUESTED,
    val completionNotes: String,
    val finalCompletedQuantity: BigDecimal,
    val evidenceReferences: List<String> = emptyList(),
    val submittedBy: String,
    val submittedAt: Long = System.currentTimeMillis(),
    val reviewedBy: String? = null,
    val reviewedAt: Long? = null,
    val reviewNotes: String? = null,
    val version: Long = 1L
)

/**
 * Immutable audit event for vendor collaboration operations.
 */
data class VendorCollaborationAuditEvent(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val eventType: VendorCollaborationAuditEventType,
    val resourceType: String,
    val resourceId: String,
    val actorId: String,
    val description: String,
    val previousState: String? = null,
    val newState: String? = null,
    val correlationId: String? = null,
    val metadataJson: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// ============================================================================
// PROJECTIONS FOR VENDOR PORTAL (Strictly non-confidential & vendor-scoped)
// ============================================================================

data class VendorPortalPurchaseOrderSummary(
    val purchaseOrderId: String,
    val orderNumber: String,
    val vendorId: String,
    val status: VendorPurchaseOrderStatus,
    val orderDate: Long,
    val expectedDeliveryDate: Long?,
    val deliveryLocation: String?,
    val currency: String,
    val totalAmount: Money,
    val acknowledgementStatus: VendorPoAcknowledgementType?,
    val acknowledgedAt: Long?,
    val activeWorkOrdersCount: Int,
    val openBlockersCount: Int
)

data class VendorPortalPurchaseOrderDetails(
    val purchaseOrderId: String,
    val orderNumber: String,
    val vendorId: String,
    val status: VendorPurchaseOrderStatus,
    val orderDate: Long,
    val expectedDeliveryDate: Long?,
    val deliveryLocation: String?,
    val currency: String,
    val subtotal: Money,
    val taxAmount: Money,
    val discountAmount: Money,
    val totalAmount: Money,
    val notes: String?,
    val items: List<VendorPurchaseOrderItem>,
    val acknowledgement: VendorPoAcknowledgement?,
    val relatedWorkOrders: List<VendorPortalWorkOrderSummary> = emptyList(),
    val openBlockers: List<VendorBlocker> = emptyList(),
    val evidenceList: List<VendorCollaborationEvidence> = emptyList()
)

data class VendorPortalWorkOrderSummary(
    val workOrderId: String,
    val workOrderNumber: String,
    val purchaseOrderId: String?,
    val title: String,
    val capabilityType: CapabilityType,
    val quantity: BigDecimal,
    val unitOfMeasure: UnitOfMeasure,
    val status: VendorWorkOrderStatus,
    val priority: String,
    val scheduledStartAt: Long?,
    val scheduledDueAt: Long?,
    val estimatedAmount: Money,
    val currency: String,
    val acknowledgementStatus: VendorWoAcknowledgementType?,
    val latestProgressPercentage: Double?,
    val completionStatus: VendorCompletionStatus?,
    val openBlockersCount: Int
)

data class VendorPortalWorkOrderDetails(
    val workOrderId: String,
    val workOrderNumber: String,
    val purchaseOrderId: String?,
    val title: String,
    val description: String?,
    val capabilityType: CapabilityType,
    val quantity: BigDecimal,
    val unitOfMeasure: UnitOfMeasure,
    val pricingMethod: PricingMethod,
    val estimatedAmount: Money,
    val currency: String,
    val status: VendorWorkOrderStatus,
    val priority: String,
    val scheduledStartAt: Long?,
    val scheduledDueAt: Long?,
    val notes: String?,
    val acknowledgement: VendorWoAcknowledgement?,
    val progressUpdates: List<VendorProgressUpdate> = emptyList(),
    val blockers: List<VendorBlocker> = emptyList(),
    val evidenceList: List<VendorCollaborationEvidence> = emptyList(),
    val completionRequest: VendorCompletionRequest? = null
)

package com.sucharu.sucharupro.domain.model.vendorportal

import java.math.BigDecimal

/**
 * Status lifecycle for Vendor Portal Delivery Notice (ASN - Advance Shipping Notice).
 */
enum class VendorPortalDeliveryNoticeStatus {
    DRAFT,
    SUBMITTED,
    ACKNOWLEDGED,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED
}

/**
 * Delivery notice item specifying quantities dispatched against a Purchase Order line.
 */
data class VendorPortalDeliveryNoticeItem(
    val itemId: String,
    val noticeId: String,
    val tenantId: String,
    val purchaseOrderItemId: String,
    val itemName: String,
    val itemCode: String? = null,
    val orderedQuantity: BigDecimal,
    val previouslyDeliveredQuantity: BigDecimal = BigDecimal.ZERO,
    val deliveryQuantity: BigDecimal,
    val unitOfMeasure: String = "PIECE",
    val lotNumber: String? = null,
    val packageCount: Int? = null,
    val remarks: String? = null
)

/**
 * Vendor-facing Advance Shipping Information / Delivery Notice (Module 13 Step 05).
 */
data class VendorPortalDeliveryNotice(
    val noticeId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val purchaseOrderId: String,
    val orderNumber: String,
    val noticeNumber: String,
    val status: VendorPortalDeliveryNoticeStatus = VendorPortalDeliveryNoticeStatus.DRAFT,
    val plannedDeliveryDate: Long,
    val carrierName: String? = null,
    val trackingNumber: String? = null,
    val vehicleNumber: String? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val vendorNotes: String? = null,
    val items: List<VendorPortalDeliveryNoticeItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = createdBy,
    val submittedAt: Long? = null,
    val submittedBy: String? = null,
    val cancelledAt: Long? = null,
    val cancelledBy: String? = null,
    val cancellationReason: String? = null,
    val version: Long = 1L
)

/**
 * Internal ERP receiving acknowledgement for an ASN / Delivery Notice.
 */
data class VendorPortalDeliveryAcknowledgement(
    val acknowledgementId: String,
    val noticeId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val deliveryReceiptId: String? = null,
    val acknowledgedBy: String,
    val acknowledgedAt: Long = System.currentTimeMillis(),
    val receivingGate: String? = null,
    val notes: String? = null
)

/**
 * Item-level canonical receiving progress projection for the vendor.
 */
data class VendorPortalReceivingItemSummary(
    val purchaseOrderItemId: String,
    val itemName: String,
    val orderedQuantity: BigDecimal,
    val notifiedQuantity: BigDecimal,
    val receivedQuantity: BigDecimal,
    val acceptedQuantity: BigDecimal,
    val rejectedQuantity: BigDecimal,
    val conditionalQuantity: BigDecimal,
    val remainingQuantity: BigDecimal,
    val unitOfMeasure: String
)

/**
 * Aggregate canonical receiving summary projection for a Purchase Order.
 */
data class VendorPortalReceivingSummary(
    val purchaseOrderId: String,
    val orderNumber: String,
    val vendorId: String,
    val status: String,
    val totalOrderedQuantity: BigDecimal,
    val totalNotifiedQuantity: BigDecimal,
    val totalReceivedQuantity: BigDecimal,
    val totalAcceptedQuantity: BigDecimal,
    val totalRejectedQuantity: BigDecimal,
    val totalConditionalQuantity: BigDecimal,
    val totalRemainingQuantity: BigDecimal,
    val receiptCount: Int,
    val latestReceiptDate: Long?,
    val items: List<VendorPortalReceivingItemSummary> = emptyList()
)

/**
 * Item-level quality inspection finding projection.
 */
data class VendorPortalQualityItemSummary(
    val inspectionItemId: String,
    val purchaseOrderItemId: String?,
    val itemName: String,
    val inspectedQuantity: BigDecimal,
    val acceptedQuantity: BigDecimal,
    val rejectedQuantity: BigDecimal,
    val conditionalQuantity: BigDecimal,
    val defectCount: Int,
    val remarks: String?
)

/**
 * Defect item projection for quality inspection.
 */
data class VendorPortalDefectSummary(
    val defectId: String,
    val defectCode: String,
    val defectCategory: String,
    val severity: String,
    val affectedQuantity: BigDecimal,
    val description: String
)

/**
 * Vendor-visible quality inspection projection over Module 12 canonical inspection.
 */
data class VendorPortalQualityInspectionSummary(
    val inspectionId: String,
    val inspectionNumber: String,
    val deliveryReceiptId: String,
    val purchaseOrderId: String,
    val vendorId: String,
    val inspectionDate: Long,
    val status: String,
    val overallResult: String,
    val inspectedQuantity: BigDecimal,
    val acceptedQuantity: BigDecimal,
    val rejectedQuantity: BigDecimal,
    val conditionalQuantity: BigDecimal,
    val rejectionId: String? = null,
    val rejectionReason: String? = null,
    val disposition: String? = null,
    val replacementRequired: Boolean = false,
    val creditRequired: Boolean = false,
    val correctiveActionRequired: Boolean = false,
    val disputeId: String? = null,
    val disputeStatus: String? = null,
    val items: List<VendorPortalQualityItemSummary> = emptyList(),
    val defects: List<VendorPortalDefectSummary> = emptyList()
)

/**
 * Action type for Vendor Quality Response.
 */
enum class VendorPortalQualityResponseType {
    ACKNOWLEDGE,
    PROPOSE_CORRECTIVE_ACTION,
    COMMIT_REPLACEMENT,
    REQUEST_DISPUTE
}

/**
 * Formal vendor response to quality findings or rejection.
 */
data class VendorPortalQualityResponse(
    val responseId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val inspectionId: String,
    val rejectionId: String? = null,
    val responseType: VendorPortalQualityResponseType,
    val comment: String,
    val correctiveActionPlan: String? = null,
    val promisedReplacementDate: Long? = null,
    val evidenceReferences: List<String> = emptyList(),
    val respondedBy: String,
    val respondedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

/**
 * Category for delivery and quality exceptions.
 */
enum class VendorPortalDeliveryExceptionType {
    DELIVERY_DELAY,
    QUANTITY_VARIANCE,
    MISSING_DOCUMENT,
    RECEIVING_HOLD,
    QUALITY_REJECTION,
    CONDITIONAL_ACCEPTANCE,
    REPLACEMENT_REQUIRED,
    CORRECTIVE_ACTION_REQUIRED,
    INSPECTION_PENDING,
    DISPUTE_PENDING
}

/**
 * Severity for delivery/quality exceptions.
 */
enum class VendorPortalDeliveryExceptionSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Status for delivery/quality exceptions.
 */
enum class VendorPortalDeliveryExceptionStatus {
    OPEN,
    ACKNOWLEDGED,
    RESOLVED,
    CLOSED
}

/**
 * Operational exception projection requiring vendor awareness or action.
 */
data class VendorPortalDeliveryException(
    val exceptionId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val sourceType: String, // "PURCHASE_ORDER", "DELIVERY_NOTICE", "DELIVERY_RECEIPT", "QUALITY_INSPECTION", "REJECTION"
    val sourceId: String,
    val exceptionType: VendorPortalDeliveryExceptionType,
    val severity: VendorPortalDeliveryExceptionSeverity = VendorPortalDeliveryExceptionSeverity.MEDIUM,
    val status: VendorPortalDeliveryExceptionStatus = VendorPortalDeliveryExceptionStatus.OPEN,
    val title: String,
    val description: String,
    val requiredVendorAction: String? = null,
    val dueAt: Long? = null,
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null,
    val resolutionNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String,
    val version: Long = 1L
)

/**
 * Metadata for delivery/quality evidence documents.
 */
data class VendorPortalDeliveryEvidence(
    val evidenceId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val entityType: String, // "DELIVERY_NOTICE", "DELIVERY_RECEIPT", "QUALITY_INSPECTION", "REJECTION"
    val entityId: String,
    val filename: String,
    val fileReference: String,
    val mimeType: String,
    val sizeBytes: Long,
    val description: String? = null,
    val uploadedBy: String,
    val uploadedAt: Long = System.currentTimeMillis()
)

/**
 * Event type for delivery and quality collaboration audit.
 */
enum class VendorPortalDeliveryAuditEventType {
    DELIVERY_NOTICE_CREATED,
    DELIVERY_NOTICE_UPDATED,
    DELIVERY_NOTICE_SUBMITTED,
    DELIVERY_NOTICE_CANCELLED,
    DELIVERY_ACKNOWLEDGED,
    EVIDENCE_ATTACHED,
    QUALITY_VIEWED,
    QUALITY_ACKNOWLEDGED,
    QUALITY_RESPONSE_SUBMITTED,
    CORRECTIVE_ACTION_PROPOSED,
    REPLACEMENT_COMMITTED,
    EXCEPTION_CREATED,
    EXCEPTION_RESOLVED
}

/**
 * Immutable audit trail event for delivery, receiving, and quality collaboration.
 */
data class VendorPortalDeliveryAuditEvent(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val eventType: VendorPortalDeliveryAuditEventType,
    val entityType: String,
    val entityId: String,
    val actorId: String,
    val description: String,
    val previousState: String? = null,
    val newState: String? = null,
    val correlationId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

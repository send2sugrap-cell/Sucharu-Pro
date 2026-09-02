package com.sucharu.sucharupro.domain.model.communication.vendor.document

/**
 * Append-only immutable audit trail event types for Vendor Documents (Module 10 Step 06).
 */
enum class VendorDocumentActivityEventType(val defaultLabel: String) {
    DOCUMENT_REQUESTED("Document Requested"),
    DOCUMENT_SUBMITTED("Document Submitted"),
    DOCUMENT_VERSION_CREATED("New Document Version Created"),
    DOCUMENT_REVIEW_STARTED("Document Review Started"),
    DOCUMENT_APPROVED("Document Approved"),
    DOCUMENT_REJECTED("Document Rejected"),
    DOCUMENT_EXPIRED("Document Expired"),
    DOCUMENT_RENEWAL_REQUIRED("Document Renewal Required"),
    DOCUMENT_RENEWAL_REMINDER_SENT("Renewal Reminder Sent"),
    DOCUMENT_CANCELLED("Document Cancelled"),
    DOCUMENT_VIEWED("Document Viewed"),
    DOCUMENT_DOWNLOAD_REQUESTED("Document Download Requested"),
    DOCUMENT_ACKNOWLEDGED("Document Request Acknowledged")
}

/**
 * Append-only immutable audit activity event (Module 10 Step 06).
 */
data class VendorDocumentActivityEvent(
    val eventId: String,
    val projectId: String,
    val vendorId: String,
    val documentId: String? = null,
    val requestId: String? = null,
    val eventType: VendorDocumentActivityEventType,
    val actorId: String,
    val actorRole: String? = null,
    val previousState: String? = null,
    val newState: String? = null,
    val details: String = "",
    val metadata: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(eventId.isNotBlank()) { "eventId cannot be blank" }
        require(projectId.isNotBlank()) { "projectId cannot be blank" }
        require(vendorId.isNotBlank()) { "vendorId cannot be blank" }
        require(actorId.isNotBlank()) { "actorId cannot be blank" }
    }
}

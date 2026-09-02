package com.sucharu.sucharupro.domain.model.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialReportFormat
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.CustomerFinancialReportType

/**
 * Finite delivery statuses for customer financial documents (Module 14 Step 11).
 */
enum class CustomerFinancialDeliveryStatus {
    CREATED,
    READY,
    NOTIFIED,
    ACCESSED,
    EXPIRED,
    REVOKED,
    FAILED
}

/**
 * Notification delivery status (Module 14 Step 11).
 */
enum class CustomerFinancialNotificationStatus {
    PENDING,
    SENT,
    FAILED,
    SUPPRESSED
}

/**
 * Append-only audit event types for document delivery tracking (Module 14 Step 11).
 */
enum class CustomerFinancialDeliveryEventType {
    DOCUMENT_CREATED,
    DOCUMENT_READY,
    DOCUMENT_NOTIFIED,
    DOCUMENT_ACCESSED,
    DOCUMENT_DOWNLOADED,
    DOCUMENT_EXPIRED,
    DOCUMENT_REVOKED,
    DOCUMENT_DELIVERY_FAILED
}

/**
 * Canonical delivery record for generated customer financial documents (Module 14 Step 11).
 */
data class CustomerFinancialDocumentDelivery(
    val deliveryId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val documentId: String,
    val documentType: CustomerFinancialReportType,
    val documentFormat: CustomerFinancialReportFormat,
    val documentName: String,
    val storageReference: String,
    val checksum: String,
    val fileSize: Long,
    val mimeType: String = "application/octet-stream",
    val deliveryStatus: CustomerFinancialDeliveryStatus = CustomerFinancialDeliveryStatus.CREATED,
    val accessCount: Int = 0,
    val lastAccessedAt: Long? = null,
    val lastAccessedBy: String? = null,
    val expiresAt: Long? = null,
    val isRevoked: Boolean = false,
    val revokedAt: Long? = null,
    val revokedBy: String? = null,
    val revocationReason: String? = null,
    val notificationStatus: CustomerFinancialNotificationStatus = CustomerFinancialNotificationStatus.PENDING,
    val notifiedAt: Long? = null,
    val notificationId: String? = null,
    val failureReason: String? = null,
    val idempotencyKey: String? = null,
    val metadataJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String,
    val version: Long = 1L
) {
    init {
        require(deliveryId.isNotBlank()) { "Delivery ID cannot be blank." }
        require(tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(documentId.isNotBlank()) { "Document ID cannot be blank." }
        require(documentName.isNotBlank()) { "Document name cannot be blank." }
        require(checksum.isNotBlank()) { "Document checksum cannot be blank." }
        require(fileSize >= 0) { "File size must be non-negative." }
    }

    val isExpired: Boolean get() = expiresAt != null && System.currentTimeMillis() > expiresAt
    val isDownloadable: Boolean get() = !isRevoked && !isExpired && deliveryStatus != CustomerFinancialDeliveryStatus.FAILED
}

/**
 * Append-only audit record for document delivery events (Module 14 Step 11).
 */
data class CustomerFinancialDocumentDeliveryAuditEvent(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val deliveryId: String,
    val documentId: String,
    val eventType: CustomerFinancialDeliveryEventType,
    val actorId: String,
    val actorRole: String,
    val timestamp: Long = System.currentTimeMillis(),
    val correlationId: String? = null,
    val detailsJson: String? = null,
    val checksum: String? = null
)

/**
 * Access and download container for authorized client delivery (Module 14 Step 11).
 */
data class CustomerFinancialDocumentAccessPayload(
    val deliveryId: String,
    val documentId: String,
    val documentName: String,
    val mimeType: String,
    val content: ByteArray,
    val checksum: String,
    val fileSize: Long,
    val isExpired: Boolean,
    val isRevoked: Boolean
)

/**
 * Result of document notification dispatch (Module 14 Step 11).
 */
data class CustomerFinancialDocumentNotificationResult(
    val deliveryId: String,
    val notificationId: String?,
    val status: CustomerFinancialNotificationStatus,
    val recipientUserId: String,
    val notifiedAt: Long?,
    val message: String
)

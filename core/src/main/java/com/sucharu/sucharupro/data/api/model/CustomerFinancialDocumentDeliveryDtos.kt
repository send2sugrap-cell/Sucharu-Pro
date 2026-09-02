package com.sucharu.sucharupro.data.api.model

import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import java.util.Base64

/**
 * DTOs for Customer Financial Document Delivery, Secure Access & Notification (Module 14 Step 11).
 */

data class CustomerFinancialDocumentDeliveryDto(
    val deliveryId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val documentId: String,
    val documentType: String,
    val documentFormat: String,
    val documentName: String,
    val storageReference: String,
    val checksum: String,
    val fileSize: Long,
    val mimeType: String,
    val deliveryStatus: String,
    val accessCount: Int,
    val lastAccessedAt: Long? = null,
    val lastAccessedBy: String? = null,
    val expiresAt: Long? = null,
    val isRevoked: Boolean,
    val revokedAt: Long? = null,
    val revokedBy: String? = null,
    val revocationReason: String? = null,
    val notificationStatus: String,
    val notifiedAt: Long? = null,
    val notificationId: String? = null,
    val failureReason: String? = null,
    val idempotencyKey: String? = null,
    val metadataJson: String? = null,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val isExpired: Boolean,
    val isDownloadable: Boolean,
    val version: Long
)

data class CreateCustomerFinancialDocumentDeliveryRequest(
    val reportType: String,
    val format: String = "JSON",
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val invoiceId: String? = null,
    val expiresInHours: Long? = null,
    val idempotencyKey: String? = null
)

data class RevokeCustomerFinancialDocumentDeliveryRequest(
    val reason: String
)

data class NotifyCustomerFinancialDocumentRequest(
    val recipientUserId: String? = null,
    val customMessage: String? = null,
    val idempotencyKey: String? = null
)

data class CustomerFinancialDocumentAccessResponseDto(
    val deliveryId: String,
    val documentId: String,
    val documentName: String,
    val mimeType: String,
    val contentBase64: String,
    val checksum: String,
    val fileSize: Long,
    val isExpired: Boolean,
    val isRevoked: Boolean
)

data class CustomerFinancialDocumentDeliveryAuditEventDto(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val deliveryId: String,
    val documentId: String,
    val eventType: String,
    val actorId: String,
    val actorRole: String,
    val timestamp: Long,
    val correlationId: String? = null,
    val detailsJson: String? = null,
    val checksum: String? = null
)

fun CustomerFinancialDocumentDelivery.toDto(): CustomerFinancialDocumentDeliveryDto = CustomerFinancialDocumentDeliveryDto(
    deliveryId = deliveryId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    documentId = documentId,
    documentType = documentType.name,
    documentFormat = documentFormat.name,
    documentName = documentName,
    storageReference = storageReference,
    checksum = checksum,
    fileSize = fileSize,
    mimeType = mimeType,
    deliveryStatus = deliveryStatus.name,
    accessCount = accessCount,
    lastAccessedAt = lastAccessedAt,
    lastAccessedBy = lastAccessedBy,
    expiresAt = expiresAt,
    isRevoked = isRevoked,
    revokedAt = revokedAt,
    revokedBy = revokedBy,
    revocationReason = revocationReason,
    notificationStatus = notificationStatus.name,
    notifiedAt = notifiedAt,
    notificationId = notificationId,
    failureReason = failureReason,
    idempotencyKey = idempotencyKey,
    metadataJson = metadataJson,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    isExpired = isExpired,
    isDownloadable = isDownloadable,
    version = version
)

fun CustomerFinancialDocumentAccessPayload.toDto(): CustomerFinancialDocumentAccessResponseDto = CustomerFinancialDocumentAccessResponseDto(
    deliveryId = deliveryId,
    documentId = documentId,
    documentName = documentName,
    mimeType = mimeType,
    contentBase64 = Base64.getEncoder().encodeToString(content),
    checksum = checksum,
    fileSize = fileSize,
    isExpired = isExpired,
    isRevoked = isRevoked
)

fun CustomerFinancialDocumentDeliveryAuditEvent.toDto(): CustomerFinancialDocumentDeliveryAuditEventDto = CustomerFinancialDocumentDeliveryAuditEventDto(
    auditId = auditId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    deliveryId = deliveryId,
    documentId = documentId,
    eventType = eventType.name,
    actorId = actorId,
    actorRole = actorRole,
    timestamp = timestamp,
    correlationId = correlationId,
    detailsJson = detailsJson,
    checksum = checksum
)

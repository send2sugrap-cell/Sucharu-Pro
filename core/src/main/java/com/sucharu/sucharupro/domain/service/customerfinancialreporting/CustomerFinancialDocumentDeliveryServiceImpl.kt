package com.sucharu.sucharupro.domain.service.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.notification.NotificationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import com.sucharu.sucharupro.domain.repository.customerfinancialreporting.CustomerFinancialDocumentDeliveryRepository
import com.sucharu.sucharupro.domain.repository.notification.NotificationRepository
import com.sucharu.sucharupro.domain.validation.customerfinancialreporting.CustomerFinancialDocumentDeliveryValidator
import java.security.MessageDigest
import java.util.UUID

/**
 * Production implementation of CustomerFinancialDocumentDeliveryService (Module 14 Step 11).
 *
 * Implements a secure, auditable document delivery layer over canonical reports from Step 10
 * without performing any financial database mutations.
 */
class CustomerFinancialDocumentDeliveryServiceImpl(
    private val deliveryRepository: CustomerFinancialDocumentDeliveryRepository,
    private val reportingService: CustomerFinancialReportingService,
    private val customerRepository: CustomerRepository,
    private val notificationRepository: NotificationRepository? = null
) : CustomerFinancialDocumentDeliveryService {

    private fun calculateChecksum(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return "SHA256:" + hash.joinToString("") { "%02x".format(it) }
    }

    override suspend fun generateAndRegisterDelivery(
        tenantId: String,
        projectId: String,
        customerId: String,
        reportType: CustomerFinancialReportType,
        format: CustomerFinancialReportFormat,
        fromDate: Long?,
        toDate: Long?,
        invoiceId: String?,
        expiresInHours: Long?,
        actorId: String,
        actorRole: String,
        correlationId: String?,
        idempotencyKey: String?
    ): DomainResult<CustomerFinancialDocumentDelivery> {
        // 1. Idempotency Check
        if (!idempotencyKey.isNullOrBlank()) {
            when (val existingRes = deliveryRepository.getDeliveryByIdempotencyKey(tenantId, projectId, idempotencyKey)) {
                is DomainResult.Success -> {
                    if (existingRes.data != null) {
                        return DomainResult.Success(existingRes.data)
                    }
                }
                is DomainResult.Error -> return existingRes
                DomainResult.Loading -> {}
            }
        }

        // 2. Customer Validation
        val custRes = customerRepository.findCustomerById(customerId)
        val customer = when (custRes) {
            is DomainResult.Success -> custRes.data
            is DomainResult.Error -> return DomainResult.Error(custRes.exception, "Customer '$customerId' not found.")
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        // 3. Generate Canonical Report Document from Step 10 (Zero financial mutation)
        val reportReq = CustomerFinancialReportRequest(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            reportType = reportType,
            format = format,
            fromDate = fromDate,
            toDate = toDate,
            invoiceId = invoiceId,
            requestedBy = actorId
        )

        val docRes = reportingService.exportFinancialReport(reportReq)
        if (docRes is DomainResult.Error) return DomainResult.Error(docRes.exception, docRes.message)
        val doc = (docRes as DomainResult.Success).data

        val checksum = calculateChecksum(doc.contentBytes)
        val fileSize = doc.contentBytes.size.toLong()
        val fileExtension = when (doc.format) {
            CustomerFinancialReportFormat.CSV -> "csv"
            CustomerFinancialReportFormat.JSON -> "json"
            CustomerFinancialReportFormat.PDF -> "pdf"
        }

        // 4. Validate Delivery Creation Properties
        val valRes = CustomerFinancialDocumentDeliveryValidator.validateCreation(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            documentName = doc.fileName,
            checksum = checksum,
            fileSize = fileSize
        )
        if (valRes is DomainResult.Error) return valRes

        // 5. Store Payload Bytes & Create Delivery Record
        deliveryRepository.saveDocumentPayload(doc.documentId, doc.contentBytes)

        val now = System.currentTimeMillis()
        val expiresAt = expiresInHours?.let { now + (it * 3600_000L) }
        val deliveryId = "DEL-${UUID.randomUUID().toString().take(12).uppercase()}"

        val delivery = CustomerFinancialDocumentDelivery(
            deliveryId = deliveryId,
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            documentId = doc.documentId,
            documentType = reportType,
            documentFormat = format,
            documentName = doc.fileName,
            storageReference = "docstore://$projectId/$customerId/${doc.documentId}.${fileExtension}",
            checksum = checksum,
            fileSize = fileSize,
            mimeType = doc.contentType,
            deliveryStatus = CustomerFinancialDeliveryStatus.READY,
            accessCount = 0,
            expiresAt = expiresAt,
            isRevoked = false,
            notificationStatus = CustomerFinancialNotificationStatus.PENDING,
            idempotencyKey = idempotencyKey,
            metadataJson = """{"customerCode":"${customer.customerCode}","customerName":"${customer.displayName}","generatedAt":${doc.generatedAt}}""",
            createdAt = now,
            createdBy = actorId,
            updatedAt = now,
            updatedBy = actorId,
            version = 1L
        )

        val saveRes = deliveryRepository.saveDelivery(delivery)
        if (saveRes is DomainResult.Error) return saveRes

        // 6. Record Audit Event
        val auditEvent = CustomerFinancialDocumentDeliveryAuditEvent(
            auditId = "AUD-${UUID.randomUUID().toString().take(12).uppercase()}",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            deliveryId = deliveryId,
            documentId = doc.documentId,
            eventType = CustomerFinancialDeliveryEventType.DOCUMENT_READY,
            actorId = actorId,
            actorRole = actorRole,
            timestamp = now,
            correlationId = correlationId,
            detailsJson = """{"documentName":"${doc.fileName}","fileSize":$fileSize,"format":"${format.name}"}""",
            checksum = checksum
        )
        deliveryRepository.recordAuditEvent(auditEvent)

        return DomainResult.Success(delivery)
    }

    override suspend fun getDelivery(
        tenantId: String,
        projectId: String,
        deliveryId: String
    ): DomainResult<CustomerFinancialDocumentDelivery> {
        val res = deliveryRepository.getDeliveryById(tenantId, projectId, deliveryId)
        return when (res) {
            is DomainResult.Success -> {
                val d = res.data ?: return DomainResult.Error(IllegalArgumentException("Document delivery '$deliveryId' not found."))
                DomainResult.Success(d)
            }
            is DomainResult.Error -> res
            DomainResult.Loading -> DomainResult.Error(IllegalStateException("Loading state"))
        }
    }

    override suspend fun listDeliveries(
        tenantId: String,
        projectId: String,
        customerId: String?,
        documentType: CustomerFinancialReportType?,
        status: CustomerFinancialDeliveryStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerFinancialDocumentDelivery>> {
        return deliveryRepository.listDeliveries(tenantId, projectId, customerId, documentType, status, limit, offset)
    }

    override suspend fun accessDocument(
        tenantId: String,
        projectId: String,
        deliveryId: String,
        actorId: String,
        actorRole: String,
        correlationId: String?
    ): DomainResult<CustomerFinancialDocumentAccessPayload> {
        val delRes = deliveryRepository.getDeliveryById(tenantId, projectId, deliveryId)
        val delivery = when (delRes) {
            is DomainResult.Success -> delRes.data ?: return DomainResult.Error(IllegalArgumentException("Document delivery '$deliveryId' not found."))
            is DomainResult.Error -> return delRes
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        // Validate Access (Revocation, Expiration, Failure Status)
        val valRes = CustomerFinancialDocumentDeliveryValidator.validateAccess(delivery)
        if (valRes is DomainResult.Error) return valRes

        // Fetch Payload Bytes
        val payloadRes = deliveryRepository.getDocumentPayload(delivery.documentId)
        val bytes = when (payloadRes) {
            is DomainResult.Success -> payloadRes.data ?: ByteArray(0)
            is DomainResult.Error -> return DomainResult.Error(payloadRes.exception, payloadRes.message)
            DomainResult.Loading -> ByteArray(0)
        }

        val now = System.currentTimeMillis()
        val updatedDelivery = delivery.copy(
            accessCount = delivery.accessCount + 1,
            lastAccessedAt = now,
            lastAccessedBy = actorId,
            deliveryStatus = if (delivery.deliveryStatus == CustomerFinancialDeliveryStatus.READY || delivery.deliveryStatus == CustomerFinancialDeliveryStatus.NOTIFIED) {
                CustomerFinancialDeliveryStatus.ACCESSED
            } else {
                delivery.deliveryStatus
            },
            updatedAt = now,
            updatedBy = actorId,
            version = delivery.version + 1
        )
        deliveryRepository.saveDelivery(updatedDelivery)

        // Append-only Access Audit
        val auditEvent = CustomerFinancialDocumentDeliveryAuditEvent(
            auditId = "AUD-${UUID.randomUUID().toString().take(12).uppercase()}",
            tenantId = tenantId,
            projectId = projectId,
            customerId = delivery.customerId,
            deliveryId = delivery.deliveryId,
            documentId = delivery.documentId,
            eventType = CustomerFinancialDeliveryEventType.DOCUMENT_DOWNLOADED,
            actorId = actorId,
            actorRole = actorRole,
            timestamp = now,
            correlationId = correlationId,
            detailsJson = """{"accessCount":${updatedDelivery.accessCount},"documentName":"${delivery.documentName}"}""",
            checksum = delivery.checksum
        )
        deliveryRepository.recordAuditEvent(auditEvent)

        val payload = CustomerFinancialDocumentAccessPayload(
            deliveryId = delivery.deliveryId,
            documentId = delivery.documentId,
            documentName = delivery.documentName,
            mimeType = delivery.mimeType,
            content = bytes,
            checksum = delivery.checksum,
            fileSize = delivery.fileSize,
            isExpired = delivery.isExpired,
            isRevoked = delivery.isRevoked
        )
        return DomainResult.Success(payload)
    }

    override suspend fun notifyCustomer(
        tenantId: String,
        projectId: String,
        deliveryId: String,
        recipientUserId: String?,
        customMessage: String?,
        actorId: String,
        actorRole: String,
        correlationId: String?,
        idempotencyKey: String?
    ): DomainResult<CustomerFinancialDocumentNotificationResult> {
        val delRes = deliveryRepository.getDeliveryById(tenantId, projectId, deliveryId)
        val delivery = when (delRes) {
            is DomainResult.Success -> delRes.data ?: return DomainResult.Error(IllegalArgumentException("Document delivery '$deliveryId' not found."))
            is DomainResult.Error -> return delRes
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        if (delivery.isRevoked) {
            return DomainResult.Error(IllegalStateException("Cannot notify customer for revoked document."))
        }
        if (delivery.isExpired) {
            return DomainResult.Error(IllegalStateException("Cannot notify customer for expired document."))
        }

        val targetRecipient = recipientUserId ?: delivery.customerId
        val now = System.currentTimeMillis()
        val notificationMsg = customMessage ?: "Your financial report '${delivery.documentName}' is available for secure access."

        var notificationId = "NOTIF-${UUID.randomUUID().toString().take(10).uppercase()}"
        var status = CustomerFinancialNotificationStatus.SENT

        if (notificationRepository != null) {
            val role = try { UserRole.valueOf(actorRole) } catch (_: Exception) { UserRole.STAFF }
            val notifRes = notificationRepository.createNotification(
                projectId = projectId,
                recipientUserId = targetRecipient,
                recipientType = "CUSTOMER",
                notificationType = NotificationType.FINANCIAL_ALERT,
                channel = NotificationChannel.IN_APP,
                priority = NotificationPriority.HIGH,
                title = "Financial Document Ready: ${delivery.documentName}",
                message = notificationMsg,
                referenceType = "CUSTOMER_FINANCIAL_DOCUMENT",
                referenceId = delivery.deliveryId,
                idempotencyKey = idempotencyKey,
                metadata = mapOf("deliveryId" to delivery.deliveryId, "documentType" to delivery.documentType.name),
                actorId = actorId,
                callerRole = role
            )
            if (notifRes is DomainResult.Success) {
                notificationId = notifRes.data.notificationId
            }
        }

        val updatedDelivery = delivery.copy(
            notificationStatus = status,
            notifiedAt = now,
            notificationId = notificationId,
            deliveryStatus = if (delivery.deliveryStatus == CustomerFinancialDeliveryStatus.READY) CustomerFinancialDeliveryStatus.NOTIFIED else delivery.deliveryStatus,
            updatedAt = now,
            updatedBy = actorId,
            version = delivery.version + 1
        )
        deliveryRepository.saveDelivery(updatedDelivery)

        val auditEvent = CustomerFinancialDocumentDeliveryAuditEvent(
            auditId = "AUD-${UUID.randomUUID().toString().take(12).uppercase()}",
            tenantId = tenantId,
            projectId = projectId,
            customerId = delivery.customerId,
            deliveryId = delivery.deliveryId,
            documentId = delivery.documentId,
            eventType = CustomerFinancialDeliveryEventType.DOCUMENT_NOTIFIED,
            actorId = actorId,
            actorRole = actorRole,
            timestamp = now,
            correlationId = correlationId,
            detailsJson = """{"recipient":"$targetRecipient","notificationId":"$notificationId"}""",
            checksum = delivery.checksum
        )
        deliveryRepository.recordAuditEvent(auditEvent)

        val result = CustomerFinancialDocumentNotificationResult(
            deliveryId = delivery.deliveryId,
            notificationId = notificationId,
            status = status,
            recipientUserId = targetRecipient,
            notifiedAt = now,
            message = "Notification dispatched successfully."
        )
        return DomainResult.Success(result)
    }

    override suspend fun revokeDelivery(
        tenantId: String,
        projectId: String,
        deliveryId: String,
        reason: String,
        actorId: String,
        actorRole: String,
        correlationId: String?
    ): DomainResult<CustomerFinancialDocumentDelivery> {
        val delRes = deliveryRepository.getDeliveryById(tenantId, projectId, deliveryId)
        val delivery = when (delRes) {
            is DomainResult.Success -> delRes.data ?: return DomainResult.Error(IllegalArgumentException("Document delivery '$deliveryId' not found."))
            is DomainResult.Error -> return delRes
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val valRes = CustomerFinancialDocumentDeliveryValidator.validateRevocation(delivery, reason)
        if (valRes is DomainResult.Error) return valRes

        val now = System.currentTimeMillis()
        val updatedDelivery = delivery.copy(
            isRevoked = true,
            revokedAt = now,
            revokedBy = actorId,
            revocationReason = reason,
            deliveryStatus = CustomerFinancialDeliveryStatus.REVOKED,
            updatedAt = now,
            updatedBy = actorId,
            version = delivery.version + 1
        )
        val saveRes = deliveryRepository.saveDelivery(updatedDelivery)
        if (saveRes is DomainResult.Error) return saveRes

        val auditEvent = CustomerFinancialDocumentDeliveryAuditEvent(
            auditId = "AUD-${UUID.randomUUID().toString().take(12).uppercase()}",
            tenantId = tenantId,
            projectId = projectId,
            customerId = delivery.customerId,
            deliveryId = delivery.deliveryId,
            documentId = delivery.documentId,
            eventType = CustomerFinancialDeliveryEventType.DOCUMENT_REVOKED,
            actorId = actorId,
            actorRole = actorRole,
            timestamp = now,
            correlationId = correlationId,
            detailsJson = """{"reason":"$reason"}""",
            checksum = delivery.checksum
        )
        deliveryRepository.recordAuditEvent(auditEvent)

        return DomainResult.Success(updatedDelivery)
    }

    override suspend fun getDeliveryAuditHistory(
        tenantId: String,
        projectId: String,
        deliveryId: String
    ): DomainResult<List<CustomerFinancialDocumentDeliveryAuditEvent>> {
        return deliveryRepository.listAuditEvents(tenantId, projectId, deliveryId)
    }
}

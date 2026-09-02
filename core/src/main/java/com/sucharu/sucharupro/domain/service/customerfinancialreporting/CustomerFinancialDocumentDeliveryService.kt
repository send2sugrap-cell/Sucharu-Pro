package com.sucharu.sucharupro.domain.service.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*

/**
 * Domain service interface for Customer Financial Document Delivery, Secure Access & Notification (Module 14 Step 11).
 */
interface CustomerFinancialDocumentDeliveryService {

    suspend fun generateAndRegisterDelivery(
        tenantId: String,
        projectId: String,
        customerId: String,
        reportType: CustomerFinancialReportType,
        format: CustomerFinancialReportFormat,
        fromDate: Long? = null,
        toDate: Long? = null,
        invoiceId: String? = null,
        expiresInHours: Long? = null,
        actorId: String,
        actorRole: String,
        correlationId: String? = null,
        idempotencyKey: String? = null
    ): DomainResult<CustomerFinancialDocumentDelivery>

    suspend fun getDelivery(
        tenantId: String,
        projectId: String,
        deliveryId: String
    ): DomainResult<CustomerFinancialDocumentDelivery>

    suspend fun listDeliveries(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        documentType: CustomerFinancialReportType? = null,
        status: CustomerFinancialDeliveryStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<CustomerFinancialDocumentDelivery>>

    suspend fun accessDocument(
        tenantId: String,
        projectId: String,
        deliveryId: String,
        actorId: String,
        actorRole: String,
        correlationId: String? = null
    ): DomainResult<CustomerFinancialDocumentAccessPayload>

    suspend fun notifyCustomer(
        tenantId: String,
        projectId: String,
        deliveryId: String,
        recipientUserId: String? = null,
        customMessage: String? = null,
        actorId: String,
        actorRole: String,
        correlationId: String? = null,
        idempotencyKey: String? = null
    ): DomainResult<CustomerFinancialDocumentNotificationResult>

    suspend fun revokeDelivery(
        tenantId: String,
        projectId: String,
        deliveryId: String,
        reason: String,
        actorId: String,
        actorRole: String,
        correlationId: String? = null
    ): DomainResult<CustomerFinancialDocumentDelivery>

    suspend fun getDeliveryAuditHistory(
        tenantId: String,
        projectId: String,
        deliveryId: String
    ): DomainResult<List<CustomerFinancialDocumentDeliveryAuditEvent>>
}

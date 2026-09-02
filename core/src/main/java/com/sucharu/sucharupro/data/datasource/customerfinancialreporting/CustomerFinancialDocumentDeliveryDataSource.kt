package com.sucharu.sucharupro.data.datasource.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*

/**
 * Data source interface for Customer Financial Document Delivery (Module 14 Step 11).
 */
interface CustomerFinancialDocumentDeliveryDataSource {

    suspend fun saveDelivery(
        delivery: CustomerFinancialDocumentDelivery
    ): DomainResult<CustomerFinancialDocumentDelivery>

    suspend fun getDeliveryById(
        tenantId: String,
        projectId: String,
        deliveryId: String
    ): DomainResult<CustomerFinancialDocumentDelivery?>

    suspend fun getDeliveryByDocumentId(
        tenantId: String,
        projectId: String,
        documentId: String
    ): DomainResult<CustomerFinancialDocumentDelivery?>

    suspend fun getDeliveryByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerFinancialDocumentDelivery?>

    suspend fun listDeliveries(
        tenantId: String,
        projectId: String,
        customerId: String?,
        documentType: CustomerFinancialReportType?,
        status: CustomerFinancialDeliveryStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerFinancialDocumentDelivery>>

    suspend fun saveAuditEvent(
        event: CustomerFinancialDocumentDeliveryAuditEvent
    ): DomainResult<CustomerFinancialDocumentDeliveryAuditEvent>

    suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        deliveryId: String
    ): DomainResult<List<CustomerFinancialDocumentDeliveryAuditEvent>>

    suspend fun saveDocumentPayload(
        documentId: String,
        content: ByteArray
    ): DomainResult<Unit>

    suspend fun getDocumentPayload(
        documentId: String
    ): DomainResult<ByteArray?>
}

package com.sucharu.sucharupro.data.repository.customerfinancialreporting

import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.CustomerFinancialDocumentDeliveryDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import com.sucharu.sucharupro.domain.repository.customerfinancialreporting.CustomerFinancialDocumentDeliveryRepository

class CustomerFinancialDocumentDeliveryRepositoryImpl(
    private val dataSource: CustomerFinancialDocumentDeliveryDataSource
) : CustomerFinancialDocumentDeliveryRepository {

    override suspend fun saveDelivery(delivery: CustomerFinancialDocumentDelivery): DomainResult<CustomerFinancialDocumentDelivery> {
        return dataSource.saveDelivery(delivery)
    }

    override suspend fun getDeliveryById(
        tenantId: String,
        projectId: String,
        deliveryId: String
    ): DomainResult<CustomerFinancialDocumentDelivery?> {
        return dataSource.getDeliveryById(tenantId, projectId, deliveryId)
    }

    override suspend fun getDeliveryByDocumentId(
        tenantId: String,
        projectId: String,
        documentId: String
    ): DomainResult<CustomerFinancialDocumentDelivery?> {
        return dataSource.getDeliveryByDocumentId(tenantId, projectId, documentId)
    }

    override suspend fun getDeliveryByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerFinancialDocumentDelivery?> {
        return dataSource.getDeliveryByIdempotencyKey(tenantId, projectId, idempotencyKey)
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
        return dataSource.listDeliveries(tenantId, projectId, customerId, documentType, status, limit, offset)
    }

    override suspend fun recordAuditEvent(event: CustomerFinancialDocumentDeliveryAuditEvent): DomainResult<CustomerFinancialDocumentDeliveryAuditEvent> {
        return dataSource.saveAuditEvent(event)
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        deliveryId: String
    ): DomainResult<List<CustomerFinancialDocumentDeliveryAuditEvent>> {
        return dataSource.listAuditEvents(tenantId, projectId, deliveryId)
    }

    override suspend fun saveDocumentPayload(documentId: String, content: ByteArray): DomainResult<Unit> {
        return dataSource.saveDocumentPayload(documentId, content)
    }

    override suspend fun getDocumentPayload(documentId: String): DomainResult<ByteArray?> {
        return dataSource.getDocumentPayload(documentId)
    }
}

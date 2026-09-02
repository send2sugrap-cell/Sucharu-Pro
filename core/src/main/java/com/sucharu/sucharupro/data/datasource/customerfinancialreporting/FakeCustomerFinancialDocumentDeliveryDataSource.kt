package com.sucharu.sucharupro.data.datasource.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class FakeCustomerFinancialDocumentDeliveryDataSource : CustomerFinancialDocumentDeliveryDataSource {

    private val deliveries = ConcurrentHashMap<String, CustomerFinancialDocumentDelivery>()
    private val auditEvents = ConcurrentHashMap<String, CopyOnWriteArrayList<CustomerFinancialDocumentDeliveryAuditEvent>>()
    private val payloads = ConcurrentHashMap<String, ByteArray>()

    override suspend fun saveDelivery(delivery: CustomerFinancialDocumentDelivery): DomainResult<CustomerFinancialDocumentDelivery> {
        val key = "${delivery.tenantId}_${delivery.projectId}_${delivery.deliveryId}"
        deliveries[key] = delivery
        return DomainResult.Success(delivery)
    }

    override suspend fun getDeliveryById(
        tenantId: String,
        projectId: String,
        deliveryId: String
    ): DomainResult<CustomerFinancialDocumentDelivery?> {
        val key = "${tenantId}_${projectId}_${deliveryId}"
        return DomainResult.Success(deliveries[key])
    }

    override suspend fun getDeliveryByDocumentId(
        tenantId: String,
        projectId: String,
        documentId: String
    ): DomainResult<CustomerFinancialDocumentDelivery?> {
        val delivery = deliveries.values.firstOrNull {
            it.tenantId == tenantId && it.projectId == projectId && it.documentId == documentId
        }
        return DomainResult.Success(delivery)
    }

    override suspend fun getDeliveryByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerFinancialDocumentDelivery?> {
        val delivery = deliveries.values.firstOrNull {
            it.tenantId == tenantId && it.projectId == projectId && it.idempotencyKey == idempotencyKey
        }
        return DomainResult.Success(delivery)
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
        val filtered = deliveries.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { customerId == null || it.customerId == customerId }
            .filter { documentType == null || it.documentType == documentType }
            .filter { status == null || it.deliveryStatus == status }
            .sortedByDescending { it.createdAt }
            .drop(offset)
            .take(limit)
        return DomainResult.Success(filtered)
    }

    override suspend fun saveAuditEvent(event: CustomerFinancialDocumentDeliveryAuditEvent): DomainResult<CustomerFinancialDocumentDeliveryAuditEvent> {
        val key = "${event.tenantId}_${event.projectId}_${event.deliveryId}"
        auditEvents.computeIfAbsent(key) { CopyOnWriteArrayList() }.add(event)
        return DomainResult.Success(event)
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        deliveryId: String
    ): DomainResult<List<CustomerFinancialDocumentDeliveryAuditEvent>> {
        val key = "${tenantId}_${projectId}_${deliveryId}"
        val list = auditEvents[key]?.sortedBy { it.timestamp } ?: emptyList()
        return DomainResult.Success(list)
    }

    override suspend fun saveDocumentPayload(documentId: String, content: ByteArray): DomainResult<Unit> {
        payloads[documentId] = content
        return DomainResult.Success(Unit)
    }

    override suspend fun getDocumentPayload(documentId: String): DomainResult<ByteArray?> {
        return DomainResult.Success(payloads[documentId])
    }
}

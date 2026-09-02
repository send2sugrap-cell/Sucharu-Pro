package com.sucharu.sucharupro.data.repository.customersettlement

import com.sucharu.sucharupro.data.datasource.customersettlement.CustomerPaymentAllocationDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocation
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocationStatus
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerSettlementAuditEvent
import com.sucharu.sucharupro.domain.repository.customersettlement.CustomerPaymentAllocationRepository

/**
 * Concrete repository implementation for Customer Payment Allocations.
 */
class CustomerPaymentAllocationRepositoryImpl(
    private val dataSource: CustomerPaymentAllocationDataSource
) : CustomerPaymentAllocationRepository {

    override suspend fun createAllocation(allocation: CustomerPaymentAllocation): DomainResult<CustomerPaymentAllocation> {
        return dataSource.createAllocation(allocation)
    }

    override suspend fun getAllocationById(
        tenantId: String,
        projectId: String,
        allocationId: String
    ): DomainResult<CustomerPaymentAllocation> {
        return dataSource.getAllocationById(tenantId, projectId, allocationId)
    }

    override suspend fun findByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerPaymentAllocation?> {
        return dataSource.findByIdempotencyKey(tenantId, projectId, idempotencyKey)
    }

    override suspend fun listAllocations(
        tenantId: String,
        projectId: String,
        paymentId: String?,
        invoiceId: String?,
        customerId: String?,
        status: CustomerPaymentAllocationStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerPaymentAllocation>> {
        return dataSource.listAllocations(tenantId, projectId, paymentId, invoiceId, customerId, status, limit, offset)
    }

    override suspend fun updateAllocationStatus(
        tenantId: String,
        projectId: String,
        allocationId: String,
        newStatus: CustomerPaymentAllocationStatus,
        reversalReason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerPaymentAllocation> {
        return dataSource.updateAllocationStatus(tenantId, projectId, allocationId, newStatus, reversalReason, actorId, expectedVersion)
    }

    override suspend fun recordAuditEvent(event: CustomerSettlementAuditEvent): DomainResult<CustomerSettlementAuditEvent> {
        return dataSource.recordAuditEvent(event)
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        allocationId: String?,
        paymentId: String?,
        invoiceId: String?
    ): DomainResult<List<CustomerSettlementAuditEvent>> {
        return dataSource.getAuditEvents(tenantId, projectId, allocationId, paymentId, invoiceId)
    }
}

package com.sucharu.sucharupro.data.datasource.customersettlement

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocation
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocationStatus
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerSettlementAuditEvent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory data source for Customer Payment Allocations.
 */
class FakeCustomerPaymentAllocationDataSource : CustomerPaymentAllocationDataSource {

    private val mutex = Mutex()
    private val allocations = mutableMapOf<String, CustomerPaymentAllocation>()
    private val auditEvents = mutableListOf<CustomerSettlementAuditEvent>()

    override suspend fun createAllocation(allocation: CustomerPaymentAllocation): DomainResult<CustomerPaymentAllocation> = mutex.withLock {
        allocations[allocation.allocationId] = allocation
        DomainResult.Success(allocation)
    }

    override suspend fun getAllocationById(
        tenantId: String,
        projectId: String,
        allocationId: String
    ): DomainResult<CustomerPaymentAllocation> = mutex.withLock {
        val allocation = allocations[allocationId]
        if (allocation != null && allocation.tenantId == tenantId && allocation.projectId == projectId) {
            DomainResult.Success(allocation)
        } else {
            DomainResult.Error(IllegalArgumentException("CustomerPaymentAllocation '$allocationId' not found."))
        }
    }

    override suspend fun findByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerPaymentAllocation?> = mutex.withLock {
        val match = allocations.values.firstOrNull {
            it.tenantId == tenantId && it.projectId == projectId && it.idempotencyKey == idempotencyKey
        }
        DomainResult.Success(match)
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
    ): DomainResult<List<CustomerPaymentAllocation>> = mutex.withLock {
        val filtered = allocations.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { paymentId == null || it.paymentId == paymentId }
            .filter { invoiceId == null || it.invoiceId == invoiceId }
            .filter { customerId == null || it.customerId == customerId }
            .filter { status == null || it.status == status }
            .sortedByDescending { it.allocatedAt }
            .drop(offset)
            .take(limit)
        DomainResult.Success(filtered)
    }

    override suspend fun updateAllocationStatus(
        tenantId: String,
        projectId: String,
        allocationId: String,
        newStatus: CustomerPaymentAllocationStatus,
        reversalReason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerPaymentAllocation> = mutex.withLock {
        val existing = allocations[allocationId]
            ?: return@withLock DomainResult.Error(IllegalArgumentException("Allocation '$allocationId' not found"))

        if (existing.tenantId != tenantId || existing.projectId != projectId) {
            return@withLock DomainResult.Error(IllegalArgumentException("Allocation '$allocationId' not found"))
        }

        if (existing.version != expectedVersion) {
            return@withLock DomainResult.Error(IllegalStateException("Optimistic locking failure for allocation '$allocationId'"))
        }

        val updated = existing.copy(
            status = newStatus,
            reversalReason = reversalReason ?: existing.reversalReason,
            reversedAt = if (newStatus == CustomerPaymentAllocationStatus.REVERSED) System.currentTimeMillis() else existing.reversedAt,
            reversedBy = if (newStatus == CustomerPaymentAllocationStatus.REVERSED) actorId else existing.reversedBy,
            version = existing.version + 1
        )
        allocations[allocationId] = updated
        DomainResult.Success(updated)
    }

    override suspend fun recordAuditEvent(event: CustomerSettlementAuditEvent): DomainResult<CustomerSettlementAuditEvent> = mutex.withLock {
        auditEvents.add(event)
        DomainResult.Success(event)
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        allocationId: String?,
        paymentId: String?,
        invoiceId: String?
    ): DomainResult<List<CustomerSettlementAuditEvent>> = mutex.withLock {
        val filtered = auditEvents
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { allocationId == null || it.allocationId == allocationId }
            .filter { paymentId == null || it.paymentId == paymentId }
            .filter { invoiceId == null || it.invoiceId == invoiceId }
            .sortedByDescending { it.occurredAt }
        DomainResult.Success(filtered)
    }
}

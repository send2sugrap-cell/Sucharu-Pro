package com.sucharu.sucharupro.data.datasource.customersettlement

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocation
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocationStatus
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerSettlementAuditEvent

/**
 * Data Source interface for Customer Payment Allocations (Module 14 Step 06).
 */
interface CustomerPaymentAllocationDataSource {

    suspend fun createAllocation(allocation: CustomerPaymentAllocation): DomainResult<CustomerPaymentAllocation>

    suspend fun getAllocationById(
        tenantId: String,
        projectId: String,
        allocationId: String
    ): DomainResult<CustomerPaymentAllocation>

    suspend fun findByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerPaymentAllocation?>

    suspend fun listAllocations(
        tenantId: String,
        projectId: String,
        paymentId: String? = null,
        invoiceId: String? = null,
        customerId: String? = null,
        status: CustomerPaymentAllocationStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<CustomerPaymentAllocation>>

    suspend fun updateAllocationStatus(
        tenantId: String,
        projectId: String,
        allocationId: String,
        newStatus: CustomerPaymentAllocationStatus,
        reversalReason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerPaymentAllocation>

    suspend fun recordAuditEvent(event: CustomerSettlementAuditEvent): DomainResult<CustomerSettlementAuditEvent>

    suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        allocationId: String? = null,
        paymentId: String? = null,
        invoiceId: String? = null
    ): DomainResult<List<CustomerSettlementAuditEvent>>
}

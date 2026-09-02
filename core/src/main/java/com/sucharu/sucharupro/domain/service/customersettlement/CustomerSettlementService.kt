package com.sucharu.sucharupro.domain.service.customersettlement

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocation
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocationStatus
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerSettlementAuditEvent
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerSettlementResult
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerSettlementSummary
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerUnallocatedPayment
import com.sucharu.sucharupro.domain.model.customersettlement.InvoiceAllocationRequestItem
import java.math.BigDecimal

/**
 * Service interface for Customer Financial Settlement & Payment Allocation (Module 14 Step 06).
 */
interface CustomerSettlementService {

    suspend fun allocatePayment(
        tenantId: String,
        projectId: String,
        paymentId: String,
        invoiceId: String,
        amount: BigDecimal,
        idempotencyKey: String? = null,
        actorId: String = "system",
        actorRole: String = "STAFF"
    ): DomainResult<CustomerPaymentAllocation>

    suspend fun allocatePaymentMulti(
        tenantId: String,
        projectId: String,
        paymentId: String,
        allocations: List<InvoiceAllocationRequestItem>,
        idempotencyKey: String? = null,
        actorId: String = "system",
        actorRole: String = "STAFF"
    ): DomainResult<CustomerSettlementResult>

    suspend fun reverseAllocation(
        tenantId: String,
        projectId: String,
        allocationId: String,
        reason: String,
        actorId: String = "system",
        actorRole: String = "STAFF",
        expectedVersion: Long = 1L
    ): DomainResult<CustomerPaymentAllocation>

    suspend fun getAllocationById(
        tenantId: String,
        projectId: String,
        allocationId: String
    ): DomainResult<CustomerPaymentAllocation>

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

    suspend fun getUnallocatedPayments(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<List<CustomerUnallocatedPayment>>

    suspend fun getCustomerSettlementSummary(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerSettlementSummary>

    suspend fun getAuditHistory(
        tenantId: String,
        projectId: String,
        allocationId: String? = null,
        paymentId: String? = null,
        invoiceId: String? = null
    ): DomainResult<List<CustomerSettlementAuditEvent>>
}

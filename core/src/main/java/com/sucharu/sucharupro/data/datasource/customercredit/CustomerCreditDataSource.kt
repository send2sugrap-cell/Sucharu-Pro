package com.sucharu.sucharupro.data.datasource.customercredit

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustment
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvance
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvanceStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAllocationStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditAllocation
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditAuditEvent
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditSummary
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefund
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefundStatus
import java.math.BigDecimal

/**
 * Data source contract for Customer Advances, Credits, Allocations, Adjustments, and Refunds (Module 14 Step 04).
 */
interface CustomerCreditDataSource {

    // Advance operations
    suspend fun insertAdvance(advance: CustomerAdvance): DomainResult<CustomerAdvance>
    suspend fun findAdvanceById(tenantId: String, projectId: String, advanceId: String): DomainResult<CustomerAdvance>
    suspend fun findAdvanceByNumber(tenantId: String, advanceNumber: String): DomainResult<CustomerAdvance>
    suspend fun findAdvanceByIdempotencyKey(tenantId: String, projectId: String, idempotencyKey: String): DomainResult<CustomerAdvance?>
    suspend fun listAdvances(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        status: CustomerAdvanceStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<CustomerAdvance>>
    suspend fun updateAdvanceAllocation(
        tenantId: String,
        projectId: String,
        advanceId: String,
        newAllocatedAmount: BigDecimal,
        newAvailableAmount: BigDecimal,
        newStatus: CustomerAdvanceStatus,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerAdvance>
    suspend fun cancelAdvance(
        tenantId: String,
        projectId: String,
        advanceId: String,
        reason: String,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerAdvance>

    // Allocation operations
    suspend fun insertAllocation(allocation: CustomerCreditAllocation): DomainResult<CustomerCreditAllocation>
    suspend fun findAllocationById(tenantId: String, projectId: String, allocationId: String): DomainResult<CustomerCreditAllocation>
    suspend fun findAllocationByIdempotencyKey(tenantId: String, projectId: String, idempotencyKey: String): DomainResult<CustomerCreditAllocation?>
    suspend fun listAllocations(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        invoiceId: String? = null,
        advanceId: String? = null,
        status: CustomerAllocationStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<CustomerCreditAllocation>>
    suspend fun updateAllocationStatus(
        tenantId: String,
        projectId: String,
        allocationId: String,
        newStatus: CustomerAllocationStatus,
        reason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerCreditAllocation>

    // Adjustment operations
    suspend fun insertAdjustment(adjustment: CustomerAdjustment): DomainResult<CustomerAdjustment>
    suspend fun findAdjustmentById(tenantId: String, projectId: String, adjustmentId: String): DomainResult<CustomerAdjustment>
    suspend fun findAdjustmentByIdempotencyKey(tenantId: String, projectId: String, idempotencyKey: String): DomainResult<CustomerAdjustment?>
    suspend fun listAdjustments(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<CustomerAdjustment>>

    // Refund operations
    suspend fun insertRefund(refund: CustomerRefund): DomainResult<CustomerRefund>
    suspend fun findRefundById(tenantId: String, projectId: String, refundId: String): DomainResult<CustomerRefund>
    suspend fun findRefundByIdempotencyKey(tenantId: String, projectId: String, idempotencyKey: String): DomainResult<CustomerRefund?>
    suspend fun listRefunds(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        status: CustomerRefundStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<CustomerRefund>>
    suspend fun updateRefundStatus(
        tenantId: String,
        projectId: String,
        refundId: String,
        newStatus: CustomerRefundStatus,
        reason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerRefund>

    // Aggregations
    suspend fun getCustomerCreditSummary(tenantId: String, projectId: String, customerId: String): DomainResult<CustomerCreditSummary>

    // Audit events
    suspend fun insertAuditEvent(event: CustomerCreditAuditEvent): DomainResult<CustomerCreditAuditEvent>
    suspend fun getAuditEvents(tenantId: String, projectId: String, entityId: String): DomainResult<List<CustomerCreditAuditEvent>>
}

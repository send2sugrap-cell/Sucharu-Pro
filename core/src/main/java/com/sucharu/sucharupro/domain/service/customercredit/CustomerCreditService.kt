package com.sucharu.sucharupro.domain.service.customercredit

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustment
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustmentType
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvance
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvanceStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAllocationStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditAllocation
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditAuditEvent
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditSummary
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefund
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefundStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import java.math.BigDecimal

/**
 * Domain service contract for Customer Advance, Credit, Adjustment, and Refund operations (Module 14 Step 04).
 */
interface CustomerCreditService {

    suspend fun recordAdvance(
        tenantId: String,
        projectId: String,
        customerId: String,
        customerFinancialAccountId: String,
        amount: BigDecimal,
        currency: String = "BDT",
        paymentMethod: CustomerPaymentMethod = CustomerPaymentMethod.CASH,
        receiptDate: Long = System.currentTimeMillis(),
        referenceNumber: String? = null,
        externalReference: String? = null,
        notes: String? = null,
        idempotencyKey: String? = null,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerAdvance>

    suspend fun cancelAdvance(
        tenantId: String,
        projectId: String,
        advanceId: String,
        reason: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerAdvance>

    suspend fun allocateCreditToInvoice(
        tenantId: String,
        projectId: String,
        customerId: String,
        invoiceId: String,
        advanceId: String? = null,
        amount: BigDecimal,
        idempotencyKey: String? = null,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerCreditAllocation>

    suspend fun reverseCreditAllocation(
        tenantId: String,
        projectId: String,
        allocationId: String,
        reason: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerCreditAllocation>

    suspend fun recordAdjustment(
        tenantId: String,
        projectId: String,
        customerId: String,
        customerFinancialAccountId: String,
        adjustmentType: CustomerAdjustmentType,
        amount: BigDecimal,
        currency: String = "BDT",
        reason: String,
        referenceNumber: String? = null,
        notes: String? = null,
        idempotencyKey: String? = null,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerAdjustment>

    suspend fun requestRefund(
        tenantId: String,
        projectId: String,
        customerId: String,
        customerFinancialAccountId: String,
        paymentId: String? = null,
        advanceId: String? = null,
        amount: BigDecimal,
        currency: String = "BDT",
        refundMethod: CustomerPaymentMethod = CustomerPaymentMethod.CASH,
        reason: String,
        idempotencyKey: String? = null,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerRefund>

    suspend fun approveRefund(
        tenantId: String,
        projectId: String,
        refundId: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerRefund>

    suspend fun processRefund(
        tenantId: String,
        projectId: String,
        refundId: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerRefund>

    suspend fun completeRefund(
        tenantId: String,
        projectId: String,
        refundId: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerRefund>

    suspend fun cancelRefund(
        tenantId: String,
        projectId: String,
        refundId: String,
        reason: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerRefund>

    suspend fun getCustomerCreditSummary(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerCreditSummary>

    suspend fun listAdvances(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        status: CustomerAdvanceStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<CustomerAdvance>>

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

    suspend fun listAdjustments(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<CustomerAdjustment>>

    suspend fun listRefunds(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        status: CustomerRefundStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<CustomerRefund>>

    suspend fun getAuditHistory(
        tenantId: String,
        projectId: String,
        entityId: String
    ): DomainResult<List<CustomerCreditAuditEvent>>
}

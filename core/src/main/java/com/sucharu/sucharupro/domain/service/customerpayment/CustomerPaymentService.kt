package com.sucharu.sucharupro.domain.service.customerpayment

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentAuditEvent
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import java.math.BigDecimal

/**
 * Domain service contract for Customer Payment operations (Module 14 Step 03).
 */
interface CustomerPaymentService {

    suspend fun recordPayment(
        tenantId: String,
        projectId: String,
        customerId: String,
        customerFinancialAccountId: String,
        invoiceId: String? = null,
        amount: BigDecimal,
        currency: String = "BDT",
        paymentMethod: CustomerPaymentMethod = CustomerPaymentMethod.CASH,
        paymentDate: Long = System.currentTimeMillis(),
        referenceNumber: String? = null,
        externalReference: String? = null,
        notes: String? = null,
        idempotencyKey: String? = null,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerPayment>

    suspend fun confirmPayment(
        tenantId: String,
        projectId: String,
        paymentId: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerPayment>

    suspend fun cancelPayment(
        tenantId: String,
        projectId: String,
        paymentId: String,
        reason: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerPayment>

    suspend fun getPaymentById(
        tenantId: String,
        projectId: String,
        paymentId: String
    ): DomainResult<CustomerPayment>

    suspend fun getPaymentByNumber(
        tenantId: String,
        paymentNumber: String
    ): DomainResult<CustomerPayment>

    suspend fun listPayments(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        invoiceId: String? = null,
        status: CustomerPaymentStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<CustomerPayment>>

    suspend fun getAuditHistory(
        tenantId: String,
        projectId: String,
        paymentId: String
    ): DomainResult<List<CustomerPaymentAuditEvent>>
}

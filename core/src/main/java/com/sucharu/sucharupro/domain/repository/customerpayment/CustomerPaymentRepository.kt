package com.sucharu.sucharupro.domain.repository.customerpayment

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentAuditEvent
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus

/**
 * Repository interface contract for Customer Payments (Module 14 Step 03).
 */
interface CustomerPaymentRepository {

    suspend fun createPayment(payment: CustomerPayment): DomainResult<CustomerPayment>

    suspend fun getPaymentById(
        tenantId: String,
        projectId: String,
        paymentId: String
    ): DomainResult<CustomerPayment>

    suspend fun getPaymentByNumber(
        tenantId: String,
        paymentNumber: String
    ): DomainResult<CustomerPayment>

    suspend fun findByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerPayment?>

    suspend fun listPayments(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        invoiceId: String? = null,
        status: CustomerPaymentStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<CustomerPayment>>

    suspend fun updatePaymentStatus(
        tenantId: String,
        projectId: String,
        paymentId: String,
        newStatus: CustomerPaymentStatus,
        reason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerPayment>

    suspend fun recordAuditEvent(event: CustomerPaymentAuditEvent): DomainResult<CustomerPaymentAuditEvent>

    suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        paymentId: String
    ): DomainResult<List<CustomerPaymentAuditEvent>>
}

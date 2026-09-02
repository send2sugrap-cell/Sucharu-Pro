package com.sucharu.sucharupro.data.datasource.customerpayment

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentAuditEvent
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus

/**
 * Data source contract for Customer Payment recording and persistence (Module 14 Step 03).
 */
interface CustomerPaymentDataSource {

    suspend fun insertPayment(payment: CustomerPayment): DomainResult<CustomerPayment>

    suspend fun findPaymentById(
        tenantId: String,
        projectId: String,
        paymentId: String
    ): DomainResult<CustomerPayment>

    suspend fun findPaymentByNumber(
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

    suspend fun updateStatus(
        tenantId: String,
        projectId: String,
        paymentId: String,
        newStatus: CustomerPaymentStatus,
        cancellationReason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerPayment>

    suspend fun insertAuditEvent(event: CustomerPaymentAuditEvent): DomainResult<CustomerPaymentAuditEvent>

    suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        paymentId: String
    ): DomainResult<List<CustomerPaymentAuditEvent>>
}

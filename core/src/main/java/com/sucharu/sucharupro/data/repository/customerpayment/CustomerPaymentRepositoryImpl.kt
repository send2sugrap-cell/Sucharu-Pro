package com.sucharu.sucharupro.data.repository.customerpayment

import com.sucharu.sucharupro.data.datasource.customerpayment.CustomerPaymentDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentAuditEvent
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.repository.customerpayment.CustomerPaymentRepository

/**
 * Production implementation of [CustomerPaymentRepository] (Module 14 Step 03).
 */
class CustomerPaymentRepositoryImpl(
    private val dataSource: CustomerPaymentDataSource
) : CustomerPaymentRepository {

    override suspend fun createPayment(payment: CustomerPayment): DomainResult<CustomerPayment> {
        return dataSource.insertPayment(payment)
    }

    override suspend fun getPaymentById(
        tenantId: String,
        projectId: String,
        paymentId: String
    ): DomainResult<CustomerPayment> {
        return dataSource.findPaymentById(tenantId, projectId, paymentId)
    }

    override suspend fun getPaymentByNumber(
        tenantId: String,
        paymentNumber: String
    ): DomainResult<CustomerPayment> {
        return dataSource.findPaymentByNumber(tenantId, paymentNumber)
    }

    override suspend fun findByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerPayment?> {
        return dataSource.findByIdempotencyKey(tenantId, projectId, idempotencyKey)
    }

    override suspend fun listPayments(
        tenantId: String,
        projectId: String,
        customerId: String?,
        invoiceId: String?,
        status: CustomerPaymentStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerPayment>> {
        return dataSource.listPayments(tenantId, projectId, customerId, invoiceId, status, limit, offset)
    }

    override suspend fun updatePaymentStatus(
        tenantId: String,
        projectId: String,
        paymentId: String,
        newStatus: CustomerPaymentStatus,
        reason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerPayment> {
        return dataSource.updateStatus(tenantId, projectId, paymentId, newStatus, reason, actorId, expectedVersion)
    }

    override suspend fun recordAuditEvent(event: CustomerPaymentAuditEvent): DomainResult<CustomerPaymentAuditEvent> {
        return dataSource.insertAuditEvent(event)
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        paymentId: String
    ): DomainResult<List<CustomerPaymentAuditEvent>> {
        return dataSource.getAuditEvents(tenantId, projectId, paymentId)
    }
}

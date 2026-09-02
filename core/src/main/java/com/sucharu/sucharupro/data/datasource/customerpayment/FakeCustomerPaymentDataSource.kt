package com.sucharu.sucharupro.data.datasource.customerpayment

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentAuditEvent
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory thread-safe fake data source for Customer Payment (Module 14 Step 03).
 */
class FakeCustomerPaymentDataSource : CustomerPaymentDataSource {

    private val mutex = Mutex()
    private val payments = mutableMapOf<String, CustomerPayment>()
    private val auditEvents = mutableListOf<CustomerPaymentAuditEvent>()

    override suspend fun insertPayment(payment: CustomerPayment): DomainResult<CustomerPayment> = mutex.withLock {
        if (payments.containsKey(payment.paymentId)) {
            return DomainResult.Error(IllegalArgumentException("Payment ID '${payment.paymentId}' already exists"))
        }
        val duplicateNumber = payments.values.any {
            it.tenantId == payment.tenantId && it.paymentNumber == payment.paymentNumber
        }
        if (duplicateNumber) {
            return DomainResult.Error(IllegalArgumentException("Payment number '${payment.paymentNumber}' already exists for tenant '${payment.tenantId}'"))
        }
        payments[payment.paymentId] = payment
        DomainResult.Success(payment)
    }

    override suspend fun findPaymentById(
        tenantId: String,
        projectId: String,
        paymentId: String
    ): DomainResult<CustomerPayment> = mutex.withLock {
        val payment = payments[paymentId]
        if (payment != null && payment.tenantId == tenantId && payment.projectId == projectId) {
            DomainResult.Success(payment)
        } else {
            DomainResult.Error(IllegalArgumentException("CustomerPayment '$paymentId' not found"))
        }
    }

    override suspend fun findPaymentByNumber(
        tenantId: String,
        paymentNumber: String
    ): DomainResult<CustomerPayment> = mutex.withLock {
        val payment = payments.values.find {
            it.tenantId == tenantId && it.paymentNumber == paymentNumber
        }
        if (payment != null) {
            DomainResult.Success(payment)
        } else {
            DomainResult.Error(IllegalArgumentException("CustomerPayment with number '$paymentNumber' not found"))
        }
    }

    override suspend fun findByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerPayment?> = mutex.withLock {
        val payment = payments.values.find {
            it.tenantId == tenantId && it.projectId == projectId && it.idempotencyKey == idempotencyKey
        }
        DomainResult.Success(payment)
    }

    override suspend fun listPayments(
        tenantId: String,
        projectId: String,
        customerId: String?,
        invoiceId: String?,
        status: CustomerPaymentStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerPayment>> = mutex.withLock {
        val filtered = payments.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { customerId == null || it.customerId == customerId }
            .filter { invoiceId == null || it.invoiceId == invoiceId }
            .filter { status == null || it.status == status }
            .sortedByDescending { it.paymentDate }
            .drop(offset)
            .take(limit)
        DomainResult.Success(filtered)
    }

    override suspend fun updateStatus(
        tenantId: String,
        projectId: String,
        paymentId: String,
        newStatus: CustomerPaymentStatus,
        cancellationReason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerPayment> = mutex.withLock {
        val existing = payments[paymentId]
        if (existing == null || existing.tenantId != tenantId || existing.projectId != projectId) {
            return DomainResult.Error(IllegalArgumentException("CustomerPayment '$paymentId' not found"))
        }
        if (existing.version != expectedVersion) {
            return DomainResult.Error(
                IllegalStateException("Version conflict on CustomerPayment '$paymentId': expected $expectedVersion but found ${existing.version}")
            )
        }
        val updated = existing.copy(
            status = newStatus,
            cancellationReason = cancellationReason ?: existing.cancellationReason,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId,
            version = existing.version + 1
        )
        payments[paymentId] = updated
        DomainResult.Success(updated)
    }

    override suspend fun insertAuditEvent(event: CustomerPaymentAuditEvent): DomainResult<CustomerPaymentAuditEvent> = mutex.withLock {
        auditEvents.add(event)
        DomainResult.Success(event)
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        paymentId: String
    ): DomainResult<List<CustomerPaymentAuditEvent>> = mutex.withLock {
        val filtered = auditEvents
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.paymentId == paymentId }
            .sortedBy { it.occurredAt }
        DomainResult.Success(filtered)
    }
}

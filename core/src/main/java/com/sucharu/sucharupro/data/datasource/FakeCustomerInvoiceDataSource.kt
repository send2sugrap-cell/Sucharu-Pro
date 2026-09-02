package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceAuditEvent
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe In-Memory Fake DataSource for testing Customer Invoices.
 */
class FakeCustomerInvoiceDataSource : CustomerInvoiceDataSource {

    private val mutex = Mutex()
    private val invoices = mutableMapOf<String, CustomerInvoice>()
    private val auditEvents = mutableListOf<CustomerInvoiceAuditEvent>()

    override suspend fun insertInvoice(invoice: CustomerInvoice): DomainResult<CustomerInvoice> = mutex.withLock {
        // Enforce uniqueness of (tenantId, invoiceNumber)
        val dupNumber = invoices.values.find {
            it.tenantId == invoice.tenantId && it.invoiceNumber == invoice.invoiceNumber
        }
        if (dupNumber != null) {
            return DomainResult.Error(
                IllegalStateException("CustomerInvoice with invoice number '${invoice.invoiceNumber}' already exists")
            )
        }
        invoices[invoice.invoiceId] = invoice
        return DomainResult.Success(invoice)
    }

    override suspend fun findInvoiceById(
        tenantId: String,
        projectId: String,
        invoiceId: String
    ): DomainResult<CustomerInvoice> = mutex.withLock {
        val invoice = invoices[invoiceId]
        if (invoice != null && invoice.tenantId == tenantId && invoice.projectId == projectId) {
            DomainResult.Success(invoice)
        } else {
            DomainResult.Error(NoSuchElementException("CustomerInvoice '$invoiceId' not found"))
        }
    }

    override suspend fun findInvoiceByNumber(
        tenantId: String,
        invoiceNumber: String
    ): DomainResult<CustomerInvoice> = mutex.withLock {
        val invoice = invoices.values.find {
            it.tenantId == tenantId && it.invoiceNumber == invoiceNumber
        }
        if (invoice != null) {
            DomainResult.Success(invoice)
        } else {
            DomainResult.Error(NoSuchElementException("CustomerInvoice with number '$invoiceNumber' not found"))
        }
    }

    override suspend fun listInvoices(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerInvoiceStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerInvoice>> = mutex.withLock {
        val filtered = invoices.values.filter {
            it.tenantId == tenantId &&
            it.projectId == projectId &&
            (customerId == null || it.customerId == customerId) &&
            (status == null || it.status == status)
        }.sortedByDescending { it.createdAt }.drop(offset).take(limit)
        DomainResult.Success(filtered)
    }

    override suspend fun updateDraft(
        tenantId: String,
        projectId: String,
        invoice: CustomerInvoice,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice> = mutex.withLock {
        val existing = invoices[invoice.invoiceId]
            ?: return DomainResult.Error(NoSuchElementException("CustomerInvoice '${invoice.invoiceId}' not found"))
        if (existing.tenantId != tenantId || existing.projectId != projectId) {
            return DomainResult.Error(NoSuchElementException("CustomerInvoice '${invoice.invoiceId}' not found in scope"))
        }
        if (existing.version != expectedVersion) {
            return DomainResult.Error(
                IllegalStateException("Optimistic lock conflict for invoice '${invoice.invoiceId}'. Expected version $expectedVersion, found ${existing.version}")
            )
        }
        if (existing.status != CustomerInvoiceStatus.DRAFT) {
            return DomainResult.Error(
                IllegalStateException("Cannot modify non-draft invoice '${invoice.invoiceId}' with status ${existing.status}")
            )
        }
        val updated = invoice.copy(
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1
        )
        invoices[invoice.invoiceId] = updated
        DomainResult.Success(updated)
    }

    override suspend fun updateStatus(
        tenantId: String,
        projectId: String,
        invoiceId: String,
        newStatus: CustomerInvoiceStatus,
        reason: String?,
        actorId: String,
        issueDate: Long?,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice> = mutex.withLock {
        val existing = invoices[invoiceId]
            ?: return DomainResult.Error(NoSuchElementException("CustomerInvoice '$invoiceId' not found"))
        if (existing.tenantId != tenantId || existing.projectId != projectId) {
            return DomainResult.Error(NoSuchElementException("CustomerInvoice '$invoiceId' not found in scope"))
        }
        if (existing.version != expectedVersion) {
            return DomainResult.Error(
                IllegalStateException("Optimistic lock conflict for invoice '$invoiceId'. Expected version $expectedVersion, found ${existing.version}")
            )
        }
        val updated = existing.copy(
            status = newStatus,
            issueDate = issueDate ?: existing.issueDate,
            cancellationReason = if (newStatus in setOf(CustomerInvoiceStatus.CANCELLED, CustomerInvoiceStatus.VOID)) reason else existing.cancellationReason,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId,
            version = existing.version + 1
        )
        invoices[invoiceId] = updated
        DomainResult.Success(updated)
    }

    override suspend fun updatePaymentBalance(
        tenantId: String,
        projectId: String,
        invoiceId: String,
        newPaidAmount: java.math.BigDecimal,
        newDueAmount: java.math.BigDecimal,
        newStatus: CustomerInvoiceStatus,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice> = mutex.withLock {
        val existing = invoices[invoiceId]
            ?: return DomainResult.Error(NoSuchElementException("CustomerInvoice '$invoiceId' not found"))
        if (existing.tenantId != tenantId || existing.projectId != projectId) {
            return DomainResult.Error(NoSuchElementException("CustomerInvoice '$invoiceId' not found in scope"))
        }
        if (existing.version != expectedVersion) {
            return DomainResult.Error(
                IllegalStateException("Optimistic lock conflict for invoice '$invoiceId'. Expected version $expectedVersion, found ${existing.version}")
            )
        }
        val updated = existing.copy(
            paidAmount = newPaidAmount,
            dueAmount = newDueAmount,
            status = newStatus,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId,
            version = existing.version + 1
        )
        invoices[invoiceId] = updated
        DomainResult.Success(updated)
    }

    override suspend fun insertAuditEvent(event: CustomerInvoiceAuditEvent): DomainResult<CustomerInvoiceAuditEvent> = mutex.withLock {
        auditEvents.add(event)
        DomainResult.Success(event)
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        invoiceId: String
    ): DomainResult<List<CustomerInvoiceAuditEvent>> = mutex.withLock {
        val filtered = auditEvents.filter {
            it.tenantId == tenantId && it.projectId == projectId && it.invoiceId == invoiceId
        }.sortedByDescending { it.occurredAt }
        DomainResult.Success(filtered)
    }
}

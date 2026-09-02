package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.CustomerInvoiceDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceAuditEvent
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.repository.customerinvoice.CustomerInvoiceRepository

/**
 * Production repository implementation for Customer Invoices (Module 14 Step 02).
 */
class CustomerInvoiceRepositoryImpl(
    private val dataSource: CustomerInvoiceDataSource
) : CustomerInvoiceRepository {

    override suspend fun createInvoice(invoice: CustomerInvoice): DomainResult<CustomerInvoice> {
        return dataSource.insertInvoice(invoice)
    }

    override suspend fun getInvoiceById(
        tenantId: String,
        projectId: String,
        invoiceId: String
    ): DomainResult<CustomerInvoice> {
        return dataSource.findInvoiceById(tenantId, projectId, invoiceId)
    }

    override suspend fun getInvoiceByNumber(
        tenantId: String,
        invoiceNumber: String
    ): DomainResult<CustomerInvoice> {
        return dataSource.findInvoiceByNumber(tenantId, invoiceNumber)
    }

    override suspend fun listInvoices(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerInvoiceStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerInvoice>> {
        return dataSource.listInvoices(tenantId, projectId, customerId, status, limit, offset)
    }

    override suspend fun updateDraftInvoice(
        tenantId: String,
        projectId: String,
        invoice: CustomerInvoice,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice> {
        return dataSource.updateDraft(tenantId, projectId, invoice, expectedVersion)
    }

    override suspend fun updateInvoiceStatus(
        tenantId: String,
        projectId: String,
        invoiceId: String,
        newStatus: CustomerInvoiceStatus,
        reason: String?,
        actorId: String,
        issueDate: Long?,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice> {
        return dataSource.updateStatus(tenantId, projectId, invoiceId, newStatus, reason, actorId, issueDate, expectedVersion)
    }

    override suspend fun updateInvoicePayment(
        tenantId: String,
        projectId: String,
        invoiceId: String,
        newPaidAmount: java.math.BigDecimal,
        newDueAmount: java.math.BigDecimal,
        newStatus: CustomerInvoiceStatus,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice> {
        return dataSource.updatePaymentBalance(tenantId, projectId, invoiceId, newPaidAmount, newDueAmount, newStatus, actorId, expectedVersion)
    }

    override suspend fun recordAuditEvent(event: CustomerInvoiceAuditEvent): DomainResult<CustomerInvoiceAuditEvent> {
        return dataSource.insertAuditEvent(event)
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        invoiceId: String
    ): DomainResult<List<CustomerInvoiceAuditEvent>> {
        return dataSource.getAuditEvents(tenantId, projectId, invoiceId)
    }
}

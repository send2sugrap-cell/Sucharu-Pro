package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceAuditEvent
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus

/**
 * Data source contract for Customer Invoices (Module 14 Step 02).
 */
interface CustomerInvoiceDataSource {

    suspend fun insertInvoice(invoice: CustomerInvoice): DomainResult<CustomerInvoice>

    suspend fun findInvoiceById(
        tenantId: String,
        projectId: String,
        invoiceId: String
    ): DomainResult<CustomerInvoice>

    suspend fun findInvoiceByNumber(
        tenantId: String,
        invoiceNumber: String
    ): DomainResult<CustomerInvoice>

    suspend fun listInvoices(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerInvoiceStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerInvoice>>

    suspend fun updateDraft(
        tenantId: String,
        projectId: String,
        invoice: CustomerInvoice,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice>

    suspend fun updateStatus(
        tenantId: String,
        projectId: String,
        invoiceId: String,
        newStatus: CustomerInvoiceStatus,
        reason: String?,
        actorId: String,
        issueDate: Long?,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice>

    suspend fun updatePaymentBalance(
        tenantId: String,
        projectId: String,
        invoiceId: String,
        newPaidAmount: java.math.BigDecimal,
        newDueAmount: java.math.BigDecimal,
        newStatus: CustomerInvoiceStatus,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice>

    suspend fun insertAuditEvent(event: CustomerInvoiceAuditEvent): DomainResult<CustomerInvoiceAuditEvent>

    suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        invoiceId: String
    ): DomainResult<List<CustomerInvoiceAuditEvent>>
}

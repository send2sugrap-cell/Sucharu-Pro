package com.sucharu.sucharupro.domain.repository.customerinvoice

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceAuditEvent
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceLine
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus

/**
 * Repository interface contract for Customer Invoices (Module 14 Step 02).
 */
interface CustomerInvoiceRepository {

    suspend fun createInvoice(invoice: CustomerInvoice): DomainResult<CustomerInvoice>

    suspend fun getInvoiceById(
        tenantId: String,
        projectId: String,
        invoiceId: String
    ): DomainResult<CustomerInvoice>

    suspend fun getInvoiceByNumber(
        tenantId: String,
        invoiceNumber: String
    ): DomainResult<CustomerInvoice>

    suspend fun listInvoices(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        status: CustomerInvoiceStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<CustomerInvoice>>

    suspend fun updateDraftInvoice(
        tenantId: String,
        projectId: String,
        invoice: CustomerInvoice,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice>

    suspend fun updateInvoiceStatus(
        tenantId: String,
        projectId: String,
        invoiceId: String,
        newStatus: CustomerInvoiceStatus,
        reason: String?,
        actorId: String,
        issueDate: Long? = null,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice>

    suspend fun updateInvoicePayment(
        tenantId: String,
        projectId: String,
        invoiceId: String,
        newPaidAmount: java.math.BigDecimal,
        newDueAmount: java.math.BigDecimal,
        newStatus: CustomerInvoiceStatus,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice>

    suspend fun recordAuditEvent(event: CustomerInvoiceAuditEvent): DomainResult<CustomerInvoiceAuditEvent>

    suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        invoiceId: String
    ): DomainResult<List<CustomerInvoiceAuditEvent>>
}

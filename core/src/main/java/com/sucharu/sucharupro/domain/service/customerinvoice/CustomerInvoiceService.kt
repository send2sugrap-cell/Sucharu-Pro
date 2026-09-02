package com.sucharu.sucharupro.domain.service.customerinvoice

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceAuditEvent
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceLine
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import java.math.BigDecimal

/**
 * Domain Service for Customer Invoices & Receivable Foundation (Module 14 Step 02).
 */
interface CustomerInvoiceService {

    suspend fun createDraftInvoice(
        tenantId: String,
        projectId: String,
        customerId: String,
        customerFinancialAccountId: String,
        sourceOrderId: String? = null,
        sourceJobId: String? = null,
        dueDate: Long? = null,
        currency: String = "BDT",
        lines: List<CustomerInvoiceLine>,
        discount: BigDecimal = BigDecimal.ZERO,
        tax: BigDecimal = BigDecimal.ZERO,
        adjustment: BigDecimal = BigDecimal.ZERO,
        notes: String? = null,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerInvoice>

    suspend fun updateDraftInvoice(
        tenantId: String,
        projectId: String,
        invoiceId: String,
        lines: List<CustomerInvoiceLine>,
        discount: BigDecimal = BigDecimal.ZERO,
        tax: BigDecimal = BigDecimal.ZERO,
        adjustment: BigDecimal = BigDecimal.ZERO,
        notes: String? = null,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice>

    suspend fun issueInvoice(
        tenantId: String,
        projectId: String,
        invoiceId: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice>

    suspend fun cancelInvoice(
        tenantId: String,
        projectId: String,
        invoiceId: String,
        reason: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice>

    suspend fun voidInvoice(
        tenantId: String,
        projectId: String,
        invoiceId: String,
        reason: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice>

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

    suspend fun getAuditHistory(
        tenantId: String,
        projectId: String,
        invoiceId: String
    ): DomainResult<List<CustomerInvoiceAuditEvent>>
}

package com.sucharu.sucharupro.domain.service.customerinvoice

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceAuditEvent
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceLine
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import com.sucharu.sucharupro.domain.repository.customerfinancial.CustomerFinancialAccountRepository
import com.sucharu.sucharupro.domain.repository.customerinvoice.CustomerInvoiceRepository
import com.sucharu.sucharupro.domain.validation.customerinvoice.CustomerInvoiceCalculator
import com.sucharu.sucharupro.domain.validation.customerinvoice.CustomerInvoiceValidator
import java.math.BigDecimal
import java.util.UUID

/**
 * Production implementation of [CustomerInvoiceService] (Module 14 Step 02).
 */
class CustomerInvoiceServiceImpl(
    private val repository: CustomerInvoiceRepository,
    private val customerRepository: CustomerRepository,
    private val accountRepository: CustomerFinancialAccountRepository
) : CustomerInvoiceService {

    override suspend fun createDraftInvoice(
        tenantId: String,
        projectId: String,
        customerId: String,
        customerFinancialAccountId: String,
        sourceOrderId: String?,
        sourceJobId: String?,
        dueDate: Long?,
        currency: String,
        lines: List<CustomerInvoiceLine>,
        discount: BigDecimal,
        tax: BigDecimal,
        adjustment: BigDecimal,
        notes: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerInvoice> {
        // 1. Verify customer exists
        val customerRes = customerRepository.findCustomerById(customerId)
        if (customerRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("Customer '$customerId' not found: ${customerRes.message}"))
        }

        // 2. Verify financial account exists and is ACTIVE
        val accountRes = accountRepository.getAccountById(tenantId, projectId, customerFinancialAccountId)
        if (accountRes is DomainResult.Error) {
            return DomainResult.Error(IllegalArgumentException("CustomerFinancialAccount '$customerFinancialAccountId' not found"))
        }
        val account = (accountRes as DomainResult.Success).data

        // 3. Validate creation
        val valRes = CustomerInvoiceValidator.validateDraftCreation(
            tenantId, projectId, customerId, customerFinancialAccountId, currency, lines, account
        )
        if (valRes is DomainResult.Error) return valRes

        // 4. Calculate line totals and grand totals
        val processedLines = lines.mapIndexed { idx, line ->
            val calcLineTotal = CustomerInvoiceCalculator.calculateLineTotal(
                quantity = line.quantity,
                unitPrice = line.unitPrice,
                lineDiscount = line.discount,
                lineTax = line.tax
            )
            line.copy(
                lineId = if (line.lineId.isBlank()) "CIL-${UUID.randomUUID().toString().take(8).uppercase()}" else line.lineId,
                lineTotal = calcLineTotal,
                lineOrder = idx + 1
            )
        }

        val totals = CustomerInvoiceCalculator.calculateInvoiceTotals(
            lines = processedLines,
            invoiceDiscount = discount,
            invoiceTax = tax,
            adjustment = adjustment,
            paidAmount = BigDecimal.ZERO
        )

        val invoiceId = "INV-${UUID.randomUUID().toString().take(8).uppercase()}"
        val invoiceNumber = "INV-${System.currentTimeMillis().toString().takeLast(6)}-${UUID.randomUUID().toString().take(4).uppercase()}"
        val now = System.currentTimeMillis()

        val invoice = CustomerInvoice(
            invoiceId = invoiceId,
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = customerFinancialAccountId,
            invoiceNumber = invoiceNumber,
            sourceOrderId = sourceOrderId,
            sourceJobId = sourceJobId,
            issueDate = null,
            dueDate = dueDate,
            currency = currency.uppercase(),
            subtotal = totals.subtotal,
            discount = totals.discount,
            tax = totals.tax,
            adjustment = totals.adjustment,
            grandTotal = totals.grandTotal,
            paidAmount = totals.paidAmount,
            dueAmount = totals.dueAmount,
            status = CustomerInvoiceStatus.DRAFT,
            lines = processedLines.map { it.copy(invoiceId = invoiceId, tenantId = tenantId, projectId = projectId) },
            notes = notes,
            createdAt = now,
            createdBy = actorId,
            updatedAt = now,
            updatedBy = actorId,
            version = 1L
        )

        val createdRes = repository.createInvoice(invoice)
        if (createdRes is DomainResult.Success) {
            repository.recordAuditEvent(
                CustomerInvoiceAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    invoiceId = invoiceId,
                    customerId = customerId,
                    tenantId = tenantId,
                    projectId = projectId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "INVOICE_DRAFT_CREATED",
                    previousStatus = null,
                    newStatus = CustomerInvoiceStatus.DRAFT,
                    reason = "Draft invoice created",
                    occurredAt = now,
                    metadataJson = """{"invoiceNumber":"$invoiceNumber","grandTotal":"${totals.grandTotal}"}"""
                )
            )
        }
        return createdRes
    }

    override suspend fun updateDraftInvoice(
        tenantId: String,
        projectId: String,
        invoiceId: String,
        lines: List<CustomerInvoiceLine>,
        discount: BigDecimal,
        tax: BigDecimal,
        adjustment: BigDecimal,
        notes: String?,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice> {
        val existingRes = repository.getInvoiceById(tenantId, projectId, invoiceId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data

        if (existing.status != CustomerInvoiceStatus.DRAFT) {
            return DomainResult.Error(
                IllegalStateException("Cannot modify invoice '$invoiceId' because it is in status ${existing.status}")
            )
        }

        val processedLines = lines.mapIndexed { idx, line ->
            val calcLineTotal = CustomerInvoiceCalculator.calculateLineTotal(
                quantity = line.quantity,
                unitPrice = line.unitPrice,
                lineDiscount = line.discount,
                lineTax = line.tax
            )
            line.copy(
                lineId = if (line.lineId.isBlank()) "CIL-${UUID.randomUUID().toString().take(8).uppercase()}" else line.lineId,
                invoiceId = invoiceId,
                tenantId = tenantId,
                projectId = projectId,
                lineTotal = calcLineTotal,
                lineOrder = idx + 1
            )
        }

        val totals = CustomerInvoiceCalculator.calculateInvoiceTotals(
            lines = processedLines,
            invoiceDiscount = discount,
            invoiceTax = tax,
            adjustment = adjustment,
            paidAmount = existing.paidAmount
        )

        val updatedInvoice = existing.copy(
            subtotal = totals.subtotal,
            discount = totals.discount,
            tax = totals.tax,
            adjustment = totals.adjustment,
            grandTotal = totals.grandTotal,
            dueAmount = totals.dueAmount,
            lines = processedLines,
            notes = notes ?: existing.notes,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId
        )

        val updateRes = repository.updateDraftInvoice(tenantId, projectId, updatedInvoice, expectedVersion)
        if (updateRes is DomainResult.Success) {
            repository.recordAuditEvent(
                CustomerInvoiceAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    invoiceId = invoiceId,
                    customerId = existing.customerId,
                    tenantId = tenantId,
                    projectId = projectId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "INVOICE_DRAFT_UPDATED",
                    previousStatus = CustomerInvoiceStatus.DRAFT,
                    newStatus = CustomerInvoiceStatus.DRAFT,
                    reason = "Draft line items and totals updated",
                    occurredAt = System.currentTimeMillis(),
                    metadataJson = """{"grandTotal":"${totals.grandTotal}"}"""
                )
            )
        }
        return updateRes
    }

    override suspend fun issueInvoice(
        tenantId: String,
        projectId: String,
        invoiceId: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice> {
        val existingRes = repository.getInvoiceById(tenantId, projectId, invoiceId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data

        val valRes = CustomerInvoiceValidator.validateStatusTransition(
            existing,
            CustomerInvoiceStatus.ISSUED,
            null
        )
        if (valRes is DomainResult.Error) return valRes

        val now = System.currentTimeMillis()
        val updateRes = repository.updateInvoiceStatus(
            tenantId, projectId, invoiceId,
            CustomerInvoiceStatus.ISSUED, null, actorId, issueDate = now, expectedVersion = expectedVersion
        )
        if (updateRes is DomainResult.Success) {
            repository.recordAuditEvent(
                CustomerInvoiceAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    invoiceId = invoiceId,
                    customerId = existing.customerId,
                    tenantId = tenantId,
                    projectId = projectId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "INVOICE_ISSUED",
                    previousStatus = existing.status,
                    newStatus = CustomerInvoiceStatus.ISSUED,
                    reason = "Invoice issued and receivable established",
                    occurredAt = now,
                    metadataJson = """{"receivableDue":"${existing.dueAmount}"}"""
                )
            )
        }
        return updateRes
    }

    override suspend fun cancelInvoice(
        tenantId: String,
        projectId: String,
        invoiceId: String,
        reason: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice> {
        val existingRes = repository.getInvoiceById(tenantId, projectId, invoiceId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data

        val valRes = CustomerInvoiceValidator.validateStatusTransition(
            existing,
            CustomerInvoiceStatus.CANCELLED,
            reason
        )
        if (valRes is DomainResult.Error) return valRes

        val updateRes = repository.updateInvoiceStatus(
            tenantId, projectId, invoiceId,
            CustomerInvoiceStatus.CANCELLED, reason, actorId, issueDate = null, expectedVersion = expectedVersion
        )
        if (updateRes is DomainResult.Success) {
            repository.recordAuditEvent(
                CustomerInvoiceAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    invoiceId = invoiceId,
                    customerId = existing.customerId,
                    tenantId = tenantId,
                    projectId = projectId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "INVOICE_CANCELLED",
                    previousStatus = existing.status,
                    newStatus = CustomerInvoiceStatus.CANCELLED,
                    reason = reason,
                    occurredAt = System.currentTimeMillis()
                )
            )
        }
        return updateRes
    }

    override suspend fun voidInvoice(
        tenantId: String,
        projectId: String,
        invoiceId: String,
        reason: String,
        actorId: String,
        actorRole: String,
        expectedVersion: Long
    ): DomainResult<CustomerInvoice> {
        val existingRes = repository.getInvoiceById(tenantId, projectId, invoiceId)
        if (existingRes is DomainResult.Error) return existingRes
        val existing = (existingRes as DomainResult.Success).data

        val valRes = CustomerInvoiceValidator.validateStatusTransition(
            existing,
            CustomerInvoiceStatus.VOID,
            reason
        )
        if (valRes is DomainResult.Error) return valRes

        val updateRes = repository.updateInvoiceStatus(
            tenantId, projectId, invoiceId,
            CustomerInvoiceStatus.VOID, reason, actorId, issueDate = null, expectedVersion = expectedVersion
        )
        if (updateRes is DomainResult.Success) {
            repository.recordAuditEvent(
                CustomerInvoiceAuditEvent(
                    auditId = "AUD-${UUID.randomUUID().toString().take(8).uppercase()}",
                    invoiceId = invoiceId,
                    customerId = existing.customerId,
                    tenantId = tenantId,
                    projectId = projectId,
                    actorId = actorId,
                    actorRole = actorRole,
                    action = "INVOICE_VOIDED",
                    previousStatus = existing.status,
                    newStatus = CustomerInvoiceStatus.VOID,
                    reason = reason,
                    occurredAt = System.currentTimeMillis()
                )
            )
        }
        return updateRes
    }

    override suspend fun getInvoiceById(
        tenantId: String,
        projectId: String,
        invoiceId: String
    ): DomainResult<CustomerInvoice> {
        return repository.getInvoiceById(tenantId, projectId, invoiceId)
    }

    override suspend fun getInvoiceByNumber(
        tenantId: String,
        invoiceNumber: String
    ): DomainResult<CustomerInvoice> {
        return repository.getInvoiceByNumber(tenantId, invoiceNumber)
    }

    override suspend fun listInvoices(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerInvoiceStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerInvoice>> {
        return repository.listInvoices(tenantId, projectId, customerId, status, limit, offset)
    }

    override suspend fun getAuditHistory(
        tenantId: String,
        projectId: String,
        invoiceId: String
    ): DomainResult<List<CustomerInvoiceAuditEvent>> {
        return repository.getAuditEvents(tenantId, projectId, invoiceId)
    }
}

package com.sucharu.sucharupro.domain.service.customerledger

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustmentStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustmentType
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvanceStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAllocationStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefundStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerledger.CustomerLedgerAuditEvent
import com.sucharu.sucharupro.domain.model.customerledger.CustomerLedgerEntry
import com.sucharu.sucharupro.domain.model.customerledger.CustomerLedgerEntryType
import com.sucharu.sucharupro.domain.model.customerledger.CustomerReceivableReconciliation
import com.sucharu.sucharupro.domain.model.customerledger.CustomerStatement
import com.sucharu.sucharupro.domain.model.customerledger.CustomerStatementSummary
import com.sucharu.sucharupro.domain.model.customerledger.ReceivableReconciliationStatus
import com.sucharu.sucharupro.domain.model.customerledger.ReconciliationDiscrepancy
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import com.sucharu.sucharupro.domain.repository.customercredit.CustomerCreditRepository
import com.sucharu.sucharupro.domain.repository.customerfinancial.CustomerFinancialAccountRepository
import com.sucharu.sucharupro.domain.repository.customerinvoice.CustomerInvoiceRepository
import com.sucharu.sucharupro.domain.repository.customerledger.CustomerLedgerRepository
import com.sucharu.sucharupro.domain.repository.customerpayment.CustomerPaymentRepository
import com.sucharu.sucharupro.domain.validation.customerledger.CustomerLedgerValidator
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Production implementation of [CustomerLedgerService] (Module 14 Step 05).
 */
class CustomerLedgerServiceImpl(
    private val ledgerRepository: CustomerLedgerRepository,
    private val accountRepository: CustomerFinancialAccountRepository,
    private val invoiceRepository: CustomerInvoiceRepository,
    private val paymentRepository: CustomerPaymentRepository,
    private val creditRepository: CustomerCreditRepository,
    private val customerRepository: CustomerRepository
) : CustomerLedgerService {

    private suspend fun buildFullChronologicalLedger(
        tenantId: String,
        projectId: String,
        customerId: String,
        financialAccountId: String
    ): DomainResult<List<CustomerLedgerEntry>> {
        val rawEntries = mutableListOf<CustomerLedgerEntry>()

        // 1. Invoices
        when (val invRes = invoiceRepository.listInvoices(tenantId, projectId, customerId, null, 10000, 0)) {
            is DomainResult.Success -> {
                invRes.data
                    .filter { it.status != CustomerInvoiceStatus.VOID && it.status != CustomerInvoiceStatus.CANCELLED && it.status != CustomerInvoiceStatus.DRAFT }
                    .forEach { inv ->
                        rawEntries.add(
                            CustomerLedgerEntry(
                                entryId = "LED-INV-${inv.invoiceId}",
                                tenantId = tenantId,
                                projectId = projectId,
                                customerId = customerId,
                                customerFinancialAccountId = financialAccountId,
                                effectiveAt = inv.issueDate ?: inv.createdAt,
                                entryType = CustomerLedgerEntryType.INVOICE,
                                referenceType = "INVOICE",
                                referenceId = inv.invoiceId,
                                referenceNumber = inv.invoiceNumber,
                                description = "Customer Invoice #${inv.invoiceNumber}",
                                debitAmount = inv.grandTotal.setScale(4, RoundingMode.HALF_UP),
                                creditAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                                currency = inv.currency,
                                sourceTransactionId = inv.invoiceId,
                                metadataJson = """{"invoiceNumber":"${inv.invoiceNumber}","status":"${inv.status.name}"}"""
                            )
                        )
                    }
            }
            is DomainResult.Error -> return invRes
            DomainResult.Loading -> {}
        }

        // 2. Payments
        when (val payRes = paymentRepository.listPayments(tenantId, projectId, customerId, null, null, 10000, 0)) {
            is DomainResult.Success -> {
                payRes.data
                    .filter { it.status != CustomerPaymentStatus.CANCELLED }
                    .forEach { pay ->
                        rawEntries.add(
                            CustomerLedgerEntry(
                                entryId = "LED-PAY-${pay.paymentId}",
                                tenantId = tenantId,
                                projectId = projectId,
                                customerId = customerId,
                                customerFinancialAccountId = financialAccountId,
                                effectiveAt = pay.paymentDate,
                                entryType = CustomerLedgerEntryType.PAYMENT,
                                referenceType = "PAYMENT",
                                referenceId = pay.paymentId,
                                referenceNumber = pay.paymentNumber,
                                description = "Payment Received (${pay.paymentMethod.name}) Ref #${pay.paymentNumber}",
                                debitAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                                creditAmount = pay.amount.setScale(4, RoundingMode.HALF_UP),
                                currency = pay.currency,
                                sourceTransactionId = pay.paymentId,
                                metadataJson = """{"paymentNumber":"${pay.paymentNumber}","method":"${pay.paymentMethod.name}"}"""
                            )
                        )
                    }
            }
            is DomainResult.Error -> return payRes
            DomainResult.Loading -> {}
        }

        // 3. Advances
        when (val advRes = creditRepository.listAdvances(tenantId, projectId, customerId, null, 10000, 0)) {
            is DomainResult.Success -> {
                advRes.data
                    .filter { it.status != CustomerAdvanceStatus.CANCELLED }
                    .forEach { adv ->
                        rawEntries.add(
                            CustomerLedgerEntry(
                                entryId = "LED-ADV-${adv.advanceId}",
                                tenantId = tenantId,
                                projectId = projectId,
                                customerId = customerId,
                                customerFinancialAccountId = financialAccountId,
                                effectiveAt = adv.receiptDate,
                                entryType = CustomerLedgerEntryType.ADVANCE,
                                referenceType = "ADVANCE",
                                referenceId = adv.advanceId,
                                referenceNumber = adv.advanceNumber,
                                description = "Advance Received (${adv.paymentMethod.name}) Ref #${adv.advanceNumber}",
                                debitAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                                creditAmount = adv.amount.setScale(4, RoundingMode.HALF_UP),
                                currency = adv.currency,
                                sourceTransactionId = adv.advanceId,
                                metadataJson = """{"advanceNumber":"${adv.advanceNumber}","method":"${adv.paymentMethod.name}"}"""
                            )
                        )
                    }
            }
            is DomainResult.Error -> return advRes
            DomainResult.Loading -> {}
        }

        // 4. Adjustments
        when (val adjRes = creditRepository.listAdjustments(tenantId, projectId, customerId, 10000, 0)) {
            is DomainResult.Success -> {
                adjRes.data
                    .filter { it.status == CustomerAdjustmentStatus.APPLIED }
                    .forEach { adj ->
                        val isDebit = adj.adjustmentType == CustomerAdjustmentType.DEBIT
                        rawEntries.add(
                            CustomerLedgerEntry(
                                entryId = "LED-ADJ-${adj.adjustmentId}",
                                tenantId = tenantId,
                                projectId = projectId,
                                customerId = customerId,
                                customerFinancialAccountId = financialAccountId,
                                effectiveAt = adj.createdAt,
                                entryType = if (isDebit) CustomerLedgerEntryType.DEBIT_ADJUSTMENT else CustomerLedgerEntryType.CREDIT_ADJUSTMENT,
                                referenceType = "ADJUSTMENT",
                                referenceId = adj.adjustmentId,
                                referenceNumber = adj.adjustmentNumber,
                                description = "Account Adjustment (${adj.adjustmentType.name}) - ${adj.reason}",
                                debitAmount = if (isDebit) adj.amount.setScale(4, RoundingMode.HALF_UP) else BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                                creditAmount = if (!isDebit) adj.amount.setScale(4, RoundingMode.HALF_UP) else BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                                currency = adj.currency,
                                sourceTransactionId = adj.adjustmentId,
                                metadataJson = """{"adjustmentNumber":"${adj.adjustmentNumber}","type":"${adj.adjustmentType.name}"}"""
                            )
                        )
                    }
            }
            is DomainResult.Error -> return adjRes
            DomainResult.Loading -> {}
        }

        // 5. Refunds
        when (val refRes = creditRepository.listRefunds(tenantId, projectId, customerId, null, 10000, 0)) {
            is DomainResult.Success -> {
                refRes.data
                    .filter { it.status in setOf(CustomerRefundStatus.APPROVED, CustomerRefundStatus.PROCESSED, CustomerRefundStatus.COMPLETED) }
                    .forEach { ref ->
                        rawEntries.add(
                            CustomerLedgerEntry(
                                entryId = "LED-REF-${ref.refundId}",
                                tenantId = tenantId,
                                projectId = projectId,
                                customerId = customerId,
                                customerFinancialAccountId = financialAccountId,
                                effectiveAt = ref.processedAt ?: ref.approvedAt ?: ref.createdAt,
                                entryType = CustomerLedgerEntryType.REFUND,
                                referenceType = "REFUND",
                                referenceId = ref.refundId,
                                referenceNumber = ref.refundNumber,
                                description = "Refund Disbursement (${ref.refundMethod.name}) - ${ref.reason}",
                                debitAmount = ref.amount.setScale(4, RoundingMode.HALF_UP),
                                creditAmount = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                                currency = ref.currency,
                                sourceTransactionId = ref.refundId,
                                metadataJson = """{"refundNumber":"${ref.refundNumber}","status":"${ref.status.name}"}"""
                            )
                        )
                    }
            }
            is DomainResult.Error -> return refRes
            DomainResult.Loading -> {}
        }

        // 6. Sort deterministically and calculate running balances
        val sorted = rawEntries.sortedWith(
            compareBy<CustomerLedgerEntry> { it.effectiveAt }
                .thenBy { it.entryType.ordinal }
                .thenBy { it.entryId }
        )

        var runningBalance = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        val calculated = sorted.map { entry ->
            runningBalance = runningBalance.add(entry.debitAmount).subtract(entry.creditAmount).setScale(4, RoundingMode.HALF_UP)
            entry.copy(balanceAfter = runningBalance)
        }

        return DomainResult.Success(calculated)
    }

    override suspend fun getCustomerLedger(
        tenantId: String,
        projectId: String,
        customerId: String,
        fromDate: Long?,
        toDate: Long?,
        entryType: CustomerLedgerEntryType?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerLedgerEntry>> {
        val pageVal = CustomerLedgerValidator.validatePagination(limit, offset)
        if (pageVal is DomainResult.Error) return pageVal

        val accountRes = accountRepository.getAccountByCustomerId(tenantId, projectId, customerId)
        val account = when (accountRes) {
            is DomainResult.Success -> accountRes.data
            is DomainResult.Error -> return accountRes
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val valRes = CustomerLedgerValidator.validateStatementQuery(tenantId, projectId, customerId, fromDate, toDate, account)
        if (valRes is DomainResult.Error) return valRes

        val fullLedgerRes = buildFullChronologicalLedger(tenantId, projectId, customerId, account.financialAccountId)
        val fullLedger = when (fullLedgerRes) {
            is DomainResult.Success -> fullLedgerRes.data
            is DomainResult.Error -> return fullLedgerRes
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val filtered = fullLedger
            .filter { fromDate == null || it.effectiveAt >= fromDate }
            .filter { toDate == null || it.effectiveAt <= toDate }
            .filter { entryType == null || it.entryType == entryType }
            .drop(offset)
            .take(limit)

        return DomainResult.Success(filtered)
    }

    override suspend fun getCustomerStatement(
        tenantId: String,
        projectId: String,
        customerId: String,
        fromDate: Long?,
        toDate: Long?
    ): DomainResult<CustomerStatement> {
        val accountRes = accountRepository.getAccountByCustomerId(tenantId, projectId, customerId)
        val account = when (accountRes) {
            is DomainResult.Success -> accountRes.data
            is DomainResult.Error -> return accountRes
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val customerRes = customerRepository.findCustomerById(customerId)
        val customerName = if (customerRes is DomainResult.Success) customerRes.data.displayName else "Customer $customerId"

        val valRes = CustomerLedgerValidator.validateStatementQuery(tenantId, projectId, customerId, fromDate, toDate, account)
        if (valRes is DomainResult.Error) return valRes

        val fullLedgerRes = buildFullChronologicalLedger(tenantId, projectId, customerId, account.financialAccountId)
        val fullLedger = when (fullLedgerRes) {
            is DomainResult.Success -> fullLedgerRes.data
            is DomainResult.Error -> return fullLedgerRes
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val effectiveFrom = fromDate ?: 0L
        val effectiveTo = toDate ?: Long.MAX_VALUE

        val priorEntries = fullLedger.filter { it.effectiveAt < effectiveFrom }
        val openingBalance = if (priorEntries.isEmpty()) {
            BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        } else {
            priorEntries.last().balanceAfter
        }

        val periodEntries = fullLedger.filter { it.effectiveAt in effectiveFrom..effectiveTo }
        var totalDebit = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var totalCredit = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)

        periodEntries.forEach { entry ->
            totalDebit = totalDebit.add(entry.debitAmount).setScale(4, RoundingMode.HALF_UP)
            totalCredit = totalCredit.add(entry.creditAmount).setScale(4, RoundingMode.HALF_UP)
        }

        val closingBalance = openingBalance.add(totalDebit).subtract(totalCredit).setScale(4, RoundingMode.HALF_UP)

        val statement = CustomerStatement(
            statementId = "STMT-${UUID.randomUUID().toString().take(8).uppercase()}",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = account.financialAccountId,
            customerDisplayName = customerName,
            accountNumber = account.accountNumber,
            currency = "BDT",
            fromDate = effectiveFrom,
            toDate = if (effectiveTo == Long.MAX_VALUE) System.currentTimeMillis() else effectiveTo,
            generatedAt = System.currentTimeMillis(),
            openingBalance = openingBalance,
            totalDebit = totalDebit,
            totalCredit = totalCredit,
            closingBalance = closingBalance,
            entries = periodEntries
        )

        return DomainResult.Success(statement)
    }

    override suspend fun getCustomerStatementSummary(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerStatementSummary> {
        val accountRes = accountRepository.getAccountByCustomerId(tenantId, projectId, customerId)
        val account = when (accountRes) {
            is DomainResult.Success -> accountRes.data
            is DomainResult.Error -> return accountRes
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val creditSummaryRes = creditRepository.getCustomerCreditSummary(tenantId, projectId, customerId)
        val creditSummary = when (creditSummaryRes) {
            is DomainResult.Success -> creditSummaryRes.data
            is DomainResult.Error -> return creditSummaryRes
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        var totalInvoiced = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var totalPaid = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var currentReceivable = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)

        when (val invRes = invoiceRepository.listInvoices(tenantId, projectId, customerId, null, 10000, 0)) {
            is DomainResult.Success -> {
                invRes.data
                    .filter { it.status != CustomerInvoiceStatus.VOID && it.status != CustomerInvoiceStatus.CANCELLED && it.status != CustomerInvoiceStatus.DRAFT }
                    .forEach {
                        totalInvoiced = totalInvoiced.add(it.grandTotal).setScale(4, RoundingMode.HALF_UP)
                        currentReceivable = currentReceivable.add(it.dueAmount).setScale(4, RoundingMode.HALF_UP)
                    }
            }
            is DomainResult.Error -> return invRes
            DomainResult.Loading -> {}
        }

        when (val payRes = paymentRepository.listPayments(tenantId, projectId, customerId, null, null, 10000, 0)) {
            is DomainResult.Success -> {
                payRes.data
                    .filter { it.status != CustomerPaymentStatus.CANCELLED }
                    .forEach {
                        totalPaid = totalPaid.add(it.amount).setScale(4, RoundingMode.HALF_UP)
                    }
            }
            is DomainResult.Error -> return payRes
            DomainResult.Loading -> {}
        }

        val netBalance = currentReceivable.subtract(creditSummary.totalAvailableCredit).setScale(4, RoundingMode.HALF_UP)

        val summary = CustomerStatementSummary(
            customerId = customerId,
            customerFinancialAccountId = account.financialAccountId,
            openingBalance = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
            totalInvoiced = totalInvoiced,
            totalPaid = totalPaid,
            totalAdvances = creditSummary.totalAdvances,
            totalAdjustmentsCredit = creditSummary.totalAdjustmentsCredit,
            totalAdjustmentsDebit = creditSummary.totalAdjustmentsDebit,
            totalRefunds = creditSummary.totalRefunds,
            totalAllocated = creditSummary.totalAllocated,
            currentReceivableBalance = currentReceivable,
            availableCreditBalance = creditSummary.totalAvailableCredit,
            netBalance = netBalance,
            currency = "BDT"
        )

        return DomainResult.Success(summary)
    }

    override suspend fun reconcileCustomerReceivable(
        tenantId: String,
        projectId: String,
        customerId: String,
        notes: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerReceivableReconciliation> {
        val accountRes = accountRepository.getAccountByCustomerId(tenantId, projectId, customerId)
        val account = when (accountRes) {
            is DomainResult.Success -> accountRes.data
            is DomainResult.Error -> return accountRes
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val valRes = CustomerLedgerValidator.validateReconciliation(tenantId, projectId, customerId, account)
        if (valRes is DomainResult.Error) return valRes

        val discrepancies = mutableListOf<ReconciliationDiscrepancy>()

        // 1. Calculate Invoices Total Due
        var invoiceTotalReceivable = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        when (val invRes = invoiceRepository.listInvoices(tenantId, projectId, customerId, null, 10000, 0)) {
            is DomainResult.Success -> {
                invRes.data
                    .filter { it.status != CustomerInvoiceStatus.VOID && it.status != CustomerInvoiceStatus.CANCELLED && it.status != CustomerInvoiceStatus.DRAFT }
                    .forEach { inv ->
                        invoiceTotalReceivable = invoiceTotalReceivable.add(inv.dueAmount).setScale(4, RoundingMode.HALF_UP)
                        val expectedDue = inv.grandTotal.subtract(inv.paidAmount).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)
                        if (inv.dueAmount.compareTo(expectedDue) != 0) {
                            discrepancies.add(
                                ReconciliationDiscrepancy(
                                    discrepancyType = "INVOICE_BALANCE_MISMATCH",
                                    referenceType = "INVOICE",
                                    referenceId = inv.invoiceId,
                                    expectedAmount = expectedDue,
                                    actualAmount = inv.dueAmount,
                                    difference = inv.dueAmount.subtract(expectedDue).abs().setScale(4, RoundingMode.HALF_UP),
                                    description = "Invoice #${inv.invoiceNumber} due amount (${inv.dueAmount}) differs from expected ($expectedDue)"
                                )
                            )
                        }
                    }
            }
            is DomainResult.Error -> return invRes
            DomainResult.Loading -> {}
        }

        // 2. Calculate Available Credit
        val creditSummaryRes = creditRepository.getCustomerCreditSummary(tenantId, projectId, customerId)
        val creditSummary = when (creditSummaryRes) {
            is DomainResult.Success -> creditSummaryRes.data
            is DomainResult.Error -> return creditSummaryRes
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }
        val availableCredit = creditSummary.totalAvailableCredit

        // 3. Build Full Ledger and calculate net ledger balance
        val fullLedgerRes = buildFullChronologicalLedger(tenantId, projectId, customerId, account.financialAccountId)
        val fullLedger = when (fullLedgerRes) {
            is DomainResult.Success -> fullLedgerRes.data
            is DomainResult.Error -> return fullLedgerRes
            DomainResult.Loading -> return DomainResult.Error(IllegalStateException("Loading state"))
        }

        val ledgerCalculatedBalance = if (fullLedger.isEmpty()) {
            BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        } else {
            fullLedger.last().balanceAfter
        }

        // 4. Invariant: Expected Net Balance = invoiceTotalReceivable - availableCredit
        val expectedNet = invoiceTotalReceivable.subtract(availableCredit).setScale(4, RoundingMode.HALF_UP)
        val diff = ledgerCalculatedBalance.subtract(expectedNet).abs().setScale(4, RoundingMode.HALF_UP)

        if (diff.compareTo(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)) != 0) {
            discrepancies.add(
                ReconciliationDiscrepancy(
                    discrepancyType = "LEDGER_NET_BALANCE_MISMATCH",
                    referenceType = "CUSTOMER_ACCOUNT",
                    referenceId = account.financialAccountId,
                    expectedAmount = expectedNet,
                    actualAmount = ledgerCalculatedBalance,
                    difference = diff,
                    description = "Ledger calculated balance ($ledgerCalculatedBalance) does not match expected invoice-to-credit position ($expectedNet)"
                )
            )
        }

        val isConsistent = discrepancies.isEmpty()
        val status = if (isConsistent) ReceivableReconciliationStatus.CONSISTENT else ReceivableReconciliationStatus.INCONSISTENT

        val reconciliation = CustomerReceivableReconciliation(
            reconciliationId = "REC-${UUID.randomUUID().toString().take(8).uppercase()}",
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = account.financialAccountId,
            reconciledAt = System.currentTimeMillis(),
            reconciledBy = actorId,
            status = status,
            invoiceTotalReceivable = invoiceTotalReceivable,
            ledgerCalculatedBalance = ledgerCalculatedBalance,
            availableCreditBalance = availableCredit,
            difference = diff,
            isConsistent = isConsistent,
            discrepancyCount = discrepancies.size,
            discrepancies = discrepancies,
            notes = notes,
            createdAt = System.currentTimeMillis(),
            version = 1L
        )

        val saveRes = ledgerRepository.saveReconciliation(reconciliation)
        return when (saveRes) {
            is DomainResult.Success -> DomainResult.Success(reconciliation)
            is DomainResult.Error -> saveRes
            DomainResult.Loading -> DomainResult.Success(reconciliation)
        }
    }

    override suspend fun listReconciliations(
        tenantId: String,
        projectId: String,
        customerId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerReceivableReconciliation>> {
        val pageVal = CustomerLedgerValidator.validatePagination(limit, offset)
        if (pageVal is DomainResult.Error) return pageVal

        return ledgerRepository.listReconciliations(tenantId, projectId, customerId, limit, offset)
    }
}

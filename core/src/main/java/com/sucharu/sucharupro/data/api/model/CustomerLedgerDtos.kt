package com.sucharu.sucharupro.data.api.model

import com.sucharu.sucharupro.domain.model.customerledger.CustomerLedgerEntry
import com.sucharu.sucharupro.domain.model.customerledger.CustomerReceivableReconciliation
import com.sucharu.sucharupro.domain.model.customerledger.CustomerStatement
import com.sucharu.sucharupro.domain.model.customerledger.CustomerStatementSummary
import com.sucharu.sucharupro.domain.model.customerledger.ReconciliationDiscrepancy
import java.math.BigDecimal

/**
 * DTOs for Customer Ledger, Statement, and Reconciliation (Module 14 Step 05).
 */

data class CustomerLedgerEntryDto(
    val entryId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val effectiveAt: Long,
    val entryType: String,
    val referenceType: String,
    val referenceId: String,
    val referenceNumber: String? = null,
    val description: String,
    val debitAmount: BigDecimal,
    val creditAmount: BigDecimal,
    val balanceAfter: BigDecimal,
    val currency: String,
    val sourceTransactionId: String,
    val metadataJson: String? = null
)

data class CustomerStatementDto(
    val statementId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val customerDisplayName: String,
    val accountNumber: String,
    val currency: String,
    val fromDate: Long,
    val toDate: Long,
    val generatedAt: Long,
    val openingBalance: BigDecimal,
    val totalDebit: BigDecimal,
    val totalCredit: BigDecimal,
    val closingBalance: BigDecimal,
    val entries: List<CustomerLedgerEntryDto>
)

data class CustomerStatementSummaryDto(
    val customerId: String,
    val customerFinancialAccountId: String,
    val openingBalance: BigDecimal,
    val totalInvoiced: BigDecimal,
    val totalPaid: BigDecimal,
    val totalAdvances: BigDecimal,
    val totalAdjustmentsCredit: BigDecimal,
    val totalAdjustmentsDebit: BigDecimal,
    val totalRefunds: BigDecimal,
    val totalAllocated: BigDecimal,
    val currentReceivableBalance: BigDecimal,
    val availableCreditBalance: BigDecimal,
    val netBalance: BigDecimal,
    val currency: String
)

data class ReconciliationDiscrepancyDto(
    val discrepancyType: String,
    val referenceType: String,
    val referenceId: String,
    val expectedAmount: BigDecimal,
    val actualAmount: BigDecimal,
    val difference: BigDecimal,
    val description: String
)

data class CustomerReceivableReconciliationDto(
    val reconciliationId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val reconciledAt: Long,
    val reconciledBy: String,
    val status: String,
    val invoiceTotalReceivable: BigDecimal,
    val ledgerCalculatedBalance: BigDecimal,
    val availableCreditBalance: BigDecimal,
    val difference: BigDecimal,
    val isConsistent: Boolean,
    val discrepancyCount: Int,
    val discrepancies: List<ReconciliationDiscrepancyDto>,
    val notes: String? = null,
    val createdAt: Long,
    val version: Long
)

data class ReconcileCustomerReceivableRequest(
    val notes: String? = null
)

fun CustomerLedgerEntry.toDto(): CustomerLedgerEntryDto = CustomerLedgerEntryDto(
    entryId = entryId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    customerFinancialAccountId = customerFinancialAccountId,
    effectiveAt = effectiveAt,
    entryType = entryType.name,
    referenceType = referenceType,
    referenceId = referenceId,
    referenceNumber = referenceNumber,
    description = description,
    debitAmount = debitAmount,
    creditAmount = creditAmount,
    balanceAfter = balanceAfter,
    currency = currency,
    sourceTransactionId = sourceTransactionId,
    metadataJson = metadataJson
)

fun CustomerStatement.toDto(): CustomerStatementDto = CustomerStatementDto(
    statementId = statementId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    customerFinancialAccountId = customerFinancialAccountId,
    customerDisplayName = customerDisplayName,
    accountNumber = accountNumber,
    currency = currency,
    fromDate = fromDate,
    toDate = toDate,
    generatedAt = generatedAt,
    openingBalance = openingBalance,
    totalDebit = totalDebit,
    totalCredit = totalCredit,
    closingBalance = closingBalance,
    entries = entries.map { it.toDto() }
)

fun CustomerStatementSummary.toDto(): CustomerStatementSummaryDto = CustomerStatementSummaryDto(
    customerId = customerId,
    customerFinancialAccountId = customerFinancialAccountId,
    openingBalance = openingBalance,
    totalInvoiced = totalInvoiced,
    totalPaid = totalPaid,
    totalAdvances = totalAdvances,
    totalAdjustmentsCredit = totalAdjustmentsCredit,
    totalAdjustmentsDebit = totalAdjustmentsDebit,
    totalRefunds = totalRefunds,
    totalAllocated = totalAllocated,
    currentReceivableBalance = currentReceivableBalance,
    availableCreditBalance = availableCreditBalance,
    netBalance = netBalance,
    currency = currency
)

fun ReconciliationDiscrepancy.toDto(): ReconciliationDiscrepancyDto = ReconciliationDiscrepancyDto(
    discrepancyType = discrepancyType,
    referenceType = referenceType,
    referenceId = referenceId,
    expectedAmount = expectedAmount,
    actualAmount = actualAmount,
    difference = difference,
    description = description
)

fun CustomerReceivableReconciliation.toDto(): CustomerReceivableReconciliationDto = CustomerReceivableReconciliationDto(
    reconciliationId = reconciliationId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    customerFinancialAccountId = customerFinancialAccountId,
    reconciledAt = reconciledAt,
    reconciledBy = reconciledBy,
    status = status.name,
    invoiceTotalReceivable = invoiceTotalReceivable,
    ledgerCalculatedBalance = ledgerCalculatedBalance,
    availableCreditBalance = availableCreditBalance,
    difference = difference,
    isConsistent = isConsistent,
    discrepancyCount = discrepancyCount,
    discrepancies = discrepancies.map { it.toDto() },
    notes = notes,
    createdAt = createdAt,
    version = version
)

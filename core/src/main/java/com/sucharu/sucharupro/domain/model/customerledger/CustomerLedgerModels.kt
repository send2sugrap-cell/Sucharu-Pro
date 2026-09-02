package com.sucharu.sucharupro.domain.model.customerledger

import java.math.BigDecimal

/**
 * Entry types representing canonical financial movements in the Customer Ledger (Module 14 Step 05).
 */
enum class CustomerLedgerEntryType {
    OPENING_BALANCE,
    INVOICE,             // Customer billed (Debit: increases receivable)
    PAYMENT,             // Customer payment confirmed (Credit: decreases receivable)
    ADVANCE,             // Advance received (Credit: customer prepayment balance)
    CREDIT_ALLOCATION,   // Advance allocated to invoice (Settlement record)
    CREDIT_ADJUSTMENT,   // Goodwill/Discount/Rebate (Credit: decreases receivable)
    DEBIT_ADJUSTMENT,    // Surcharge/Penalty/Fee (Debit: increases receivable)
    REFUND,              // Disbursement of funds back to customer (Debit: decreases credit / increases balance)
    REVERSAL;            // Allocation or payment reversed

    val isDebit: Boolean get() = this in setOf(INVOICE, DEBIT_ADJUSTMENT, REFUND)
    val isCredit: Boolean get() = this in setOf(PAYMENT, ADVANCE, CREDIT_ADJUSTMENT)
}

/**
 * Immutable line item in a Customer Ledger projection.
 */
data class CustomerLedgerEntry(
    val entryId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val effectiveAt: Long,
    val entryType: CustomerLedgerEntryType,
    val referenceType: String,
    val referenceId: String,
    val referenceNumber: String? = null,
    val description: String,
    val debitAmount: BigDecimal = BigDecimal.ZERO,
    val creditAmount: BigDecimal = BigDecimal.ZERO,
    val balanceAfter: BigDecimal = BigDecimal.ZERO,
    val currency: String = "BDT",
    val sourceTransactionId: String,
    val metadataJson: String? = null
)

/**
 * Read-only statement of a customer's financial activity over a given date window.
 */
data class CustomerStatement(
    val statementId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val customerDisplayName: String,
    val accountNumber: String,
    val currency: String = "BDT",
    val fromDate: Long,
    val toDate: Long,
    val generatedAt: Long = System.currentTimeMillis(),
    val openingBalance: BigDecimal = BigDecimal.ZERO,
    val totalDebit: BigDecimal = BigDecimal.ZERO,
    val totalCredit: BigDecimal = BigDecimal.ZERO,
    val closingBalance: BigDecimal = BigDecimal.ZERO,
    val entries: List<CustomerLedgerEntry> = emptyList()
)

/**
 * Concise financial summary of a customer's current statement.
 */
data class CustomerStatementSummary(
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
    val currency: String = "BDT"
)

/**
 * Reconciliation evaluation outcome.
 */
enum class ReceivableReconciliationStatus {
    CONSISTENT,
    INCONSISTENT
}

/**
 * Identified discrepancy during reconciliation.
 */
data class ReconciliationDiscrepancy(
    val discrepancyType: String,
    val referenceType: String,
    val referenceId: String,
    val expectedAmount: BigDecimal,
    val actualAmount: BigDecimal,
    val difference: BigDecimal,
    val description: String
)

/**
 * Comprehensive result of a Receivable Reconciliation diagnostic run.
 */
data class CustomerReceivableReconciliation(
    val reconciliationId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val reconciledAt: Long = System.currentTimeMillis(),
    val reconciledBy: String = "system",
    val status: ReceivableReconciliationStatus = ReceivableReconciliationStatus.CONSISTENT,
    val invoiceTotalReceivable: BigDecimal = BigDecimal.ZERO,
    val ledgerCalculatedBalance: BigDecimal = BigDecimal.ZERO,
    val availableCreditBalance: BigDecimal = BigDecimal.ZERO,
    val difference: BigDecimal = BigDecimal.ZERO,
    val isConsistent: Boolean = true,
    val discrepancyCount: Int = 0,
    val discrepancies: List<ReconciliationDiscrepancy> = emptyList(),
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

/**
 * Audit event for customer ledger and reconciliation actions.
 */
data class CustomerLedgerAuditEvent(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val occurredAt: Long = System.currentTimeMillis(),
    val details: String? = null
)

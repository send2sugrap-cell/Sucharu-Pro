package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Report containing diagnostic integrity findings from ledger reconciliation (Module 09 Step 08).
 */
data class LedgerReconciliationReport(
    val projectId: String,
    val periodId: String,
    val totalTransactions: Int,
    val totalLedgerEntries: Int,
    val totalDebitAmount: Money,
    val totalCreditAmount: Money,
    val isBalanced: Boolean,
    val orphanTransactions: List<String> = emptyList(),
    val orphanLedgerEntries: List<String> = emptyList(),
    val duplicateLedgerEntries: List<String> = emptyList(),
    val amountMismatches: List<String> = emptyList(),
    val projectMismatches: List<String> = emptyList()
) {
    val isClean: Boolean
        get() = isBalanced &&
                orphanTransactions.isEmpty() &&
                orphanLedgerEntries.isEmpty() &&
                duplicateLedgerEntries.isEmpty() &&
                amountMismatches.isEmpty() &&
                projectMismatches.isEmpty()
}

/**
 * Service verifying General Ledger debit/credit consistency and entity integrity (Module 09 Step 08).
 * Read-only analysis: Never mutates existing ledger records.
 */
object LedgerReconciliationService {

    fun reconcile(
        projectId: String,
        periodId: String,
        transactions: List<FinancialTransaction>,
        ledgerEntries: List<FinancialLedgerEntry>
    ): LedgerReconciliationReport {
        var totalDebit = Money.ZERO
        var totalCredit = Money.ZERO

        val orphanTxns = mutableListOf<String>()
        val orphanEntries = mutableListOf<String>()
        val dupEntries = mutableListOf<String>()
        val amountMismatches = mutableListOf<String>()
        val projectMismatches = mutableListOf<String>()

        val entriesByTxnId = ledgerEntries.groupBy { it.transactionId }
        val seenEntryIds = mutableSetOf<String>()

        for (entry in ledgerEntries) {
            if (entry.projectId != projectId) {
                projectMismatches.add("Entry '${entry.entryId}' belongs to project '${entry.projectId}', expected '$projectId'")
            }

            if (!seenEntryIds.add(entry.entryId)) {
                dupEntries.add("Duplicate entry ID '${entry.entryId}'")
            }

            when (entry.entryType) {
                FinancialEntryType.DEBIT -> totalDebit = totalDebit.plus(entry.amount)
                FinancialEntryType.CREDIT -> totalCredit = totalCredit.plus(entry.amount)
            }
        }

        val txnMap = transactions.associateBy { it.transactionId }

        for (txn in transactions) {
            if (txn.projectId != projectId) {
                projectMismatches.add("Transaction '${txn.transactionId}' belongs to project '${txn.projectId}', expected '$projectId'")
            }

            if (txn.transactionStatus == FinancialTransactionStatus.POSTED) {
                val attachedEntries = entriesByTxnId[txn.transactionId] ?: emptyList()
                if (attachedEntries.isEmpty()) {
                    orphanTxns.add("Posted transaction '${txn.transactionNo}' (${txn.transactionId}) has no ledger entries.")
                } else {
                    val debitSum = attachedEntries.filter { it.entryType == FinancialEntryType.DEBIT }.fold(Money.ZERO) { acc, e -> acc.plus(e.amount) }
                    val creditSum = attachedEntries.filter { it.entryType == FinancialEntryType.CREDIT }.fold(Money.ZERO) { acc, e -> acc.plus(e.amount) }
                    if (debitSum != txn.amount || creditSum != txn.amount) {
                        amountMismatches.add(
                            "Transaction '${txn.transactionNo}' amount (${txn.amount.formatted()}) does not match debit entries (${debitSum.formatted()}) or credit entries (${creditSum.formatted()})."
                        )
                    }
                }
            }
        }

        for (entry in ledgerEntries) {
            if (!txnMap.containsKey(entry.transactionId)) {
                orphanEntries.add("Ledger entry '${entry.entryId}' references non-existent transaction '${entry.transactionId}'.")
            }
        }

        val isBalanced = totalDebit == totalCredit

        return LedgerReconciliationReport(
            projectId = projectId,
            periodId = periodId,
            totalTransactions = transactions.size,
            totalLedgerEntries = ledgerEntries.size,
            totalDebitAmount = totalDebit,
            totalCreditAmount = totalCredit,
            isBalanced = isBalanced,
            orphanTransactions = orphanTxns,
            orphanLedgerEntries = orphanEntries,
            duplicateLedgerEntries = dupEntries,
            amountMismatches = amountMismatches,
            projectMismatches = projectMismatches
        )
    }
}

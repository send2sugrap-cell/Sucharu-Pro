package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure, side-effect-free calculation engine for financial reports (Module 09 Step 09).
 *
 * Derives all financial report values from canonical existing data.
 * Never creates a second ledger. Never mutates input data.
 * All calculations are deterministic and floating-point free (BigDecimal via Money).
 */
object FinancialReportCalculator {

    // ─── P&L Calculations ───────────────────────────────────────────────────

    /**
     * Calculate total revenue from posted SALE/RECEIPT financial transactions.
     */
    fun calculateTotalRevenue(
        transactions: List<FinancialTransaction>,
        startDate: Long,
        endDate: Long
    ): Money {
        return transactions
            .filter { tx ->
                tx.projectId.isNotBlank() &&
                tx.transactionStatus == FinancialTransactionStatus.POSTED &&
                tx.transactionType in listOf(FinancialTransactionType.SALE, FinancialTransactionType.RECEIPT) &&
                tx.transactionDate in startDate..endDate
            }
            .fold(Money.ZERO) { acc, tx -> acc.plus(tx.amount) }
    }

    /**
     * Calculate total posted expenses from Expense list.
     */
    fun calculateTotalPostedExpenses(
        expenses: List<Expense>,
        startDate: Long,
        endDate: Long
    ): Money {
        return expenses
            .filter { exp ->
                exp.status == ExpenseStatus.POSTED &&
                exp.expenseDate in startDate..endDate
            }
            .fold(Money.ZERO) { acc, exp -> acc.plus(exp.amount) }
    }

    /**
     * Calculate net adjustment effect: credit adjustments add to income (CREDIT direction),
     * debit adjustments reduce it (DEBIT direction).
     */
    fun calculateAdjustmentNetEffect(
        adjustments: List<FinancialAdjustment>,
        startDate: Long,
        endDate: Long
    ): Money {
        return adjustments
            .filter { adj ->
                adj.status == FinancialAdjustmentStatus.POSTED &&
                adj.createdAt in startDate..endDate
            }
            .fold(Money.ZERO) { acc, adj ->
                when (adj.direction) {
                    FinancialAdjustmentDirection.CREDIT -> acc.plus(adj.amount)
                    FinancialAdjustmentDirection.DEBIT -> acc.minus(adj.amount)
                }
            }
    }

    /**
     * Calculate net profit: Revenue - Expenses + AdjustmentEffect
     */
    fun calculateNetProfit(
        totalRevenue: Money,
        totalExpenses: Money,
        adjustmentEffect: Money = Money.ZERO
    ): Money {
        return totalRevenue.minus(totalExpenses).plus(adjustmentEffect)
    }

    // ─── Balance Sheet Calculations ─────────────────────────────────────────

    /**
     * Calculate equation variance: Assets - (Liabilities + Equity).
     * Returns Money.ZERO if balanced.
     */
    fun calculateBalanceSheetVariance(
        totalAssets: Money,
        totalLiabilities: Money,
        totalEquity: Money
    ): Money {
        return totalAssets.minus(totalLiabilities.plus(totalEquity))
    }

    // ─── Trial Balance ───────────────────────────────────────────────────────

    /**
     * Build trial balance lines from ledger entries.
     * Groups by accountHead, sums debit/credit per account.
     */
    fun buildTrialBalanceLines(
        ledgerEntries: List<FinancialLedgerEntry>,
        startDate: Long,
        endDate: Long
    ): List<TrialBalanceLine> {
        val filtered = ledgerEntries.filter { entry ->
            entry.entryDate in startDate..endDate
        }
        return filtered
            .groupBy { it.accountHead }
            .map { (accountHead, entries) ->
                val totalDebit = entries
                    .filter { it.entryType == FinancialEntryType.DEBIT }
                    .fold(Money.ZERO) { acc, e -> acc.plus(e.amount) }
                val totalCredit = entries
                    .filter { it.entryType == FinancialEntryType.CREDIT }
                    .fold(Money.ZERO) { acc, e -> acc.plus(e.amount) }
                TrialBalanceLine(
                    accountHead = accountHead,
                    accountHeadName = accountHead, // Name resolved by caller if available
                    totalDebit = totalDebit,
                    totalCredit = totalCredit
                )
            }
            .sortedBy { it.accountHead }
    }

    /**
     * Calculate trial balance totals from lines.
     */
    fun calculateTrialBalanceTotals(lines: List<TrialBalanceLine>): Pair<Money, Money> {
        val totalDebit = lines.fold(Money.ZERO) { acc, l -> acc.plus(l.totalDebit) }
        val totalCredit = lines.fold(Money.ZERO) { acc, l -> acc.plus(l.totalCredit) }
        return totalDebit to totalCredit
    }

    // ─── General Ledger ──────────────────────────────────────────────────────

    /**
     * Build general ledger entries with running balance from canonical ledger entries.
     * Entries are sorted by entryDate ASC, then entryNo ASC for deterministic ordering.
     */
    fun buildGeneralLedgerEntries(
        ledgerEntries: List<FinancialLedgerEntry>,
        transactions: List<FinancialTransaction>,
        openingBalance: Money,
        startDate: Long,
        endDate: Long,
        accountHeadFilter: String? = null,
        page: Int = 0,
        pageSize: Int = 50
    ): List<GeneralLedgerEntry> {
        val txMap = transactions.associateBy { it.transactionId }
        val filtered = ledgerEntries
            .filter { entry ->
                entry.entryDate in startDate..endDate &&
                (accountHeadFilter == null || entry.accountHead == accountHeadFilter)
            }
            .sortedWith(compareBy({ it.entryDate }, { it.entryNo }))

        var runningBalance = openingBalance
        val all = filtered.map { entry ->
            val debit = if (entry.entryType == FinancialEntryType.DEBIT) entry.amount else Money.ZERO
            val credit = if (entry.entryType == FinancialEntryType.CREDIT) entry.amount else Money.ZERO
            runningBalance = runningBalance.plus(debit).minus(credit)
            val tx = txMap[entry.transactionId]
            GeneralLedgerEntry(
                entryId = entry.entryId,
                transactionId = entry.transactionId,
                transactionDate = tx?.transactionDate ?: entry.entryDate,
                entryDate = entry.entryDate,
                accountHead = entry.accountHead,
                referenceType = entry.referenceType,
                referenceId = entry.referenceId,
                narration = entry.narration,
                debit = debit,
                credit = credit,
                runningBalance = runningBalance
            )
        }
        val fromIdx = (page * pageSize).coerceIn(0, all.size)
        val toIdx = (fromIdx + pageSize).coerceIn(0, all.size)
        return all.subList(fromIdx, toIdx)
    }

    // ─── Cash Flow ───────────────────────────────────────────────────────────

    /**
     * Calculate cash inflow from posted customer payments.
     */
    fun calculateCashIn(
        transactions: List<FinancialTransaction>,
        startDate: Long,
        endDate: Long
    ): Money {
        return transactions
            .filter { tx ->
                tx.transactionStatus == FinancialTransactionStatus.POSTED &&
                tx.transactionType == FinancialTransactionType.RECEIPT &&
                tx.transactionDate in startDate..endDate
            }
            .fold(Money.ZERO) { acc, tx -> acc.plus(tx.amount) }
    }

    /**
     * Calculate cash outflow from posted supplier payments + posted expenses.
     */
    fun calculateCashOut(
        transactions: List<FinancialTransaction>,
        startDate: Long,
        endDate: Long
    ): Money {
        return transactions
            .filter { tx ->
                tx.transactionStatus == FinancialTransactionStatus.POSTED &&
                tx.transactionType in listOf(
                    FinancialTransactionType.PAYMENT,
                    FinancialTransactionType.EXPENSE,
                    FinancialTransactionType.REFUND
                ) &&
                tx.transactionDate in startDate..endDate
            }
            .fold(Money.ZERO) { acc, tx -> acc.plus(tx.amount) }
    }

    // ─── Aging ───────────────────────────────────────────────────────────────

    /**
     * Classify receivables into aging buckets based on evaluationTimestamp.
     */
    fun buildReceivableAgingBuckets(
        receivables: List<CustomerReceivable>,
        evaluationTimestamp: Long
    ): ReceivableAgingBucketSummary {
        val MS_PER_DAY = 86_400_000L
        var current = Money.ZERO
        var d1to30 = Money.ZERO
        var d31to60 = Money.ZERO
        var d61to90 = Money.ZERO
        var d90plus = Money.ZERO

        receivables.filter { it.status != CustomerReceivableStatus.SETTLED &&
                it.status != CustomerReceivableStatus.CANCELLED }
            .forEach { r ->
                val outstanding = r.outstandingAmount
                if (outstanding.isZero()) return@forEach
                val daysOverdue = ((evaluationTimestamp - r.dueDate) / MS_PER_DAY).toInt()
                when {
                    daysOverdue <= 0 -> current = current.plus(outstanding)
                    daysOverdue in 1..30 -> d1to30 = d1to30.plus(outstanding)
                    daysOverdue in 31..60 -> d31to60 = d31to60.plus(outstanding)
                    daysOverdue in 61..90 -> d61to90 = d61to90.plus(outstanding)
                    else -> d90plus = d90plus.plus(outstanding)
                }
            }
        return ReceivableAgingBucketSummary(
            current = current,
            overdue1To30 = d1to30,
            overdue31To60 = d31to60,
            overdue61To90 = d61to90,
            overdue90Plus = d90plus
        )
    }

    /**
     * Classify payables into aging buckets based on evaluationTimestamp.
     */
    fun buildPayableAgingBuckets(
        payables: List<VendorPayable>,
        evaluationTimestamp: Long
    ): PayableAgingBucketSummary {
        val MS_PER_DAY = 86_400_000L
        var current = Money.ZERO
        var d1to30 = Money.ZERO
        var d31to60 = Money.ZERO
        var d61to90 = Money.ZERO
        var d90plus = Money.ZERO

        payables.filter { it.status != VendorPayableStatus.SETTLED &&
                it.status != VendorPayableStatus.CANCELLED }
            .forEach { p ->
                val outstanding = p.outstandingAmount
                if (outstanding.isZero()) return@forEach
                val daysOverdue = ((evaluationTimestamp - p.dueDate) / MS_PER_DAY).toInt()
                when {
                    daysOverdue <= 0 -> current = current.plus(outstanding)
                    daysOverdue in 1..30 -> d1to30 = d1to30.plus(outstanding)
                    daysOverdue in 31..60 -> d31to60 = d31to60.plus(outstanding)
                    daysOverdue in 61..90 -> d61to90 = d61to90.plus(outstanding)
                    else -> d90plus = d90plus.plus(outstanding)
                }
            }
        return PayableAgingBucketSummary(
            current = current,
            overdue1To30 = d1to30,
            overdue31To60 = d31to60,
            overdue61To90 = d61to90,
            overdue90Plus = d90plus
        )
    }
}

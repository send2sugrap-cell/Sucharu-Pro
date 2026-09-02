package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal

/**
 * Pure calculation engine for financial control and reconciliation calculations (Module 09 Step 08).
 * Deterministic, floating-point free, operates against explicit input parameters.
 */
object FinancialReconciliationCalculator {

    fun calculateReconciliationResult(
        expectedAmount: Money,
        actualAmount: Money,
        tolerance: Money = Money.ZERO,
        matchedCount: Int = 0,
        unmatchedCount: Int = 0
    ): ReconciliationResult {
        val diff = actualAmount.minus(expectedAmount)
        val isWithinTol = diff.abs() <= tolerance

        val status = when {
            diff.isZero() -> FinancialReconciliationStatus.MATCHED
            isWithinTol -> FinancialReconciliationStatus.PARTIALLY_MATCHED
            else -> FinancialReconciliationStatus.MISMATCHED
        }

        val details = when (status) {
            FinancialReconciliationStatus.MATCHED -> "Fully balanced with zero variance."
            FinancialReconciliationStatus.PARTIALLY_MATCHED -> "Variance of ${diff.formatted()} is within allowable tolerance (${tolerance.formatted()})."
            FinancialReconciliationStatus.MISMATCHED -> "Discrepancy detected: Expected ${expectedAmount.formatted()}, Actual ${actualAmount.formatted()} (Variance: ${diff.formatted()})."
            else -> "Calculation completed."
        }

        return ReconciliationResult(
            status = status,
            expected = expectedAmount,
            actual = actualAmount,
            difference = diff,
            tolerance = tolerance,
            isWithinTolerance = isWithinTol,
            matchedCount = matchedCount,
            unmatchedCount = unmatchedCount,
            details = details
        )
    }

    fun calculateLedgerBalance(
        debitTransactions: List<Money>,
        creditTransactions: List<Money>
    ): Pair<Money, Money> {
        val totalDebit = debitTransactions.fold(Money.ZERO) { acc, m -> acc.plus(m) }
        val totalCredit = creditTransactions.fold(Money.ZERO) { acc, m -> acc.plus(m) }
        return totalDebit to totalCredit
    }

    fun calculateExpectedClosingCash(
        openingCash: Money,
        cashReceipts: Money,
        cashPayments: Money,
        cashAdjustments: Money = Money.ZERO
    ): Money {
        return openingCash.plus(cashReceipts).minus(cashPayments).plus(cashAdjustments)
    }

    fun calculateExpectedClosingBank(
        openingBank: Money,
        deposits: Money,
        withdrawals: Money,
        outstandingDeposits: Money = Money.ZERO,
        outstandingWithdrawals: Money = Money.ZERO,
        adjustments: Money = Money.ZERO
    ): Money {
        val ledgerBank = openingBank.plus(deposits).minus(withdrawals)
        return ledgerBank.plus(outstandingWithdrawals).minus(outstandingDeposits).minus(adjustments)
    }

    fun calculateNetFinancialPosition(
        totalReceivable: Money,
        closingCash: Money,
        closingBank: Money,
        totalPayable: Money
    ): Money {
        return totalReceivable.plus(closingCash).plus(closingBank).minus(totalPayable)
    }
}

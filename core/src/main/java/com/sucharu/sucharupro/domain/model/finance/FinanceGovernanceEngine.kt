package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Governance Controls and Invariants Verifier (Module 09 Step 10).
 */
object FinanceGovernanceEngine {

    fun executeGovernanceAudit(
        isTrialBalanced: Boolean,
        trialBalanceVariance: Money,
        isBalanceSheetBalanced: Boolean,
        balanceSheetVariance: Money,
        hasOpenAccountingPeriod: Boolean,
        discrepancyCount: Int,
        criticalDiscrepancyCount: Int
    ): List<AnalyticsControlResult> {
        val results = mutableListOf<AnalyticsControlResult>()

        // 1. Dual-Entry Ledger Equation Control
        results.add(
            AnalyticsControlResult(
                controlCode = "GOV-CTRL-01",
                title = "Trial Balance Ledger Balance Check",
                status = if (isTrialBalanced) FinancialGovernanceStatus.PASSED else FinancialGovernanceStatus.CRITICAL_EXCEPTION,
                severity = if (isTrialBalanced) FinancialGovernanceSeverity.INFO else FinancialGovernanceSeverity.CRITICAL,
                description = if (isTrialBalanced) "Total debits strictly equal total credits." else "Debit/Credit imbalance detected (Variance: ${trialBalanceVariance.formatted()}).",
                source = "TrialBalanceReport"
            )
        )

        // 2. Statement of Financial Position Balance Control
        results.add(
            AnalyticsControlResult(
                controlCode = "GOV-CTRL-02",
                title = "Balance Sheet Fundamental Equation Control",
                status = if (isBalanceSheetBalanced) FinancialGovernanceStatus.PASSED else FinancialGovernanceStatus.CRITICAL_EXCEPTION,
                severity = if (isBalanceSheetBalanced) FinancialGovernanceSeverity.INFO else FinancialGovernanceSeverity.CRITICAL,
                description = if (isBalanceSheetBalanced) "Assets strictly equal Liabilities + Equity." else "Balance sheet equation violation (Variance: ${balanceSheetVariance.formatted()}).",
                source = "BalanceSheetReport"
            )
        )

        // 3. Reconciliation Discrepancy Control
        results.add(
            AnalyticsControlResult(
                controlCode = "GOV-CTRL-03",
                title = "Account Reconciliation & Integrity Check",
                status = when {
                    criticalDiscrepancyCount > 0 -> FinancialGovernanceStatus.CRITICAL_EXCEPTION
                    discrepancyCount > 0 -> FinancialGovernanceStatus.WARNING
                    else -> FinancialGovernanceStatus.PASSED
                },
                severity = when {
                    criticalDiscrepancyCount > 0 -> FinancialGovernanceSeverity.CRITICAL
                    discrepancyCount > 0 -> FinancialGovernanceSeverity.MEDIUM
                    else -> FinancialGovernanceSeverity.INFO
                },
                description = if (discrepancyCount == 0) "All financial accounts reconciled without open discrepancies." else "$discrepancyCount unresolved account discrepancies identified ($criticalDiscrepancyCount critical).",
                source = "FinancialReconciliation"
            )
        )

        // 4. Accounting Period Closure Readiness Control
        results.add(
            AnalyticsControlResult(
                controlCode = "GOV-CTRL-04",
                title = "Accounting Period Governance State",
                status = if (hasOpenAccountingPeriod) FinancialGovernanceStatus.PASSED else FinancialGovernanceStatus.WARNING,
                severity = FinancialGovernanceSeverity.LOW,
                description = if (hasOpenAccountingPeriod) "Active accounting period open and processing transactions." else "No active accounting period detected.",
                source = "AccountingPeriod"
            )
        )

        return results
    }
}

package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * High-level aggregated metrics for the Financial Control & Reconciliation Dashboard (Module 09 Step 08).
 */
data class FinancialControlSummary(
    val activePeriod: AccountingPeriod? = null,
    val totalDebit: Money = Money.ZERO,
    val totalCredit: Money = Money.ZERO,
    val isLedgerBalanced: Boolean = totalDebit == totalCredit,
    val totalReceivableOutstanding: Money = Money.ZERO,
    val totalPayableOutstanding: Money = Money.ZERO,
    val totalExpenses: Money = Money.ZERO,
    val totalCustomerPayments: Money = Money.ZERO,
    val totalSupplierPayments: Money = Money.ZERO,
    val totalRefunds: Money = Money.ZERO,
    val totalCreditNotes: Money = Money.ZERO,
    val totalDebitNotes: Money = Money.ZERO,
    val cashInHandBalance: Money = Money.ZERO,
    val bankBalance: Money = Money.ZERO,
    val totalReconciliationsCount: Int = 0,
    val matchedReconciliationsCount: Int = 0,
    val openDiscrepanciesCount: Int = 0,
    val criticalDiscrepanciesCount: Int = 0,
    val closingReadiness: FinancialClosingReadinessStatus = FinancialClosingReadinessStatus.NOT_READY
)

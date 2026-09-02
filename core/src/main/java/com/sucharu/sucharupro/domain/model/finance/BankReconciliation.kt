package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Bank reconciliation entity linking ledger bank balances with statement balances (Module 09 Step 08).
 */
data class BankReconciliation(
    val reconciliationId: String,
    val projectId: String,
    val periodId: String,
    val bankAccountId: String = "PRIMARY_BANK",
    val bankName: String = "Standard Commercial Bank",
    val openingBankBalance: Money,
    val ledgerDeposits: Money,
    val ledgerWithdrawals: Money,
    val bankStatementBalance: Money,
    val outstandingDeposits: Money = Money.ZERO,
    val outstandingWithdrawals: Money = Money.ZERO,
    val adjustments: Money = Money.ZERO,
    val reconciledBalance: Money = bankStatementBalance.plus(outstandingDeposits).minus(outstandingWithdrawals).plus(adjustments),
    val difference: Money = reconciledBalance.minus(openingBankBalance.plus(ledgerDeposits).minus(ledgerWithdrawals)),
    val currency: String = "BDT",
    val status: FinancialReconciliationStatus = if (difference.isZero()) FinancialReconciliationStatus.MATCHED else FinancialReconciliationStatus.MISMATCHED,
    val verifiedBy: String? = null,
    val verifiedAt: Long? = null,
    val notes: String? = null
)

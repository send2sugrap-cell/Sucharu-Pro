package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Cash in Hand physical cash count vs ledger cash reconciliation entity (Module 09 Step 08).
 */
data class CashReconciliation(
    val reconciliationId: String,
    val projectId: String,
    val periodId: String,
    val openingCash: Money,
    val cashReceipts: Money,
    val cashPayments: Money,
    val cashAdjustments: Money = Money.ZERO,
    val expectedClosingCash: Money = openingCash.plus(cashReceipts).minus(cashPayments).plus(cashAdjustments),
    val actualClosingCash: Money,
    val difference: Money = actualClosingCash.minus(expectedClosingCash),
    val currency: String = "BDT",
    val status: FinancialReconciliationStatus = if (actualClosingCash == expectedClosingCash) FinancialReconciliationStatus.MATCHED else FinancialReconciliationStatus.MISMATCHED,
    val verifiedBy: String? = null,
    val verifiedAt: Long? = null,
    val notes: String? = null
)

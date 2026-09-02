package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Immutable historical financial control snapshot generated upon accounting period close (Module 09 Step 08).
 * Represents verified financial balances and metrics at the exact moment of closing.
 */
data class FinancialPeriodClosingSnapshot(
    val snapshotId: String,
    val snapshotNo: String,
    val projectId: String,
    val periodId: String,
    val periodName: String,
    val startDate: Long,
    val endDate: Long,
    val totalDebit: Money,
    val totalCredit: Money,
    val isLedgerBalanced: Boolean = totalDebit == totalCredit,
    val closingCash: Money,
    val closingBank: Money,
    val totalReceivable: Money,
    val totalPayable: Money,
    val totalExpense: Money,
    val totalCustomerPayment: Money,
    val totalSupplierPayment: Money,
    val totalRefund: Money,
    val totalAdjustment: Money,
    val netFinancialPosition: Money = totalReceivable.plus(closingCash).plus(closingBank).minus(totalPayable),
    val reconciliationStatus: FinancialReconciliationStatus = FinancialReconciliationStatus.MATCHED,
    val totalDiscrepanciesCount: Int = 0,
    val criticalDiscrepanciesCount: Int = 0,
    val generatedAt: Long = System.currentTimeMillis(),
    val generatedBy: String,
    val snapshotHash: String,
    val version: Int = 1
)

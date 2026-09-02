package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Financial Reconciliation aggregate entity tracking expected vs actual balances (Module 09 Step 08).
 */
data class FinancialReconciliation(
    val reconciliationId: String,
    val reconciliationNo: String,
    val projectId: String,
    val periodId: String,
    val reconciliationType: FinancialReconciliationType,
    val referenceId: String? = null,
    val expectedAmount: Money,
    val actualAmount: Money,
    val differenceAmount: Money = actualAmount.minus(expectedAmount),
    val currency: String = "BDT",
    val status: FinancialReconciliationStatus = FinancialReconciliationStatus.DRAFT,
    val reconciledBy: String? = null,
    val approvedBy: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val reconciledAt: Long? = null,
    val approvedAt: Long? = null,
    val closedAt: Long? = null,
    val idempotencyKey: String? = null
)

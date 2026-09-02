package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Deterministic calculation result produced by the reconciliation engine (Module 09 Step 08).
 */
data class ReconciliationResult(
    val status: FinancialReconciliationStatus,
    val expected: Money,
    val actual: Money,
    val difference: Money = actual.minus(expected),
    val tolerance: Money = Money.ZERO,
    val isWithinTolerance: Boolean = difference.abs() <= tolerance,
    val matchedCount: Int = 0,
    val unmatchedCount: Int = 0,
    val details: String = ""
)

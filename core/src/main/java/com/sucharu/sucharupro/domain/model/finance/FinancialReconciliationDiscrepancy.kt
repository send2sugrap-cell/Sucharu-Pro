package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Discrepancy item identified during financial reconciliation (Module 09 Step 08).
 */
data class FinancialReconciliationDiscrepancy(
    val discrepancyId: String,
    val discrepancyNo: String,
    val projectId: String,
    val periodId: String,
    val reconciliationId: String,
    val type: FinancialReconciliationType,
    val expectedAmount: Money,
    val actualAmount: Money,
    val differenceAmount: Money = actualAmount.minus(expectedAmount),
    val currency: String = "BDT",
    val severity: FinancialDiscrepancySeverity,
    val status: FinancialDiscrepancyStatus = FinancialDiscrepancyStatus.OPEN,
    val description: String,
    val detectedAt: Long = System.currentTimeMillis(),
    val resolvedBy: String? = null,
    val resolvedAt: Long? = null,
    val resolutionNote: String? = null,
    val waivedBy: String? = null,
    val waivedAt: Long? = null,
    val waiverReason: String? = null
)

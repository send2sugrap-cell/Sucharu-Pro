package com.sucharu.sucharupro.domain.model.returns

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Financial and commercial resolution breakdown aggregation (Module 11 Step 06).
 */
data class ReturnFinancialBreakdown(
    val resolutionType: ReturnResolutionType,
    val count: Int,
    val totalAmount: Money,
    val percentage: Double
) {
    init {
        require(count >= 0) { "Financial resolution count cannot be negative." }
        require(!totalAmount.isNegative()) { "Total financial resolution amount cannot be negative." }
        require(percentage in 0.0..100.0) { "Percentage must be between 0.0 and 100.0 (was $percentage)." }
    }
}

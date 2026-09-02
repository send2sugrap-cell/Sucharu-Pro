package com.sucharu.sucharupro.domain.model.returns

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Chronological trend point for Return Analytics (Module 11 Step 06).
 */
data class ReturnAnalyticsTrendPoint(
    val timestamp: Long,
    val periodLabel: String,
    val returnCount: Int,
    val acceptedQuantity: Int,
    val rejectedQuantity: Int,
    val financialValue: Money
) {
    init {
        require(timestamp > 0) { "Timestamp must be positive." }
        require(periodLabel.isNotBlank()) { "Period label cannot be blank." }
        require(returnCount >= 0) { "Return count cannot be negative." }
        require(acceptedQuantity >= 0) { "Accepted quantity cannot be negative." }
        require(rejectedQuantity >= 0) { "Rejected quantity cannot be negative." }
        require(!financialValue.isNegative()) { "Financial value cannot be negative." }
    }
}

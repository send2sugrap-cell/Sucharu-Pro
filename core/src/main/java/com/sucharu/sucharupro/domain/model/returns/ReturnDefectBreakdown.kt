package com.sucharu.sucharupro.domain.model.returns

/**
 * Root cause defect breakdown aggregation (Module 11 Step 06).
 */
data class ReturnDefectBreakdown(
    val reason: ReturnReason,
    val count: Int,
    val quantity: Int,
    val percentage: Double
) {
    init {
        require(count >= 0) { "Defect count cannot be negative." }
        require(quantity >= 0) { "Defect quantity cannot be negative." }
        require(percentage in 0.0..100.0) { "Percentage must be between 0.0 and 100.0 (was $percentage)." }
    }
}

package com.sucharu.sucharupro.domain.model.finance

/**
 * Lifecycle status of a detected financial discrepancy (Module 09 Step 08).
 */
enum class FinancialDiscrepancyStatus(val defaultLabel: String) {
    OPEN("Open / Unresolved"),
    UNDER_REVIEW("Under Review"),
    RESOLVED("Resolved & Settled"),
    WAIVED("Waived by Authority");

    val isResolvedOrWaived: Boolean
        get() = this == RESOLVED || this == WAIVED
}

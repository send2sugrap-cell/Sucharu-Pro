package com.sucharu.sucharupro.domain.model.returns

/**
 * Lifecycle states for Customer Return Settlement (Module 11 Step 05).
 */
enum class ReturnSettlementStatus(val defaultLabel: String) {
    PENDING("Pending"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean
        get() = this == COMPLETED || this == CANCELLED

    val displayName: String
        get() = defaultLabel
}

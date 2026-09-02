package com.sucharu.sucharupro.domain.model.finance

/**
 * Lifecycle status of a customer payment in Sucharu Pro (Module 09 Step 03).
 */
enum class CustomerPaymentStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    PENDING("Pending Approval / Posting"),
    POSTED("Posted & Settled"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean
        get() = this == POSTED || this == REJECTED || this == CANCELLED
}

package com.sucharu.sucharupro.domain.model.finance

/**
 * Lifecycle status of a customer receivable obligation (Module 09 Step 02).
 */
enum class CustomerReceivableStatus(val defaultLabel: String) {
    OPEN("Open / Unpaid"),
    PARTIALLY_SETTLED("Partially Paid"),
    SETTLED("Fully Settled"),
    OVERDUE("Overdue"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean
        get() = this == SETTLED || this == CANCELLED
}

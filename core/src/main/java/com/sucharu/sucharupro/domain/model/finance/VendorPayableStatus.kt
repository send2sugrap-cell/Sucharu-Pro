package com.sucharu.sucharupro.domain.model.finance

/**
 * Lifecycle status of a supplier/vendor payable obligation (Module 09 Step 04).
 */
enum class VendorPayableStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    PENDING("Pending Approval"),
    APPROVED("Approved / Open"),
    PARTIALLY_SETTLED("Partially Paid"),
    SETTLED("Fully Settled"),
    OVERDUE("Overdue"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean
        get() = this == SETTLED || this == CANCELLED
}

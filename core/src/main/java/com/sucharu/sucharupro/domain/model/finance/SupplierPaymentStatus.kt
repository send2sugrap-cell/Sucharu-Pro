package com.sucharu.sucharupro.domain.model.finance

/**
 * Lifecycle status of an outbound supplier/vendor payment disbursement (Module 09 Step 05).
 */
enum class SupplierPaymentStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    PENDING("Pending Approval"),
    APPROVED("Approved"),
    POSTED("Posted / Paid"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean
        get() = this == POSTED || this == REJECTED || this == CANCELLED
}

package com.sucharu.sucharupro.domain.model.delivery.verification

/**
 * Status lifecycle enum for Delivery Item Verification (Module 08 Step 04).
 */
enum class DeliveryItemVerificationStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    PENDING("Pending Verification"),
    IN_PROGRESS("In Progress"),
    VERIFIED("Verified"),
    CLOSED("Closed"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean
        get() = this == CLOSED || this == CANCELLED
}

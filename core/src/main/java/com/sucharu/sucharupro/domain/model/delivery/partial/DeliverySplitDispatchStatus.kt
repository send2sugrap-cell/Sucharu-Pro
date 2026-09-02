package com.sucharu.sucharupro.domain.model.delivery.partial

/**
 * Lifecycle status for logical split dispatches (Module 08 Step 06).
 */
enum class DeliverySplitDispatchStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    PENDING("Pending"),
    APPROVED("Approved"),
    READY("Ready"),
    EXECUTED("Executed"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean
        get() = this == DELIVERED || this == CANCELLED
}

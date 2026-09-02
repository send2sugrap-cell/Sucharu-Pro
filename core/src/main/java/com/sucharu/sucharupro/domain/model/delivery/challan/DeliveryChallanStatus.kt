package com.sucharu.sucharupro.domain.model.delivery.challan

/**
 * Represents the lifecycle stages of a Delivery Challan (Module 08 Step 02).
 */
enum class DeliveryChallanStatus {
    DRAFT,
    PENDING,
    APPROVED,
    READY_FOR_DISPATCH,
    DISPATCHED,
    DELIVERED,
    CANCELLED;

    val isTerminal: Boolean
        get() = this == DELIVERED || this == CANCELLED

    val isCommitted: Boolean
        get() = this == APPROVED || this == READY_FOR_DISPATCH || this == DISPATCHED || this == DELIVERED

    val consumesAllocation: Boolean
        get() = isCommitted
}

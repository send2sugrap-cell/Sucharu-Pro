package com.sucharu.sucharupro.domain.model.vendor

/**
 * State machine representing the lifecycle of a Vendor Delivery Receipt (Module 12 Step 06).
 */
enum class VendorDeliveryReceiptStatus {
    DRAFT,
    RECEIVING,
    RECEIVED,
    INSPECTED,
    ACCEPTED,
    PARTIALLY_ACCEPTED,
    REJECTED,
    CANCELLED;

    val isEditable: Boolean get() = this in setOf(DRAFT, RECEIVING)
    val isReceiving: Boolean get() = this == RECEIVING
    val isReceived: Boolean get() = this == RECEIVED
    val isInspected: Boolean get() = this == INSPECTED
    val isAccepted: Boolean get() = this in setOf(ACCEPTED, PARTIALLY_ACCEPTED)
    val isTerminal: Boolean get() = this in setOf(ACCEPTED, PARTIALLY_ACCEPTED, REJECTED, CANCELLED)
    val isCancelled: Boolean get() = this == CANCELLED

    fun canTransitionTo(target: VendorDeliveryReceiptStatus): Boolean {
        if (this == target) return true
        return when (this) {
            DRAFT -> target in setOf(RECEIVING, CANCELLED)
            RECEIVING -> target in setOf(RECEIVED, CANCELLED)
            RECEIVED -> target in setOf(INSPECTED, ACCEPTED, PARTIALLY_ACCEPTED, REJECTED, CANCELLED)
            INSPECTED -> target in setOf(ACCEPTED, PARTIALLY_ACCEPTED, REJECTED, CANCELLED)
            ACCEPTED -> false // Terminal
            PARTIALLY_ACCEPTED -> false // Terminal
            REJECTED -> false // Terminal
            CANCELLED -> false // Terminal
        }
    }
}

package com.sucharu.sucharupro.domain.model.delivery.partial

/**
 * Status lifecycle enum for Delivery Partial Settlement (Module 08 Step 06).
 */
enum class DeliverySettlementStatus(val defaultLabel: String) {
    OPEN("Open"),
    PARTIALLY_DELIVERED("Partially Delivered"),
    FULLY_DELIVERED("Fully Delivered"),
    PARTIALLY_RETURNED("Partially Returned"),
    SETTLEMENT_PENDING("Settlement Pending"),
    SETTLED("Settled"),
    DISPUTED("Disputed"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean
        get() = this == SETTLED || this == CANCELLED
}

package com.sucharu.sucharupro.domain.model.delivery.returning

/**
 * Lifecycle states for Reverse Logistics / Return Shipments (Module 08 Step 07).
 */
enum class DeliveryReturnShipmentStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    READY("Ready for Pickup"),
    PICKUP_SCHEDULED("Pickup Scheduled"),
    PICKED_UP("Picked Up"),
    IN_TRANSIT("In Reverse Transit"),
    DELIVERED_TO_WAREHOUSE("Delivered to Warehouse"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean
        get() = this == DELIVERED_TO_WAREHOUSE || this == CANCELLED
}

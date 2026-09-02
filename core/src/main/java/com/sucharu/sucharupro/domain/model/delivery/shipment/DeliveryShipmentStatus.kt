package com.sucharu.sucharupro.domain.model.delivery.shipment

/**
 * Controlled lifecycle statuses for a Delivery Shipment (Module 08 Step 05).
 */
enum class DeliveryShipmentStatus(val defaultLabel: String) {
    DRAFT("Draft"),
    READY("Ready for Shipment"),
    DISPATCHED("Dispatched"),
    IN_TRANSIT("In Transit"),
    OUT_FOR_DELIVERY("Out for Delivery"),
    DELIVERY_ATTEMPTED("Delivery Attempted"),
    DELAYED("Delayed"),
    ON_HOLD("On Hold"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    val isTerminal: Boolean
        get() = this == DELIVERED || this == CANCELLED
}

package com.sucharu.sucharupro.domain.model.delivery.shipment

/**
 * Controlled event types for shipment tracking timeline (Module 08 Step 05).
 */
enum class DeliveryShipmentEventType(val defaultLabel: String) {
    CREATED("Shipment Created"),
    READY("Ready for Dispatch"),
    DISPATCHED("Dispatched from Facility"),
    PICKED_UP("Picked Up by Carrier"),
    IN_TRANSIT("In Transit"),
    ARRIVED_AT_HUB("Arrived at Sorting Hub"),
    DEPARTED_HUB("Departed Sorting Hub"),
    OUT_FOR_DELIVERY("Out for Delivery"),
    DELIVERY_ATTEMPTED("Delivery Attempted"),
    DELAYED("Shipment Delayed"),
    ON_HOLD("Shipment On Hold"),
    DELIVERED("Delivered"),
    CANCELLED("Shipment Cancelled"),
    STATUS_UPDATED("Status Updated"),
    NOTE_ADDED("Operational Note Added")
}

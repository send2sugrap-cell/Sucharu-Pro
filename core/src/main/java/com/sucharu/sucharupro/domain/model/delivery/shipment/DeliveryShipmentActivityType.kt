package com.sucharu.sucharupro.domain.model.delivery.shipment

/**
 * Controlled activity types for Delivery Shipment audit events (Module 08 Step 05).
 */
enum class DeliveryShipmentActivityType(val defaultLabel: String) {
    CREATED("Shipment Created"),
    UPDATED("Shipment Updated"),
    READY("Marked Ready for Shipment"),
    DISPATCHED("Marked Dispatched"),
    IN_TRANSIT("Status Changed to In Transit"),
    OUT_FOR_DELIVERY("Status Changed to Out for Delivery"),
    DELAYED("Shipment Marked Delayed"),
    ON_HOLD("Shipment Placed On Hold"),
    DELIVERY_ATTEMPTED("Delivery Attempt Recorded"),
    DELIVERED("Shipment Marked Delivered"),
    CANCELLED("Shipment Cancelled"),
    TRACKING_EVENT_ADDED("Tracking Event Added"),
    ATTEMPT_RECORDED("Delivery Attempt Recorded")
}

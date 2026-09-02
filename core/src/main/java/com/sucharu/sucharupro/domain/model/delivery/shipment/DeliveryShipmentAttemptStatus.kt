package com.sucharu.sucharupro.domain.model.delivery.shipment

/**
 * Controlled statuses for a delivery attempt record (Module 08 Step 05).
 */
enum class DeliveryShipmentAttemptStatus(val defaultLabel: String) {
    SUCCESSFUL("Delivery Successful"),
    FAILED("Delivery Failed"),
    RESCHEDULED("Rescheduled"),
    RECIPIENT_UNAVAILABLE("Recipient Unavailable"),
    ADDRESS_ISSUE("Address / Location Inaccessible"),
    OTHER("Other")
}

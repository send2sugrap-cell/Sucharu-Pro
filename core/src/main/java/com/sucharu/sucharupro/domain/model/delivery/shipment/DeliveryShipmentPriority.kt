package com.sucharu.sucharupro.domain.model.delivery.shipment

/**
 * Controlled shipment priority levels (Module 08 Step 05).
 */
enum class DeliveryShipmentPriority(val defaultLabel: String) {
    LOW("Low"),
    NORMAL("Normal"),
    HIGH("High"),
    URGENT("Urgent")
}

package com.sucharu.sucharupro.domain.model.delivery.shipment

/**
 * Controlled delivery / shipment methods (Module 08 Step 05).
 */
enum class DeliveryShipmentType(val defaultLabel: String) {
    STANDARD("Standard Delivery"),
    EXPRESS("Express Delivery"),
    CUSTOMER_PICKUP("Customer Pickup"),
    DIRECT_DELIVERY("Direct Company Delivery"),
    COURIER("Third-Party Courier"),
    INTERNAL("Internal Transfer/Van"),
    OTHER("Other")
}

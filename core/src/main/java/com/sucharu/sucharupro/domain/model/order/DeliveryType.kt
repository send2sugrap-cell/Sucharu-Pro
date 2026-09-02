package com.sucharu.sucharupro.domain.model.order

/**
 * Delivery fulfillment methods for Quotations and Orders in Sucharu Pro.
 */
enum class DeliveryType(val defaultLabel: String) {
    /** Customer picks up from printing press/showroom. */
    PICKUP("Showroom / Factory Pickup"),

    /** Direct business delivery to customer's physical premises. */
    BUSINESS_DELIVERY("Direct Delivery to Client"),

    /** Dispatched via third-party courier / parcel service (e.g. Sundarban, SA Paribahan, Steadfast). */
    COURIER("Third-Party Courier"),

    /** Other custom shipping or dispatch arrangement. */
    OTHER("Other Delivery")
}

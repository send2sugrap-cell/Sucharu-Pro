package com.sucharu.sucharupro.domain.model.customer

/**
 * Address type designation for customer locations.
 */
enum class CustomerAddressType(val defaultLabel: String) {
    /** Primary or head office address. */
    PRIMARY("Primary"),

    /** Address for billing, invoices, and accounting correspondence. */
    BILLING("Billing"),

    /** Delivery location, factory, or warehouse for challan dispatches. */
    DELIVERY("Delivery"),

    /** Miscellaneous other location. */
    OTHER("Other")
}

/**
 * Structured address model for a customer in Sucharu Pro.
 * Supports Bangladesh local geographic hierarchies (Thana/Area, District)
 * as well as international formats.
 */
data class CustomerAddress(
    val addressLine: String,
    val area: String = "",
    val city: String = "",
    val district: String = "",
    val postalCode: String = "",
    val country: String = "Bangladesh",
    val addressType: CustomerAddressType = CustomerAddressType.PRIMARY,
    val isDefault: Boolean = false
) {
    /**
     * Formats the address components into a readable single-line summary.
     */
    fun formatted(): String {
        return listOf(addressLine, area, city, district, postalCode, country)
            .filter { it.isNotBlank() }
            .joinToString(", ")
    }
}

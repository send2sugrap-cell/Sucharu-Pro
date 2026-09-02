package com.sucharu.sucharupro.domain.model.order

/**
 * Customer delivery requirement specifications for a Quotation or Order.
 */
data class DeliveryRequirement(
    val deliveryType: DeliveryType = DeliveryType.PICKUP,
    val requiredDate: String? = null,
    val address: String? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val instructions: String? = null
) {
    companion object {
        val DEFAULT_PICKUP = DeliveryRequirement(
            deliveryType = DeliveryType.PICKUP,
            instructions = "Pickup from Sucharu Pro main showroom."
        )
    }
}

package com.sucharu.sucharupro.data.api.model.offer

import kotlinx.serialization.Serializable

/**
 * Customer Information payload for Single-Page Offer Checkout.
 */
@Serializable
data class OfferCheckoutCustomerInfoDto(
    val displayName: String,
    val phone: String,
    val email: String? = null,
    val deliveryAddress: String
)

/**
 * Request payload for single-page Offer Checkout & Auto-Registration.
 */
@Serializable
data class OfferCheckoutRequestDto(
    val offerId: String,
    val offerTitle: String,
    val customer: OfferCheckoutCustomerInfoDto,
    val designOption: String = "CUSTOMER_DESIGN", // "CUSTOMER_DESIGN" or "DESIGNER_ASSISTANCE"
    val designFileName: String? = null,
    val designFileUrl: String? = null,
    val specialInstructions: String? = null,
    val paymentMethod: String = "COD", // "COD", "BKASH", "NAGAD", "ROCKET", "PARTIAL_ADVANCE"
    val deliveryZone: String = "INSIDE_DHAKA",
    val idempotencyKey: String = "",
    val leadSource: String = "PROMOTIONAL_CHECKOUT",
    val campaignId: String? = "CAMP-2026-EID",
    val utmSource: String? = null,
    val utmMedium: String? = null,
    val utmCampaign: String? = null
)

/**
 * Response payload returned after successful Offer Checkout completion.
 */
@Serializable
data class OfferCheckoutResponseDto(
    val success: Boolean,
    val orderId: String,
    val orderNumber: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val offerId: String,
    val offerTitle: String,
    val basePrice: Double,
    val deliveryCharge: Double,
    val discountAmount: Double,
    val totalAmount: Double,
    val paymentStatus: String = "PENDING",
    val orderStatus: String = "CONFIRMED",
    val message: String = "🎉 অর্ডার সফলভাবে নেওয়া হয়েছে!",
    val whatsAppEventEmitted: Boolean = true
)

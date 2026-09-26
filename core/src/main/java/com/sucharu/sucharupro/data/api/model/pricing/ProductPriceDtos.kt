package com.sucharu.sucharupro.data.api.model.pricing

import kotlinx.serialization.Serializable

@Serializable
data class QuantityPriceTierDto(
    val tierId: String,
    val priceConfigId: String,
    val quantityBreak: Int,
    val tierPrice: Double,
    val unitRate: Double
)

@Serializable
data class ProductPriceConfigurationDto(
    val priceConfigId: String,
    val productId: String,
    val priceVersion: Int = 1,
    val isActive: Boolean = true,
    val currency: String = "BDT",

    val basePrice: Double,
    val baseQuantity: Int = 1000,
    val unit: String = "PCS",
    val minOrderQuantity: Int = 1000,
    val maxOrderQuantity: Int = 1000000,

    val pricingModel: String = "BREAK_PRICE",
    val discountType: String = "NONE",
    val discountValue: Double = 0.0,

    val quantityTiers: List<QuantityPriceTierDto> = emptyList(),

    val surchargeAmount: Double = 0.0,
    val insideDhakaDeliveryCharge: Double = 60.0,
    val outsideDhakaDeliveryCharge: Double = 120.0,
    val taxPercentage: Double = 0.0,
    val roundingRule: String = "NEAREST",

    val validFrom: String? = null,
    val validUntil: String? = null,

    val createdAt: String = "",
    val updatedAt: String = "",
    val createdBy: String = ""
)

@Serializable
data class EvaluateCommercialPriceRequestDto(
    val productId: String,
    val quantity: Int = 1000,
    val customerId: String? = null,
    val offerId: String? = null,
    val deliveryZone: String? = "INSIDE_DHAKA"
)

@Serializable
data class CommercialPriceEvaluationDto(
    val productId: String,
    val priceConfigId: String,
    val priceVersion: Int,
    val currency: String = "BDT",

    val orderQuantity: Int,
    val basePriceSnapshot: Double,
    val matchedTierQuantityBreak: Int? = null,
    val matchedTierPrice: Double? = null,

    val subtotalAmount: Double,
    val discountAmount: Double,
    val surchargeAmount: Double,
    val deliveryCharge: Double,
    val taxAmount: Double,
    val roundingAdjustment: Double,
    val grandTotalAmount: Double,

    val appliedOfferId: String? = null,
    val appliedOfferCode: String? = null,
    val appliedOfferDiscountAmount: Double = 0.0,

    val calculatedAt: String
)

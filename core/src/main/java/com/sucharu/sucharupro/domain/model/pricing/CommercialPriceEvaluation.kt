package com.sucharu.sucharupro.domain.model.pricing

/**
 * Deterministic Commercial Price Evaluation Result.
 */
data class CommercialPriceEvaluation(
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

package com.sucharu.sucharupro.domain.model.pricing

/**
 * Immutable Order Price Snapshot Entity.
 *
 * Preserves historical commercial truth for an order even if Master Price is changed later.
 */
data class OrderPriceSnapshot(
    val snapshotId: String,
    val orderId: String,
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

    val appliedOfferSnapshot: AppliedOfferSnapshot? = null,

    val calculatedAt: String,
    val createdAt: String
) {
    init {
        require(snapshotId.isNotBlank()) { "Snapshot ID cannot be blank." }
        require(orderId.isNotBlank()) { "Order ID cannot be blank." }
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        require(priceConfigId.isNotBlank()) { "Price Config ID cannot be blank." }
        require(orderQuantity > 0) { "Order Quantity must be greater than zero." }
    }
}

/**
 * Immutable Applied Offer Snapshot Entity.
 */
data class AppliedOfferSnapshot(
    val appliedOfferSnapshotId: String,
    val snapshotId: String,
    val offerId: String,
    val offerCode: String,
    val offerName: String,
    val appliedDiscountAmount: Double,
    val eligibilityAudience: String = "CUSTOMER",
    val createdAt: String
)

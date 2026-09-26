package com.sucharu.sucharupro.domain.model.pricing

import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit

/**
 * Form 04 — Product Price Configuration Domain Entity.
 *
 * Defines canonical commercial pricing configuration for a product.
 */
data class ProductPriceConfiguration(
    val priceConfigId: String,
    val productId: String,
    val priceVersion: Int = 1,
    val isActive: Boolean = true,
    val currency: String = "BDT",

    // Base Pricing
    val basePrice: Double,
    val baseQuantity: Int = 1000,
    val unit: InventoryUnit = InventoryUnit.PCS,
    val minOrderQuantity: Int = 1000,
    val maxOrderQuantity: Int = 1000000,

    // Pricing Model & Discounts
    val pricingModel: PricingModel = PricingModel.BREAK_PRICE,
    val discountType: CommercialDiscountType = CommercialDiscountType.NONE,
    val discountValue: Double = 0.0,

    // Tiers
    val quantityTiers: List<QuantityPriceTier> = emptyList(),

    // Additional Charges
    val surchargeAmount: Double = 0.0,
    val insideDhakaDeliveryCharge: Double = 60.0,
    val outsideDhakaDeliveryCharge: Double = 120.0,
    val taxPercentage: Double = 0.0,
    val roundingRule: RoundingRule = RoundingRule.NEAREST,

    val validFrom: String? = null,
    val validUntil: String? = null,

    val createdAt: String,
    val updatedAt: String,
    val createdBy: String,
    val updatedBy: String? = null
) {
    init {
        require(priceConfigId.isNotBlank()) { "Price Config ID cannot be blank." }
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        require(basePrice >= 0.0) { "Base Price cannot be negative." }
        require(baseQuantity > 0) { "Base Quantity must be greater than zero." }
        require(minOrderQuantity > 0) { "Minimum Order Quantity must be greater than zero." }
        require(maxOrderQuantity >= minOrderQuantity) { "Maximum Order Quantity ($maxOrderQuantity) cannot be less than Minimum Order Quantity ($minOrderQuantity)." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "updatedAt timestamp cannot be blank." }
        require(createdBy.isNotBlank()) { "createdBy actor cannot be blank." }
    }
}

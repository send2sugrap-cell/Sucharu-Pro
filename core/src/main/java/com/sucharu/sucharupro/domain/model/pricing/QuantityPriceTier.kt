package com.sucharu.sucharupro.domain.model.pricing

/**
 * Quantity Price Tier for Form 04 Commercial Pricing.
 */
data class QuantityPriceTier(
    val tierId: String,
    val priceConfigId: String,
    val quantityBreak: Int,
    val tierPrice: Double,
    val unitRate: Double,
    val createdAt: String
) {
    init {
        require(tierId.isNotBlank()) { "Tier ID cannot be blank." }
        require(priceConfigId.isNotBlank()) { "Price Config ID cannot be blank." }
        require(quantityBreak > 0) { "Quantity Break must be greater than zero." }
        require(tierPrice >= 0.0) { "Tier Price cannot be negative." }
        require(unitRate >= 0.0) { "Unit Rate cannot be negative." }
    }
}

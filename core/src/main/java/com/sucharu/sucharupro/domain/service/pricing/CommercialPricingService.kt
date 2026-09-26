package com.sucharu.sucharupro.domain.service.pricing

import com.sucharu.sucharupro.domain.model.pricing.AppliedOfferSnapshot
import com.sucharu.sucharupro.domain.model.pricing.CommercialDiscountType
import com.sucharu.sucharupro.domain.model.pricing.CommercialPriceEvaluation
import com.sucharu.sucharupro.domain.model.pricing.OrderPriceSnapshot
import com.sucharu.sucharupro.domain.model.pricing.ProductPriceConfiguration
import com.sucharu.sucharupro.domain.repository.ProductPriceRepository
import java.util.UUID

/**
 * Domain Service for Form 04 — Commercial Pricing Evaluation & Immutable Price Snapshot Governance.
 */
class CommercialPricingService(
    private val repository: ProductPriceRepository
) {
    suspend fun createPriceConfig(config: ProductPriceConfiguration): ProductPriceConfiguration {
        require(config.productId.isNotBlank()) { "Product ID is required for pricing configuration." }
        require(config.basePrice >= 0.0) { "Base price cannot be negative." }
        return repository.createPriceConfiguration(config)
    }

    suspend fun listAllPriceConfigurations(): List<ProductPriceConfiguration> {
        return repository.getAllPriceConfigurations()
    }

    /**
     * Deterministically evaluates commercial price for [productId] and [quantity].
     */
    suspend fun evaluateCommercialPrice(
        productId: String,
        quantity: Int,
        customerId: String? = null,
        offerId: String? = null,
        deliveryZone: String? = "INSIDE_DHAKA"
    ): CommercialPriceEvaluation {
        val config = repository.getActivePriceConfigurationByProduct(productId)
            ?: throw IllegalArgumentException("No active commercial pricing configuration found for Product ID: $productId")

        require(quantity >= config.minOrderQuantity) {
            "Order quantity ($quantity) is below minimum order quantity (${config.minOrderQuantity})."
        }
        require(quantity <= config.maxOrderQuantity) {
            "Order quantity ($quantity) exceeds maximum order quantity (${config.maxOrderQuantity})."
        }

        // 1. Matched Quantity Tier evaluation
        val matchedTier = config.quantityTiers
            .filter { quantity >= it.quantityBreak }
            .maxByOrNull { it.quantityBreak }

        val subtotal = if (matchedTier != null) {
            matchedTier.tierPrice * (quantity.toDouble() / matchedTier.quantityBreak)
        } else {
            config.basePrice * (quantity.toDouble() / config.baseQuantity)
        }

        // 2. Base Discount evaluation
        val baseDiscount = when (config.discountType) {
            CommercialDiscountType.PERCENTAGE -> subtotal * (config.discountValue / 100.0)
            CommercialDiscountType.FIXED_AMOUNT -> config.discountValue
            CommercialDiscountType.SPECIAL_PRICE -> (subtotal - config.discountValue).coerceAtLeast(0.0)
            CommercialDiscountType.NONE -> 0.0
        }

        // 3. Delivery Charge evaluation
        val deliveryCharge = if (deliveryZone.equals("OUTSIDE_DHAKA", ignoreCase = true)) {
            config.outsideDhakaDeliveryCharge
        } else {
            config.insideDhakaDeliveryCharge
        }

        // 4. Tax evaluation
        val taxAmount = (subtotal - baseDiscount) * (config.taxPercentage / 100.0)

        // 5. Grand Total
        val rawTotal = (subtotal - baseDiscount) + config.surchargeAmount + deliveryCharge + taxAmount
        val roundedTotal = kotlin.math.round(rawTotal)
        val roundingAdjustment = roundedTotal - rawTotal

        val timestamp = "2026-09-26T12:30:00Z"

        return CommercialPriceEvaluation(
            productId = productId,
            priceConfigId = config.priceConfigId,
            priceVersion = config.priceVersion,
            currency = config.currency,
            orderQuantity = quantity,
            basePriceSnapshot = config.basePrice,
            matchedTierQuantityBreak = matchedTier?.quantityBreak,
            matchedTierPrice = matchedTier?.tierPrice,
            subtotalAmount = subtotal,
            discountAmount = baseDiscount,
            surchargeAmount = config.surchargeAmount,
            deliveryCharge = deliveryCharge,
            taxAmount = taxAmount,
            roundingAdjustment = roundingAdjustment,
            grandTotalAmount = roundedTotal,
            appliedOfferId = offerId,
            calculatedAt = timestamp
        )
    }

    /**
     * Saves an immutable Order Price Snapshot.
     * (CRITICAL INVARIANT: Preserves historical order price truth even if Master Price changes later!)
     */
    suspend fun createOrderPriceSnapshot(orderId: String, evaluation: CommercialPriceEvaluation): OrderPriceSnapshot {
        val snapshotId = "SNAP-" + UUID.randomUUID().toString().take(8).uppercase()
        val timestamp = "2026-09-26T12:30:00Z"

        val snapshot = OrderPriceSnapshot(
            snapshotId = snapshotId,
            orderId = orderId,
            productId = evaluation.productId,
            priceConfigId = evaluation.priceConfigId,
            priceVersion = evaluation.priceVersion,
            currency = evaluation.currency,
            orderQuantity = evaluation.orderQuantity,
            basePriceSnapshot = evaluation.basePriceSnapshot,
            matchedTierQuantityBreak = evaluation.matchedTierQuantityBreak,
            matchedTierPrice = evaluation.matchedTierPrice,
            subtotalAmount = evaluation.subtotalAmount,
            discountAmount = evaluation.discountAmount,
            surchargeAmount = evaluation.surchargeAmount,
            deliveryCharge = evaluation.deliveryCharge,
            taxAmount = evaluation.taxAmount,
            roundingAdjustment = evaluation.roundingAdjustment,
            grandTotalAmount = evaluation.grandTotalAmount,
            appliedOfferSnapshot = evaluation.appliedOfferId?.let { id ->
                AppliedOfferSnapshot(
                    appliedOfferSnapshotId = "OFFSNAP-$snapshotId",
                    snapshotId = snapshotId,
                    offerId = id,
                    offerCode = evaluation.appliedOfferCode ?: "EID-2026",
                    offerName = "Promotional Offer",
                    appliedDiscountAmount = evaluation.appliedOfferDiscountAmount,
                    createdAt = timestamp
                )
            },
            calculatedAt = evaluation.calculatedAt,
            createdAt = timestamp
        )

        return repository.saveOrderPriceSnapshot(snapshot)
    }

    suspend fun getOrderPriceSnapshot(orderId: String): OrderPriceSnapshot? {
        return repository.getOrderPriceSnapshotByOrder(orderId)
    }
}

package com.sucharu.sucharupro.domain.service.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.PricingMethod
import com.sucharu.sucharupro.domain.model.vendor.VendorServiceRate
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure deterministic pricing calculation engine for vendor service rates (Module 12 Step 03).
 * Operates purely on BigDecimal / Money without side-effects.
 */
object VendorServiceRateCalculator {

    fun calculateEstimatedCost(
        rate: VendorServiceRate,
        quantity: BigDecimal,
        areaSqFt: BigDecimal? = null,
        weightKg: BigDecimal? = null,
        durationHours: BigDecimal? = null
    ): Money {
        require(quantity >= BigDecimal.ZERO) { "Quantity cannot be negative" }

        return when (rate.pricingMethod) {
            PricingMethod.FIXED -> rate.rateAmount

            PricingMethod.PER_UNIT, PricingMethod.PER_QUANTITY -> {
                val effectiveQty = if (quantity < rate.minimumQuantity) rate.minimumQuantity else quantity
                rate.rateAmount * effectiveQty
            }

            PricingMethod.PER_AREA -> {
                val area = areaSqFt ?: quantity
                require(area >= BigDecimal.ZERO) { "Area cannot be negative" }
                rate.rateAmount * area
            }

            PricingMethod.PER_WEIGHT -> {
                val weight = weightKg ?: quantity
                require(weight >= BigDecimal.ZERO) { "Weight cannot be negative" }
                rate.rateAmount * weight
            }

            PricingMethod.PER_TIME -> {
                val time = durationHours ?: quantity
                require(time >= BigDecimal.ZERO) { "Duration cannot be negative" }
                rate.rateAmount * time
            }

            PricingMethod.TIERED -> {
                if (rate.tiers.isEmpty()) {
                    rate.rateAmount * quantity
                } else {
                    val matchingTier = rate.tiers.firstOrNull { tier ->
                        quantity >= tier.minimumQuantity && (tier.maximumQuantity == null || quantity <= tier.maximumQuantity)
                    } ?: rate.tiers.last()
                    matchingTier.rateAmount * quantity
                }
            }
        }
    }
}

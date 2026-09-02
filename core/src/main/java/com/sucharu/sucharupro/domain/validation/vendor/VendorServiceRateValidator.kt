package com.sucharu.sucharupro.domain.validation.vendor

import com.sucharu.sucharupro.domain.model.vendor.PricingMethod
import com.sucharu.sucharupro.domain.model.vendor.RateStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorServiceRate
import com.sucharu.sucharupro.domain.model.vendor.VendorServiceRateTier
import java.math.BigDecimal

/**
 * Domain-level validator enforcing business and financial integrity rules on VendorServiceRate entities (Module 12 Step 03).
 */
object VendorServiceRateValidator {

    private const val MAX_CODE_LENGTH = 64
    private const val MAX_NAME_LENGTH = 150
    private const val MAX_NOTES_LENGTH = 2000

    fun validate(rate: VendorServiceRate): VendorValidationResult {
        val errors = mutableListOf<String>()

        if (rate.rateId.isBlank()) {
            errors.add("rateId is required and cannot be blank")
        }
        if (rate.projectId.isBlank()) {
            errors.add("projectId is required and cannot be blank")
        }
        if (rate.vendorId.isBlank()) {
            errors.add("vendorId is required and cannot be blank")
        }
        if (rate.rateCode.isBlank()) {
            errors.add("rateCode is required and cannot be blank")
        } else if (rate.rateCode.length > MAX_CODE_LENGTH) {
            errors.add("rateCode exceeds maximum length of $MAX_CODE_LENGTH characters")
        }

        if (rate.serviceName.isBlank()) {
            errors.add("serviceName is required and cannot be blank")
        } else if (rate.serviceName.length > MAX_NAME_LENGTH) {
            errors.add("serviceName exceeds maximum length of $MAX_NAME_LENGTH characters")
        }

        if (rate.rateAmount.isNegative()) {
            errors.add("rateAmount cannot be negative")
        }

        if (rate.minimumQuantity < BigDecimal.ZERO) {
            errors.add("minimumQuantity cannot be negative")
        }

        rate.maximumQuantity?.let { maxQty ->
            if (maxQty <= rate.minimumQuantity) {
                errors.add("maximumQuantity must be greater than minimumQuantity")
            }
        }

        if (rate.effectiveFrom <= 0) {
            errors.add("effectiveFrom must be a valid positive timestamp")
        }

        rate.effectiveTo?.let { toDate ->
            if (toDate < rate.effectiveFrom) {
                errors.add("effectiveTo must be greater than or equal to effectiveFrom")
            }
        }

        rate.notes?.let {
            if (it.length > MAX_NOTES_LENGTH) {
                errors.add("notes exceed maximum length of $MAX_NOTES_LENGTH characters")
            }
        }

        if (rate.pricingMethod == PricingMethod.TIERED) {
            if (rate.tiers.isEmpty()) {
                errors.add("Tiered pricing method requires at least one rate tier definition")
            } else {
                val tierErrors = validateTiers(rate.tiers)
                errors.addAll(tierErrors)
            }
        }

        return if (errors.isEmpty()) VendorValidationResult(true) else VendorValidationResult(false, errors)
    }

    fun validateTiers(tiers: List<VendorServiceRateTier>): List<String> {
        val errors = mutableListOf<String>()

        var previousMax: BigDecimal? = null

        for ((index, tier) in tiers.withIndex()) {
            if (tier.minimumQuantity < BigDecimal.ZERO) {
                errors.add("Tier [${index + 1}]: minimumQuantity cannot be negative")
            }
            if (tier.rateAmount.isNegative()) {
                errors.add("Tier [${index + 1}]: rateAmount cannot be negative")
            }
            tier.maximumQuantity?.let { maxQty ->
                if (maxQty <= tier.minimumQuantity) {
                    errors.add("Tier [${index + 1}]: maximumQuantity must be greater than minimumQuantity")
                }
            }

            if (previousMax != null && tier.minimumQuantity < previousMax) {
                errors.add("Tier [${index + 1}]: tier minimumQuantity (${tier.minimumQuantity}) overlaps with previous tier maximum ($previousMax)")
            }
            previousMax = tier.maximumQuantity
        }

        return errors
    }

    fun validateStatusTransition(current: RateStatus, target: RateStatus): VendorValidationResult {
        return if (current.canTransitionTo(target)) {
            VendorValidationResult(true)
        } else {
            VendorValidationResult(
                false,
                listOf("Illegal rate status transition from '${current.name}' to '${target.name}'.")
            )
        }
    }
}

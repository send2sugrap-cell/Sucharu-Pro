package com.sucharu.sucharupro.domain.validation.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.VendorCostAttribution
import java.math.BigDecimal

/**
 * Domain validator for Vendor Profitability requests and attributions.
 * Module 16 Step 05.
 */
object VendorProfitabilityValidator {

    fun validateCalculateRequest(
        tenantId: String,
        projectId: String,
        vendorId: String,
        customBaselineCost: BigDecimal? = null
    ): DomainResult<Unit> {
        val errors = mutableListOf<String>()

        if (tenantId.isBlank()) errors.add("Tenant ID must not be blank")
        if (projectId.isBlank()) errors.add("Project ID must not be blank")
        if (vendorId.isBlank()) errors.add("Vendor ID must not be blank")
        if (customBaselineCost != null && customBaselineCost < BigDecimal.ZERO) {
            errors.add("Custom baseline cost cannot be negative")
        }

        return if (errors.isEmpty()) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = errors.joinToString("; ")
            )
        }
    }

    fun validateCostAttribution(attribution: VendorCostAttribution): DomainResult<Unit> {
        val errors = mutableListOf<String>()

        if (attribution.costAttributionId.isBlank()) errors.add("Cost attribution ID must not be blank")
        if (attribution.tenantId.isBlank()) errors.add("Tenant ID must not be blank")
        if (attribution.projectId.isBlank()) errors.add("Project ID must not be blank")
        if (attribution.vendorId.isBlank()) errors.add("Vendor ID must not be blank")
        if (attribution.attributedAmount < BigDecimal.ZERO) errors.add("Attributed cost amount cannot be negative")
        if (attribution.sourceEntityId.isBlank()) errors.add("Source entity ID must not be blank")

        return if (errors.isEmpty()) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = errors.joinToString("; ")
            )
        }
    }
}

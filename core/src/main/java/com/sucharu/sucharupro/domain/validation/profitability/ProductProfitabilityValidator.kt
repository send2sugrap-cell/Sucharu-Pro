package com.sucharu.sucharupro.domain.validation.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.ProductCostAllocationBasis
import com.sucharu.sucharupro.domain.model.profitability.ProductCostAttribution
import com.sucharu.sucharupro.domain.model.profitability.ProductRevenueAttribution
import java.math.BigDecimal

/**
 * Domain Validator for Product Profitability & Unit Economics (Module 16 Step 03).
 */
object ProductProfitabilityValidator {

    fun validateCalculationRequest(
        tenantId: String,
        projectId: String,
        productId: String
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID is required for Product Profitability calculation.")
        }
        if (projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID is required for Product Profitability calculation.")
        }
        if (productId.isBlank()) {
            return DomainResult.Error(message = "Product ID is required for Product Profitability calculation.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateRevenueAttribution(attribution: ProductRevenueAttribution): List<String> {
        val errors = mutableListOf<String>()
        if (attribution.productId.isBlank()) {
            errors.add("Product ID cannot be blank in revenue attribution.")
        }
        if (attribution.recognizedRevenue.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Recognized revenue cannot be negative.")
        }
        if (attribution.sourceEntityId.isBlank()) {
            errors.add("Source entity ID cannot be blank in revenue attribution.")
        }
        return errors
    }

    fun validateCostAttribution(attribution: ProductCostAttribution): List<String> {
        val errors = mutableListOf<String>()
        if (attribution.productId.isBlank()) {
            errors.add("Product ID cannot be blank in cost attribution.")
        }
        if (attribution.attributedAmount.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Attributed cost amount cannot be negative.")
        }
        if (attribution.allocationBasis != ProductCostAllocationBasis.DIRECT) {
            if (attribution.denominator != null && attribution.denominator.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Allocation denominator must be greater than zero.")
            }
        }
        if (attribution.sourceEntityId.isBlank()) {
            errors.add("Source entity ID cannot be blank in cost attribution.")
        }
        return errors
    }
}

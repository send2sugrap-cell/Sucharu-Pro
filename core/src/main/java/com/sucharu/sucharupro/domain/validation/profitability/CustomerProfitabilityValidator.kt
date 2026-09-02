package com.sucharu.sucharupro.domain.validation.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.CustomerCostAttribution
import com.sucharu.sucharupro.domain.model.profitability.CustomerRevenueAttribution
import java.math.BigDecimal

/**
 * Domain Validator for Customer Profitability & Contribution Analysis (Module 16 Step 04).
 */
object CustomerProfitabilityValidator {

    fun validateCalculationRequest(
        tenantId: String,
        projectId: String,
        customerId: String,
        periodStart: Long? = null,
        periodEnd: Long? = null
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID is required for customer profitability calculation.")
        }
        if (projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID is required for customer profitability calculation.")
        }
        if (customerId.isBlank()) {
            return DomainResult.Error(message = "Customer ID is required for customer profitability calculation.")
        }
        if (periodStart != null && periodEnd != null && periodStart > periodEnd) {
            return DomainResult.Error(message = "Period start timestamp cannot be greater than period end timestamp.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateRevenueAttribution(attribution: CustomerRevenueAttribution): List<String> {
        val errors = mutableListOf<String>()
        if (attribution.customerId.isBlank()) {
            errors.add("Customer ID cannot be blank in revenue attribution.")
        }
        if (attribution.recognizedRevenue.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Recognized revenue cannot be negative.")
        }
        if (attribution.sourceEntityId.isBlank()) {
            errors.add("Source entity ID cannot be blank in revenue attribution.")
        }
        return errors
    }

    fun validateCostAttribution(attribution: CustomerCostAttribution): List<String> {
        val errors = mutableListOf<String>()
        if (attribution.customerId.isBlank()) {
            errors.add("Customer ID cannot be blank in cost attribution.")
        }
        if (attribution.attributedAmount.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Attributed cost amount cannot be negative.")
        }
        if (attribution.allocationBasis != "DIRECT") {
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

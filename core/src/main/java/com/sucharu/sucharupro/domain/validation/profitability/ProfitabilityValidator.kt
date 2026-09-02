package com.sucharu.sucharupro.domain.validation.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.ProfitabilityScope

/**
 * Validation rules and assertions for Profitability Analysis (Module 16 Step 01).
 */
object ProfitabilityValidator {

    fun validateSnapshotGeneration(
        tenantId: String,
        projectId: String,
        scope: ProfitabilityScope,
        targetEntityId: String?,
        periodId: String?,
        currency: String
    ): DomainResult<Boolean> {
        if (tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank for profitability snapshot generation")
        }
        if (projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank for profitability snapshot generation")
        }
        if (currency.isBlank()) {
            return DomainResult.Error(message = "Currency cannot be blank")
        }

        when (scope) {
            ProfitabilityScope.JOB -> {
                if (targetEntityId.isNullOrBlank()) {
                    return DomainResult.Error(message = "Target Entity ID (jobId) is required for JOB scope profitability analysis")
                }
            }
            ProfitabilityScope.PRODUCT -> {
                if (targetEntityId.isNullOrBlank()) {
                    return DomainResult.Error(message = "Target Entity ID (productId) is required for PRODUCT scope profitability analysis")
                }
            }
            ProfitabilityScope.CUSTOMER -> {
                if (targetEntityId.isNullOrBlank()) {
                    return DomainResult.Error(message = "Target Entity ID (customerId) is required for CUSTOMER scope profitability analysis")
                }
            }
            ProfitabilityScope.VENDOR -> {
                if (targetEntityId.isNullOrBlank()) {
                    return DomainResult.Error(message = "Target Entity ID (vendorId) is required for VENDOR scope profitability analysis")
                }
            }
            ProfitabilityScope.PERIOD -> {
                if (periodId.isNullOrBlank() && targetEntityId.isNullOrBlank()) {
                    return DomainResult.Error(message = "Period ID is required for PERIOD scope profitability analysis")
                }
            }
            ProfitabilityScope.BUSINESS, ProfitabilityScope.PROJECT -> {
                // Whole business / project level analysis - targetEntityId optional
            }
        }

        return DomainResult.Success(true)
    }

    fun validateIdempotencyKey(key: String?): Boolean {
        if (key == null) return true
        return key.isNotBlank() && key.length <= 128
    }
}

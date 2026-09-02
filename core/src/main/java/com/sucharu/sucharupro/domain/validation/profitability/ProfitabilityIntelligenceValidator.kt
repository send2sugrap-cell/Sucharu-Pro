package com.sucharu.sucharupro.domain.validation.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.CrossDimensionRankingCriteria
import com.sucharu.sucharupro.domain.model.profitability.ProfitabilityDimensionType

/**
 * Domain validator for Cross-Dimensional Profitability Intelligence inputs and parameters.
 * Module 16 Step 07.
 */
object ProfitabilityIntelligenceValidator {

    fun validateTenantAndProject(tenantId: String, projectId: String): DomainResult<Unit> {
        if (tenantId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID cannot be blank.")
        }
        if (projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        return DomainResult.Success(Unit)
    }

    fun validatePeriodId(periodId: String): DomainResult<Unit> {
        if (periodId.isBlank()) {
            return DomainResult.Error(message = "Period ID cannot be blank.")
        }
        if (periodId.length > 64) {
            return DomainResult.Error(message = "Period ID exceeds maximum length of 64 characters.")
        }
        return DomainResult.Success(Unit)
    }

    fun validatePagination(limit: Int, offset: Int): DomainResult<Unit> {
        if (limit <= 0 || limit > 500) {
            return DomainResult.Error(message = "Pagination limit must be between 1 and 500.")
        }
        if (offset < 0) {
            return DomainResult.Error(message = "Pagination offset cannot be negative.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateDimensionType(dimensionType: ProfitabilityDimensionType?): DomainResult<Unit> {
        if (dimensionType == null) {
            return DomainResult.Error(message = "Dimension type cannot be null.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateEntityId(entityId: String?): DomainResult<Unit> {
        if (entityId.isNullOrBlank()) {
            return DomainResult.Error(message = "Entity ID cannot be blank.")
        }
        return DomainResult.Success(Unit)
    }
}

package com.sucharu.sucharupro.domain.validation.businesscost

import com.sucharu.sucharupro.domain.model.businesscost.BusinessCostCategory
import com.sucharu.sucharupro.domain.model.businesscost.BusinessCostCenter
import com.sucharu.sucharupro.domain.model.businesscost.BusinessCostTracking
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.math.BigDecimal

/**
 * Domain Validators for Cost Centers, Categories, Cost Tracking, and Reclassification (Module 15 Step 04).
 */
object BusinessCostValidators {

    private const val MAX_SCALE = 4

    fun validatePrecision(amount: BigDecimal, fieldName: String = "Amount"): DomainResult<Unit> {
        if (amount.scale() > MAX_SCALE) {
            return DomainResult.Error(
                message = "$fieldName precision cannot exceed $MAX_SCALE decimal places (got scale ${amount.scale()})."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateCurrency(currency: String): DomainResult<Unit> {
        if (currency.isBlank() || currency.length != 3 || !currency.all { it.isLetter() }) {
            return DomainResult.Error(
                message = "Currency code must be a valid 3-letter ISO-4217 code, got '$currency'."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateCostCenter(
        code: String,
        name: String,
        tenantId: String,
        projectId: String,
        costCenterId: String? = null,
        parentCostCenterId: String? = null
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID cannot be blank.")
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (code.trim().length < 2) return DomainResult.Error(message = "Cost center code must be at least 2 characters.")
        if (name.trim().length < 2) return DomainResult.Error(message = "Cost center name must be at least 2 characters.")
        if (costCenterId != null && parentCostCenterId != null && costCenterId == parentCostCenterId) {
            return DomainResult.Error(message = "A cost center cannot be its own parent.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateCostCategory(
        code: String,
        name: String,
        tenantId: String,
        projectId: String,
        categoryId: String? = null,
        parentCategoryId: String? = null
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID cannot be blank.")
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (code.trim().length < 2) return DomainResult.Error(message = "Cost category code must be at least 2 characters.")
        if (name.trim().length < 2) return DomainResult.Error(message = "Cost category name must be at least 2 characters.")
        if (categoryId != null && parentCategoryId != null && categoryId == parentCategoryId) {
            return DomainResult.Error(message = "A cost category cannot be its own parent.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateCostTracking(
        tenantId: String,
        projectId: String,
        sourceId: String,
        costCenter: BusinessCostCenter?,
        costCategory: BusinessCostCategory?,
        amount: BigDecimal,
        currency: String,
        createdBy: String
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID cannot be blank.")
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (sourceId.isBlank()) return DomainResult.Error(message = "Source ID cannot be blank.")
        if (createdBy.isBlank()) return DomainResult.Error(message = "Created by actor ID cannot be blank.")

        if (costCenter == null) {
            return DomainResult.Error(message = "Cost center not found or invalid.")
        }
        if (!costCenter.isActive) {
            return DomainResult.Error(message = "Inactive cost center '${costCenter.name}' cannot be used for new cost tracking.")
        }

        if (costCategory == null) {
            return DomainResult.Error(message = "Cost category not found or invalid.")
        }
        if (!costCategory.isActive) {
            return DomainResult.Error(message = "Inactive cost category '${costCategory.name}' cannot be used for new cost tracking.")
        }

        if (amount < BigDecimal.ZERO) {
            return DomainResult.Error(message = "Tracked cost amount cannot be negative.")
        }

        val precRes = validatePrecision(amount, "Tracked amount")
        if (precRes is DomainResult.Error) return precRes

        val currRes = validateCurrency(currency)
        if (currRes is DomainResult.Error) return currRes

        return DomainResult.Success(Unit)
    }

    fun validateReclassification(
        existingTracking: BusinessCostTracking,
        newCostCenter: BusinessCostCenter,
        newCostCategory: BusinessCostCategory,
        reason: String,
        actorId: String
    ): DomainResult<Unit> {
        if (actorId.isBlank()) return DomainResult.Error(message = "Actor ID cannot be blank.")
        if (reason.trim().length < 3) {
            return DomainResult.Error(message = "A mandatory reason (at least 3 characters) must be provided when reclassifying costs.")
        }
        if (!newCostCenter.isActive) {
            return DomainResult.Error(message = "Target cost center '${newCostCenter.name}' is inactive and cannot receive reclassified costs.")
        }
        if (!newCostCategory.isActive) {
            return DomainResult.Error(message = "Target cost category '${newCostCategory.name}' is inactive and cannot receive reclassified costs.")
        }
        return DomainResult.Success(Unit)
    }
}

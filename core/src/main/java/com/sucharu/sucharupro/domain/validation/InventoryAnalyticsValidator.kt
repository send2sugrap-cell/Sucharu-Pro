package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Validator for Inventory Analytics (Module 07 Step 10).
 */
object InventoryAnalyticsValidator {

    /**
     * Validates that the custom date range is valid.
     * Ensure start <= end and neither are in the future.
     */
    fun validateCustomDateRange(start: Long?, end: Long?): DomainResult<Unit> {
        if (start == null || end == null) {
            return DomainResult.Error(message = "Both start and end dates are required for custom range.")
        }

        val currentTime = System.currentTimeMillis()

        if (start > currentTime) {
            return DomainResult.Error(message = "Start date cannot be in the future.")
        }

        if (end > currentTime) {
            return DomainResult.Error(message = "End date cannot be in the future.")
        }

        if (start > end) {
            return DomainResult.Error(message = "Start date must be before or equal to end date.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates that the analytics query is project-scoped.
     */
    fun validateProjectScope(projectId: String?): DomainResult<Unit> {
        if (projectId.isNullOrBlank()) {
            return DomainResult.Error(message = "Analytics query must be scoped to a specific project.")
        }
        return DomainResult.Success(Unit)
    }
}

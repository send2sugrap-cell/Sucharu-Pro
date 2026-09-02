package com.sucharu.sucharupro.domain.validation.returns

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsSummary

/**
 * Domain validator for Return Analytics parameters and summary aggregates (Module 11 Step 06).
 */
object ReturnAnalyticsValidator {

    /**
     * Validates input parameters for analytics generation.
     */
    fun validateAnalyticsRequest(
        projectId: String,
        period: ReturnAnalyticsPeriod
    ): DomainResult<Unit> {
        if (projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates structural invariants of an analytical summary.
     */
    fun validateSummary(summary: ReturnAnalyticsSummary): DomainResult<Unit> {
        if (summary.projectId.isBlank()) {
            return DomainResult.Error(message = "Summary Project ID cannot be blank.")
        }
        if (summary.totalReturns < 0) {
            return DomainResult.Error(message = "Total returns cannot be negative.")
        }
        if (summary.returnRate < 0.0 || summary.returnRate > 100.0) {
            return DomainResult.Error(message = "Return rate must be between 0.0 and 100.0% (was ${summary.returnRate}%).")
        }
        if (summary.totalRequestedQuantity < 0 || summary.totalAcceptedQuantity < 0 || summary.totalRejectedQuantity < 0) {
            return DomainResult.Error(message = "Quantity totals cannot be negative.")
        }
        if (summary.totalSettledValue.isNegative()) {
            return DomainResult.Error(message = "Settled value cannot be negative.")
        }
        if (summary.averageTurnaroundDays < 0.0) {
            return DomainResult.Error(message = "Average turnaround days cannot be negative.")
        }
        return DomainResult.Success(Unit)
    }
}

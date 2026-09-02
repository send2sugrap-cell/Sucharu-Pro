package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.AccountingPeriod

/**
 * Validates Accounting Period payload, dates, and integrity constraints (Module 09 Step 08).
 */
object AccountingPeriodValidator {

    fun validateCreatePayload(
        projectId: String,
        periodName: String,
        startDate: Long,
        endDate: Long,
        actorId: String
    ): DomainResult<Unit> {
        if (projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (periodName.isBlank()) {
            return DomainResult.Error(message = "Period name cannot be blank.")
        }
        if (actorId.isBlank()) {
            return DomainResult.Error(message = "Actor ID cannot be blank.")
        }
        if (startDate > endDate) {
            return DomainResult.Error(message = "Period start date must be before or equal to end date.")
        }

        return DomainResult.Success(Unit)
    }

    fun validateNoOverlap(
        newStartDate: Long,
        newEndDate: Long,
        existingPeriods: List<AccountingPeriod>,
        excludePeriodId: String? = null
    ): DomainResult<Unit> {
        for (existing in existingPeriods) {
            if (excludePeriodId != null && existing.periodId == excludePeriodId) continue

            val overlaps = !(newEndDate < existing.startDate || newStartDate > existing.endDate)
            if (overlaps) {
                return DomainResult.Error(
                    message = "Accounting period dates ($newStartDate to $newEndDate) overlap with existing period '${existing.periodName}' (${existing.startDate} to ${existing.endDate})."
                )
            }
        }

        return DomainResult.Success(Unit)
    }
}

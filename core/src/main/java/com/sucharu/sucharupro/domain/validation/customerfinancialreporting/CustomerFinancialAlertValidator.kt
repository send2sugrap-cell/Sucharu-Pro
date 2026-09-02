package com.sucharu.sucharupro.domain.validation.customerfinancialreporting

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import java.time.ZoneId

/**
 * Domain validator for Customer Financial Alerts and Schedules (Module 14 Step 12).
 */
object CustomerFinancialAlertValidator {

    fun validateAlert(alert: CustomerFinancialAlert): DomainResult<Unit> {
        if (alert.alertId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("alertId cannot be blank"))
        }
        if (alert.tenantId.isBlank() || alert.projectId.isBlank() || alert.customerId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("tenantId, projectId, and customerId cannot be blank"))
        }
        if (alert.title.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Alert title cannot be blank"))
        }
        if (alert.safeMessage.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Alert safeMessage cannot be blank"))
        }
        if (alert.deduplicationKey.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Alert deduplicationKey cannot be blank"))
        }
        return DomainResult.Success(Unit)
    }

    fun validateStatusTransition(
        currentStatus: CustomerFinancialAlertStatus,
        targetStatus: CustomerFinancialAlertStatus
    ): DomainResult<Unit> {
        if (!currentStatus.canTransitionTo(targetStatus)) {
            return DomainResult.Error(
                IllegalStateException("Illegal alert status transition from $currentStatus to $targetStatus")
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateDismissal(
        alert: CustomerFinancialAlert,
        reason: String
    ): DomainResult<Unit> {
        val transitionRes = validateStatusTransition(alert.status, CustomerFinancialAlertStatus.DISMISSED)
        if (transitionRes is DomainResult.Error) return transitionRes

        if (reason.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("Dismissal reason is required and cannot be blank"))
        }
        return DomainResult.Success(Unit)
    }

    fun validateSchedule(
        schedule: CustomerFinancialReportSchedule
    ): DomainResult<Unit> {
        if (schedule.scheduleId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("scheduleId cannot be blank"))
        }
        if (schedule.tenantId.isBlank() || schedule.projectId.isBlank() || schedule.customerId.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("tenantId, projectId, and customerId cannot be blank"))
        }
        if (schedule.createdBy.isBlank()) {
            return DomainResult.Error(IllegalArgumentException("createdBy cannot be blank"))
        }
        if (schedule.nextRunAt <= 0) {
            return DomainResult.Error(IllegalArgumentException("nextRunAt must be a valid positive timestamp"))
        }
        try {
            ZoneId.of(schedule.timezone)
        } catch (_: Exception) {
            return DomainResult.Error(IllegalArgumentException("Invalid timezone '${schedule.timezone}'. Must be a valid IANA timezone identifier."))
        }
        return DomainResult.Success(Unit)
    }

    fun validateScheduleStatusTransition(
        currentStatus: CustomerFinancialReportScheduleStatus,
        targetStatus: CustomerFinancialReportScheduleStatus
    ): DomainResult<Unit> {
        if (!currentStatus.canTransitionTo(targetStatus)) {
            return DomainResult.Error(
                IllegalStateException("Illegal schedule status transition from $currentStatus to $targetStatus")
            )
        }
        return DomainResult.Success(Unit)
    }
}

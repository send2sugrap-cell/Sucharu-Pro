package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsThresholdConfig
import com.sucharu.sucharupro.domain.model.user.UserRole
import java.time.Instant

/**
 * Domain validator for QC Analytics queries, configurations, and RBAC governance.
 */
object QcAnalyticsValidator {

    fun validatePeriod(period: QcAnalyticsPeriod): DomainResult<Unit> {
        if (period.startTimestamp.isBlank()) {
            return DomainResult.Error(message = "Start timestamp cannot be blank.")
        }
        if (period.endTimestamp.isBlank()) {
            return DomainResult.Error(message = "End timestamp cannot be blank.")
        }
        try {
            val start = Instant.parse(period.startTimestamp)
            val end = Instant.parse(period.endTimestamp)
            if (start.isAfter(end)) {
                return DomainResult.Error(message = "Start timestamp ($start) cannot be after end timestamp ($end).")
            }
        } catch (e: Exception) {
            return DomainResult.Error(message = "Invalid timestamp format: ${e.message}")
        }
        return DomainResult.Success(Unit)
    }

    fun validateThresholdConfig(config: QcAnalyticsThresholdConfig): DomainResult<Unit> {
        if (config.maxAcceptableCostVariance < 0.0) {
            return DomainResult.Error(message = "Max acceptable cost variance cannot be negative.")
        }
        if (config.maxAcceptableTimeVarianceMinutes < 0L) {
            return DomainResult.Error(message = "Max acceptable time variance cannot be negative.")
        }
        if (config.maxAcceptableDefectRate < 0.0 || config.maxAcceptableDefectRate > 100.0) {
            return DomainResult.Error(message = "Max acceptable defect rate must be between 0.0 and 100.0%.")
        }
        if (config.maxAcceptableReworkRate < 0.0 || config.maxAcceptableReworkRate > 100.0) {
            return DomainResult.Error(message = "Max acceptable rework rate must be between 0.0 and 100.0%.")
        }
        if (config.maxAcceptableReQcRate < 0.0 || config.maxAcceptableReQcRate > 100.0) {
            return DomainResult.Error(message = "Max acceptable Re-QC rate must be between 0.0 and 100.0%.")
        }
        if (config.repeatedFailureCycleThreshold < 1) {
            return DomainResult.Error(message = "Repeated failure cycle threshold must be at least 1.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateRbac(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole == null) {
            return DomainResult.Error(message = "Role authentication required for QC analytics access.")
        }
        return when (callerRole) {
            UserRole.ADMIN,
            UserRole.MANAGER,
            UserRole.QC_INSPECTOR -> DomainResult.Success(Unit)
            else -> DomainResult.Error(message = "Role '${callerRole.name}' is not authorized to access QC analytics.")
        }
    }

    fun validateProjectContext(requestedProjectId: String?, targetProjectId: String): DomainResult<Unit> {
        if (requestedProjectId != null && requestedProjectId != targetProjectId) {
            return DomainResult.Error(message = "Cross-project access violation: Requested project '$requestedProjectId' does not match job project '$targetProjectId'.")
        }
        return DomainResult.Success(Unit)
    }
}

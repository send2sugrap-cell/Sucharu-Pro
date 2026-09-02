package com.sucharu.sucharupro.domain.validation.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Domain Validation Rules for Profitability Alerts & Management Action Engine.
 * Module 16 Step 09.
 */
object ProfitabilityAlertValidator {

    fun validateTenantAndProject(tenantId: String?, projectId: String?): DomainResult<Unit> {
        if (tenantId.isNullOrBlank()) {
            return DomainResult.Error(message = "tenantId must not be blank.")
        }
        if (projectId.isNullOrBlank()) {
            return DomainResult.Error(message = "projectId must not be blank.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateAlertRule(rule: ProfitabilityAlertRule): DomainResult<Unit> {
        val baseVal = validateTenantAndProject(rule.tenantId, rule.projectId)
        if (baseVal is DomainResult.Error) return baseVal

        if (rule.ruleName.isBlank()) {
            return DomainResult.Error(message = "ruleName must not be blank.")
        }
        if (rule.thresholdMetric.isBlank()) {
            return DomainResult.Error(message = "thresholdMetric must not be blank.")
        }
        if (rule.effectiveTo != null && rule.effectiveTo < rule.effectiveFrom) {
            return DomainResult.Error(message = "effectiveTo must be greater than or equal to effectiveFrom.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateAlertStatusTransition(
        currentStatus: ProfitabilityAlertStatus,
        newStatus: ProfitabilityAlertStatus
    ): DomainResult<Unit> {
        if (currentStatus == newStatus) {
            return DomainResult.Success(Unit) // Idempotent same-state
        }

        val isValid = when (currentStatus) {
            ProfitabilityAlertStatus.DETECTED -> newStatus in setOf(
                ProfitabilityAlertStatus.ACKNOWLEDGED,
                ProfitabilityAlertStatus.IN_REVIEW,
                ProfitabilityAlertStatus.ACTION_REQUIRED,
                ProfitabilityAlertStatus.DISMISSED,
                ProfitabilityAlertStatus.SUPPRESSED
            )
            ProfitabilityAlertStatus.ACKNOWLEDGED -> newStatus in setOf(
                ProfitabilityAlertStatus.IN_REVIEW,
                ProfitabilityAlertStatus.ACTION_REQUIRED,
                ProfitabilityAlertStatus.ACTION_IN_PROGRESS,
                ProfitabilityAlertStatus.DISMISSED,
                ProfitabilityAlertStatus.SUPPRESSED
            )
            ProfitabilityAlertStatus.IN_REVIEW -> newStatus in setOf(
                ProfitabilityAlertStatus.ACTION_REQUIRED,
                ProfitabilityAlertStatus.ACTION_IN_PROGRESS,
                ProfitabilityAlertStatus.RESOLVED,
                ProfitabilityAlertStatus.DISMISSED,
                ProfitabilityAlertStatus.SUPPRESSED
            )
            ProfitabilityAlertStatus.ACTION_REQUIRED -> newStatus in setOf(
                ProfitabilityAlertStatus.ACTION_IN_PROGRESS,
                ProfitabilityAlertStatus.RESOLVED,
                ProfitabilityAlertStatus.DISMISSED,
                ProfitabilityAlertStatus.SUPPRESSED
            )
            ProfitabilityAlertStatus.ACTION_IN_PROGRESS -> newStatus in setOf(
                ProfitabilityAlertStatus.ACTION_REQUIRED,
                ProfitabilityAlertStatus.RESOLVED,
                ProfitabilityAlertStatus.DISMISSED,
                ProfitabilityAlertStatus.SUPPRESSED
            )
            ProfitabilityAlertStatus.RESOLVED -> newStatus == ProfitabilityAlertStatus.REOPENED
            ProfitabilityAlertStatus.DISMISSED -> newStatus == ProfitabilityAlertStatus.REOPENED
            ProfitabilityAlertStatus.SUPPRESSED -> newStatus in setOf(
                ProfitabilityAlertStatus.REOPENED,
                ProfitabilityAlertStatus.DETECTED
            )
            ProfitabilityAlertStatus.REOPENED -> newStatus in setOf(
                ProfitabilityAlertStatus.ACKNOWLEDGED,
                ProfitabilityAlertStatus.IN_REVIEW,
                ProfitabilityAlertStatus.ACTION_REQUIRED,
                ProfitabilityAlertStatus.ACTION_IN_PROGRESS,
                ProfitabilityAlertStatus.DISMISSED
            )
        }

        return if (isValid) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Illegal alert lifecycle transition from $currentStatus to $newStatus.")
        }
    }

    fun validateManagementActionTransition(
        currentStatus: ManagementActionStatus,
        newStatus: ManagementActionStatus
    ): DomainResult<Unit> {
        if (currentStatus == newStatus) {
            return DomainResult.Success(Unit)
        }

        val isValid = when (currentStatus) {
            ManagementActionStatus.PROPOSED -> newStatus in setOf(
                ManagementActionStatus.APPROVED,
                ManagementActionStatus.CANCELLED
            )
            ManagementActionStatus.APPROVED -> newStatus in setOf(
                ManagementActionStatus.ASSIGNED,
                ManagementActionStatus.IN_PROGRESS,
                ManagementActionStatus.CANCELLED
            )
            ManagementActionStatus.ASSIGNED -> newStatus in setOf(
                ManagementActionStatus.IN_PROGRESS,
                ManagementActionStatus.BLOCKED,
                ManagementActionStatus.CANCELLED
            )
            ManagementActionStatus.IN_PROGRESS -> newStatus in setOf(
                ManagementActionStatus.BLOCKED,
                ManagementActionStatus.COMPLETED,
                ManagementActionStatus.CANCELLED
            )
            ManagementActionStatus.BLOCKED -> newStatus in setOf(
                ManagementActionStatus.IN_PROGRESS,
                ManagementActionStatus.CANCELLED
            )
            ManagementActionStatus.COMPLETED -> newStatus in setOf(
                ManagementActionStatus.VERIFIED,
                ManagementActionStatus.IN_PROGRESS
            )
            ManagementActionStatus.VERIFIED -> false
            ManagementActionStatus.CANCELLED -> false
        }

        return if (isValid) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Illegal management action status transition from $currentStatus to $newStatus.")
        }
    }

    fun validateManagementAction(action: ProfitabilityManagementAction): DomainResult<Unit> {
        val baseVal = validateTenantAndProject(action.tenantId, action.projectId)
        if (baseVal is DomainResult.Error) return baseVal

        if (action.actionTitle.isBlank()) {
            return DomainResult.Error(message = "actionTitle must not be blank.")
        }
        if (action.alertId.isBlank()) {
            return DomainResult.Error(message = "alertId must not be blank.")
        }
        return DomainResult.Success(Unit)
    }
}

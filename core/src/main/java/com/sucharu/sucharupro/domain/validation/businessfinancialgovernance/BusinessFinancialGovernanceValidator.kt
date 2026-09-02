package com.sucharu.sucharupro.domain.validation.businessfinancialgovernance

import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.math.BigDecimal

/**
 * Domain validator for Business Financial Governance, Budget Control & Decision Intelligence.
 */
object BusinessFinancialGovernanceValidator {

    fun validateBudgetCreation(budget: BusinessFinancialBudget): DomainResult<Unit> {
        val errors = mutableListOf<String>()

        if (budget.id.isBlank()) errors.add("Budget ID cannot be blank.")
        if (budget.tenantId.isBlank()) errors.add("Tenant ID cannot be blank.")
        if (budget.projectId.isBlank()) errors.add("Project ID cannot be blank.")
        if (budget.budgetName.isBlank()) errors.add("Budget name cannot be blank.")
        if (budget.periodId.isBlank()) errors.add("Period ID cannot be blank.")
        if (budget.dimensionId.isBlank()) errors.add("Dimension ID cannot be blank.")
        if (budget.currency.isBlank() || budget.currency.length !in 3..4) errors.add("Currency must be a valid 3 or 4 letter ISO code.")

        if (budget.allocatedAmount <= BigDecimal.ZERO) {
            errors.add("Allocated budget amount must be strictly greater than zero.")
        }
        if (budget.allocatedAmount.scale() > 4) {
            errors.add("Allocated budget amount precision must not exceed 4 decimal places.")
        }

        if (budget.effectiveStartDate >= budget.effectiveEndDate) {
            errors.add("Effective start date must be before effective end date.")
        }

        if (budget.createdBy.isBlank()) errors.add("Created by cannot be blank.")

        return if (errors.isEmpty()) DomainResult.Success(Unit) else DomainResult.Error(message = errors.joinToString(" "))
    }

    fun validateBudgetStatusTransition(
        budget: BusinessFinancialBudget,
        targetStatus: BusinessFinancialBudgetStatus,
        actorId: String,
        actorRole: String,
        rejectionReason: String? = null
    ): DomainResult<Unit> {
        val errors = mutableListOf<String>()

        if (budget.status == targetStatus) {
            return DomainResult.Success(Unit)
        }

        when (targetStatus) {
            BusinessFinancialBudgetStatus.SUBMITTED -> {
                if (!budget.status.canBeSubmitted) {
                    errors.add("Cannot submit budget in status ${budget.status}.")
                }
            }
            BusinessFinancialBudgetStatus.REVIEWED -> {
                if (!budget.status.canBeReviewed) {
                    errors.add("Cannot review budget in status ${budget.status}.")
                }
                if (actorId == budget.createdBy) {
                    errors.add("Separation of Duties violation: Creator cannot review their own budget.")
                }
            }
            BusinessFinancialBudgetStatus.APPROVED -> {
                if (!budget.status.canBeApproved) {
                    errors.add("Cannot approve budget in status ${budget.status}.")
                }
                if (actorId == budget.createdBy) {
                    errors.add("Separation of Duties violation: Creator cannot approve their own budget.")
                }
                if (actorRole !in setOf("ADMIN", "MANAGER")) {
                    errors.add("Only ADMIN or MANAGER roles can approve financial budgets.")
                }
            }
            BusinessFinancialBudgetStatus.ACTIVE -> {
                if (!budget.status.canBeActivated) {
                    errors.add("Cannot activate budget in status ${budget.status}. Budget must be APPROVED first.")
                }
            }
            BusinessFinancialBudgetStatus.REJECTED -> {
                if (budget.status.isTerminal) {
                    errors.add("Cannot reject a budget in terminal status ${budget.status}.")
                }
                if (rejectionReason.isNullOrBlank()) {
                    errors.add("Rejection reason is required when rejecting a budget.")
                }
            }
            BusinessFinancialBudgetStatus.REVISED -> {
                if (!budget.status.canBeRevised) {
                    errors.add("Cannot revise budget in status ${budget.status}. Only APPROVED or ACTIVE budgets can be revised.")
                }
            }
            BusinessFinancialBudgetStatus.CLOSED -> {
                if (!budget.status.canBeClosed) {
                    errors.add("Cannot close budget in status ${budget.status}. Only ACTIVE budgets can be closed.")
                }
            }
            BusinessFinancialBudgetStatus.DRAFT -> {
                errors.add("Cannot transition an existing budget back to DRAFT.")
            }
        }

        return if (errors.isEmpty()) DomainResult.Success(Unit) else DomainResult.Error(message = errors.joinToString(" "))
    }

    fun validateBudgetRevision(
        budget: BusinessFinancialBudget,
        newAmount: BigDecimal,
        reason: String,
        revisedBy: String
    ): DomainResult<Unit> {
        val errors = mutableListOf<String>()

        if (!budget.status.canBeRevised) {
            errors.add("Only APPROVED or ACTIVE budgets can be revised.")
        }
        if (newAmount <= BigDecimal.ZERO) {
            errors.add("Revised budget amount must be strictly greater than zero.")
        }
        if (newAmount.scale() > 4) {
            errors.add("Revised budget amount precision must not exceed 4 decimal places.")
        }
        if (newAmount.compareTo(budget.allocatedAmount) == 0) {
            errors.add("Revised budget amount must be different from current allocated amount.")
        }
        if (reason.isBlank()) {
            errors.add("Revision reason cannot be blank.")
        }
        if (revisedBy.isBlank()) {
            errors.add("Revised by cannot be blank.")
        }

        return if (errors.isEmpty()) DomainResult.Success(Unit) else DomainResult.Error(message = errors.joinToString(" "))
    }

    fun validateThreshold(threshold: BusinessFinancialBudgetThreshold): DomainResult<Unit> {
        val errors = mutableListOf<String>()

        if (threshold.id.isBlank()) errors.add("Threshold ID cannot be blank.")
        if (threshold.tenantId.isBlank()) errors.add("Tenant ID cannot be blank.")
        if (threshold.thresholdName.isBlank()) errors.add("Threshold name cannot be blank.")

        if (threshold.warningUtilizationPct < BigDecimal.ZERO) {
            errors.add("Warning utilization percentage cannot be negative.")
        }
        if (threshold.criticalUtilizationPct < threshold.warningUtilizationPct) {
            errors.add("Critical utilization percentage must be greater than or equal to warning percentage.")
        }
        if (threshold.largeExpenseThresholdAmount < BigDecimal.ZERO) {
            errors.add("Large expense threshold amount cannot be negative.")
        }
        if (threshold.commitmentExposureThresholdPct < BigDecimal.ZERO) {
            errors.add("Commitment exposure threshold percentage cannot be negative.")
        }

        return if (errors.isEmpty()) DomainResult.Success(Unit) else DomainResult.Error(message = errors.joinToString(" "))
    }

    fun validateAlertStatusTransition(
        alert: BusinessFinancialGovernanceAlert,
        targetStatus: GovernanceAlertStatus,
        notes: String?,
        actorId: String
    ): DomainResult<Unit> {
        val errors = mutableListOf<String>()

        if (alert.status == targetStatus) {
            return DomainResult.Success(Unit)
        }

        when (targetStatus) {
            GovernanceAlertStatus.ACKNOWLEDGED -> {
                if (alert.status != GovernanceAlertStatus.OPEN) {
                    errors.add("Only OPEN alerts can be acknowledged.")
                }
            }
            GovernanceAlertStatus.RESOLVED -> {
                if (alert.status !in setOf(GovernanceAlertStatus.OPEN, GovernanceAlertStatus.ACKNOWLEDGED)) {
                    errors.add("Only OPEN or ACKNOWLEDGED alerts can be resolved.")
                }
            }
            GovernanceAlertStatus.DISMISSED -> {
                if (alert.status !in setOf(GovernanceAlertStatus.OPEN, GovernanceAlertStatus.ACKNOWLEDGED)) {
                    errors.add("Only OPEN or ACKNOWLEDGED alerts can be dismissed.")
                }
                if (notes.isNullOrBlank()) {
                    errors.add("Dismissal reason/notes are mandatory when dismissing an alert.")
                }
            }
            GovernanceAlertStatus.OPEN -> {
                errors.add("Cannot transition an alert back to OPEN.")
            }
        }

        if (actorId.isBlank()) {
            errors.add("Actor ID cannot be blank for alert transition.")
        }

        return if (errors.isEmpty()) DomainResult.Success(Unit) else DomainResult.Error(message = errors.joinToString(" "))
    }
}

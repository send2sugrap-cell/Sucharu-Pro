package com.sucharu.sucharupro.domain.validation.businessfinancialadjustment

import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.math.BigDecimal

object BusinessFinancialAdjustmentValidators {

    fun validateAdjustmentCreation(
        tenantId: String,
        projectId: String,
        adjustmentNumber: String,
        adjustmentType: BusinessFinancialAdjustmentType,
        sourceType: AdjustmentSourceType,
        sourceId: String,
        originalAmount: BigDecimal,
        adjustmentAmount: BigDecimal,
        currency: String,
        reason: String,
        justification: String,
        periodId: String,
        createdBy: String
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID is required.")
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID is required.")
        if (adjustmentNumber.isBlank()) return DomainResult.Error(message = "Adjustment number is required.")
        if (sourceId.isBlank()) return DomainResult.Error(message = "Source ID is required.")
        if (periodId.isBlank()) return DomainResult.Error(message = "Financial period ID is required.")
        if (createdBy.isBlank()) return DomainResult.Error(message = "Creator user ID is required.")

        if (currency.isBlank() || currency.length !in 3..4) {
            return DomainResult.Error(message = "Valid currency code is required (e.g. BDT, USD).")
        }

        if (reason.trim().length < 3) {
            return DomainResult.Error(message = "Adjustment reason must be at least 3 characters.")
        }
        if (justification.trim().length < 10) {
            return DomainResult.Error(message = "Adjustment justification must be at least 10 characters.")
        }

        if (originalAmount < BigDecimal.ZERO) {
            return DomainResult.Error(message = "Original amount cannot be negative.")
        }
        if (originalAmount.scale() > 4 || adjustmentAmount.scale() > 4) {
            return DomainResult.Error(message = "Financial amounts cannot exceed 4 decimal places of precision.")
        }
        if (adjustmentAmount.compareTo(BigDecimal.ZERO) == 0) {
            return DomainResult.Error(message = "Adjustment amount cannot be zero.")
        }

        return DomainResult.Success(Unit)
    }

    fun validateAdjustmentStateTransition(
        currentStatus: AdjustmentStatus,
        newStatus: AdjustmentStatus
    ): DomainResult<Unit> {
        val validTransitions = mapOf(
            AdjustmentStatus.DRAFT to setOf(AdjustmentStatus.SUBMITTED, AdjustmentStatus.CANCELLED),
            AdjustmentStatus.SUBMITTED to setOf(AdjustmentStatus.UNDER_REVIEW, AdjustmentStatus.APPROVED, AdjustmentStatus.REJECTED, AdjustmentStatus.CANCELLED),
            AdjustmentStatus.UNDER_REVIEW to setOf(AdjustmentStatus.APPROVED, AdjustmentStatus.REJECTED, AdjustmentStatus.CANCELLED),
            AdjustmentStatus.APPROVED to setOf(AdjustmentStatus.POSTED, AdjustmentStatus.CANCELLED),
            AdjustmentStatus.POSTED to setOf(AdjustmentStatus.RECONCILED, AdjustmentStatus.REVERSAL_REQUESTED, AdjustmentStatus.VOIDED),
            AdjustmentStatus.RECONCILED to setOf(AdjustmentStatus.REVERSAL_REQUESTED),
            AdjustmentStatus.REVERSAL_REQUESTED to setOf(AdjustmentStatus.REVERSAL_APPROVED, AdjustmentStatus.POSTED),
            AdjustmentStatus.REVERSAL_APPROVED to setOf(AdjustmentStatus.REVERSED)
        )

        val allowed = validTransitions[currentStatus] ?: emptySet()
        if (newStatus !in allowed) {
            return DomainResult.Error(
                message = "Illegal adjustment status transition from '$currentStatus' to '$newStatus'."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateSeparationOfDuties(
        creatorId: String,
        actorId: String,
        actionName: String
    ): DomainResult<Unit> {
        if (creatorId.isNotBlank() && creatorId == actorId) {
            return DomainResult.Error(
                message = "Separation of Duties violation: The creator cannot $actionName the same financial entity ($actorId)."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateRefundCreation(
        tenantId: String,
        projectId: String,
        refundNumber: String,
        sourceId: String,
        eligibleBalance: BigDecimal,
        requestedAmount: BigDecimal,
        currency: String,
        refundReason: String,
        periodId: String,
        requestedBy: String
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID is required.")
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID is required.")
        if (refundNumber.isBlank()) return DomainResult.Error(message = "Refund number is required.")
        if (sourceId.isBlank()) return DomainResult.Error(message = "Source ID is required.")
        if (periodId.isBlank()) return DomainResult.Error(message = "Financial period ID is required.")
        if (requestedBy.isBlank()) return DomainResult.Error(message = "Requester user ID is required.")

        if (currency.isBlank() || currency.length !in 3..4) {
            return DomainResult.Error(message = "Valid currency code is required (e.g. BDT).")
        }
        if (refundReason.trim().length < 5) {
            return DomainResult.Error(message = "Refund reason must be at least 5 characters.")
        }

        if (requestedAmount <= BigDecimal.ZERO) {
            return DomainResult.Error(message = "Requested refund amount must be greater than zero.")
        }
        if (requestedAmount.scale() > 4) {
            return DomainResult.Error(message = "Refund amount cannot exceed 4 decimal places of precision.")
        }
        if (eligibleBalance > BigDecimal.ZERO && requestedAmount > eligibleBalance) {
            return DomainResult.Error(
                message = "Requested refund amount ($requestedAmount) cannot exceed eligible balance ($eligibleBalance)."
            )
        }

        return DomainResult.Success(Unit)
    }

    fun validateWriteOffCreation(
        tenantId: String,
        projectId: String,
        writeOffNumber: String,
        sourceId: String,
        eligibleBalance: BigDecimal,
        amount: BigDecimal,
        currency: String,
        reason: String,
        justification: String,
        periodId: String,
        requestedBy: String
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID is required.")
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID is required.")
        if (writeOffNumber.isBlank()) return DomainResult.Error(message = "Write-off number is required.")
        if (sourceId.isBlank()) return DomainResult.Error(message = "Source ID is required.")
        if (periodId.isBlank()) return DomainResult.Error(message = "Financial period ID is required.")
        if (requestedBy.isBlank()) return DomainResult.Error(message = "Requester user ID is required.")

        if (currency.isBlank() || currency.length !in 3..4) {
            return DomainResult.Error(message = "Valid currency code is required.")
        }
        if (reason.trim().length < 5) {
            return DomainResult.Error(message = "Write-off reason must be at least 5 characters.")
        }
        if (justification.trim().length < 10) {
            return DomainResult.Error(message = "Write-off justification must be at least 10 characters.")
        }

        if (amount <= BigDecimal.ZERO) {
            return DomainResult.Error(message = "Write-off amount must be greater than zero.")
        }
        if (amount.scale() > 4) {
            return DomainResult.Error(message = "Write-off amount cannot exceed 4 decimal places of precision.")
        }
        if (eligibleBalance > BigDecimal.ZERO && amount > eligibleBalance) {
            return DomainResult.Error(
                message = "Write-off amount ($amount) cannot exceed eligible balance ($eligibleBalance)."
            )
        }

        return DomainResult.Success(Unit)
    }

    fun validateReversalRequest(
        adjustment: BusinessFinancialAdjustment,
        reversalReason: String,
        requestedBy: String
    ): DomainResult<Unit> {
        if (!adjustment.status.canBeReversed) {
            return DomainResult.Error(
                message = "Cannot request reversal for adjustment in status '${adjustment.status}'. Must be POSTED or RECONCILED."
            )
        }
        if (reversalReason.trim().length < 10) {
            return DomainResult.Error(message = "Reversal reason must be at least 10 characters.")
        }
        if (requestedBy.isBlank()) {
            return DomainResult.Error(message = "Reversal requester user ID is required.")
        }
        return DomainResult.Success(Unit)
    }
}

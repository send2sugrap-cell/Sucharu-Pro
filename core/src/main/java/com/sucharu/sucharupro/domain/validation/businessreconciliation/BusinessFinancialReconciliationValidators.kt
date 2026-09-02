package com.sucharu.sucharupro.domain.validation.businessreconciliation

import com.sucharu.sucharupro.domain.model.businessreconciliation.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.math.BigDecimal

object BusinessFinancialReconciliationValidators {

    fun validatePrecision(amount: BigDecimal, fieldName: String = "Amount"): DomainResult<Unit> {
        if (amount.scale() > 4) {
            return DomainResult.Error(
                message = "$fieldName precision exceeds maximum allowed scale of 4 decimal places (scale was ${amount.scale()})."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateCurrency(currency: String): DomainResult<Unit> {
        val trimmed = currency.trim()
        if (trimmed.length != 3 || !trimmed.all { it.isLetter() }) {
            return DomainResult.Error(
                message = "Currency code must be a valid 3-letter ISO code (e.g. 'BDT', 'USD'). Provided: '$currency'."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateRunCreation(
        periodId: String,
        runNumber: String,
        tenantId: String,
        projectId: String,
        createdBy: String
    ): DomainResult<Unit> {
        if (tenantId.isBlank()) return DomainResult.Error(message = "Tenant ID cannot be blank.")
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (periodId.isBlank()) return DomainResult.Error(message = "Period ID cannot be blank.")
        if (runNumber.trim().length < 2) return DomainResult.Error(message = "Run number must be at least 2 characters.")
        if (createdBy.isBlank()) return DomainResult.Error(message = "Created by actor ID cannot be blank.")
        return DomainResult.Success(Unit)
    }

    fun validateDiscrepancyResolution(
        discrepancy: BusinessFinancialReconciliationDiscrepancy,
        resolutionNote: String,
        resolvedBy: String
    ): DomainResult<Unit> {
        if (resolvedBy.isBlank()) return DomainResult.Error(message = "Resolver actor ID cannot be blank.")
        if (resolutionNote.trim().length < 5) {
            return DomainResult.Error(message = "Resolution note must be at least 5 characters.")
        }
        if (discrepancy.status.isClosed) {
            return DomainResult.Error(message = "Discrepancy '${discrepancy.id}' is already in terminal/closed status '${discrepancy.status}'.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateDiscrepancyWaiver(
        discrepancy: BusinessFinancialReconciliationDiscrepancy,
        waiverReason: String,
        waivedBy: String
    ): DomainResult<Unit> {
        if (waivedBy.isBlank()) return DomainResult.Error(message = "Authorizer actor ID cannot be blank.")
        if (waiverReason.trim().length < 10) {
            return DomainResult.Error(message = "Explicit waiver requires detailed justification (minimum 10 characters).")
        }
        if (discrepancy.status.isClosed) {
            return DomainResult.Error(message = "Discrepancy '${discrepancy.id}' is already in terminal/closed status '${discrepancy.status}'.")
        }
        return DomainResult.Success(Unit)
    }

    fun validateRunApproval(
        run: BusinessFinancialReconciliationRun,
        approverId: String
    ): DomainResult<Unit> {
        if (approverId.isBlank()) return DomainResult.Error(message = "Approver actor ID cannot be blank.")
        if (run.createdBy == approverId) {
            return DomainResult.Error(
                message = "Separation of Duties violation: Run creator '$approverId' cannot approve their own reconciliation run."
            )
        }
        if (!run.status.canBeApproved) {
            return DomainResult.Error(
                message = "Cannot approve reconciliation run in status '${run.status}'. Run must be COMPLETED or UNDER_REVIEW."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateCorrectionLinkage(
        discrepancy: BusinessFinancialReconciliationDiscrepancy,
        correctionType: String,
        correctionId: String,
        actorId: String
    ): DomainResult<Unit> {
        if (actorId.isBlank()) return DomainResult.Error(message = "Actor ID cannot be blank.")
        if (correctionType.isBlank()) return DomainResult.Error(message = "Correction type cannot be blank.")
        if (correctionId.isBlank()) return DomainResult.Error(message = "Correction ID cannot be blank.")
        return DomainResult.Success(Unit)
    }
}

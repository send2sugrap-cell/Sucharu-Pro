package com.sucharu.sucharupro.domain.validation.returns

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.InspectionChecklistItem
import com.sucharu.sucharupro.domain.model.returns.ReturnDecision
import com.sucharu.sucharupro.domain.model.returns.ReturnInspection
import com.sucharu.sucharupro.domain.model.returns.ReturnInspectionStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus

/**
 * Domain validator for Return Inspection & Decision operations (Module 11 Step 03).
 */
object ReturnInspectionValidator {

    /**
     * Validates that the Return Request is currently in UNDER_INSPECTION status.
     * Inspections cannot be conducted or finalized on returns in other states.
     */
    fun validateEligibleForInspection(request: ReturnRequest): DomainResult<Unit> {
        if (request.status != ReturnStatus.UNDER_INSPECTION) {
            return DomainResult.Error(
                message = "Invalid status for inspection: Return '${request.returnId}' is in '${request.status.name}'. " +
                    "Inspections can only be recorded when the Return is in UNDER_INSPECTION status."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates domain constraints and business invariants on [ReturnInspection].
     */
    fun validateInspection(inspection: ReturnInspection): DomainResult<Unit> {
        if (inspection.inspectionId.isBlank()) {
            return DomainResult.Error(message = "Inspection ID cannot be blank.")
        }
        if (inspection.returnId.isBlank()) {
            return DomainResult.Error(message = "Return ID reference is required on ReturnInspection.")
        }
        if (inspection.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID is required on ReturnInspection.")
        }
        if (inspection.inspectorId.isBlank()) {
            return DomainResult.Error(message = "Inspector ID is required on ReturnInspection.")
        }
        if (inspection.version <= 0) {
            return DomainResult.Error(message = "Inspection version must be positive.")
        }
        if (inspection.inspectedAt <= 0) {
            return DomainResult.Error(message = "Inspection timestamp must be positive.")
        }

        for (item in inspection.checklist) {
            val checklistRes = validateChecklistItem(item)
            if (checklistRes is DomainResult.Error) return checklistRes
        }

        if (inspection.status == ReturnInspectionStatus.COMPLETED) {
            if (inspection.decision == null) {
                return DomainResult.Error(
                    message = "A decision (APPROVE or REJECT) is required when completing an inspection."
                )
            }
            if (inspection.decision == ReturnDecision.REJECT && inspection.decisionReason.isNullOrBlank()) {
                return DomainResult.Error(
                    message = "A decision reason is mandatory when rejecting a return."
                )
            }
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates an individual checklist item.
     */
    fun validateChecklistItem(item: InspectionChecklistItem): DomainResult<Unit> {
        if (item.itemId.isBlank()) {
            return DomainResult.Error(message = "Checklist item ID cannot be blank.")
        }
        if (item.title.isBlank()) {
            return DomainResult.Error(message = "Checklist title cannot be blank.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates project isolation for ReturnInspection.
     */
    fun validateProjectIsolation(
        inspection: ReturnInspection,
        contextProjectId: String
    ): DomainResult<Unit> {
        if (inspection.projectId != contextProjectId) {
            return DomainResult.Error(
                message = "Project isolation violation: Inspection belongs to project '${inspection.projectId}' " +
                    "but context is '$contextProjectId'."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates the item quantities for an inspection decision.
     */
    fun validateInspectionItemQuantities(
        items: List<ReturnItem>,
        decision: ReturnDecision?
    ): DomainResult<Unit> {
        if (items.isEmpty()) {
            return DomainResult.Error(message = "Inspection requires at least one Return Item.")
        }

        for (item in items) {
            if (item.acceptedQuantity < 0) {
                return DomainResult.Error(message = "Accepted quantity cannot be negative on item '${item.returnItemId}'.")
            }
            if (item.rejectedQuantity < 0) {
                return DomainResult.Error(message = "Rejected quantity cannot be negative on item '${item.returnItemId}'.")
            }
            if (item.acceptedQuantity + item.rejectedQuantity > item.requestedQuantity) {
                return DomainResult.Error(
                    message = "Sum of accepted (${item.acceptedQuantity}) and rejected (${item.rejectedQuantity}) " +
                        "quantities exceeds requested quantity (${item.requestedQuantity}) on item '${item.returnItemId}'."
                )
            }
        }

        if (decision == ReturnDecision.APPROVE) {
            val totalAccepted = items.sumOf { it.acceptedQuantity }
            if (totalAccepted <= 0) {
                return DomainResult.Error(
                    message = "Cannot approve return: At least one item must have an accepted quantity > 0."
                )
            }
        }

        if (decision == ReturnDecision.REJECT) {
            val totalRejected = items.sumOf { it.rejectedQuantity }
            if (totalRejected <= 0) {
                return DomainResult.Error(
                    message = "Cannot reject return: At least one item must have a rejected quantity > 0."
                )
            }
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates a decision and its mandatory reason.
     */
    fun validateDecision(
        decision: ReturnDecision,
        decisionReason: String?
    ): DomainResult<Unit> {
        if (decision == ReturnDecision.REJECT && decisionReason.isNullOrBlank()) {
            return DomainResult.Error(
                message = "Rejection reason is required when rejecting a return request."
            )
        }
        return DomainResult.Success(Unit)
    }
}

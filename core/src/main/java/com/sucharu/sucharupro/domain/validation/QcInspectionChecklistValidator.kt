package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcChecklistItem
import com.sucharu.sucharupro.domain.model.qc.QcChecklistStatus
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
import com.sucharu.sucharupro.domain.model.qc.QcInspectionResponse
import com.sucharu.sucharupro.domain.model.qc.QcResponseStatus
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Validator for concrete [QcInspectionChecklist] execution, state transitions, and completion integrity (Module 06 Step 03).
 */
object QcInspectionChecklistValidator {

    val AUTHORIZED_INSPECTION_ROLES = setOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.QC_INSPECTOR)

    fun validateInspectionPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_INSPECTION_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to execute QC inspection checklists."
            )
        }
        return DomainResult.Success(Unit)
    }

    fun validateStatusTransition(
        checklist: QcInspectionChecklist,
        targetStatus: QcChecklistStatus
    ): DomainResult<Unit> {
        if (checklist.status == targetStatus) {
            return DomainResult.Error(
                message = "Inspection checklist is already in ${checklist.status.defaultLabel} state."
            )
        }
        if (checklist.isTerminal) {
            return DomainResult.Error(
                message = "Cannot modify terminal inspection checklist (Status: ${checklist.status.defaultLabel})."
            )
        }
        if (!checklist.status.canTransitionTo(targetStatus)) {
            return DomainResult.Error(
                message = "Invalid checklist status transition from ${checklist.status.defaultLabel} to ${targetStatus.defaultLabel}."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates checklist completion requirements against responses.
     */
    fun validateCompletion(
        checklist: QcInspectionChecklist,
        items: List<QcChecklistItem>,
        responses: List<QcInspectionResponse>,
        targetDecision: QcDecision,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbac = validateInspectionPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        if (checklist.isTerminal) {
            return DomainResult.Error(
                message = "Cannot complete already terminal inspection checklist '${checklist.inspectionChecklistId}'."
            )
        }

        if (items.isEmpty()) {
            return DomainResult.Error(message = "Cannot complete checklist with no check items.")
        }

        val requiredItems = items.filter { it.isRequired }
        val responseMap = responses.associateBy { it.checklistItemId }

        val pendingRequiredItems = requiredItems.filter { item ->
            val resp = responseMap[item.itemId]
            resp == null || resp.status == QcResponseStatus.PENDING
        }

        if (pendingRequiredItems.isNotEmpty()) {
            return DomainResult.Error(
                message = "Cannot complete checklist with ${pendingRequiredItems.size} pending required item(s)."
            )
        }

        val failedRequiredItems = requiredItems.filter { item ->
            val resp = responseMap[item.itemId]
            resp != null && resp.status == QcResponseStatus.FAIL
        }

        if (targetDecision == QcDecision.PASS) {
            if (failedRequiredItems.isNotEmpty()) {
                return DomainResult.Error(
                    message = "Cannot submit PASS decision when ${failedRequiredItems.size} required item(s) have failed."
                )
            }
        } else if (targetDecision == QcDecision.FAIL) {
            val allFailed = responses.filter { it.status == QcResponseStatus.FAIL }
            if (allFailed.isEmpty()) {
                return DomainResult.Error(
                    message = "Cannot submit FAIL decision without at least one failed checklist response."
                )
            }
        } else {
            return DomainResult.Error(message = "Target decision must be PASS or FAIL.")
        }

        return DomainResult.Success(Unit)
    }
}

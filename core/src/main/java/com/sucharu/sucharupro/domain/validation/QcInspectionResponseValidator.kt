package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcChecklistItem
import com.sucharu.sucharupro.domain.model.qc.QcChecklistItemType
import com.sucharu.sucharupro.domain.model.qc.QcInspectionResponse
import com.sucharu.sucharupro.domain.model.qc.QcResponseStatus

/**
 * Validator for individual [QcInspectionResponse] entries (Module 06 Step 03).
 */
object QcInspectionResponseValidator {

    fun validateResponse(
        response: QcInspectionResponse,
        item: QcChecklistItem
    ): DomainResult<Unit> {
        if (response.responseId.isBlank()) {
            return DomainResult.Error(message = "Response ID cannot be blank.")
        }
        if (response.inspectionId.isBlank()) {
            return DomainResult.Error(message = "Inspection ID cannot be blank.")
        }
        if (response.checklistItemId != item.itemId) {
            return DomainResult.Error(
                message = "Response checklist item ID '${response.checklistItemId}' does not match Item '${item.itemId}'."
            )
        }

        // Validate type-specific numeric input
        if (item.itemType == QcChecklistItemType.NUMERIC && response.status != QcResponseStatus.NOT_APPLICABLE && response.status != QcResponseStatus.PENDING) {
            if (response.numericValue == null && response.value.isNullOrBlank()) {
                return DomainResult.Error(message = "Numeric measurement value is required for item '${item.title}'.")
            }
        }

        // Require failure remarks if status is FAIL
        if (response.status == QcResponseStatus.FAIL && response.remarks.isNullOrBlank()) {
            return DomainResult.Error(
                message = "Failure remarks are required when marking item '${item.title}' as FAIL."
            )
        }

        return DomainResult.Success(Unit)
    }
}

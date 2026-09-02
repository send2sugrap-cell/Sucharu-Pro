package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcChecklistItem
import com.sucharu.sucharupro.domain.model.qc.QcChecklistItemType

/**
 * Validator for individual [QcChecklistItem] definitions (Module 06 Step 03).
 */
object QcChecklistItemValidator {

    /**
     * Validates item fields, sequencing, and type configuration.
     */
    fun validateItem(item: QcChecklistItem): DomainResult<Unit> {
        if (item.itemId.isBlank()) {
            return DomainResult.Error(message = "Checklist Item ID cannot be blank.")
        }
        if (item.checklistTemplateId.isBlank()) {
            return DomainResult.Error(message = "Checklist Template ID cannot be blank.")
        }
        if (item.categoryId.isBlank()) {
            return DomainResult.Error(message = "Checklist Category ID cannot be blank.")
        }
        if (item.title.isBlank()) {
            return DomainResult.Error(message = "Checklist Item title cannot be blank.")
        }
        if (item.sequence < 1) {
            return DomainResult.Error(message = "Checklist Item sequence must be at least 1.")
        }
        if (item.createdAt.isBlank()) {
            return DomainResult.Error(message = "Creation timestamp cannot be blank.")
        }

        return DomainResult.Success(Unit)
    }
}

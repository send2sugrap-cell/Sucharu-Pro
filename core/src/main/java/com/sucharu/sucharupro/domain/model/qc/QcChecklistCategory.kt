package com.sucharu.sucharupro.domain.model.qc

/**
 * Logical category grouping for inspection check items within a template (Module 06 Step 03).
 */
data class QcChecklistCategory(
    val categoryId: String,
    val checklistTemplateId: String,
    val name: String,
    val sequence: Int = 1
) {
    init {
        require(categoryId.isNotBlank()) { "Category ID cannot be blank." }
        require(checklistTemplateId.isNotBlank()) { "Checklist Template ID cannot be blank." }
        require(name.isNotBlank()) { "Category name cannot be blank." }
    }
}

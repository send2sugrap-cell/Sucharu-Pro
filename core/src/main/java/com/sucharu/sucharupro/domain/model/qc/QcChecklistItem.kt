package com.sucharu.sucharupro.domain.model.qc

/**
 * Domain entity representing an individual checklist item definition within a [QcChecklistTemplate] (Module 06 Step 03).
 */
data class QcChecklistItem(
    val itemId: String,
    val checklistTemplateId: String,
    val categoryId: String,
    val sequence: Int = 1,
    val code: String? = null,
    val title: String,
    val description: String? = null,
    val itemType: QcChecklistItemType = QcChecklistItemType.PASS_FAIL,
    val isRequired: Boolean = true,
    val expectedValue: String? = null,
    val tolerance: String? = null,
    val unit: String? = null,
    val instructions: String? = null,
    val active: Boolean = true,
    val createdAt: String
) {
    init {
        require(itemId.isNotBlank()) { "Item ID cannot be blank." }
        require(checklistTemplateId.isNotBlank()) { "Checklist Template ID cannot be blank." }
        require(categoryId.isNotBlank()) { "Category ID cannot be blank." }
        require(title.isNotBlank()) { "Item title cannot be blank." }
        require(createdAt.isNotBlank()) { "Creation timestamp cannot be blank." }
    }
}

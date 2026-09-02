package com.sucharu.sucharupro.domain.model.returns

/**
 * Individual checklist checkpoint evaluated during return inspection (Module 11 Step 03).
 */
data class InspectionChecklistItem(
    val itemId: String,
    val title: String,
    val isPassed: Boolean = false,
    val notes: String? = null
) {
    init {
        require(itemId.isNotBlank()) { "Checklist item ID cannot be blank." }
        require(title.isNotBlank()) { "Checklist title cannot be blank." }
    }
}

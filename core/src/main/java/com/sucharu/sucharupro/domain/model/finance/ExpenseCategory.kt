package com.sucharu.sucharupro.domain.model.finance

/**
 * Configurable classification category for business expenses (Module 09 Step 06).
 */
data class ExpenseCategory(
    val categoryId: String,
    val projectId: String,
    val categoryCode: String,
    val categoryName: String,
    val description: String? = null,
    val parentCategoryId: String? = null,
    val accountHead: String? = null,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val createdBy: String,
    val updatedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(categoryId.isNotBlank()) { "Category ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(categoryCode.isNotBlank()) { "Category Code cannot be blank." }
        require(categoryName.isNotBlank()) { "Category Name cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation timestamp." }
    }
}

package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.ExpenseCategory

/**
 * Domain invariants and payload validation for Expense Categories (Module 09 Step 06).
 */
object ExpenseCategoryValidator {

    fun validateCategoryPayload(
        projectId: String,
        categoryCode: String,
        categoryName: String,
        parentCategoryId: String?,
        actorId: String
    ): DomainResult<Unit> {
        if (projectId.isBlank()) return DomainResult.Error(message = "Project ID cannot be blank.")
        if (categoryCode.isBlank()) return DomainResult.Error(message = "Category Code cannot be blank.")
        if (categoryName.isBlank()) return DomainResult.Error(message = "Category Name cannot be blank.")
        if (actorId.isBlank()) return DomainResult.Error(message = "Actor ID cannot be blank.")

        return DomainResult.Success(Unit)
    }

    fun validateHierarchy(
        categoryToUpdateId: String,
        newParentId: String?,
        existingCategories: List<ExpenseCategory>
    ): DomainResult<Unit> {
        if (newParentId.isNullOrBlank()) {
            return DomainResult.Success(Unit)
        }

        if (categoryToUpdateId == newParentId) {
            return DomainResult.Error(message = "Circular hierarchy detected: Category cannot be its own parent.")
        }

        // Trace up the ancestry tree from newParentId to ensure categoryToUpdateId is not an ancestor
        var currentParentId: String? = newParentId
        val visited = mutableSetOf<String>()

        while (currentParentId != null) {
            if (currentParentId == categoryToUpdateId) {
                return DomainResult.Error(
                    message = "Circular hierarchy detected: Category '$categoryToUpdateId' is an ancestor of target parent '$newParentId'."
                )
            }
            if (!visited.add(currentParentId)) {
                return DomainResult.Error(message = "Cycle detected in existing category hierarchy.")
            }
            val parentCategory = existingCategories.firstOrNull { it.categoryId == currentParentId }
            currentParentId = parentCategory?.parentCategoryId
        }

        return DomainResult.Success(Unit)
    }
}

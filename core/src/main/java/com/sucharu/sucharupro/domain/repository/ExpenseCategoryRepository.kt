package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.ExpenseCategory
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Expense Category operations (Module 09 Step 06).
 */
interface ExpenseCategoryRepository {

    suspend fun createCategory(
        projectId: String,
        categoryCode: String,
        categoryName: String,
        description: String? = null,
        parentCategoryId: String? = null,
        accountHead: String? = null,
        sortOrder: Int = 0,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<ExpenseCategory>

    suspend fun updateCategory(
        categoryId: String,
        categoryName: String? = null,
        description: String? = null,
        parentCategoryId: String? = null,
        accountHead: String? = null,
        sortOrder: Int? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<ExpenseCategory>

    suspend fun activateCategory(
        categoryId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<ExpenseCategory>

    suspend fun deactivateCategory(
        categoryId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<ExpenseCategory>

    suspend fun getCategoryById(
        categoryId: String,
        callerRole: UserRole
    ): DomainResult<ExpenseCategory>

    suspend fun getCategoryByCode(
        projectId: String,
        categoryCode: String,
        callerRole: UserRole
    ): DomainResult<ExpenseCategory>

    fun observeCategories(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<ExpenseCategory>>

    suspend fun seedDefaultCategoriesIfEmpty(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<ExpenseCategory>>
}

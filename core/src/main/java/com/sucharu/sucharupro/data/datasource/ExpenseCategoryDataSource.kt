package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.ExpenseCategory
import kotlinx.coroutines.flow.Flow

/**
 * Data Source contract for Expense Categories (Module 09 Step 06).
 */
interface ExpenseCategoryDataSource {

    suspend fun insertCategory(category: ExpenseCategory): Boolean

    suspend fun updateCategory(category: ExpenseCategory): Boolean

    suspend fun getCategoryById(categoryId: String): ExpenseCategory?

    suspend fun getCategoryByCode(projectId: String, categoryCode: String): ExpenseCategory?

    suspend fun getCategoriesByProject(projectId: String): List<ExpenseCategory>

    fun observeCategories(projectId: String): Flow<List<ExpenseCategory>>

    suspend fun getChildCategories(parentCategoryId: String): List<ExpenseCategory>
}

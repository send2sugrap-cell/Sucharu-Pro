package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.ExpenseCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe, reactive in-memory implementation of ExpenseCategoryDataSource (Module 09 Step 06).
 */
class FakeExpenseCategoryDataSource : ExpenseCategoryDataSource {

    private val mutex = Mutex()
    private val categories = LinkedHashMap<String, ExpenseCategory>()
    private val categoriesFlow = MutableStateFlow<List<ExpenseCategory>>(emptyList())

    override suspend fun insertCategory(category: ExpenseCategory): Boolean = mutex.withLock {
        if (categories.containsKey(category.categoryId)) return@withLock false
        categories[category.categoryId] = category
        categoriesFlow.value = categories.values.toList()
        true
    }

    override suspend fun updateCategory(category: ExpenseCategory): Boolean = mutex.withLock {
        if (!categories.containsKey(category.categoryId)) return@withLock false
        categories[category.categoryId] = category
        categoriesFlow.value = categories.values.toList()
        true
    }

    override suspend fun getCategoryById(categoryId: String): ExpenseCategory? = mutex.withLock {
        categories[categoryId]
    }

    override suspend fun getCategoryByCode(
        projectId: String,
        categoryCode: String
    ): ExpenseCategory? = mutex.withLock {
        categories.values.firstOrNull {
            it.projectId == projectId && it.categoryCode.equals(categoryCode, ignoreCase = true)
        }
    }

    override suspend fun getCategoriesByProject(projectId: String): List<ExpenseCategory> = mutex.withLock {
        categories.values.filter { it.projectId == projectId }.sortedBy { it.sortOrder }
    }

    override fun observeCategories(projectId: String): Flow<List<ExpenseCategory>> {
        return categoriesFlow.map { list ->
            list.filter { it.projectId == projectId }.sortedBy { it.sortOrder }
        }
    }

    override suspend fun getChildCategories(parentCategoryId: String): List<ExpenseCategory> = mutex.withLock {
        categories.values.filter { it.parentCategoryId == parentCategoryId }.sortedBy { it.sortOrder }
    }
}

package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.ExpenseCategoryDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.ExpenseCategory
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ExpenseCategoryRepository
import com.sucharu.sucharupro.domain.validation.ExpenseAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.ExpenseCategoryValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Thread-safe implementation of ExpenseCategoryRepository with non-reentrant mutex locking (Module 09 Step 06).
 */
class ExpenseCategoryRepositoryImpl(
    private val dataSource: ExpenseCategoryDataSource
) : ExpenseCategoryRepository {

    private val mutex = Mutex()

    override suspend fun createCategory(
        projectId: String,
        categoryCode: String,
        categoryName: String,
        description: String?,
        parentCategoryId: String?,
        accountHead: String?,
        sortOrder: Int,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<ExpenseCategory> = mutex.withLock {
        val authResult = ExpenseAuthorizationValidator.validateCreateCategory(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val valResult = ExpenseCategoryValidator.validateCategoryPayload(
            projectId = projectId,
            categoryCode = categoryCode,
            categoryName = categoryName,
            parentCategoryId = parentCategoryId,
            actorId = actorId
        )
        if (valResult is DomainResult.Error) return@withLock valResult

        val existingWithCode = dataSource.getCategoryByCode(projectId, categoryCode.trim())
        if (existingWithCode != null) {
            return@withLock DomainResult.Error(
                message = "Expense category code '${categoryCode.trim()}' already exists in project '$projectId'."
            )
        }

        val allCategories = dataSource.getCategoriesByProject(projectId)
        val hierarchyCheck = ExpenseCategoryValidator.validateHierarchy(
            categoryToUpdateId = "",
            newParentId = parentCategoryId,
            existingCategories = allCategories
        )
        if (hierarchyCheck is DomainResult.Error) return@withLock hierarchyCheck

        val categoryId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val category = ExpenseCategory(
            categoryId = categoryId,
            projectId = projectId,
            categoryCode = categoryCode.trim().uppercase(),
            categoryName = categoryName.trim(),
            description = description?.trim(),
            parentCategoryId = parentCategoryId?.trim(),
            accountHead = accountHead?.trim(),
            isActive = true,
            sortOrder = sortOrder,
            createdBy = actorId,
            createdAt = now,
            updatedAt = now
        )

        val inserted = dataSource.insertCategory(category)
        if (!inserted) {
            return@withLock DomainResult.Error(message = "Failed to insert expense category.")
        }

        DomainResult.Success(category)
    }

    override suspend fun updateCategory(
        categoryId: String,
        categoryName: String?,
        description: String?,
        parentCategoryId: String?,
        accountHead: String?,
        sortOrder: Int?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<ExpenseCategory> = mutex.withLock {
        val authResult = ExpenseAuthorizationValidator.validateUpdateCategory(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val existing = dataSource.getCategoryById(categoryId)
            ?: return@withLock DomainResult.Error(message = "Expense category '$categoryId' not found.")

        val allCategories = dataSource.getCategoriesByProject(existing.projectId)
        val updatedParentId = parentCategoryId ?: existing.parentCategoryId
        val hierarchyCheck = ExpenseCategoryValidator.validateHierarchy(
            categoryToUpdateId = categoryId,
            newParentId = updatedParentId,
            existingCategories = allCategories
        )
        if (hierarchyCheck is DomainResult.Error) return@withLock hierarchyCheck

        val updated = existing.copy(
            categoryName = categoryName?.trim() ?: existing.categoryName,
            description = description?.trim() ?: existing.description,
            parentCategoryId = updatedParentId,
            accountHead = accountHead?.trim() ?: existing.accountHead,
            sortOrder = sortOrder ?: existing.sortOrder,
            updatedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )

        dataSource.updateCategory(updated)
        DomainResult.Success(updated)
    }

    override suspend fun activateCategory(
        categoryId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<ExpenseCategory> = mutex.withLock {
        val authResult = ExpenseAuthorizationValidator.validateUpdateCategory(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val existing = dataSource.getCategoryById(categoryId)
            ?: return@withLock DomainResult.Error(message = "Expense category '$categoryId' not found.")

        val updated = existing.copy(
            isActive = true,
            updatedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )
        dataSource.updateCategory(updated)
        DomainResult.Success(updated)
    }

    override suspend fun deactivateCategory(
        categoryId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<ExpenseCategory> = mutex.withLock {
        val authResult = ExpenseAuthorizationValidator.validateUpdateCategory(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val existing = dataSource.getCategoryById(categoryId)
            ?: return@withLock DomainResult.Error(message = "Expense category '$categoryId' not found.")

        val updated = existing.copy(
            isActive = false,
            updatedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )
        dataSource.updateCategory(updated)
        DomainResult.Success(updated)
    }

    override suspend fun getCategoryById(
        categoryId: String,
        callerRole: UserRole
    ): DomainResult<ExpenseCategory> = mutex.withLock {
        val authResult = ExpenseAuthorizationValidator.validateViewExpenses(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val category = dataSource.getCategoryById(categoryId)
            ?: return@withLock DomainResult.Error(message = "Expense category '$categoryId' not found.")

        DomainResult.Success(category)
    }

    override suspend fun getCategoryByCode(
        projectId: String,
        categoryCode: String,
        callerRole: UserRole
    ): DomainResult<ExpenseCategory> = mutex.withLock {
        val authResult = ExpenseAuthorizationValidator.validateViewExpenses(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val category = dataSource.getCategoryByCode(projectId, categoryCode.trim())
            ?: return@withLock DomainResult.Error(
                message = "Expense category code '$categoryCode' not found in project '$projectId'."
            )

        DomainResult.Success(category)
    }

    override fun observeCategories(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<ExpenseCategory>> {
        val authResult = ExpenseAuthorizationValidator.validateViewExpenses(callerRole)
        if (authResult is DomainResult.Error) return emptyFlow()
        return dataSource.observeCategories(projectId)
    }

    override suspend fun seedDefaultCategoriesIfEmpty(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<ExpenseCategory>> = mutex.withLock {
        val authResult = ExpenseAuthorizationValidator.validateCreateCategory(callerRole)
        if (authResult is DomainResult.Error) return@withLock authResult

        val existing = dataSource.getCategoriesByProject(projectId)
        if (existing.isNotEmpty()) {
            return@withLock DomainResult.Success(existing)
        }

        val defaultSeeds = listOf(
            Triple("OFFICE", "Office Supplies & Stationeries", "OFFICE_EXPENSE"),
            Triple("UTILITIES", "General Utilities", "UTILITIES_EXPENSE"),
            Triple("ELECTRICITY", "Electricity Bill", "ELECTRICITY_EXPENSE"),
            Triple("INTERNET", "Internet & Broadband", "INTERNET_EXPENSE"),
            Triple("TELEPHONE", "Telephone & Mobile", "TELEPHONE_EXPENSE"),
            Triple("RENT", "Office & Factory Rent", "RENT_EXPENSE"),
            Triple("TRANSPORT", "Transport & Conveyance", "TRANSPORT_EXPENSE"),
            Triple("DELIVERY", "Delivery & Courier Cost", "DELIVERY_EXPENSE"),
            Triple("MAINTENANCE", "Machine & Equipment Maintenance", "MAINTENANCE_EXPENSE"),
            Triple("MARKETING", "Marketing & Advertisement", "MARKETING_EXPENSE"),
            Triple("SOFTWARE", "Software, Server & Subscriptions", "SOFTWARE_EXPENSE"),
            Triple("STAFF_WELFARE", "Staff Welfare & Refreshments", "STAFF_WELFARE_EXPENSE"),
            Triple("BANK_CHARGES", "Bank Charges & Excise Duty", "BANK_CHARGES"),
            Triple("PRINTING_OPS", "Printing Consumables & Operations", "PRINTING_OPERATIONS_EXPENSE"),
            Triple("MISCELLANEOUS", "Miscellaneous Operating Expenses", "MISC_EXPENSE")
        )

        val createdList = mutableListOf<ExpenseCategory>()
        val now = System.currentTimeMillis()

        defaultSeeds.forEachIndexed { index, (code, name, head) ->
            val cat = ExpenseCategory(
                categoryId = UUID.randomUUID().toString(),
                projectId = projectId,
                categoryCode = code,
                categoryName = name,
                description = "Standard system expense category",
                parentCategoryId = null,
                accountHead = head,
                isActive = true,
                sortOrder = index + 1,
                createdBy = actorId,
                createdAt = now,
                updatedAt = now
            )
            dataSource.insertCategory(cat)
            createdList.add(cat)
        }

        DomainResult.Success(createdList)
    }
}

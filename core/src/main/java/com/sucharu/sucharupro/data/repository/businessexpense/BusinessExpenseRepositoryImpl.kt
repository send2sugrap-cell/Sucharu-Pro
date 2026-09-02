package com.sucharu.sucharupro.data.repository.businessexpense

import com.sucharu.sucharupro.data.datasource.businessexpense.BusinessExpenseDataSource
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseAuditEvent
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseCategory
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.businessexpense.BusinessExpenseRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe implementation of BusinessExpenseRepository using Mutex concurrency control (Module 15 Step 01).
 */
class BusinessExpenseRepositoryImpl(
    private val dataSource: BusinessExpenseDataSource
) : BusinessExpenseRepository {

    private val mutex = Mutex()

    override suspend fun createExpense(expense: BusinessExpense): DomainResult<BusinessExpense> = mutex.withLock {
        try {
            // Check idempotency if key provided
            if (!expense.idempotencyKey.isNullOrBlank()) {
                val existing = dataSource.getExpenseByIdempotencyKey(
                    expense.tenantId,
                    expense.projectId,
                    expense.idempotencyKey
                )
                if (existing != null) {
                    return DomainResult.Success(existing)
                }
            }

            val inserted = dataSource.insertExpense(expense)
            if (inserted) {
                DomainResult.Success(expense)
            } else {
                DomainResult.Error(message = "Failed to insert business expense.")
            }
        } catch (e: Exception) {
            DomainResult.Error(message = "Exception creating business expense: ${e.message}", exception = e)
        }
    }

    override suspend fun updateExpense(expense: BusinessExpense): DomainResult<BusinessExpense> = mutex.withLock {
        try {
            val updated = dataSource.updateExpense(expense)
            if (updated) {
                DomainResult.Success(expense)
            } else {
                DomainResult.Error(message = "Failed to update business expense ${expense.expenseId}.")
            }
        } catch (e: Exception) {
            DomainResult.Error(message = "Exception updating business expense: ${e.message}", exception = e)
        }
    }

    override suspend fun getExpenseById(
        tenantId: String,
        projectId: String,
        expenseId: String
    ): DomainResult<BusinessExpense?> {
        return try {
            val result = dataSource.getExpenseById(tenantId, projectId, expenseId)
            DomainResult.Success(result)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to retrieve expense $expenseId: ${e.message}", exception = e)
        }
    }

    override suspend fun getExpenseByNumber(
        tenantId: String,
        projectId: String,
        expenseNumber: String
    ): DomainResult<BusinessExpense?> {
        return try {
            val result = dataSource.getExpenseByNumber(tenantId, projectId, expenseNumber)
            DomainResult.Success(result)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to retrieve expense by number $expenseNumber: ${e.message}", exception = e)
        }
    }

    override suspend fun getExpenseByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<BusinessExpense?> {
        return try {
            val result = dataSource.getExpenseByIdempotencyKey(tenantId, projectId, idempotencyKey)
            DomainResult.Success(result)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to retrieve expense by idempotency key: ${e.message}", exception = e)
        }
    }

    override suspend fun listExpenses(
        tenantId: String,
        projectId: String,
        status: BusinessExpenseStatus?,
        categoryId: String?,
        vendorId: String?,
        jobId: String?,
        fromDate: Long?,
        toDate: Long?,
        limit: Int,
        offset: Int
    ): DomainResult<List<BusinessExpense>> {
        return try {
            val list = dataSource.listExpenses(
                tenantId = tenantId,
                projectId = projectId,
                status = status,
                categoryId = categoryId,
                vendorId = vendorId,
                jobId = jobId,
                fromDate = fromDate,
                toDate = toDate,
                limit = limit,
                offset = offset
            )
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to list expenses: ${e.message}", exception = e)
        }
    }

    override suspend fun countExpenses(
        tenantId: String,
        projectId: String,
        status: BusinessExpenseStatus?,
        categoryId: String?,
        vendorId: String?,
        jobId: String?,
        fromDate: Long?,
        toDate: Long?
    ): DomainResult<Long> {
        return try {
            val count = dataSource.countExpenses(
                tenantId = tenantId,
                projectId = projectId,
                status = status,
                categoryId = categoryId,
                vendorId = vendorId,
                jobId = jobId,
                fromDate = fromDate,
                toDate = toDate
            )
            DomainResult.Success(count)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to count expenses: ${e.message}", exception = e)
        }
    }

    override suspend fun generateNextExpenseNumber(tenantId: String, projectId: String): String {
        return dataSource.generateNextExpenseNumber(tenantId, projectId)
    }

    override suspend fun createCategory(category: BusinessExpenseCategory): DomainResult<BusinessExpenseCategory> = mutex.withLock {
        try {
            val existing = dataSource.getCategoryByCode(category.tenantId, category.projectId, category.code)
            if (existing != null) {
                return DomainResult.Error(message = "Active category with code '${category.code}' already exists.")
            }
            val inserted = dataSource.insertCategory(category)
            if (inserted) {
                DomainResult.Success(category)
            } else {
                DomainResult.Error(message = "Failed to create expense category.")
            }
        } catch (e: Exception) {
            DomainResult.Error(message = "Exception creating expense category: ${e.message}", exception = e)
        }
    }

    override suspend fun updateCategory(category: BusinessExpenseCategory): DomainResult<BusinessExpenseCategory> = mutex.withLock {
        try {
            val updated = dataSource.updateCategory(category)
            if (updated) {
                DomainResult.Success(category)
            } else {
                DomainResult.Error(message = "Failed to update expense category.")
            }
        } catch (e: Exception) {
            DomainResult.Error(message = "Exception updating expense category: ${e.message}", exception = e)
        }
    }

    override suspend fun getCategoryById(
        tenantId: String,
        projectId: String,
        categoryId: String
    ): DomainResult<BusinessExpenseCategory?> {
        return try {
            val result = dataSource.getCategoryById(tenantId, projectId, categoryId)
            DomainResult.Success(result)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to retrieve category $categoryId: ${e.message}", exception = e)
        }
    }

    override suspend fun getCategoryByCode(
        tenantId: String,
        projectId: String,
        code: String
    ): DomainResult<BusinessExpenseCategory?> {
        return try {
            val result = dataSource.getCategoryByCode(tenantId, projectId, code)
            DomainResult.Success(result)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to retrieve category code $code: ${e.message}", exception = e)
        }
    }

    override suspend fun listCategories(
        tenantId: String,
        projectId: String,
        activeOnly: Boolean
    ): DomainResult<List<BusinessExpenseCategory>> {
        return try {
            val list = dataSource.listCategories(tenantId, projectId, activeOnly)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to list categories: ${e.message}", exception = e)
        }
    }

    override suspend fun recordAuditEvent(event: BusinessExpenseAuditEvent): DomainResult<Unit> {
        return try {
            dataSource.insertAuditEvent(event)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to record audit event: ${e.message}", exception = e)
        }
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        expenseId: String
    ): DomainResult<List<BusinessExpenseAuditEvent>> {
        return try {
            val list = dataSource.getAuditEvents(tenantId, projectId, expenseId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to get audit events: ${e.message}", exception = e)
        }
    }
}

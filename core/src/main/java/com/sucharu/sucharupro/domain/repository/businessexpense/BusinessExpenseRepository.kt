package com.sucharu.sucharupro.domain.repository.businessexpense

import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseAuditEvent
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseCategory
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Repository interface for Business Expenses (Module 15 Step 01).
 */
interface BusinessExpenseRepository {

    suspend fun createExpense(expense: BusinessExpense): DomainResult<BusinessExpense>

    suspend fun updateExpense(expense: BusinessExpense): DomainResult<BusinessExpense>

    suspend fun getExpenseById(tenantId: String, projectId: String, expenseId: String): DomainResult<BusinessExpense?>

    suspend fun getExpenseByNumber(tenantId: String, projectId: String, expenseNumber: String): DomainResult<BusinessExpense?>

    suspend fun getExpenseByIdempotencyKey(tenantId: String, projectId: String, idempotencyKey: String): DomainResult<BusinessExpense?>

    suspend fun listExpenses(
        tenantId: String,
        projectId: String,
        status: BusinessExpenseStatus? = null,
        categoryId: String? = null,
        vendorId: String? = null,
        jobId: String? = null,
        fromDate: Long? = null,
        toDate: Long? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<BusinessExpense>>

    suspend fun countExpenses(
        tenantId: String,
        projectId: String,
        status: BusinessExpenseStatus? = null,
        categoryId: String? = null,
        vendorId: String? = null,
        jobId: String? = null,
        fromDate: Long? = null,
        toDate: Long? = null
    ): DomainResult<Long>

    suspend fun generateNextExpenseNumber(tenantId: String, projectId: String): String

    suspend fun createCategory(category: BusinessExpenseCategory): DomainResult<BusinessExpenseCategory>

    suspend fun updateCategory(category: BusinessExpenseCategory): DomainResult<BusinessExpenseCategory>

    suspend fun getCategoryById(tenantId: String, projectId: String, categoryId: String): DomainResult<BusinessExpenseCategory?>

    suspend fun getCategoryByCode(tenantId: String, projectId: String, code: String): DomainResult<BusinessExpenseCategory?>

    suspend fun listCategories(tenantId: String, projectId: String, activeOnly: Boolean = true): DomainResult<List<BusinessExpenseCategory>>

    suspend fun recordAuditEvent(event: BusinessExpenseAuditEvent): DomainResult<Unit>

    suspend fun getAuditEvents(tenantId: String, projectId: String, expenseId: String): DomainResult<List<BusinessExpenseAuditEvent>>
}

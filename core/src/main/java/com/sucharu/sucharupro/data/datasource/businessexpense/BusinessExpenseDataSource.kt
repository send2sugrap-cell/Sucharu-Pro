package com.sucharu.sucharupro.data.datasource.businessexpense

import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseAuditEvent
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseCategory
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus

/**
 * Data Source contract for Business Expenses, Categories, and Audit Events (Module 15 Step 01).
 */
interface BusinessExpenseDataSource {

    suspend fun insertExpense(expense: BusinessExpense): Boolean

    suspend fun updateExpense(expense: BusinessExpense): Boolean

    suspend fun getExpenseById(tenantId: String, projectId: String, expenseId: String): BusinessExpense?

    suspend fun getExpenseByNumber(tenantId: String, projectId: String, expenseNumber: String): BusinessExpense?

    suspend fun getExpenseByIdempotencyKey(tenantId: String, projectId: String, idempotencyKey: String): BusinessExpense?

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
    ): List<BusinessExpense>

    suspend fun countExpenses(
        tenantId: String,
        projectId: String,
        status: BusinessExpenseStatus? = null,
        categoryId: String? = null,
        vendorId: String? = null,
        jobId: String? = null,
        fromDate: Long? = null,
        toDate: Long? = null
    ): Long

    suspend fun generateNextExpenseNumber(tenantId: String, projectId: String): String

    suspend fun insertCategory(category: BusinessExpenseCategory): Boolean

    suspend fun updateCategory(category: BusinessExpenseCategory): Boolean

    suspend fun getCategoryById(tenantId: String, projectId: String, categoryId: String): BusinessExpenseCategory?

    suspend fun getCategoryByCode(tenantId: String, projectId: String, code: String): BusinessExpenseCategory?

    suspend fun listCategories(tenantId: String, projectId: String, activeOnly: Boolean = true): List<BusinessExpenseCategory>

    suspend fun insertAuditEvent(event: BusinessExpenseAuditEvent): Boolean

    suspend fun getAuditEvents(tenantId: String, projectId: String, expenseId: String): List<BusinessExpenseAuditEvent>
}

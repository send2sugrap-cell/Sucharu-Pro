package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.Expense
import com.sucharu.sucharupro.domain.model.finance.ExpenseActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import kotlinx.coroutines.flow.Flow

/**
 * Data Source contract for Expenses (Module 09 Step 06).
 */
interface ExpenseDataSource {

    suspend fun insertExpense(expense: Expense): Boolean

    suspend fun updateExpense(expense: Expense): Boolean

    suspend fun getExpenseById(expenseId: String): Expense?

    suspend fun getExpenseByNumber(projectId: String, expenseNo: String): Expense?

    suspend fun getExpenseByIdempotencyKey(projectId: String, idempotencyKey: String): Expense?

    suspend fun getExpensesByReference(
        projectId: String,
        referenceType: FinancialReferenceType,
        referenceId: String
    ): List<Expense>

    fun observeExpenses(projectId: String): Flow<List<Expense>>

    fun observeExpensesByCategory(projectId: String, categoryId: String): Flow<List<Expense>>

    suspend fun insertActivityEvent(event: ExpenseActivityEvent): Boolean

    suspend fun getActivityEvents(expenseId: String): List<ExpenseActivityEvent>

    suspend fun generateNextExpenseNo(projectId: String): String
}

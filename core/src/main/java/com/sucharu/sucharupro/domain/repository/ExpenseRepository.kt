package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.Expense
import com.sucharu.sucharupro.domain.model.finance.ExpenseActivityEvent
import com.sucharu.sucharupro.domain.model.finance.ExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.finance.ExpenseSummary
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for operational Expense management (Module 09 Step 06).
 */
interface ExpenseRepository {

    suspend fun createExpense(
        projectId: String,
        categoryId: String,
        amount: Money,
        currency: String = "BDT",
        description: String,
        paymentMethod: ExpensePaymentMethod,
        paymentReference: String? = null,
        vendorId: String? = null,
        referenceType: FinancialReferenceType? = null,
        referenceId: String? = null,
        expenseDate: Long = System.currentTimeMillis(),
        notes: String? = null,
        idempotencyKey: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Expense>

    suspend fun updateDraftExpense(
        expenseId: String,
        categoryId: String? = null,
        amount: Money? = null,
        description: String? = null,
        paymentMethod: ExpensePaymentMethod? = null,
        paymentReference: String? = null,
        vendorId: String? = null,
        referenceType: FinancialReferenceType? = null,
        referenceId: String? = null,
        expenseDate: Long? = null,
        notes: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Expense>

    suspend fun submitExpense(
        expenseId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Expense>

    suspend fun approveExpense(
        expenseId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Expense>

    suspend fun rejectExpense(
        expenseId: String,
        rejectionReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Expense>

    suspend fun cancelExpense(
        expenseId: String,
        cancellationReason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Expense>

    suspend fun postExpense(
        expenseId: String,
        overrideAccountHead: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Expense>

    suspend fun getExpenseById(
        expenseId: String,
        callerRole: UserRole
    ): DomainResult<Expense>

    suspend fun getExpenseByNumber(
        projectId: String,
        expenseNo: String,
        callerRole: UserRole
    ): DomainResult<Expense>

    suspend fun getExpenseByIdempotencyKey(
        projectId: String,
        idempotencyKey: String,
        callerRole: UserRole
    ): DomainResult<Expense?>

    fun observeExpenses(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<Expense>>

    fun observeExpensesByCategory(
        projectId: String,
        categoryId: String,
        callerRole: UserRole
    ): Flow<List<Expense>>

    suspend fun getExpenseSummary(
        projectId: String,
        startDate: Long? = null,
        endDate: Long? = null,
        callerRole: UserRole
    ): DomainResult<ExpenseSummary>

    suspend fun getActivityEvents(
        expenseId: String,
        callerRole: UserRole
    ): DomainResult<List<ExpenseActivityEvent>>
}

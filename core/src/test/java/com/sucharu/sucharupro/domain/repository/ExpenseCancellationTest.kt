package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeExpenseCategoryDataSource
import com.sucharu.sucharupro.data.datasource.FakeExpenseDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.repository.ExpenseCategoryRepositoryImpl
import com.sucharu.sucharupro.data.repository.ExpenseRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.ExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.finance.ExpenseStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ExpenseCancellationTest {

    private lateinit var categoryDataSource: FakeExpenseCategoryDataSource
    private lateinit var expenseDataSource: FakeExpenseDataSource
    private lateinit var financeTransactionDataSource: FakeFinancialTransactionDataSource

    private lateinit var categoryRepository: ExpenseCategoryRepository
    private lateinit var financialTransactionRepository: FinancialTransactionRepository
    private lateinit var expenseRepository: ExpenseRepository

    @Before
    fun setUp() {
        categoryDataSource = FakeExpenseCategoryDataSource()
        expenseDataSource = FakeExpenseDataSource()
        financeTransactionDataSource = FakeFinancialTransactionDataSource()

        categoryRepository = ExpenseCategoryRepositoryImpl(categoryDataSource)
        financialTransactionRepository = FinancialTransactionRepositoryImpl(financeTransactionDataSource)
        expenseRepository = ExpenseRepositoryImpl(
            expenseDataSource,
            categoryDataSource,
            financialTransactionRepository
        )
    }

    @Test
    fun `cancelling draft or pending expense transitions to CANCELLED state and prevents posting`() = runBlocking {
        val catId = (categoryRepository.createCategory(
            projectId = "PRJ-01",
            categoryCode = "OFFICE",
            categoryName = "Office Supplies",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data.categoryId

        val exp = (expenseRepository.createExpense(
            projectId = "PRJ-01",
            categoryId = catId,
            amount = Money(BigDecimal("1500.00")),
            currency = "BDT",
            description = "Expense cancel test",
            paymentMethod = ExpensePaymentMethod.CASH,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        val cancelRes = expenseRepository.cancelExpense(exp.expenseId, "Budget cut", "acct-1", UserRole.ACCOUNTS)
        assertTrue(cancelRes is DomainResult.Success)
        val cancelled = (cancelRes as DomainResult.Success).data
        assertEquals(ExpenseStatus.CANCELLED, cancelled.status)
        assertEquals("Budget cut", cancelled.cancellationReason)

        // Attempting to post cancelled expense -> Rejected
        val postRes = expenseRepository.postExpense(exp.expenseId, "OFFICE_EXPENSE", "acct-2", UserRole.ACCOUNTS)
        assertTrue(postRes is DomainResult.Error)
    }
}

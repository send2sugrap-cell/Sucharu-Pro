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
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ExpenseBalanceTest {

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
    fun `summary aggregates total, posted, pending, and draft amounts accurately`() = runBlocking {
        val catId = (categoryRepository.createCategory(
            projectId = "PRJ-01",
            categoryCode = "OFFICE",
            categoryName = "Office Supplies",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data.categoryId

        // Draft expense ৳1000
        expenseRepository.createExpense(
            projectId = "PRJ-01",
            categoryId = catId,
            amount = Money(BigDecimal("1000.00")),
            description = "Draft expense",
            paymentMethod = ExpensePaymentMethod.CASH,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )

        // Pending expense ৳2000
        val p2 = (expenseRepository.createExpense(
            projectId = "PRJ-01",
            categoryId = catId,
            amount = Money(BigDecimal("2000.00")),
            description = "Pending expense",
            paymentMethod = ExpensePaymentMethod.CASH,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        // Posted expense ৳5000
        val p3 = (expenseRepository.createExpense(
            projectId = "PRJ-01",
            categoryId = catId,
            amount = Money(BigDecimal("5000.00")),
            description = "Posted expense",
            paymentMethod = ExpensePaymentMethod.CASH,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data
        expenseRepository.postExpense(p3.expenseId, "OFFICE_EXPENSE", "acct-2", UserRole.ACCOUNTS)

        val summaryRes = expenseRepository.getExpenseSummary("PRJ-01", null, null, UserRole.ACCOUNTS)
        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data

        assertEquals(Money(BigDecimal("8000.00")), summary.totalExpenses)
        assertEquals(Money(BigDecimal("5000.00")), summary.postedExpenses)
        assertEquals(Money(BigDecimal("2000.00")), summary.pendingExpenses)
        assertEquals(Money(BigDecimal("1000.00")), summary.draftExpenses)
        assertEquals(3, summary.totalCount)
        assertEquals(1, summary.postedCount)
    }
}

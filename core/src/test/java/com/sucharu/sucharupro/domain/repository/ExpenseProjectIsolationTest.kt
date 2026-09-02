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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ExpenseProjectIsolationTest {

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
    fun `expenses in Project A are completely isolated from Project B`() = runBlocking {
        // Project A
        val catA = (categoryRepository.createCategory(
            projectId = "PRJ-A",
            categoryCode = "OFFICE",
            categoryName = "Office Supplies A",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data.categoryId

        expenseRepository.createExpense(
            projectId = "PRJ-A",
            categoryId = catA,
            amount = Money(BigDecimal("1000.00")),
            currency = "BDT",
            description = "Expense A",
            paymentMethod = ExpensePaymentMethod.CASH,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )

        // Project B
        val catB = (categoryRepository.createCategory(
            projectId = "PRJ-B",
            categoryCode = "OFFICE",
            categoryName = "Office Supplies B",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data.categoryId

        expenseRepository.createExpense(
            projectId = "PRJ-B",
            categoryId = catB,
            amount = Money(BigDecimal("2000.00")),
            currency = "BDT",
            description = "Expense B",
            paymentMethod = ExpensePaymentMethod.CASH,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )

        val expensesA = expenseRepository.observeExpenses("PRJ-A", UserRole.ACCOUNTS).first()
        val expensesB = expenseRepository.observeExpenses("PRJ-B", UserRole.ACCOUNTS).first()

        assertEquals(1, expensesA.size)
        assertEquals("PRJ-A", expensesA[0].projectId)

        assertEquals(1, expensesB.size)
        assertEquals("PRJ-B", expensesB[0].projectId)
    }
}

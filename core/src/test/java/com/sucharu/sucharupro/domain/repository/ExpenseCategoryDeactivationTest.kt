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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ExpenseCategoryDeactivationTest {

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
    fun `deactivated category prevents new expense creation but preserves existing history`() = runBlocking {
        val cat = (categoryRepository.createCategory(
            projectId = "PRJ-01",
            categoryCode = "TEMP",
            categoryName = "Temporary Category",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        // Deactivate category
        val deactRes = categoryRepository.deactivateCategory(cat.categoryId, "acct-1", UserRole.ACCOUNTS)
        assertTrue(deactRes is DomainResult.Success)
        assertFalse((deactRes as DomainResult.Success).data.isActive)

        // Attempt new expense under deactivated category -> Rejected
        val expRes = expenseRepository.createExpense(
            projectId = "PRJ-01",
            categoryId = cat.categoryId,
            amount = Money(BigDecimal("1000.00")),
            currency = "BDT",
            description = "Expense in deactivated category",
            paymentMethod = ExpensePaymentMethod.CASH,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(expRes is DomainResult.Error)
    }
}

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ExpenseConcurrencyTest {

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
    fun `concurrent posting of the same expense produces exactly one success and no duplicate transactions`() = runBlocking {
        val catId = (categoryRepository.createCategory(
            projectId = "PRJ-01",
            categoryCode = "OFFICE",
            categoryName = "Office Supplies",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data.categoryId

        val expense = (expenseRepository.createExpense(
            projectId = "PRJ-01",
            categoryId = catId,
            amount = Money(BigDecimal("10000.00")),
            currency = "BDT",
            description = "Office AC servicing",
            paymentMethod = ExpensePaymentMethod.CASH,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        // Launch 20 concurrent posting requests for the same expense
        val deferredPosts = (1..20).map {
            async(Dispatchers.Default) {
                expenseRepository.postExpense(expense.expenseId, "OFFICE_EXPENSE", "acct-2", UserRole.ACCOUNTS)
            }
        }

        val results = deferredPosts.awaitAll()
        val successCount = results.count { it is DomainResult.Success }
        val errorCount = results.count { it is DomainResult.Error }

        assertEquals(1, successCount)
        assertEquals(19, errorCount)

        val postedExpense = (expenseRepository.getExpenseById(expense.expenseId, UserRole.ACCOUNTS) as DomainResult.Success).data
        assertEquals(ExpenseStatus.POSTED, postedExpense.status)
    }
}

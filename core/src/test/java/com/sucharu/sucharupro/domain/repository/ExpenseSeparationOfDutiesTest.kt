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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ExpenseSeparationOfDutiesTest {

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
    fun `creator cannot approve or post their own expense unless they are ADMIN`() = runBlocking {
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
            amount = Money(BigDecimal("5000.00")),
            currency = "BDT",
            description = "Expense SOD test",
            paymentMethod = ExpensePaymentMethod.CASH,
            actorId = "creator-acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        // Creator attempts to approve as ACCOUNTS -> Fails SOD
        val selfApprove = expenseRepository.approveExpense(exp.expenseId, "creator-acct-1", UserRole.ACCOUNTS)
        assertTrue(selfApprove is DomainResult.Error)

        // Creator attempts to post as ACCOUNTS -> Fails SOD
        val selfPost = expenseRepository.postExpense(exp.expenseId, "OFFICE_EXPENSE", "creator-acct-1", UserRole.ACCOUNTS)
        assertTrue(selfPost is DomainResult.Error)

        // ADMIN can post even if creator
        val adminPost = expenseRepository.postExpense(exp.expenseId, "OFFICE_EXPENSE", "admin-1", UserRole.ADMIN)
        assertTrue(adminPost is DomainResult.Success)
    }
}

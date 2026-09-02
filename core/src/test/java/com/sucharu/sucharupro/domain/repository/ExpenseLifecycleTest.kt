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

class ExpenseLifecycleTest {

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
    fun `full lifecycle transitions from draft to submit to approve to posted`() = runBlocking {
        val catId = (categoryRepository.createCategory(
            projectId = "PRJ-01",
            categoryCode = "OFFICE",
            categoryName = "Office Supplies",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data.categoryId

        // 1. Staff creates draft
        val createRes = expenseRepository.createExpense(
            projectId = "PRJ-01",
            categoryId = catId,
            amount = Money(BigDecimal("2500.00")),
            currency = "BDT",
            description = "Printer toners",
            paymentMethod = ExpensePaymentMethod.CASH,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(createRes is DomainResult.Success)
        val expenseId = (createRes as DomainResult.Success).data.expenseId
        assertEquals(ExpenseStatus.DRAFT, createRes.data.status)

        // 2. Submit
        val submitRes = expenseRepository.submitExpense(expenseId, "staff-1", UserRole.STAFF)
        assertTrue(submitRes is DomainResult.Success)
        assertEquals(ExpenseStatus.PENDING, (submitRes as DomainResult.Success).data.status)

        // 3. Approve
        val approveRes = expenseRepository.approveExpense(expenseId, "mgr-1", UserRole.MANAGER)
        assertTrue(approveRes is DomainResult.Success)
        assertEquals(ExpenseStatus.APPROVED, (approveRes as DomainResult.Success).data.status)

        // 4. Post
        val postRes = expenseRepository.postExpense(expenseId, "OFFICE_EXPENSE", "acct-1", UserRole.ACCOUNTS)
        assertTrue(postRes is DomainResult.Success)
        assertEquals(ExpenseStatus.POSTED, (postRes as DomainResult.Success).data.status)
    }

    @Test
    fun `rejection transitions expense to terminal REJECTED state`() = runBlocking {
        val catId = (categoryRepository.createCategory(
            projectId = "PRJ-01",
            categoryCode = "OFFICE",
            categoryName = "Office Supplies",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data.categoryId

        val expRes = expenseRepository.createExpense(
            projectId = "PRJ-01",
            categoryId = catId,
            amount = Money(BigDecimal("2000.00")),
            currency = "BDT",
            description = "Snacks purchase",
            paymentMethod = ExpensePaymentMethod.CASH,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val expenseId = (expRes as DomainResult.Success).data.expenseId

        val rejectRes = expenseRepository.rejectExpense(expenseId, "Not approved for this quarter", "mgr-1", UserRole.MANAGER)
        assertTrue(rejectRes is DomainResult.Success)
        val rejected = (rejectRes as DomainResult.Success).data
        assertEquals(ExpenseStatus.REJECTED, rejected.status)
        assertEquals("Not approved for this quarter", rejected.cancellationReason)
    }
}

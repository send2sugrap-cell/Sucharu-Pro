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
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ExpenseValidationTest {

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
    fun `valid expense creation passes validation`() = runBlocking {
        val catRes = categoryRepository.createCategory(
            projectId = "PRJ-01",
            categoryCode = "OFFICE",
            categoryName = "Office Supplies",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val catId = (catRes as DomainResult.Success).data.categoryId

        val expRes = expenseRepository.createExpense(
            projectId = "PRJ-01",
            categoryId = catId,
            amount = Money(BigDecimal("1500.00")),
            currency = "BDT",
            description = "Stationery purchase for office",
            paymentMethod = ExpensePaymentMethod.CASH,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(expRes is DomainResult.Success)
    }

    @Test
    fun `zero or negative amount is rejected`() = runBlocking {
        val catRes = categoryRepository.createCategory(
            projectId = "PRJ-01",
            categoryCode = "OFFICE",
            categoryName = "Office Supplies",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val catId = (catRes as DomainResult.Success).data.categoryId

        val zeroRes = expenseRepository.createExpense(
            projectId = "PRJ-01",
            categoryId = catId,
            amount = Money.ZERO,
            currency = "BDT",
            description = "Zero amount",
            paymentMethod = ExpensePaymentMethod.CASH,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(zeroRes is DomainResult.Error)

        val negRes = expenseRepository.createExpense(
            projectId = "PRJ-01",
            categoryId = catId,
            amount = Money(BigDecimal("-500.00")),
            currency = "BDT",
            description = "Negative amount",
            paymentMethod = ExpensePaymentMethod.CASH,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(negRes is DomainResult.Error)
    }

    @Test
    fun `non-cash method without payment reference is rejected`() = runBlocking {
        val catRes = categoryRepository.createCategory(
            projectId = "PRJ-01",
            categoryCode = "UTILITIES",
            categoryName = "Electricity Bill",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val catId = (catRes as DomainResult.Success).data.categoryId

        val noRefRes = expenseRepository.createExpense(
            projectId = "PRJ-01",
            categoryId = catId,
            amount = Money(BigDecimal("5000.00")),
            currency = "BDT",
            description = "DESCO electricity bill",
            paymentMethod = ExpensePaymentMethod.BANK_TRANSFER,
            paymentReference = "   ",
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(noRefRes is DomainResult.Error)
    }

    @Test
    fun `blank description or missing category is rejected`() = runBlocking {
        val catRes = categoryRepository.createCategory(
            projectId = "PRJ-01",
            categoryCode = "OFFICE",
            categoryName = "Office Supplies",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val catId = (catRes as DomainResult.Success).data.categoryId

        val blankDescRes = expenseRepository.createExpense(
            projectId = "PRJ-01",
            categoryId = catId,
            amount = Money(BigDecimal("1000.00")),
            currency = "BDT",
            description = "  ",
            paymentMethod = ExpensePaymentMethod.CASH,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(blankDescRes is DomainResult.Error)

        val nonExistentCatRes = expenseRepository.createExpense(
            projectId = "PRJ-01",
            categoryId = "NON-EXISTENT-CAT",
            amount = Money(BigDecimal("1000.00")),
            currency = "BDT",
            description = "Valid description",
            paymentMethod = ExpensePaymentMethod.CASH,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(nonExistentCatRes is DomainResult.Error)
    }
}

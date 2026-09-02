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
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ExpenseStep01IntegrationTest {

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
    fun `posting expense automatically creates and posts canonical Step 01 FinancialTransaction and FinancialLedgerEntry`() = runBlocking {
        val catId = (categoryRepository.createCategory(
            projectId = "PRJ-01",
            categoryCode = "OFFICE",
            categoryName = "Office Supplies",
            accountHead = "OFFICE_EXPENSE",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data.categoryId

        val exp = (expenseRepository.createExpense(
            projectId = "PRJ-01",
            categoryId = catId,
            amount = Money(BigDecimal("4200.00")),
            currency = "BDT",
            description = "Printer maintenance toner",
            paymentMethod = ExpensePaymentMethod.CASH,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        val postRes = expenseRepository.postExpense(exp.expenseId, null, "acct-2", UserRole.ACCOUNTS)
        assertTrue(postRes is DomainResult.Success)
        val postedExp = (postRes as DomainResult.Success).data

        assertNotNull(postedExp.financialTransactionId)

        val txnRes = financialTransactionRepository.getTransactionById(postedExp.financialTransactionId!!, UserRole.ACCOUNTS)
        assertTrue(txnRes is DomainResult.Success)
        val txn = (txnRes as DomainResult.Success).data

        assertEquals(FinancialTransactionType.EXPENSE, txn.transactionType)
        assertEquals(FinancialEntryType.DEBIT, txn.entryType)
        assertEquals(FinancialReferenceType.EXPENSE, txn.referenceType)
        assertEquals(exp.expenseId, txn.referenceId)
        assertEquals(Money(BigDecimal("4200.00")), txn.amount)

        val ledgerEntriesRes = financialTransactionRepository.getLedgerEntriesByTransaction(txn.transactionId, UserRole.ACCOUNTS)
        assertTrue(ledgerEntriesRes is DomainResult.Success)
        val entries = (ledgerEntriesRes as DomainResult.Success).data
        assertEquals(1, entries.size)
        assertEquals("OFFICE_EXPENSE", entries[0].accountHead)
        assertEquals(FinancialEntryType.DEBIT, entries[0].entryType)
        assertEquals(Money(BigDecimal("4200.00")), entries[0].amount)
    }
}

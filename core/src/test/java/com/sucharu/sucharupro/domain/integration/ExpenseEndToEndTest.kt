package com.sucharu.sucharupro.domain.integration

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
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ExpenseCategoryRepository
import com.sucharu.sucharupro.domain.repository.ExpenseRepository
import com.sucharu.sucharupro.domain.repository.FinancialTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ExpenseEndToEndTest {

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
    fun `end-to-end expense lifecycle from category creation to staff draft submission to manager approval to accounts ledger posting`() = runBlocking {
        // Step 1: Accounts creates Categories
        val seedRes = categoryRepository.seedDefaultCategoriesIfEmpty("PRJ-01", "acct-1", UserRole.ACCOUNTS)
        assertTrue(seedRes is DomainResult.Success)
        val categories = (seedRes as DomainResult.Success).data
        assertTrue(categories.isNotEmpty())

        val electricityCategory = categories.first { it.categoryCode == "ELECTRICITY" }

        // Step 2: Staff creates Draft Expense for electricity
        val draftRes = expenseRepository.createExpense(
            projectId = "PRJ-01",
            categoryId = electricityCategory.categoryId,
            amount = Money(BigDecimal("18500.00")),
            currency = "BDT",
            description = "DESCO monthly electricity bill for August 2026",
            paymentMethod = ExpensePaymentMethod.BANK_TRANSFER,
            paymentReference = "EFT-DESCO-998811",
            expenseDate = System.currentTimeMillis(),
            notes = "Checked meter reading #88392",
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(draftRes is DomainResult.Success)
        val draftExpense = (draftRes as DomainResult.Success).data
        assertEquals(ExpenseStatus.DRAFT, draftExpense.status)

        // Step 3: Staff submits for approval
        val submitRes = expenseRepository.submitExpense(draftExpense.expenseId, "staff-1", UserRole.STAFF)
        assertTrue(submitRes is DomainResult.Success)
        assertEquals(ExpenseStatus.PENDING, (submitRes as DomainResult.Success).data.status)

        // Step 4: Manager approves expense
        val approveRes = expenseRepository.approveExpense(draftExpense.expenseId, "mgr-1", UserRole.MANAGER)
        assertTrue(approveRes is DomainResult.Success)
        assertEquals(ExpenseStatus.APPROVED, (approveRes as DomainResult.Success).data.status)

        // Step 5: Accounts posts expense to ledger
        val postRes = expenseRepository.postExpense(draftExpense.expenseId, null, "acct-1", UserRole.ACCOUNTS)
        assertTrue(postRes is DomainResult.Success)
        val postedExpense = (postRes as DomainResult.Success).data
        assertEquals(ExpenseStatus.POSTED, postedExpense.status)
        assertNotNull(postedExpense.financialTransactionId)

        // Verify Step 01 Transaction & Ledger Entry
        val txnRes = financialTransactionRepository.getTransactionById(postedExpense.financialTransactionId!!, UserRole.ACCOUNTS)
        assertTrue(txnRes is DomainResult.Success)
        val txn = (txnRes as DomainResult.Success).data

        assertEquals(FinancialTransactionType.EXPENSE, txn.transactionType)
        assertEquals(FinancialEntryType.DEBIT, txn.entryType)
        assertEquals(FinancialReferenceType.EXPENSE, txn.referenceType)
        assertEquals(draftExpense.expenseId, txn.referenceId)
        assertEquals(Money(BigDecimal("18500.00")), txn.amount)

        val ledgerEntriesRes = financialTransactionRepository.getLedgerEntriesByTransaction(txn.transactionId, UserRole.ACCOUNTS)
        assertTrue(ledgerEntriesRes is DomainResult.Success)
        val ledgerEntries = (ledgerEntriesRes as DomainResult.Success).data
        assertEquals(1, ledgerEntries.size)
        assertEquals("ELECTRICITY_EXPENSE", ledgerEntries[0].accountHead)

        // Verify summary
        val summaryRes = expenseRepository.getExpenseSummary("PRJ-01", null, null, UserRole.ACCOUNTS)
        assertTrue(summaryRes is DomainResult.Success)
        val summary = (summaryRes as DomainResult.Success).data

        assertEquals(Money(BigDecimal("18500.00")), summary.totalExpenses)
        assertEquals(Money(BigDecimal("18500.00")), summary.postedExpenses)
        assertEquals(Money.ZERO, summary.pendingExpenses)
        assertEquals(1, summary.totalCount)
        assertEquals(1, summary.postedCount)
    }
}

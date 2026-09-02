package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.datasource.FakeExpenseCategoryDataSource
import com.sucharu.sucharupro.data.datasource.FakeExpenseDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.data.repository.ExpenseCategoryRepositoryImpl
import com.sucharu.sucharupro.data.repository.ExpenseRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialAdjustmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.ExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialAdjustmentExpenseBoundaryTest {

    private lateinit var expenseDataSource: FakeExpenseDataSource
    private lateinit var categoryDataSource: FakeExpenseCategoryDataSource
    private lateinit var adjustmentDataSource: FakeFinancialAdjustmentDataSource
    private lateinit var financialTransactionDataSource: FakeFinancialTransactionDataSource
    private lateinit var receivableDataSource: FakeCustomerReceivableDataSource
    private lateinit var payableDataSource: FakeVendorPayableDataSource

    private lateinit var financialTransactionRepository: FinancialTransactionRepository
    private lateinit var customerReceivableRepository: CustomerReceivableRepository
    private lateinit var vendorPayableRepository: VendorPayableRepository
    private lateinit var categoryRepository: ExpenseCategoryRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var adjustmentRepository: FinancialAdjustmentRepository

    @Before
    fun setUp() {
        expenseDataSource = FakeExpenseDataSource()
        categoryDataSource = FakeExpenseCategoryDataSource()
        adjustmentDataSource = FakeFinancialAdjustmentDataSource()
        financialTransactionDataSource = FakeFinancialTransactionDataSource()
        receivableDataSource = FakeCustomerReceivableDataSource()
        payableDataSource = FakeVendorPayableDataSource()

        financialTransactionRepository = FinancialTransactionRepositoryImpl(financialTransactionDataSource)
        customerReceivableRepository = CustomerReceivableRepositoryImpl(receivableDataSource)
        vendorPayableRepository = VendorPayableRepositoryImpl(payableDataSource)

        categoryRepository = ExpenseCategoryRepositoryImpl(categoryDataSource)
        expenseRepository = ExpenseRepositoryImpl(
            expenseDataSource,
            categoryDataSource,
            financialTransactionRepository
        )
        adjustmentRepository = FinancialAdjustmentRepositoryImpl(
            adjustmentDataSource,
            financialTransactionRepository,
            customerReceivableRepository,
            vendorPayableRepository
        )
    }

    @Test
    fun `financial adjustments do not modify expense entries`() = runBlocking {
        val projectId = "PRJ-01"

        val catRes = categoryRepository.createCategory(
            projectId = projectId,
            categoryCode = "UTIL",
            categoryName = "Office Utilities",
            accountHead = "UTILITY_EXPENSE",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val catId = (catRes as DomainResult.Success).data.categoryId

        val expRes = expenseRepository.createExpense(
            projectId = projectId,
            categoryId = catId,
            description = "Monthly Power Bill",
            amount = Money(BigDecimal("4500.00")),
            paymentMethod = ExpensePaymentMethod.CASH,
            expenseDate = System.currentTimeMillis(),
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val expenseId = (expRes as DomainResult.Success).data.expenseId
        expenseRepository.postExpense(expenseId, "CASH_IN_HAND", "acct-2", UserRole.ACCOUNTS)

        val initialExpenses = expenseDataSource.observeExpenses(projectId).first().size

        val adj = (adjustmentRepository.createAdjustment(
            projectId = projectId,
            adjustmentType = FinancialAdjustmentType.CUSTOMER_CREDIT_NOTE,
            amount = Money(BigDecimal("1500.00")),
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-1001",
            reasonCode = "DAMAGE",
            reason = "Damaged packaging",
            description = "Boundary test",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        adjustmentRepository.postAdjustment(adj.adjustmentId, "SALES_RETURN", "acct-2", UserRole.ACCOUNTS)

        val postExpenses = expenseDataSource.observeExpenses(projectId).first().size
        assertEquals(initialExpenses, postExpenses)
    }
}

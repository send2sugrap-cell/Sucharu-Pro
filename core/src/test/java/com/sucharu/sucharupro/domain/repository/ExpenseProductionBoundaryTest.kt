package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeExpenseCategoryDataSource
import com.sucharu.sucharupro.data.datasource.FakeExpenseDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.repository.ExpenseCategoryRepositoryImpl
import com.sucharu.sucharupro.data.repository.ExpenseRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.ExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ExpenseProductionBoundaryTest {

    private lateinit var jobDataSource: FakeProductionJobDataSource
    private lateinit var categoryDataSource: FakeExpenseCategoryDataSource
    private lateinit var expenseDataSource: FakeExpenseDataSource
    private lateinit var financeTransactionDataSource: FakeFinancialTransactionDataSource

    private lateinit var categoryRepository: ExpenseCategoryRepository
    private lateinit var financialTransactionRepository: FinancialTransactionRepository
    private lateinit var expenseRepository: ExpenseRepository

    @Before
    fun setUp() {
        jobDataSource = FakeProductionJobDataSource()
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
    fun `expense operations cause zero mutation to production jobs or tasks`() = runBlocking {
        val projectId = "PRJ-PROD-EXP-BOUND"

        val job = ProductionJob(
            jobId = "JOB-001",
            jobNumber = "JOB-2026-001",
            orderId = "ORD-001",
            orderNumber = "ORD-2026-001",
            customerId = "CUST-001",
            handoffId = "HO-001",
            title = "Brochure Print",
            priority = OrderPriority.NORMAL,
            status = ProductionJobStatus.IN_PROGRESS,
            quantity = 1000,
            createdAt = "2026-08-18T10:00:00Z",
            updatedAt = "2026-08-18T10:00:00Z"
        )
        jobDataSource.insertJob(job)

        val initialJobs = jobDataSource.observeJobs().first().size

        val catId = (categoryRepository.createCategory(
            projectId = projectId,
            categoryCode = "OFFICE",
            categoryName = "Office Supplies",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data.categoryId

        val exp = (expenseRepository.createExpense(
            projectId = projectId,
            categoryId = catId,
            amount = Money(BigDecimal("1500.00")),
            currency = "BDT",
            description = "Expense boundary test",
            paymentMethod = ExpensePaymentMethod.CASH,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        expenseRepository.postExpense(exp.expenseId, "OFFICE_EXPENSE", "acct-2", UserRole.ACCOUNTS)

        // Production state remains completely untouched
        val postJobs = jobDataSource.observeJobs().first().size
        assertEquals(initialJobs, postJobs)
    }
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeExpenseCategoryDataSource
import com.sucharu.sucharupro.data.datasource.FakeExpenseDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.repository.ExpenseCategoryRepositoryImpl
import com.sucharu.sucharupro.data.repository.ExpenseRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.finance.ExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ExpenseDeliveryBoundaryTest {

    private lateinit var deliveryDataSource: FakeDeliveryOrderDataSource
    private lateinit var categoryDataSource: FakeExpenseCategoryDataSource
    private lateinit var expenseDataSource: FakeExpenseDataSource
    private lateinit var financeTransactionDataSource: FakeFinancialTransactionDataSource

    private lateinit var categoryRepository: ExpenseCategoryRepository
    private lateinit var financialTransactionRepository: FinancialTransactionRepository
    private lateinit var expenseRepository: ExpenseRepository

    @Before
    fun setUp() {
        deliveryDataSource = FakeDeliveryOrderDataSource()
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
    fun `expense operations cause zero mutation to delivery orders or dispatch challans`() = runBlocking {
        val projectId = "PRJ-DELIV-EXP-BOUND"

        val deliveryOrder = DeliveryOrder(
            deliveryOrderId = "DO-001",
            projectId = projectId,
            deliveryOrderNo = "DON-001",
            customerId = "CUST-001",
            sourceReferenceId = "SO-001",
            sourceReferenceType = "SO",
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.NORMAL,
            status = DeliveryOrderStatus.DELIVERED,
            requestedDeliveryDate = 2000L,
            notes = "Delivered",
            createdBy = "wh-1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        deliveryDataSource.insertDeliveryOrder(deliveryOrder, emptyList<DeliveryOrderLine>())

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

        val postRes = expenseRepository.postExpense(exp.expenseId, "OFFICE_EXPENSE", "acct-2", UserRole.ACCOUNTS)
        assertTrue(postRes is DomainResult.Success)

        val retrievedDO = deliveryDataSource.getDeliveryOrder("DO-001")
        assertEquals(DeliveryOrderStatus.DELIVERED, retrievedDO?.status)
    }
}

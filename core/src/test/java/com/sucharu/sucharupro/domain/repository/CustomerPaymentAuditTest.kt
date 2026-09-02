package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.repository.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentActivityType
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerPaymentAuditTest {

    private lateinit var paymentDataSource: FakeCustomerPaymentDataSource
    private lateinit var receivableDataSource: FakeCustomerReceivableDataSource
    private lateinit var financeTransactionDataSource: FakeFinancialTransactionDataSource

    private lateinit var receivableRepository: CustomerReceivableRepository
    private lateinit var financialTransactionRepository: FinancialTransactionRepository
    private lateinit var paymentRepository: CustomerPaymentRepository

    @Before
    fun setUp() {
        paymentDataSource = FakeCustomerPaymentDataSource()
        receivableDataSource = FakeCustomerReceivableDataSource()
        financeTransactionDataSource = FakeFinancialTransactionDataSource()

        receivableRepository = CustomerReceivableRepositoryImpl(receivableDataSource)
        financialTransactionRepository = FinancialTransactionRepositoryImpl(financeTransactionDataSource)
        paymentRepository = CustomerPaymentRepositoryImpl(
            paymentDataSource,
            receivableRepository,
            financialTransactionRepository
        )
    }

    @Test
    fun `payment lifecycle emits chronological immutable audit events`() = runBlocking {
        val recRes = receivableRepository.createReceivable(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-AUDIT",
            originalAmount = Money(BigDecimal("15000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Audit test invoice",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val recId = (recRes as DomainResult.Success).data.receivableId

        val payRes = paymentRepository.createPayment(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            receivableId = recId,
            amount = Money(BigDecimal("15000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.CASH,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        val payId = (payRes as DomainResult.Success).data.paymentId

        paymentRepository.updateDraftPayment(payId, notes = "Updated notes for audit", actorId = "staff-1", callerRole = UserRole.STAFF)
        paymentRepository.submitPayment(payId, "staff-1", UserRole.STAFF)
        paymentRepository.postPayment(payId, "CASH_IN_HAND", "acct-1", UserRole.ACCOUNTS)

        val eventsRes = paymentRepository.getActivityEvents(payId, UserRole.ACCOUNTS)
        assertTrue(eventsRes is DomainResult.Success)
        val events = (eventsRes as DomainResult.Success).data

        assertEquals(5, events.size)
        assertEquals(CustomerPaymentActivityType.PAYMENT_CREATED, events[0].activityType)
        assertEquals(CustomerPaymentActivityType.PAYMENT_UPDATED, events[1].activityType)
        assertEquals(CustomerPaymentActivityType.PAYMENT_SUBMITTED, events[2].activityType)
        assertEquals(CustomerPaymentActivityType.PAYMENT_POSTED, events[3].activityType)
        assertEquals(CustomerPaymentActivityType.RECEIPT_ISSUED, events[4].activityType)
    }
}

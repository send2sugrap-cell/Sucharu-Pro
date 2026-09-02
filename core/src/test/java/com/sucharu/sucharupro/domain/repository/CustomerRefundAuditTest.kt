package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerRefundDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.repository.CustomerRefundRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerRefundMethod
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentActivityType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerRefundAuditTest {

    private lateinit var refundDataSource: FakeCustomerRefundDataSource
    private lateinit var transactionDataSource: FakeFinancialTransactionDataSource
    private lateinit var transactionRepository: FinancialTransactionRepository
    private lateinit var refundRepository: CustomerRefundRepository

    @Before
    fun setUp() {
        refundDataSource = FakeCustomerRefundDataSource()
        transactionDataSource = FakeFinancialTransactionDataSource()
        transactionRepository = FinancialTransactionRepositoryImpl(transactionDataSource)
        refundRepository = CustomerRefundRepositoryImpl(refundDataSource, transactionRepository)
    }

    @Test
    fun `refund lifecycle records chronological audit events`() = runBlocking {
        val refund = (refundRepository.createRefund(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            amount = Money(BigDecimal("1500.00")),
            refundMethod = CustomerRefundMethod.CASH,
            reason = "Audit test refund",
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        ) as DomainResult.Success).data

        refundRepository.submitRefund(refund.refundId, "staff-1", UserRole.STAFF)
        refundRepository.approveRefund(refund.refundId, "mgr-1", UserRole.MANAGER)
        refundRepository.postRefund(refund.refundId, null, "acct-1", UserRole.ACCOUNTS)

        val eventsRes = refundRepository.getActivityEvents(refund.refundId, UserRole.ACCOUNTS)
        assertTrue(eventsRes is DomainResult.Success)
        val events = (eventsRes as DomainResult.Success).data

        assertTrue(events.size >= 4)
        assertEquals(FinancialAdjustmentActivityType.REFUND_CREATED, events[0].activityType)
        assertEquals(FinancialAdjustmentActivityType.REFUND_SUBMITTED, events[1].activityType)
        assertEquals(FinancialAdjustmentActivityType.REFUND_APPROVED, events[2].activityType)
        assertEquals(FinancialAdjustmentActivityType.REFUND_POSTED, events[3].activityType)
    }
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerRefundDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.repository.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRefundRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.CustomerRefundMethod
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerRefundPaymentIntegrationTest {

    private lateinit var paymentDataSource: FakeCustomerPaymentDataSource
    private lateinit var receivableDataSource: FakeCustomerReceivableDataSource
    private lateinit var refundDataSource: FakeCustomerRefundDataSource
    private lateinit var transactionDataSource: FakeFinancialTransactionDataSource

    private lateinit var transactionRepository: FinancialTransactionRepository
    private lateinit var receivableRepository: CustomerReceivableRepository
    private lateinit var paymentRepository: CustomerPaymentRepository
    private lateinit var refundRepository: CustomerRefundRepository

    @Before
    fun setUp() {
        paymentDataSource = FakeCustomerPaymentDataSource()
        receivableDataSource = FakeCustomerReceivableDataSource()
        refundDataSource = FakeCustomerRefundDataSource()
        transactionDataSource = FakeFinancialTransactionDataSource()

        transactionRepository = FinancialTransactionRepositoryImpl(transactionDataSource)
        receivableRepository = CustomerReceivableRepositoryImpl(receivableDataSource)
        paymentRepository = CustomerPaymentRepositoryImpl(paymentDataSource, receivableRepository, transactionRepository)
        refundRepository = CustomerRefundRepositoryImpl(refundDataSource, transactionRepository)
    }

    @Test
    fun `refund issued for overpayment links back to source payment ID`() = runBlocking {
        val projectId = "PRJ-01"

        val recRes = receivableRepository.createReceivable(
            projectId = projectId,
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-1001",
            originalAmount = Money(BigDecimal("10000.00")),
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Receivable for test",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val receivableId = (recRes as DomainResult.Success).data.receivableId

        val payRes = paymentRepository.createPayment(
            projectId = projectId,
            customerId = "CUST-001",
            receivableId = receivableId,
            amount = Money(BigDecimal("10000.00")),
            paymentMethod = CustomerPaymentMethod.CASH,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val paymentId = (payRes as DomainResult.Success).data.paymentId

        val refundRes = refundRepository.createRefund(
            projectId = projectId,
            customerId = "CUST-001",
            amount = Money(BigDecimal("2000.00")),
            refundMethod = CustomerRefundMethod.CASH,
            reason = "Excess overpayment refund",
            sourcePaymentId = paymentId,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(refundRes is DomainResult.Success)
        val refund = (refundRes as DomainResult.Success).data

        assertEquals(paymentId, refund.sourcePaymentId)
        assertEquals(FinancialAdjustmentStatus.PENDING, refund.status)
    }
}

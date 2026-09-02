package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerRefundDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRefundRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
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

class CustomerRefundReceivableIntegrationTest {

    private lateinit var receivableDataSource: FakeCustomerReceivableDataSource
    private lateinit var refundDataSource: FakeCustomerRefundDataSource
    private lateinit var transactionDataSource: FakeFinancialTransactionDataSource

    private lateinit var transactionRepository: FinancialTransactionRepository
    private lateinit var receivableRepository: CustomerReceivableRepository
    private lateinit var refundRepository: CustomerRefundRepository

    @Before
    fun setUp() {
        receivableDataSource = FakeCustomerReceivableDataSource()
        refundDataSource = FakeCustomerRefundDataSource()
        transactionDataSource = FakeFinancialTransactionDataSource()

        transactionRepository = FinancialTransactionRepositoryImpl(transactionDataSource)
        receivableRepository = CustomerReceivableRepositoryImpl(receivableDataSource)
        refundRepository = CustomerRefundRepositoryImpl(refundDataSource, transactionRepository)
    }

    @Test
    fun `refund created referencing customer receivable maintains reference integrity`() = runBlocking {
        val projectId = "PRJ-01"

        val recRes = receivableRepository.createReceivable(
            projectId = projectId,
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-9900",
            originalAmount = Money(BigDecimal("10000.00")),
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Receivable for refund reference test",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val receivableId = (recRes as DomainResult.Success).data.receivableId

        val refundRes = refundRepository.createRefund(
            projectId = projectId,
            customerId = "CUST-001",
            amount = Money(BigDecimal("2500.00")),
            refundMethod = CustomerRefundMethod.CASH,
            reason = "Partial invoice cancellation refund",
            receivableId = receivableId,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(refundRes is DomainResult.Success)
        val refund = (refundRes as DomainResult.Success).data

        assertEquals(receivableId, refund.receivableId)
        assertEquals(FinancialAdjustmentStatus.PENDING, refund.status)
    }
}

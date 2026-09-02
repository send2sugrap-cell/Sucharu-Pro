package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerRefundDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.repository.CustomerRefundRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerRefundMethod
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerRefundDuplicateTest {

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
    fun `distinct customer refunds generate distinct refund records and numbers`() = runBlocking {
        val r1 = (refundRepository.createRefund(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            amount = Money(BigDecimal("1000.00")),
            refundMethod = CustomerRefundMethod.CASH,
            reason = "Reason 1",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        val r2 = (refundRepository.createRefund(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            amount = Money(BigDecimal("2000.00")),
            refundMethod = CustomerRefundMethod.CASH,
            reason = "Reason 2",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        assertNotEquals(r1.refundId, r2.refundId)
        assertNotEquals(r1.refundNo, r2.refundNo)
    }
}

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerRefundValidationTest {

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
    fun `valid customer refund payload passes validation`() = runBlocking {
        val res = refundRepository.createRefund(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            amount = Money(BigDecimal("5000.00")),
            refundMethod = CustomerRefundMethod.CASH,
            reason = "Excess payment return",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `zero or negative refund amount is rejected`() = runBlocking {
        val zeroRes = refundRepository.createRefund(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            amount = Money.ZERO,
            refundMethod = CustomerRefundMethod.CASH,
            reason = "Zero amount",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(zeroRes is DomainResult.Error)

        val negRes = refundRepository.createRefund(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            amount = Money(BigDecimal("-1000.00")),
            refundMethod = CustomerRefundMethod.CASH,
            reason = "Negative amount",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(negRes is DomainResult.Error)
    }

    @Test
    fun `bank transfer refund without payment reference is rejected`() = runBlocking {
        val res = refundRepository.createRefund(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            amount = Money(BigDecimal("2500.00")),
            refundMethod = CustomerRefundMethod.BANK_TRANSFER,
            refundReference = null,
            reason = "Bank refund without ref",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(res is DomainResult.Error)
    }
}

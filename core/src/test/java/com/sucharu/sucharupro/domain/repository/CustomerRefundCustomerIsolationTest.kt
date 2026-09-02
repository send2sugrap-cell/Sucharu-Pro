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

class CustomerRefundCustomerIsolationTest {

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
    fun `Customer 1 cannot access refunds belonging to Customer 2`() = runBlocking {
        val refund1 = (refundRepository.createRefund(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            amount = Money(BigDecimal("1000.00")),
            refundMethod = CustomerRefundMethod.CASH,
            reason = "Cust 1 refund",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        // Cust 1 accesses own refund -> Success
        val cust1Access = refundRepository.getRefundById(
            refundId = refund1.refundId,
            callerRole = UserRole.CUSTOMER,
            authenticatedCustomerId = "CUST-001"
        )
        assertTrue(cust1Access is DomainResult.Success)

        // Cust 2 attempts access to Cust 1 refund -> Blocked
        val cust2Access = refundRepository.getRefundById(
            refundId = refund1.refundId,
            callerRole = UserRole.CUSTOMER,
            authenticatedCustomerId = "CUST-002"
        )
        assertTrue(cust2Access is DomainResult.Error)
    }
}

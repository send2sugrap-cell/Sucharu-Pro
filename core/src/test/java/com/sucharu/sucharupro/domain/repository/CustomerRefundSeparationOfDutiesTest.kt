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

class CustomerRefundSeparationOfDutiesTest {

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
    fun `creator of refund cannot approve or post it unless role is ADMIN`() = runBlocking {
        val refund = (refundRepository.createRefund(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            amount = Money(BigDecimal("2500.00")),
            refundMethod = CustomerRefundMethod.CASH,
            reason = "SOD test",
            actorId = "creator-acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        val selfApprove = refundRepository.approveRefund(refund.refundId, "creator-acct-1", UserRole.ACCOUNTS)
        assertTrue(selfApprove is DomainResult.Error)

        val selfPost = refundRepository.postRefund(refund.refundId, null, "creator-acct-1", UserRole.ACCOUNTS)
        assertTrue(selfPost is DomainResult.Error)

        val adminPost = refundRepository.postRefund(refund.refundId, null, "admin-1", UserRole.ADMIN)
        assertTrue(adminPost is DomainResult.Success)
    }
}

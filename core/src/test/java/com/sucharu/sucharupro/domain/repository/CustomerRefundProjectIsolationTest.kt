package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerRefundDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.repository.CustomerRefundRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerRefundMethod
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerRefundProjectIsolationTest {

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
    fun `refunds in Project A are strictly isolated from Project B`() = runBlocking {
        refundRepository.createRefund(
            projectId = "PRJ-A",
            customerId = "CUST-001",
            amount = Money(BigDecimal("1000.00")),
            refundMethod = CustomerRefundMethod.CASH,
            reason = "Project A Refund",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )

        refundRepository.createRefund(
            projectId = "PRJ-B",
            customerId = "CUST-001",
            amount = Money(BigDecimal("2000.00")),
            refundMethod = CustomerRefundMethod.CASH,
            reason = "Project B Refund",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )

        val listA = refundRepository.observeRefunds("PRJ-A", UserRole.ACCOUNTS).first()
        val listB = refundRepository.observeRefunds("PRJ-B", UserRole.ACCOUNTS).first()

        assertEquals(1, listA.size)
        assertEquals("PRJ-A", listA[0].projectId)

        assertEquals(1, listB.size)
        assertEquals("PRJ-B", listB[0].projectId)
    }
}

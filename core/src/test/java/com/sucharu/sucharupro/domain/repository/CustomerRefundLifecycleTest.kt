package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerRefundDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.repository.CustomerRefundRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerRefundMethod
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerRefundLifecycleTest {

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
    fun `full lifecycle transitions from draft to submitted to approved to posted`() = runBlocking {
        val createRes = refundRepository.createRefund(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            amount = Money(BigDecimal("3000.00")),
            refundMethod = CustomerRefundMethod.CASH,
            reason = "Overpayment return",
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(createRes is DomainResult.Success)
        val refundId = (createRes as DomainResult.Success).data.refundId
        assertEquals(FinancialAdjustmentStatus.DRAFT, createRes.data.status)

        val submitRes = refundRepository.submitRefund(refundId, "staff-1", UserRole.STAFF)
        assertTrue(submitRes is DomainResult.Success)
        assertEquals(FinancialAdjustmentStatus.PENDING, (submitRes as DomainResult.Success).data.status)

        val approveRes = refundRepository.approveRefund(refundId, "mgr-1", UserRole.MANAGER)
        assertTrue(approveRes is DomainResult.Success)
        assertEquals(FinancialAdjustmentStatus.APPROVED, (approveRes as DomainResult.Success).data.status)

        val postRes = refundRepository.postRefund(refundId, "CASH_IN_HAND", "acct-1", UserRole.ACCOUNTS)
        assertTrue(postRes is DomainResult.Success)
        assertEquals(FinancialAdjustmentStatus.POSTED, (postRes as DomainResult.Success).data.status)
    }
}

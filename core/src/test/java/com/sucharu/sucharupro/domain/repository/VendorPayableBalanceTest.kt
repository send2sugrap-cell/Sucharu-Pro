package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.VendorPayableStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPayableBalanceTest {

    private lateinit var dataSource: FakeVendorPayableDataSource
    private lateinit var repository: VendorPayableRepository

    @Before
    fun setUp() {
        dataSource = FakeVendorPayableDataSource()
        repository = VendorPayableRepositoryImpl(dataSource)
    }

    @Test
    fun `initial payable balance has zero settled and full outstanding amount`() = runBlocking {
        val res = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE_ORDER,
            referenceId = "PO-BAL-1",
            originalAmount = Money(BigDecimal("100000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Balance test",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(res is DomainResult.Success)
        val payable = (res as DomainResult.Success).data
        assertEquals(Money(BigDecimal("100000.00")), payable.originalAmount)
        assertEquals(Money.ZERO, payable.settledAmount)
        assertEquals(Money(BigDecimal("100000.00")), payable.outstandingAmount)
    }

    @Test
    fun `partial settlement reduces outstanding correctly and transitions to PARTIALLY_SETTLED`() = runBlocking {
        val res = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE_ORDER,
            referenceId = "PO-BAL-2",
            originalAmount = Money(BigDecimal("100000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Balance test 2",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val payableId = (res as DomainResult.Success).data.payableId

        val settleRes = repository.recordSettlement(
            payableId = payableId,
            settlementAmount = Money(BigDecimal("30000.00")),
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(settleRes is DomainResult.Success)
        val updated = (settleRes as DomainResult.Success).data

        assertEquals(Money(BigDecimal("30000.00")), updated.settledAmount)
        assertEquals(Money(BigDecimal("70000.00")), updated.outstandingAmount)
        assertEquals(VendorPayableStatus.PARTIALLY_SETTLED, updated.status)
    }

    @Test
    fun `full settlement reduces outstanding to zero and transitions to SETTLED`() = runBlocking {
        val res = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE_ORDER,
            referenceId = "PO-BAL-3",
            originalAmount = Money(BigDecimal("100000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Balance test 3",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val payableId = (res as DomainResult.Success).data.payableId

        val settleRes = repository.recordSettlement(
            payableId = payableId,
            settlementAmount = Money(BigDecimal("100000.00")),
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(settleRes is DomainResult.Success)
        val updated = (settleRes as DomainResult.Success).data

        assertEquals(Money(BigDecimal("100000.00")), updated.settledAmount)
        assertEquals(Money.ZERO, updated.outstandingAmount)
        assertEquals(VendorPayableStatus.SETTLED, updated.status)
    }

    @Test
    fun `over-settlement exceeding outstanding liability is strictly rejected`() = runBlocking {
        val res = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE_ORDER,
            referenceId = "PO-BAL-4",
            originalAmount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Balance test 4",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val payableId = (res as DomainResult.Success).data.payableId

        val settleRes = repository.recordSettlement(
            payableId = payableId,
            settlementAmount = Money(BigDecimal("60000.00")),
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(settleRes is DomainResult.Error)
    }
}

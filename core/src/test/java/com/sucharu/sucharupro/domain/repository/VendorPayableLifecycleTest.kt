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

class VendorPayableLifecycleTest {

    private lateinit var dataSource: FakeVendorPayableDataSource
    private lateinit var repository: VendorPayableRepository

    @Before
    fun setUp() {
        dataSource = FakeVendorPayableDataSource()
        repository = VendorPayableRepositoryImpl(dataSource)
    }

    @Test
    fun `full standard lifecycle from draft to submit to approve to settlement`() = runBlocking {
        // 1. Staff creates draft
        val draftRes = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-LIFE-1",
            originalAmount = Money(BigDecimal("20000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Lifecycle test",
            notes = null,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(draftRes is DomainResult.Success)
        val payableId = (draftRes as DomainResult.Success).data.payableId
        assertEquals(VendorPayableStatus.DRAFT, draftRes.data.status)

        // 2. Staff submits
        val submitRes = repository.submitPayable(payableId, "staff-1", UserRole.STAFF)
        assertTrue(submitRes is DomainResult.Success)
        assertEquals(VendorPayableStatus.PENDING, (submitRes as DomainResult.Success).data.status)

        // 3. Accounts approves
        val approveRes = repository.approvePayable(payableId, "acct-1", UserRole.ACCOUNTS)
        assertTrue(approveRes is DomainResult.Success)
        assertEquals(VendorPayableStatus.APPROVED, (approveRes as DomainResult.Success).data.status)

        // 4. Accounts records partial settlement
        val partialRes = repository.recordSettlement(payableId, Money(BigDecimal("10000.00")), "acct-1", UserRole.ACCOUNTS)
        assertTrue(partialRes is DomainResult.Success)
        assertEquals(VendorPayableStatus.PARTIALLY_SETTLED, (partialRes as DomainResult.Success).data.status)

        // 5. Accounts records final settlement
        val finalRes = repository.recordSettlement(payableId, Money(BigDecimal("10000.00")), "acct-1", UserRole.ACCOUNTS)
        assertTrue(finalRes is DomainResult.Success)
        assertEquals(VendorPayableStatus.SETTLED, (finalRes as DomainResult.Success).data.status)
    }

    @Test
    fun `cancellation transitions payable to terminal CANCELLED state`() = runBlocking {
        val res = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-LIFE-2",
            originalAmount = Money(BigDecimal("15000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Cancel test",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val payableId = (res as DomainResult.Success).data.payableId

        val cancelRes = repository.cancelPayable(payableId, "Purchase order revoked", "acct-1", UserRole.ACCOUNTS)
        assertTrue(cancelRes is DomainResult.Success)
        val cancelled = (cancelRes as DomainResult.Success).data
        assertEquals(VendorPayableStatus.CANCELLED, cancelled.status)
        assertEquals("Purchase order revoked", cancelled.cancellationReason)

        // Cannot record settlement on cancelled payable
        val settleRes = repository.recordSettlement(payableId, Money(BigDecimal("5000.00")), "acct-1", UserRole.ACCOUNTS)
        assertTrue(settleRes is DomainResult.Error)
    }
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPayableVendorIsolationTest {

    private lateinit var dataSource: FakeVendorPayableDataSource
    private lateinit var repository: VendorPayableRepository

    @Before
    fun setUp() {
        dataSource = FakeVendorPayableDataSource()
        repository = VendorPayableRepositoryImpl(dataSource)
    }

    @Test
    fun `vendors can only access their own payables and cannot inspect other vendor payables`() = runBlocking {
        repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-V1",
            originalAmount = Money(BigDecimal("10000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Vendor 1 Bill",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )

        val v2Res = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-002",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-V2",
            originalAmount = Money(BigDecimal("20000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Vendor 2 Bill",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val v2PayableId = (v2Res as DomainResult.Success).data.payableId

        // Vendor 1 queries their own stream
        val v1Payables = repository.observeVendorPayables(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            callerRole = UserRole.VENDOR,
            authenticatedVendorId = "VEND-001"
        ).first()
        assertEquals(1, v1Payables.size)
        assertEquals("VEND-001", v1Payables[0].vendorId)

        // Vendor 1 attempting to view Vendor 2's payable by direct ID is blocked
        val unauthorizedAccess = repository.getPayableById(
            payableId = v2PayableId,
            callerRole = UserRole.VENDOR,
            authenticatedVendorId = "VEND-001"
        )
        assertTrue(unauthorizedAccess is DomainResult.Error)
    }
}

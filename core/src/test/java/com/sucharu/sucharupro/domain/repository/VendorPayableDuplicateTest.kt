package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPayableDuplicateTest {

    private lateinit var dataSource: FakeVendorPayableDataSource
    private lateinit var repository: VendorPayableRepository

    @Before
    fun setUp() {
        dataSource = FakeVendorPayableDataSource()
        repository = VendorPayableRepositoryImpl(dataSource)
    }

    @Test
    fun `duplicate active payable for same vendor and reference is rejected`() = runBlocking {
        val p1 = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE_ORDER,
            referenceId = "PO-DUP-1",
            originalAmount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "PO Dup test",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(p1 is DomainResult.Success)

        val p2 = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE_ORDER,
            referenceId = "PO-DUP-1",
            originalAmount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "PO Dup attempt",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(p2 is DomainResult.Error)
    }
}

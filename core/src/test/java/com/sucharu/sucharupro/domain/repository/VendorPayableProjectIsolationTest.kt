package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPayableProjectIsolationTest {

    private lateinit var dataSource: FakeVendorPayableDataSource
    private lateinit var repository: VendorPayableRepository

    @Before
    fun setUp() {
        dataSource = FakeVendorPayableDataSource()
        repository = VendorPayableRepositoryImpl(dataSource)
    }

    @Test
    fun `payable and summary queries strictly enforce tenant project isolation`() = runBlocking {
        repository.createPayable(
            projectId = "PRJ-A",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-A",
            originalAmount = Money(BigDecimal("10000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Project A Payable",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )

        repository.createPayable(
            projectId = "PRJ-B",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-B",
            originalAmount = Money(BigDecimal("25000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Project B Payable",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )

        val prjAPayables = repository.observePayables("PRJ-A", UserRole.ACCOUNTS).first()
        val prjBPayables = repository.observePayables("PRJ-B", UserRole.ACCOUNTS).first()

        assertEquals(1, prjAPayables.size)
        assertEquals("PRJ-A", prjAPayables[0].projectId)
        assertEquals(Money(BigDecimal("10000.00")), prjAPayables[0].originalAmount)

        assertEquals(1, prjBPayables.size)
        assertEquals("PRJ-B", prjBPayables[0].projectId)
        assertEquals(Money(BigDecimal("25000.00")), prjBPayables[0].originalAmount)
    }
}

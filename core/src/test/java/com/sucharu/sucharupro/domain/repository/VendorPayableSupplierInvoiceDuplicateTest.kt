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

class VendorPayableSupplierInvoiceDuplicateTest {

    private lateinit var dataSource: FakeVendorPayableDataSource
    private lateinit var repository: VendorPayableRepository

    @Before
    fun setUp() {
        dataSource = FakeVendorPayableDataSource()
        repository = VendorPayableRepositoryImpl(dataSource)
    }

    @Test
    fun `duplicate active supplier invoice number for same vendor is rejected`() = runBlocking {
        val p1 = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-INV-1",
            supplierInvoiceNo = "BILL-9999",
            originalAmount = Money(BigDecimal("30000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Bill test",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(p1 is DomainResult.Success)

        // Different reference ID, but duplicate supplier invoice number for same vendor
        val p2 = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-INV-2",
            supplierInvoiceNo = "BILL-9999",
            originalAmount = Money(BigDecimal("30000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Bill dup attempt",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(p2 is DomainResult.Error)
    }

    @Test
    fun `same invoice number for different vendors is permitted`() = runBlocking {
        val p1 = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-INV-A",
            supplierInvoiceNo = "INV-COMMON-100",
            originalAmount = Money(BigDecimal("30000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Vendor 1 Bill",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(p1 is DomainResult.Success)

        val p2 = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-002",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-INV-B",
            supplierInvoiceNo = "INV-COMMON-100",
            originalAmount = Money(BigDecimal("40000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Vendor 2 Bill",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(p2 is DomainResult.Success)
    }
}

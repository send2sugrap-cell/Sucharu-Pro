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

class VendorPayableValidationTest {

    private lateinit var dataSource: FakeVendorPayableDataSource
    private lateinit var repository: VendorPayableRepository

    @Before
    fun setUp() {
        dataSource = FakeVendorPayableDataSource()
        repository = VendorPayableRepositoryImpl(dataSource)
    }

    @Test
    fun `valid payable creation succeeds`() = runBlocking {
        val res = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE_ORDER,
            referenceId = "PO-1001",
            financialTransactionId = null,
            supplierInvoiceNo = "INV-VEND-1001",
            originalAmount = Money(BigDecimal("45000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Paper supply raw materials",
            notes = "Net 30 terms",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `zero or negative amount payable is rejected`() = runBlocking {
        val zeroRes = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-1002",
            originalAmount = Money.ZERO,
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Zero amount bill",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(zeroRes is DomainResult.Error)

        val negRes = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-1003",
            originalAmount = Money(BigDecimal("-5000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Negative bill",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(negRes is DomainResult.Error)
    }

    @Test
    fun `blank vendor or missing reference is rejected`() = runBlocking {
        val blankVendorRes = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "   ",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-1004",
            originalAmount = Money(BigDecimal("10000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Blank vendor",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(blankVendorRes is DomainResult.Error)

        val blankRefRes = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "  ",
            originalAmount = Money(BigDecimal("10000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Blank ref",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(blankRefRes is DomainResult.Error)
    }

    @Test
    fun `invalid currency code is rejected`() = runBlocking {
        val invalidCurrRes = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-1005",
            originalAmount = Money(BigDecimal("10000.00")),
            currency = "bdt",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Lowercase currency",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(invalidCurrRes is DomainResult.Error)
    }
}

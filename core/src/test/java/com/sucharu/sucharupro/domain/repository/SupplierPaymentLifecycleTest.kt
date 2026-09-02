package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.datasource.FakeSupplierPaymentDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.data.repository.SupplierPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SupplierPaymentLifecycleTest {

    private lateinit var paymentDataSource: FakeSupplierPaymentDataSource
    private lateinit var payableDataSource: FakeVendorPayableDataSource
    private lateinit var financeTransactionDataSource: FakeFinancialTransactionDataSource

    private lateinit var payableRepository: VendorPayableRepository
    private lateinit var financialTransactionRepository: FinancialTransactionRepository
    private lateinit var paymentRepository: SupplierPaymentRepository

    @Before
    fun setUp() {
        paymentDataSource = FakeSupplierPaymentDataSource()
        payableDataSource = FakeVendorPayableDataSource()
        financeTransactionDataSource = FakeFinancialTransactionDataSource()

        payableRepository = VendorPayableRepositoryImpl(payableDataSource)
        financialTransactionRepository = FinancialTransactionRepositoryImpl(financeTransactionDataSource)
        paymentRepository = SupplierPaymentRepositoryImpl(
            paymentDataSource,
            payableRepository,
            financialTransactionRepository
        )
    }

    @Test
    fun `full lifecycle transitions from draft to submit to approve to posted`() = runBlocking {
        val payableRes = payableRepository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-LIFE-1",
            originalAmount = Money(BigDecimal("40000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Lifecycle payable",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val payableId = (payableRes as DomainResult.Success).data.payableId

        // 1. Staff creates draft
        val createRes = paymentRepository.createPayment(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            payableId = payableId,
            amount = Money(BigDecimal("20000.00")),
            currency = "BDT",
            paymentMethod = SupplierPaymentMethod.BANK_TRANSFER,
            paymentReference = "EFT-LIFE-01",
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(createRes is DomainResult.Success)
        val paymentId = (createRes as DomainResult.Success).data.paymentId
        assertEquals(SupplierPaymentStatus.DRAFT, createRes.data.status)

        // 2. Staff submits
        val submitRes = paymentRepository.submitPayment(paymentId, "staff-1", UserRole.STAFF)
        assertTrue(submitRes is DomainResult.Success)
        assertEquals(SupplierPaymentStatus.PENDING, (submitRes as DomainResult.Success).data.status)

        // 3. Manager approves
        val approveRes = paymentRepository.approvePayment(paymentId, "mgr-1", UserRole.MANAGER)
        assertTrue(approveRes is DomainResult.Success)
        assertEquals(SupplierPaymentStatus.APPROVED, (approveRes as DomainResult.Success).data.status)

        // 4. Accounts posts
        val postRes = paymentRepository.postPayment(paymentId, "BANK_ACCOUNT", "acct-1", UserRole.ACCOUNTS)
        assertTrue(postRes is DomainResult.Success)
        assertEquals(SupplierPaymentStatus.POSTED, (postRes as DomainResult.Success).data.status)
    }

    @Test
    fun `rejection transitions payment to terminal REJECTED state`() = runBlocking {
        val payableRes = payableRepository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-LIFE-2",
            originalAmount = Money(BigDecimal("30000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Lifecycle payable",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val payableId = (payableRes as DomainResult.Success).data.payableId

        val createRes = paymentRepository.createPayment(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            payableId = payableId,
            amount = Money(BigDecimal("15000.00")),
            currency = "BDT",
            paymentMethod = SupplierPaymentMethod.CASH,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val paymentId = (createRes as DomainResult.Success).data.paymentId

        val rejectRes = paymentRepository.rejectPayment(paymentId, "Insufficient documentation", "mgr-1", UserRole.MANAGER)
        assertTrue(rejectRes is DomainResult.Success)
        val rejected = (rejectRes as DomainResult.Success).data
        assertEquals(SupplierPaymentStatus.REJECTED, rejected.status)
        assertEquals("Insufficient documentation", rejected.cancellationReason)
    }
}

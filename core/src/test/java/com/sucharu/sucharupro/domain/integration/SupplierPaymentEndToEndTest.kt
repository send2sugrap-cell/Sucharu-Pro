package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.datasource.FakeSupplierPaymentDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.data.repository.SupplierPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentStatus
import com.sucharu.sucharupro.domain.model.finance.VendorPayableStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinancialTransactionRepository
import com.sucharu.sucharupro.domain.repository.SupplierPaymentRepository
import com.sucharu.sucharupro.domain.repository.VendorPayableRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SupplierPaymentEndToEndTest {

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
    fun `complete end-to-end flow from supplier payable to supplier payment posting and ledger recording`() = runBlocking {
        // 1. Create Payable for Supplier Invoice ৳120,000
        val payableRes = payableRepository.createPayable(
            projectId = "PRJ-E2E",
            vendorId = "VEND-PAPER-CORP",
            referenceType = FinancialReferenceType.SUPPLIER_INVOICE,
            referenceId = "INV-2026-999",
            supplierInvoiceNo = "BILL-999",
            originalAmount = Money(BigDecimal("120000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L * 15,
            description = "100 reams art paper supply",
            notes = "Terms net 15",
            actorId = "acct-user-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(payableRes is DomainResult.Success)
        val payable = (payableRes as DomainResult.Success).data
        val payableId = payable.payableId

        // 2. Create Payment 1: Bank Transfer ৳70,000
        val pay1Res = paymentRepository.createPayment(
            projectId = "PRJ-E2E",
            vendorId = "VEND-PAPER-CORP",
            payableId = payableId,
            amount = Money(BigDecimal("70000.00")),
            currency = "BDT",
            paymentMethod = SupplierPaymentMethod.BANK_TRANSFER,
            paymentReference = "EFT-E2E-001",
            notes = "First installment via City Bank EFT",
            actorId = "staff-user-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(pay1Res is DomainResult.Success)
        val payment1 = (pay1Res as DomainResult.Success).data
        assertEquals(SupplierPaymentStatus.DRAFT, payment1.status)

        // Submit & Approve Payment 1
        paymentRepository.submitPayment(payment1.paymentId, "staff-user-1", UserRole.STAFF)
        paymentRepository.approvePayment(payment1.paymentId, "mgr-user-1", UserRole.MANAGER)

        // Post Payment 1
        val post1Res = paymentRepository.postPayment(payment1.paymentId, "BANK_ACCOUNT", "acct-user-2", UserRole.ACCOUNTS)
        assertTrue(post1Res is DomainResult.Success)
        val posted1 = (post1Res as DomainResult.Success).data
        assertEquals(SupplierPaymentStatus.POSTED, posted1.status)

        // Verify Step 04 Payable status: PARTIALLY_SETTLED, ৳50,000 remaining
        var currentPayable = (payableRepository.getPayableById(payableId, UserRole.ACCOUNTS) as DomainResult.Success).data
        assertEquals(Money(BigDecimal("70000.00")), currentPayable.settledAmount)
        assertEquals(Money(BigDecimal("50000.00")), currentPayable.outstandingAmount)
        assertEquals(VendorPayableStatus.PARTIALLY_SETTLED, currentPayable.status)

        // 3. Create Payment 2: Cheque ৳50,000 (Final settlement)
        val pay2Res = paymentRepository.createPayment(
            projectId = "PRJ-E2E",
            vendorId = "VEND-PAPER-CORP",
            payableId = payableId,
            amount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            paymentMethod = SupplierPaymentMethod.CHEQUE,
            paymentReference = "CHQ-E2E-7788",
            notes = "Final clearing cheque",
            actorId = "acct-user-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(pay2Res is DomainResult.Success)
        val payment2 = (pay2Res as DomainResult.Success).data

        // Post Payment 2
        val post2Res = paymentRepository.postPayment(payment2.paymentId, "BANK_ACCOUNT", "acct-user-2", UserRole.ACCOUNTS)
        assertTrue(post2Res is DomainResult.Success)
        val posted2 = (post2Res as DomainResult.Success).data
        assertEquals(SupplierPaymentStatus.POSTED, posted2.status)

        // 4. Verify Final Payable status: SETTLED, ৳0 remaining
        currentPayable = (payableRepository.getPayableById(payableId, UserRole.ACCOUNTS) as DomainResult.Success).data
        assertEquals(Money(BigDecimal("120000.00")), currentPayable.settledAmount)
        assertEquals(Money.ZERO, currentPayable.outstandingAmount)
        assertEquals(VendorPayableStatus.SETTLED, currentPayable.status)

        // 5. Verify Settlements
        val settlementsRes = paymentRepository.getSettlementsByPayable(payableId, UserRole.ACCOUNTS)
        assertTrue(settlementsRes is DomainResult.Success)
        val settlements = (settlementsRes as DomainResult.Success).data
        assertEquals(2, settlements.size)

        // 6. Verify Financial Transactions in Step 01 Canonical Ledger
        val ft1 = (financialTransactionRepository.getTransactionById(posted1.financialTransactionId!!, UserRole.ACCOUNTS) as DomainResult.Success).data
        val ft2 = (financialTransactionRepository.getTransactionById(posted2.financialTransactionId!!, UserRole.ACCOUNTS) as DomainResult.Success).data

        assertEquals(FinancialTransactionType.PAYMENT, ft1.transactionType)
        assertEquals(FinancialEntryType.DEBIT, ft1.entryType)
        assertEquals(FinancialTransactionStatus.POSTED, ft1.transactionStatus)
        assertEquals(Money(BigDecimal("70000.00")), ft1.amount)

        assertEquals(FinancialTransactionType.PAYMENT, ft2.transactionType)
        assertEquals(FinancialEntryType.DEBIT, ft2.entryType)
        assertEquals(FinancialTransactionStatus.POSTED, ft2.transactionStatus)
        assertEquals(Money(BigDecimal("50000.00")), ft2.amount)
    }
}

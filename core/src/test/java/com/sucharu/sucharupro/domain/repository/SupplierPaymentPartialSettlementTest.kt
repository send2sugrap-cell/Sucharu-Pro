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
import com.sucharu.sucharupro.domain.model.finance.VendorPayableStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SupplierPaymentPartialSettlementTest {

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
    fun `multiple partial payments sequentially reduce payable balance`() = runBlocking {
        val payableRes = payableRepository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-PARTIAL-1",
            originalAmount = Money(BigDecimal("100000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Partial payable",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val payableId = (payableRes as DomainResult.Success).data.payableId

        // Payment 1: 30,000
        val p1 = (paymentRepository.createPayment(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            payableId = payableId,
            amount = Money(BigDecimal("30000.00")),
            paymentMethod = SupplierPaymentMethod.BANK_TRANSFER,
            paymentReference = "EFT-P1",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data.paymentId

        paymentRepository.postPayment(p1, "BANK_ACCOUNT", "acct-2", UserRole.ACCOUNTS)

        var payable = (payableRepository.getPayableById(payableId, UserRole.ACCOUNTS) as DomainResult.Success).data
        assertEquals(Money(BigDecimal("30000.00")), payable.settledAmount)
        assertEquals(Money(BigDecimal("70000.00")), payable.outstandingAmount)
        assertEquals(VendorPayableStatus.PARTIALLY_SETTLED, payable.status)

        // Payment 2: 20,000
        val p2 = (paymentRepository.createPayment(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            payableId = payableId,
            amount = Money(BigDecimal("20000.00")),
            paymentMethod = SupplierPaymentMethod.MOBILE_BANKING,
            paymentReference = "BKASH-P2",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data.paymentId

        paymentRepository.postPayment(p2, "MOBILE_WALLET", "acct-2", UserRole.ACCOUNTS)

        payable = (payableRepository.getPayableById(payableId, UserRole.ACCOUNTS) as DomainResult.Success).data
        assertEquals(Money(BigDecimal("50000.00")), payable.settledAmount)
        assertEquals(Money(BigDecimal("50000.00")), payable.outstandingAmount)
        assertEquals(VendorPayableStatus.PARTIALLY_SETTLED, payable.status)
    }
}

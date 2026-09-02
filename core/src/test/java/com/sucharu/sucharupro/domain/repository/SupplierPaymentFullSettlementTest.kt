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

class SupplierPaymentFullSettlementTest {

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
    fun `full settlement transitions payable to SETTLED status`() = runBlocking {
        val payableRes = payableRepository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-FULL-1",
            originalAmount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Full settle payable",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val payableId = (payableRes as DomainResult.Success).data.payableId

        val payRes = paymentRepository.createPayment(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            payableId = payableId,
            amount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            paymentMethod = SupplierPaymentMethod.CHEQUE,
            paymentReference = "CHQ-998877",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val paymentId = (payRes as DomainResult.Success).data.paymentId

        val postRes = paymentRepository.postPayment(paymentId, "BANK_ACCOUNT", "acct-2", UserRole.ACCOUNTS)
        assertTrue(postRes is DomainResult.Success)

        val updatedPayable = (payableRepository.getPayableById(payableId, UserRole.ACCOUNTS) as DomainResult.Success).data
        assertEquals(Money(BigDecimal("50000.00")), updatedPayable.settledAmount)
        assertEquals(Money.ZERO, updatedPayable.outstandingAmount)
        assertEquals(VendorPayableStatus.SETTLED, updatedPayable.status)
    }
}

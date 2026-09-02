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

class SupplierPaymentCancellationTest {

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
    fun `payment can be cancelled before posting and terminal status is reached`() = runBlocking {
        val payableRes = payableRepository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-CANCEL-1",
            originalAmount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Cancel payable",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val payableId = (payableRes as DomainResult.Success).data.payableId

        val payRes = paymentRepository.createPayment(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            payableId = payableId,
            amount = Money(BigDecimal("20000.00")),
            currency = "BDT",
            paymentMethod = SupplierPaymentMethod.CASH,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val paymentId = (payRes as DomainResult.Success).data.paymentId

        val cancelRes = paymentRepository.cancelPayment(paymentId, "Supplier requested cheque instead", "acct-1", UserRole.ACCOUNTS)
        assertTrue(cancelRes is DomainResult.Success)
        val cancelled = (cancelRes as DomainResult.Success).data
        assertEquals(SupplierPaymentStatus.CANCELLED, cancelled.status)
        assertEquals("Supplier requested cheque instead", cancelled.cancellationReason)
    }
}

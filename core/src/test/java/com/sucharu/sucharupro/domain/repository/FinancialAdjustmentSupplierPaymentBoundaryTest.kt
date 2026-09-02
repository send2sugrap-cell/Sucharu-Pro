package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.datasource.FakeSupplierPaymentDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialAdjustmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.data.repository.SupplierPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentMethod
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialAdjustmentSupplierPaymentBoundaryTest {

    private lateinit var supplierPaymentDataSource: FakeSupplierPaymentDataSource
    private lateinit var adjustmentDataSource: FakeFinancialAdjustmentDataSource
    private lateinit var financialTransactionDataSource: FakeFinancialTransactionDataSource
    private lateinit var receivableDataSource: FakeCustomerReceivableDataSource
    private lateinit var payableDataSource: FakeVendorPayableDataSource

    private lateinit var financialTransactionRepository: FinancialTransactionRepository
    private lateinit var customerReceivableRepository: CustomerReceivableRepository
    private lateinit var vendorPayableRepository: VendorPayableRepository
    private lateinit var supplierPaymentRepository: SupplierPaymentRepository
    private lateinit var adjustmentRepository: FinancialAdjustmentRepository

    @Before
    fun setUp() {
        supplierPaymentDataSource = FakeSupplierPaymentDataSource()
        adjustmentDataSource = FakeFinancialAdjustmentDataSource()
        financialTransactionDataSource = FakeFinancialTransactionDataSource()
        receivableDataSource = FakeCustomerReceivableDataSource()
        payableDataSource = FakeVendorPayableDataSource()

        financialTransactionRepository = FinancialTransactionRepositoryImpl(financialTransactionDataSource)
        customerReceivableRepository = CustomerReceivableRepositoryImpl(receivableDataSource)
        vendorPayableRepository = VendorPayableRepositoryImpl(payableDataSource)

        supplierPaymentRepository = SupplierPaymentRepositoryImpl(
            supplierPaymentDataSource,
            vendorPayableRepository,
            financialTransactionRepository
        )
        adjustmentRepository = FinancialAdjustmentRepositoryImpl(
            adjustmentDataSource,
            financialTransactionRepository,
            customerReceivableRepository,
            vendorPayableRepository
        )
    }

    @Test
    fun `financial adjustments do not mutate supplier payment records`() = runBlocking {
        val projectId = "PRJ-01"

        val payabRes = vendorPayableRepository.createPayable(
            projectId = projectId,
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.VENDOR_BILL,
            referenceId = "BILL-2001",
            originalAmount = Money(BigDecimal("15000.00")),
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Payable for test",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val payableId = (payabRes as DomainResult.Success).data.payableId

        val payRes = supplierPaymentRepository.createPayment(
            projectId = projectId,
            vendorId = "VEND-001",
            payableId = payableId,
            amount = Money(BigDecimal("12000.00")),
            paymentMethod = SupplierPaymentMethod.BANK_TRANSFER,
            paymentReference = "TRX-SUPP-1",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val paymentId = (payRes as DomainResult.Success).data.paymentId
        supplierPaymentRepository.postPayment(paymentId, "BANK_ACCOUNT", "acct-2", UserRole.ACCOUNTS)

        val initialPayments = supplierPaymentDataSource.observePayments(projectId).first().size

        val adj = (adjustmentRepository.createAdjustment(
            projectId = projectId,
            adjustmentType = FinancialAdjustmentType.VENDOR_BALANCE_ADJUSTMENT,
            amount = Money(BigDecimal("2000.00")),
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.SUPPLIER_PAYMENT,
            referenceId = paymentId,
            reasonCode = "CORRECTION",
            reason = "Correction",
            description = "Adjustment against supplier payment",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        adjustmentRepository.postAdjustment(adj.adjustmentId, null, "acct-2", UserRole.ACCOUNTS)

        val postPayments = supplierPaymentDataSource.observePayments(projectId).first().size
        assertEquals(initialPayments, postPayments)
    }
}

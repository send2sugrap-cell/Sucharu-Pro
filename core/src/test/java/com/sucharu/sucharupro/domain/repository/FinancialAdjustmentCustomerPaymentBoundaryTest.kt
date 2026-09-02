package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialAdjustmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialAdjustmentCustomerPaymentBoundaryTest {

    private lateinit var paymentDataSource: FakeCustomerPaymentDataSource
    private lateinit var adjustmentDataSource: FakeFinancialAdjustmentDataSource
    private lateinit var financialTransactionDataSource: FakeFinancialTransactionDataSource
    private lateinit var receivableDataSource: FakeCustomerReceivableDataSource
    private lateinit var payableDataSource: FakeVendorPayableDataSource

    private lateinit var financialTransactionRepository: FinancialTransactionRepository
    private lateinit var customerReceivableRepository: CustomerReceivableRepository
    private lateinit var vendorPayableRepository: VendorPayableRepository
    private lateinit var paymentRepository: CustomerPaymentRepository
    private lateinit var adjustmentRepository: FinancialAdjustmentRepository

    @Before
    fun setUp() {
        paymentDataSource = FakeCustomerPaymentDataSource()
        adjustmentDataSource = FakeFinancialAdjustmentDataSource()
        financialTransactionDataSource = FakeFinancialTransactionDataSource()
        receivableDataSource = FakeCustomerReceivableDataSource()
        payableDataSource = FakeVendorPayableDataSource()

        financialTransactionRepository = FinancialTransactionRepositoryImpl(financialTransactionDataSource)
        customerReceivableRepository = CustomerReceivableRepositoryImpl(receivableDataSource)
        vendorPayableRepository = VendorPayableRepositoryImpl(payableDataSource)

        paymentRepository = CustomerPaymentRepositoryImpl(
            paymentDataSource,
            customerReceivableRepository,
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
    fun `financial adjustments do not rewrite posted CustomerPayment records`() = runBlocking {
        val projectId = "PRJ-01"

        val recRes = customerReceivableRepository.createReceivable(
            projectId = projectId,
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-1001",
            originalAmount = Money(BigDecimal("10000.00")),
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Receivable for test",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val receivableId = (recRes as DomainResult.Success).data.receivableId

        val payRes = paymentRepository.createPayment(
            projectId = projectId,
            customerId = "CUST-001",
            receivableId = receivableId,
            amount = Money(BigDecimal("10000.00")),
            paymentMethod = CustomerPaymentMethod.CASH,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val paymentId = (payRes as DomainResult.Success).data.paymentId
        paymentRepository.postPayment(paymentId, "CASH_IN_HAND", "acct-2", UserRole.ACCOUNTS)

        val initialPayments = paymentDataSource.observePayments(projectId).first().size

        // Post adjustment referencing payment
        val adj = (adjustmentRepository.createAdjustment(
            projectId = projectId,
            adjustmentType = FinancialAdjustmentType.CUSTOMER_BALANCE_ADJUSTMENT,
            amount = Money(BigDecimal("2000.00")),
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.PAYMENT,
            referenceId = paymentId,
            reasonCode = "CORRECTION",
            reason = "Correction",
            description = "Adjustment against payment",
            relatedPaymentId = paymentId,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        adjustmentRepository.postAdjustment(adj.adjustmentId, null, "acct-2", UserRole.ACCOUNTS)

        val postPayments = paymentDataSource.observePayments(projectId).first().size
        assertEquals(initialPayments, postPayments)
    }
}

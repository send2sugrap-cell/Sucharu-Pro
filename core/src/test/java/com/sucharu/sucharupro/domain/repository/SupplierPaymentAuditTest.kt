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
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentActivityType
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentMethod
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SupplierPaymentAuditTest {

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
    fun `full lifecycle generates chronological audit events`() = runBlocking {
        val payableRes = payableRepository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-AUDIT-1",
            originalAmount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Audit payable",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val payableId = (payableRes as DomainResult.Success).data.payableId

        val payRes = paymentRepository.createPayment(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            payableId = payableId,
            amount = Money(BigDecimal("25000.00")),
            currency = "BDT",
            paymentMethod = SupplierPaymentMethod.BANK_TRANSFER,
            paymentReference = "EFT-AUDIT-01",
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        val paymentId = (payRes as DomainResult.Success).data.paymentId

        paymentRepository.submitPayment(paymentId, "staff-1", UserRole.STAFF)
        paymentRepository.approvePayment(paymentId, "mgr-1", UserRole.MANAGER)
        paymentRepository.postPayment(paymentId, "BANK_ACCOUNT", "acct-2", UserRole.ACCOUNTS)

        val eventsRes = paymentRepository.getActivityEvents(paymentId, UserRole.ACCOUNTS)
        assertTrue(eventsRes is DomainResult.Success)
        val events = (eventsRes as DomainResult.Success).data

        assertEquals(5, events.size)
        assertEquals(SupplierPaymentActivityType.PAYMENT_CREATED, events[0].activityType)
        assertEquals(SupplierPaymentActivityType.PAYMENT_SUBMITTED, events[1].activityType)
        assertEquals(SupplierPaymentActivityType.PAYMENT_APPROVED, events[2].activityType)
        assertEquals(SupplierPaymentActivityType.PAYMENT_POSTED, events[3].activityType)
        assertEquals(SupplierPaymentActivityType.PAYMENT_SETTLEMENT_RECORDED, events[4].activityType)
    }
}

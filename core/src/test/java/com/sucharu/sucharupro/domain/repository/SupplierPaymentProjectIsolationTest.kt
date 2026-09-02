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
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SupplierPaymentProjectIsolationTest {

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
    fun `payments and settlements in Project A are completely isolated from Project B`() = runBlocking {
        // Project A
        val payableA = (payableRepository.createPayable(
            projectId = "PRJ-A",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-A-1",
            originalAmount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Payable A",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data.payableId

        paymentRepository.createPayment(
            projectId = "PRJ-A",
            vendorId = "VEND-001",
            payableId = payableA,
            amount = Money(BigDecimal("25000.00")),
            currency = "BDT",
            paymentMethod = SupplierPaymentMethod.CASH,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )

        // Project B
        val payableB = (payableRepository.createPayable(
            projectId = "PRJ-B",
            vendorId = "VEND-002",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-B-1",
            originalAmount = Money(BigDecimal("70000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Payable B",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data.payableId

        paymentRepository.createPayment(
            projectId = "PRJ-B",
            vendorId = "VEND-002",
            payableId = payableB,
            amount = Money(BigDecimal("35000.00")),
            currency = "BDT",
            paymentMethod = SupplierPaymentMethod.CASH,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )

        val paymentsA = paymentRepository.observePayments("PRJ-A", UserRole.ACCOUNTS).first()
        val paymentsB = paymentRepository.observePayments("PRJ-B", UserRole.ACCOUNTS).first()

        assertEquals(1, paymentsA.size)
        assertEquals("PRJ-A", paymentsA[0].projectId)

        assertEquals(1, paymentsB.size)
        assertEquals("PRJ-B", paymentsB[0].projectId)
    }
}

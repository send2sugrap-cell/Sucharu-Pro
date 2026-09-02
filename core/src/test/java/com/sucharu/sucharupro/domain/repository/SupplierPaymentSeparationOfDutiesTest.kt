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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SupplierPaymentSeparationOfDutiesTest {

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
    fun `creator cannot approve or post their own payment unless they are ADMIN`() = runBlocking {
        val payableRes = payableRepository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-SOD-1",
            originalAmount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "SOD payable",
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
            paymentMethod = SupplierPaymentMethod.BANK_TRANSFER,
            paymentReference = "EFT-SOD-1",
            actorId = "creator-acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val paymentId = (payRes as DomainResult.Success).data.paymentId

        // Creator attempts to approve as ACCOUNTS -> Fails SOD
        val selfApprove = paymentRepository.approvePayment(paymentId, "creator-acct-1", UserRole.ACCOUNTS)
        assertTrue(selfApprove is DomainResult.Error)

        // Creator attempts to post as ACCOUNTS -> Fails SOD
        val selfPost = paymentRepository.postPayment(paymentId, "BANK_ACCOUNT", "creator-acct-1", UserRole.ACCOUNTS)
        assertTrue(selfPost is DomainResult.Error)

        // ADMIN can post/approve even if creator
        val adminPost = paymentRepository.postPayment(paymentId, "BANK_ACCOUNT", "admin-1", UserRole.ADMIN)
        assertTrue(adminPost is DomainResult.Success)
    }
}

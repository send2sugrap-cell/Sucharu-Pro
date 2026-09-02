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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SupplierPaymentConcurrencyTest {

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
    fun `concurrent payment postings never exceed original payable amount or result in negative balance`() = runBlocking {
        val payableRes = payableRepository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-CONC-1",
            originalAmount = Money(BigDecimal("100000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Concurrency payable",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val payableId = (payableRes as DomainResult.Success).data.payableId

        // Create 20 payment drafts of 10,000 each
        val paymentIds = (1..20).map { i ->
            (paymentRepository.createPayment(
                projectId = "PRJ-01",
                vendorId = "VEND-001",
                payableId = payableId,
                amount = Money(BigDecimal("10000.00")),
                currency = "BDT",
                paymentMethod = SupplierPaymentMethod.BANK_TRANSFER,
                paymentReference = "EFT-CONC-$i",
                actorId = "acct-1",
                callerRole = UserRole.ACCOUNTS
            ) as DomainResult.Success).data.paymentId
        }

        // Post all 20 concurrently
        val deferredPosts = paymentIds.map { id ->
            async(Dispatchers.Default) {
                paymentRepository.postPayment(id, "BANK_ACCOUNT", "acct-2", UserRole.ACCOUNTS)
            }
        }

        val results = deferredPosts.awaitAll()
        val successCount = results.count { it is DomainResult.Success }
        val errorCount = results.count { it is DomainResult.Error }

        // At most 10 payments of 10,000 can be accepted for a 100,000 payable
        assertTrue("Success count ($successCount) must be <= 10", successCount <= 10)
        assertTrue("Error count ($errorCount) must be >= 10", errorCount >= 10)

        // Verify final payable outstanding is non-negative and settledAmount <= 100,000
        val finalPayable = (payableRepository.getPayableById(payableId, UserRole.ACCOUNTS) as DomainResult.Success).data
        assertTrue(finalPayable.settledAmount <= Money(BigDecimal("100000.00")))
        assertTrue(!finalPayable.outstandingAmount.isNegative())
    }
}

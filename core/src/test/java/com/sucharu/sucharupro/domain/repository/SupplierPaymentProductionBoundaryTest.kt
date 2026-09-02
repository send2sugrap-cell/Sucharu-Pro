package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.FakeSupplierPaymentDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.data.repository.SupplierPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentMethod
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SupplierPaymentProductionBoundaryTest {

    private lateinit var jobDataSource: FakeProductionJobDataSource
    private lateinit var paymentDataSource: FakeSupplierPaymentDataSource
    private lateinit var payableDataSource: FakeVendorPayableDataSource
    private lateinit var financeTransactionDataSource: FakeFinancialTransactionDataSource

    private lateinit var payableRepository: VendorPayableRepository
    private lateinit var financialTransactionRepository: FinancialTransactionRepository
    private lateinit var paymentRepository: SupplierPaymentRepository

    @Before
    fun setUp() {
        jobDataSource = FakeProductionJobDataSource()
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
    fun `supplier payment posting does not mutate production jobs`() = runBlocking {
        val projectId = "PRJ-PROD-BOUND"

        val job = ProductionJob(
            jobId = "JOB-001",
            jobNumber = "JOB-2026-001",
            orderId = "ORD-001",
            orderNumber = "ORD-2026-001",
            customerId = "CUST-001",
            handoffId = "HO-001",
            title = "Brochure Print",
            priority = OrderPriority.NORMAL,
            status = ProductionJobStatus.IN_PROGRESS,
            quantity = 1000,
            createdAt = "2026-08-18T10:00:00Z",
            updatedAt = "2026-08-18T10:00:00Z"
        )
        jobDataSource.insertJob(job)

        val initialJobs = jobDataSource.observeJobs().first().size

        val payableRes = payableRepository.createPayable(
            projectId = projectId,
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-PROD-1",
            originalAmount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Production boundary payable",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val payableId = (payableRes as DomainResult.Success).data.payableId

        val payRes = paymentRepository.createPayment(
            projectId = projectId,
            vendorId = "VEND-001",
            payableId = payableId,
            amount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            paymentMethod = SupplierPaymentMethod.CASH,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val paymentId = (payRes as DomainResult.Success).data.paymentId

        paymentRepository.postPayment(paymentId, "CASH_IN_HAND", "acct-2", UserRole.ACCOUNTS)

        val postJobs = jobDataSource.observeJobs().first().size
        assertEquals(initialJobs, postJobs)
    }
}

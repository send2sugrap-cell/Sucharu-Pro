package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPayableProductionBoundaryTest {

    private lateinit var jobDataSource: FakeProductionJobDataSource
    private lateinit var payableDataSource: FakeVendorPayableDataSource
    private lateinit var payableRepository: VendorPayableRepository

    @Before
    fun setUp() {
        jobDataSource = FakeProductionJobDataSource()
        payableDataSource = FakeVendorPayableDataSource()
        payableRepository = VendorPayableRepositoryImpl(payableDataSource)
    }

    @Test
    fun `vendor payable operations do not mutate production jobs or work orders`() = runBlocking {
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

        val createRes = payableRepository.createPayable(
            projectId = projectId,
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-PROD-1",
            originalAmount = Money(BigDecimal("12000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Plates purchase",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(createRes is DomainResult.Success)

        val jobsAfter = jobDataSource.observeJobs().first()
        assertEquals(1, jobsAfter.size)
        assertEquals(ProductionJobStatus.IN_PROGRESS, jobsAfter[0].status)
    }
}

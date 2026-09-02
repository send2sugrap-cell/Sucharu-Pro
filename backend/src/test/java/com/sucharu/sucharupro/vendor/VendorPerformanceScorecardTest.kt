package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPerformanceDataSource
import com.sucharu.sucharupro.data.repository.VendorPerformanceRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorPerformanceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

class VendorPerformanceScorecardTest {

    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var repo: VendorPerformanceRepositoryImpl
    private lateinit var service: VendorPerformanceServiceImpl

    @Before
    fun setUp() {
        runBlocking {
            vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
            val ds = FakeVendorPerformanceDataSource()
            repo = VendorPerformanceRepositoryImpl(ds)
            service = VendorPerformanceServiceImpl(
                performanceRepository = repo,
                vendorRepository = vendorRepo
            )

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-01",
                    projectId = "PRJ-01",
                    vendorCode = "V001",
                    vendorName = "Vendor 1",
                    status = VendorStatus.ACTIVE
                )
            )

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-02",
                    projectId = "PRJ-01",
                    vendorCode = "V002",
                    vendorName = "Vendor 2",
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testGenerateAndApproveScorecardWorkflow() = runBlocking {
        val now = Instant.now()
        val pStart = now.minusSeconds(86400 * 30)
        val pEnd = now

        // Create standard KPIs
        service.createKpi(
            VendorPerformanceKpi(
                kpiId = "KPI-OTD",
                projectId = "PRJ-01",
                tenantId = "PRJ-01",
                code = "ON_TIME_DELIVERY",
                name = "On Time Delivery",
                description = "OTD KPI",
                kpiType = KpiType.OPERATIONAL,
                measurementMethod = KpiMeasurementMethod.AUTOMATED,
                targetValue = 95.0,
                unit = "%",
                direction = KpiDirection.HIGHER_IS_BETTER,
                weight = 1.0,
                createdBy = "admin"
            )
        )

        // Generate Scorecard
        val genRes = service.generateScorecard(
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            vendorId = "VND-01",
            periodType = EvaluationPeriodType.MONTHLY,
            periodStart = pStart,
            periodEnd = pEnd,
            generatedBy = "staff_01"
        )
        assertTrue(genRes is DomainResult.Success)
        val scorecard = (genRes as DomainResult.Success).data
        assertEquals(ScorecardStatus.GENERATED, scorecard.status)
        assertEquals("VND-01", scorecard.vendorId)

        // Submit for review
        val submitRes = service.submitScorecardForReview("PRJ-01", scorecard.scorecardId, "staff_01")
        assertTrue(submitRes is DomainResult.Success)
        val submitted = (submitRes as DomainResult.Success).data
        assertEquals(ScorecardStatus.UNDER_REVIEW, submitted.status)

        // Approve
        val approveRes = service.approveScorecard("PRJ-01", scorecard.scorecardId, "manager_01")
        assertTrue(approveRes is DomainResult.Success)
        val approved = (approveRes as DomainResult.Success).data
        assertEquals(ScorecardStatus.APPROVED, approved.status)
        assertEquals("manager_01", approved.approvedBy)

        // Finalize
        val finalizeRes = service.finalizeScorecard("PRJ-01", scorecard.scorecardId, "manager_01")
        assertTrue(finalizeRes is DomainResult.Success)
        val finalized = (finalizeRes as DomainResult.Success).data
        assertEquals(ScorecardStatus.FINALIZED, finalized.status)

        // Verify immutability after finalization
        val reApproveRes = service.approveScorecard("PRJ-01", scorecard.scorecardId, "manager_02")
        assertTrue(reApproveRes is DomainResult.Error)
    }

    @Test
    fun testRejectScorecardWorkflow() = runBlocking {
        val now = Instant.now()
        val genRes = service.generateScorecard(
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            vendorId = "VND-02",
            periodType = EvaluationPeriodType.QUARTERLY,
            periodStart = now.minusSeconds(86400 * 90),
            periodEnd = now,
            generatedBy = "staff_01"
        )
        val scorecard = (genRes as DomainResult.Success).data
        service.submitScorecardForReview("PRJ-01", scorecard.scorecardId, "staff_01")

        val rejectRes = service.rejectScorecard("PRJ-01", scorecard.scorecardId, "manager_01", "Missing data samples")
        assertTrue(rejectRes is DomainResult.Success)
        val rejected = (rejectRes as DomainResult.Success).data
        assertEquals(ScorecardStatus.REJECTED, rejected.status)
    }
}

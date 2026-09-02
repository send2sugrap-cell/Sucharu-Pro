package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPerformanceDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPortalPerformanceComplianceDataSource
import com.sucharu.sucharupro.data.repository.VendorPerformanceRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPortalPerformanceComplianceRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorPerformanceServiceImpl
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalPerformanceComplianceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class VendorPortalPerformanceIsolationTest {

    private val tenantA = "TENANT-A"
    private val tenantB = "TENANT-B"
    private val projectA = "PRJ-A"
    private val projectB = "PRJ-B"
    private val vendorA = "VND-A"
    private val vendorB = "VND-B"

    private lateinit var portalService: VendorPortalPerformanceComplianceServiceImpl

    @Before
    fun setup() {
        runBlocking {
            val vendorDs = FakeVendorDataSource()
            val vendorRepo = VendorRepositoryImpl(vendorDs)

            vendorRepo.createVendor(
                Vendor(
                    vendorId = vendorA,
                    projectId = projectA,
                    vendorCode = "VND-A",
                    vendorName = "Vendor A",
                    status = VendorStatus.ACTIVE
                )
            )

            vendorRepo.createVendor(
                Vendor(
                    vendorId = vendorB,
                    projectId = projectA,
                    vendorCode = "VND-B",
                    vendorName = "Vendor B",
                    status = VendorStatus.ACTIVE
                )
            )

            val perfDs = FakeVendorPerformanceDataSource()
            val perfRepo = VendorPerformanceRepositoryImpl(perfDs)
            val canonicalService = VendorPerformanceServiceImpl(perfRepo, vendorRepo)

            val portalDs = FakeVendorPortalPerformanceComplianceDataSource()
            val portalRepo = VendorPortalPerformanceComplianceRepositoryImpl(portalDs)

            portalService = VendorPortalPerformanceComplianceServiceImpl(
                portalRepository = portalRepo,
                canonicalPerformanceService = canonicalService,
                vendorRepository = vendorRepo
            )

            // Seed Scorecard for Vendor A only
            perfRepo.createScorecard(
                VendorPerformanceScorecard(
                    scorecardId = "SC-A-01",
                    tenantId = tenantA,
                    projectId = projectA,
                    vendorId = vendorA,
                    periodType = EvaluationPeriodType.MONTHLY,
                    periodStart = Instant.now().minus(30, ChronoUnit.DAYS),
                    periodEnd = Instant.now(),
                    overallScore = 95.0,
                    rating = PerformanceRating.EXCELLENT,
                    riskLevel = ComplianceRiskLevel.LOW,
                    dataCompleteness = 100.0,
                    sampleSize = 20,
                    status = ScorecardStatus.APPROVED,
                    items = emptyList(),
                    generatedAt = Instant.now(),
                    generatedBy = "SYSTEM"
                )
            )
        }
    }

    @Test
    fun testVendorCannotAccessOtherVendorScorecard() {
        runBlocking {
            // Vendor B trying to access Vendor A's scorecard in same project
            val res = portalService.getScorecardById(tenantA, projectA, vendorB, "SC-A-01")
            assertTrue(res is DomainResult.Error)
            assertTrue((res as DomainResult.Error).message.contains("Access Denied"))
        }
    }

    @Test
    fun testVendorCannotAccessCrossProjectData() {
        runBlocking {
            // Vendor A trying to access within Project B
            val res = portalService.getPerformanceOverview(tenantA, projectB, vendorA)
            assertTrue(res is DomainResult.Error)
        }
    }
}

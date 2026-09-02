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

class VendorPerformanceTenantIsolationTest {

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
                    projectId = "PRJ-TENANT-A",
                    vendorCode = "VA1",
                    vendorName = "Vendor A",
                    status = VendorStatus.ACTIVE
                )
            )

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-01",
                    projectId = "PRJ-TENANT-B",
                    vendorCode = "VB1",
                    vendorName = "Vendor B",
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testTenantIsolationAcrossKpisScorecardsAndCompliance() = runBlocking {
        // Create KPI in Tenant A
        val kpiA = VendorPerformanceKpi(
            kpiId = "KPI-A1",
            projectId = "PRJ-TENANT-A",
            tenantId = "PRJ-TENANT-A",
            code = "OTD",
            name = "On Time Delivery Tenant A",
            description = "Desc A",
            kpiType = KpiType.OPERATIONAL,
            targetValue = 95.0,
            createdBy = "adminA"
        )
        service.createKpi(kpiA)

        // Create KPI in Tenant B
        val kpiB = VendorPerformanceKpi(
            kpiId = "KPI-B1",
            projectId = "PRJ-TENANT-B",
            tenantId = "PRJ-TENANT-B",
            code = "OTD",
            name = "On Time Delivery Tenant B",
            description = "Desc B",
            kpiType = KpiType.OPERATIONAL,
            targetValue = 95.0,
            createdBy = "adminB"
        )
        service.createKpi(kpiB)

        // Tenant A must NOT see Tenant B's KPI by ID
        val getRes = service.getKpiById("PRJ-TENANT-A", "KPI-B1")
        assertTrue(getRes is DomainResult.Error)

        // Tenant A listing must ONLY contain Tenant A's KPI
        val listA = (service.listKpis("PRJ-TENANT-A") as DomainResult.Success).data
        assertEquals(1, listA.size)
        assertEquals("KPI-A1", listA[0].kpiId)

        // Tenant B listing must ONLY contain Tenant B's KPI
        val listB = (service.listKpis("PRJ-TENANT-B") as DomainResult.Success).data
        assertEquals(1, listB.size)
        assertEquals("KPI-B1", listB[0].kpiId)
    }

    @Test
    fun testTenantIsolationForEvaluations() = runBlocking {
        val now = Instant.now()
        val evalA = VendorEvaluation(
            evaluationId = "EVAL-TENANT-A",
            projectId = "PRJ-TENANT-A",
            tenantId = "PRJ-TENANT-A",
            vendorId = "VND-01",
            periodStart = now.minusSeconds(86400),
            periodEnd = now,
            evaluatorId = "userA",
            evaluatorName = "User A",
            evaluationScore = 90.0,
            createdBy = "userA"
        )
        service.createEvaluation(evalA)

        // Tenant B cannot retrieve Tenant A's evaluation
        val getB = service.getEvaluationById("PRJ-TENANT-B", "EVAL-TENANT-A")
        assertTrue(getB is DomainResult.Error)

        // Tenant B cannot approve Tenant A's evaluation
        val approveB = service.approveEvaluation("PRJ-TENANT-B", "EVAL-TENANT-A", "userB", EvaluationDecision.APPROVED)
        assertTrue(approveB is DomainResult.Error)
    }
}

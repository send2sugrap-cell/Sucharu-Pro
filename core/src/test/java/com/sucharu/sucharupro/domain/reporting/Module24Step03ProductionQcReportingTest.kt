package com.sucharu.sucharupro.domain.reporting

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.datasource.productionexecution.FakeProductionExecutionDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.report.*
import com.sucharu.sucharupro.domain.service.report.Module24ReportingServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class Module24Step03ProductionQcReportingTest {

    private lateinit var orderDs: FakeOrderDataSource
    private lateinit var customerDs: FakeCustomerDataSource
    private lateinit var execDs: FakeProductionExecutionDataSource
    private lateinit var qcDs: FakeProductionQcDataSource
    private lateinit var rwkDs: FakeProductionReworkDataSource
    private lateinit var service: Module24ReportingServiceImpl

    private val tenantId = "TENANT-STEP3-01"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "adm-001",
        projectId = tenantId,
        username = "admin_user",
        role = UserRole.ADMIN
    )

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "stf-001",
        projectId = tenantId,
        username = "staff_user",
        role = UserRole.STAFF
    )

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "cus-001",
        projectId = tenantId,
        username = "customer_user",
        role = UserRole.CUSTOMER,
        customerId = "cus-001"
    )

    @Before
    fun setUp() {
        orderDs = FakeOrderDataSource()
        customerDs = FakeCustomerDataSource()
        execDs = FakeProductionExecutionDataSource()
        qcDs = FakeProductionQcDataSource()
        rwkDs = FakeProductionReworkDataSource()

        service = Module24ReportingServiceImpl(
            orderDataSource = orderDs,
            customerDataSource = customerDs,
            productionExecutionDataSource = execDs,
            productionQcDataSource = qcDs,
            productionReworkDataSource = rwkDs
        )
    }

    // =========================================================================
    // A. PRODUCTION REPORTING TESTS
    // =========================================================================
    @Test
    fun testProductionSummary_executesAndReturnsJobMetrics() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.PRODUCTION,
            reportType = "PRODUCTION_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertEquals(ReportCategory.PRODUCTION, data.meta.reportCategory)
        assertEquals("PRODUCTION_SUMMARY", data.meta.reportType)

        val totalJobsMetric = data.summaryMetrics.find { it.metricId == "totalJobsCount" }
        assertNotNull(totalJobsMetric)
        assertTrue(totalJobsMetric!!.numericValue!! >= 1.0)
    }

    @Test
    fun testStagePerformance_reportsAll13CanonicalStages() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.PRODUCTION,
            reportType = "STAGE_PERFORMANCE",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertEquals(13, data.rows.size)

        val stageNames = data.rows.mapNotNull { it.values["stageName"] }
        ProductionStageType.orderedStages.forEach { canonicalStage ->
            assertTrue("Stage '${canonicalStage.defaultLabel}' must be present", stageNames.contains(canonicalStage.defaultLabel))
        }
    }

    @Test
    fun testProductionByStatus_reportsStatusDistribution() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.PRODUCTION,
            reportType = "PRODUCTION_BY_STATUS",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertTrue(data.rows.isNotEmpty())
    }

    // =========================================================================
    // B. QC REPORTING TESTS
    // =========================================================================
    @Test
    fun testQcSummary_reportsInspectionsAndPassRate() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.QUALITY,
            reportType = "QC_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertEquals(ReportCategory.QUALITY, data.meta.reportCategory)

        val passRateMetric = data.summaryMetrics.find { it.metricId == "passRatePercentage" }
        assertNotNull(passRateMetric)
    }

    @Test
    fun testFinalQcSummary_reportsFinalQcCheckpoints() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.QUALITY,
            reportType = "FINAL_QC_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        assertTrue(data.summaryMetrics.isNotEmpty())
    }

    // =========================================================================
    // C. REWORK REPORTING TESTS
    // =========================================================================
    @Test
    fun testReworkSummary_reportsReworkRequestsAndAffectedQty() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.QUALITY,
            reportType = "REWORK_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val data = (res as DomainResult.Success).data
        val reworkMetric = data.summaryMetrics.find { it.metricId == "totalReworksCount" }
        assertNotNull(reworkMetric)
    }

    // =========================================================================
    // D. SECURITY, TENANT & IDENTITY SCOPE TESTS
    // =========================================================================
    @Test
    fun testStaff_hasAccessToProductionAndQuality() = runBlocking {
        val prodReq = ReportRequest(ReportCategory.PRODUCTION, "PRODUCTION_SUMMARY", tenantId, tenantId)
        val prodRes = service.queryReport(staffPrincipal, prodReq)
        assertTrue("Staff should access production summary", prodRes is DomainResult.Success)

        val qcReq = ReportRequest(ReportCategory.QUALITY, "QC_SUMMARY", tenantId, tenantId)
        val qcRes = service.queryReport(staffPrincipal, qcReq)
        assertTrue("Staff should access QC summary", qcRes is DomainResult.Success)
    }

    @Test
    fun testCustomer_identityScope_restrictsToOwnJobs() = runBlocking {
        val ownReq = ReportRequest(
            reportCategory = ReportCategory.PRODUCTION,
            reportType = "PRODUCTION_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId,
            filters = mapOf("customerId" to "cus-001")
        )

        val ownRes = service.queryReport(customerPrincipal, ownReq)
        assertTrue(ownRes is DomainResult.Success)

        val otherReq = ReportRequest(
            reportCategory = ReportCategory.PRODUCTION,
            reportType = "PRODUCTION_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId,
            filters = mapOf("customerId" to "cus-999-FOREIGN")
        )

        val otherRes = service.queryReport(customerPrincipal, otherReq)
        assertTrue(otherRes is DomainResult.Error)
    }

    @Test
    fun testCrossTenantRequest_strictlyDenied() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.PRODUCTION,
            reportType = "PRODUCTION_SUMMARY",
            tenantId = "TENANT-FOREIGN-XYZ",
            projectId = "TENANT-FOREIGN-XYZ"
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Error)
        val err = res as DomainResult.Error
        assertTrue(err.message.contains("Tenant isolation violation"))
    }

    // =========================================================================
    // E. EXPORT TESTS
    // =========================================================================
    @Test
    fun testExportProductionSummary_generatesCsv() = runBlocking {
        val queryReq = ReportRequest(
            reportCategory = ReportCategory.PRODUCTION,
            reportType = "PRODUCTION_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val exportReq = ExportReportRequest(queryRequest = queryReq, format = ReportExportFormat.CSV)
        val res = service.exportReport(adminPrincipal, exportReq)

        assertTrue(res is DomainResult.Success)
        val doc = (res as DomainResult.Success).data
        assertEquals("PRODUCTION_SUMMARY", doc.reportType)
        assertEquals(ReportExportFormat.CSV, doc.format)
        assertNotNull(doc.contentBase64)
    }
}

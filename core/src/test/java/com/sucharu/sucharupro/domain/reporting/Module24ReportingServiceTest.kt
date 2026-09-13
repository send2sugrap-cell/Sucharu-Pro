package com.sucharu.sucharupro.domain.reporting

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.report.*
import com.sucharu.sucharupro.domain.service.report.Module24ReportingServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class Module24ReportingServiceTest {

    private val service = Module24ReportingServiceImpl()
    private val tenantId = "PROJ-ALPHA"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "adm-1",
        projectId = tenantId,
        username = "admin_user",
        role = UserRole.ADMIN
    )

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "stf-1",
        projectId = tenantId,
        username = "staff_user",
        role = UserRole.STAFF
    )

    @Test
    fun testGetReportCatalogue_roleFiltering() = runBlocking {
        val adminCatRes = service.getReportCatalogue(adminPrincipal)
        assertTrue(adminCatRes is DomainResult.Success)
        val adminCat = (adminCatRes as DomainResult.Success).data
        assertEquals(15, adminCat.totalCategories)

        val staffCatRes = service.getReportCatalogue(staffPrincipal)
        assertTrue(staffCatRes is DomainResult.Success)
        val staffCat = (staffCatRes as DomainResult.Success).data
        assertTrue("Staff should see fewer categories than admin", staffCat.totalCategories < adminCat.totalCategories)
    }

    @Test
    fun testQueryReport_salesCategoryExecution() = runBlocking {
        val req = ReportRequest(
            reportCategory = ReportCategory.SALES,
            reportType = "SALES_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId,
            fromDate = "2026-09-01",
            toDate = "2026-09-13",
            page = 1,
            pageSize = 10
        )

        val res = service.queryReport(adminPrincipal, req)
        assertTrue(res is DomainResult.Success)

        val response = (res as DomainResult.Success).data
        assertEquals(ReportCategory.SALES, response.meta.reportCategory)
        assertEquals("SALES_SUMMARY", response.meta.reportType)
        assertEquals(tenantId, response.meta.tenantId)
        assertTrue(response.meta.executionTimeMs >= 0L)
        assertTrue(response.summaryMetrics.isNotEmpty())
        assertTrue(response.columns.isNotEmpty())
        assertTrue(response.rows.isNotEmpty())
    }

    @Test
    fun testExportReport_generatesDocument() = runBlocking {
        val queryReq = ReportRequest(
            reportCategory = ReportCategory.FINANCE,
            reportType = "FINANCE_EXECUTIVE_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        val exportReq = ExportReportRequest(
            queryRequest = queryReq,
            format = ReportExportFormat.CSV
        )

        val res = service.exportReport(adminPrincipal, exportReq)
        assertTrue(res is DomainResult.Success)

        val doc = (res as DomainResult.Success).data
        assertEquals("FINANCE_EXECUTIVE_SUMMARY", doc.reportType)
        assertEquals(ReportExportFormat.CSV, doc.format)
        assertTrue(doc.fileName.startsWith("Report_FINANCE_EXECUTIVE_SUMMARY_"))
        assertNotNull(doc.contentBase64)
        assertTrue(doc.contentLength > 0L)
    }
}

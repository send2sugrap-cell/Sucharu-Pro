package com.sucharu.sucharupro.domain.reporting

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.report.*
import com.sucharu.sucharupro.domain.model.report.*
import com.sucharu.sucharupro.domain.service.report.Module24ReportCatalogueRegistry
import org.junit.Assert.*
import org.junit.Test

class Module24ReportContractTest {

    @Test
    fun testCatalogueRegistry_containsAll15CanonicalCategories() {
        val categoriesInRegistry = ReportCategory.entries.toSet()
        assertEquals(15, categoriesInRegistry.size)

        val adminPrincipal = AuthenticatedPrincipal(
            userId = "admin-1",
            projectId = "P-TEST",
            username = "admin_user",
            role = UserRole.ADMIN
        )

        val catalogue = Module24ReportCatalogueRegistry.getCatalogueForPrincipal(adminPrincipal)
        assertEquals(15, catalogue.totalCategories)
        assertTrue(catalogue.totalReports >= 15)
    }

    @Test
    fun testReportRequestDto_mappingToDomain() {
        val dto = ReportRequestDto(
            reportCategory = "SALES",
            reportType = "SALES_SUMMARY",
            tenantId = "TENANT-001",
            projectId = "PROJ-001",
            fromDate = "2026-09-01",
            toDate = "2026-09-13",
            period = "MONTHLY",
            filters = mapOf("status" to "COMPLETED"),
            page = 2,
            pageSize = 25,
            sortBy = "grossAmount",
            sortDirection = "DESC",
            requestedMetrics = listOf("totalSalesAmount")
        )

        val domain = dto.toDomainModel("DEFAULT-T", "DEFAULT-P")

        assertEquals(ReportCategory.SALES, domain.reportCategory)
        assertEquals("SALES_SUMMARY", domain.reportType)
        assertEquals("TENANT-001", domain.tenantId)
        assertEquals("PROJ-001", domain.projectId)
        assertEquals(ReportPeriod.MONTHLY, domain.period)
        assertEquals(2, domain.page)
        assertEquals(25, domain.pageSize)
        assertEquals("DESC", domain.sortDirection)
    }

    @Test
    fun testReportResponseDto_serializationRoundtrip() {
        val meta = ReportExecutionMeta(
            reportCategory = ReportCategory.FINANCE,
            reportType = "FINANCE_EXECUTIVE_SUMMARY",
            tenantId = "P-100",
            projectId = "P-100",
            generatedAt = "1789300000",
            executionTimeMs = 12L,
            authoritativeModule = "Module 09 Finance"
        )

        val metric = ReportMetric("m1", "Revenue", "BDT 100", 100.0)
        val col = ReportDataColumn("c1", "Col 1")
        val row = ReportDataRow("r1", mapOf("c1" to "val1"))

        val response = ReportResponse(
            meta = meta,
            summaryMetrics = listOf(metric),
            columns = listOf(col),
            rows = listOf(row)
        )

        val dto = response.toDto()

        assertEquals("FINANCE", dto.meta.reportCategory)
        assertEquals("FINANCE_EXECUTIVE_SUMMARY", dto.meta.reportType)
        assertEquals(1, dto.summaryMetrics.size)
        assertEquals("Revenue", dto.summaryMetrics[0].label)
        assertEquals(1, dto.rows.size)
        assertEquals("val1", dto.rows[0].values["c1"])
    }
}

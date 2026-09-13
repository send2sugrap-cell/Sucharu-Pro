package com.sucharu.sucharupro.backend.reporting

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.report.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class Module24ReportingApiTest {

    private lateinit var useCases: BackendUseCases
    private val tenantId = "TENANT-TEST-01"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "adm-user-1",
        projectId = tenantId,
        username = "admin_tester",
        role = UserRole.ADMIN
    )

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "stf-user-1",
        projectId = tenantId,
        username = "staff_tester",
        role = UserRole.STAFF
    )

    @Before
    fun setUp() {
        val fakeTxManager = object : TransactionManager {
            override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
            override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
        }
        val customFactory = object : PostgresRepositoryFactory(fakeTxManager) {}
        useCases = BackendUseCases(fakeTxManager, customFactory)
    }

    @Test
    fun test01_getReportCatalogue_adminSuccess() = runBlocking {
        val catalogue = useCases.getReportCatalogue(adminPrincipal)
        assertEquals(15, catalogue.totalCategories)
        assertTrue(catalogue.totalReports >= 15)
    }

    @Test
    fun test02_queryReport_salesSuccess() = runBlocking {
        val reqDto = ReportRequestDto(
            reportCategory = "SALES",
            reportType = "SALES_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId,
            fromDate = "2026-09-01",
            toDate = "2026-09-13"
        )

        val response = useCases.queryReport(adminPrincipal, reqDto)
        assertEquals("SALES", response.meta.reportCategory.name)
        assertEquals("SALES_SUMMARY", response.meta.reportType)
        assertTrue(response.summaryMetrics.isNotEmpty())
    }

    @Test
    fun test03_queryReport_unauthorizedRoleForbidden() = runBlocking {
        val reqDto = ReportRequestDto(
            reportCategory = "FINANCE",
            reportType = "FINANCE_EXECUTIVE_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )

        try {
            useCases.queryReport(staffPrincipal, reqDto)
            fail("Expected ForbiddenException for staff accessing finance report")
        } catch (e: ForbiddenException) {
            assertTrue(e.message!!.contains("Unauthorized"))
        }
    }

    @Test
    fun test04_exportReport_success() = runBlocking {
        val queryDto = ReportRequestDto(
            reportCategory = "PRODUCTION",
            reportType = "PRODUCTION_SUMMARY",
            tenantId = tenantId,
            projectId = tenantId
        )
        val exportDto = ExportReportRequestDto(
            queryRequest = queryDto,
            format = "CSV"
        )

        val doc = useCases.exportReport(adminPrincipal, exportDto)
        assertEquals("PRODUCTION_SUMMARY", doc.reportType)
        assertEquals("CSV", doc.format.name)
        assertNotNull(doc.contentBase64)
    }
}

package com.sucharu.sucharupro.domain.service.businessfinancialreporting

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.businessfinancialreporting.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

class BusinessFinancialReportingApiTest {

    private lateinit var router: BackendRouter
    private lateinit var securityContext: BackendSecurityContext

    private val adminToken = "Bearer token-admin"
    private val customerToken = "Bearer token-customer"

    @Before
    fun setup() {
        val mockDb = MockPostgresEventDatabase()
        val fakeReportingDs = com.sucharu.sucharupro.data.datasource.businessfinancialreporting.FakeBusinessFinancialReportingDataSource()
        val repoFactory = object : PostgresRepositoryFactory(mockDb, defaultTenantId = "PROJ-001") {
            override fun createBusinessFinancialReportingRepository(
                tenantId: String
            ): com.sucharu.sucharupro.domain.repository.businessfinancialreporting.BusinessFinancialReportingRepository {
                return com.sucharu.sucharupro.data.repository.businessfinancialreporting.BusinessFinancialReportingRepositoryImpl(fakeReportingDs)
            }
        }
        val useCases = BackendUseCases(mockDb, repoFactory)
        securityContext = BackendSecurityContext()

        securityContext.registerToken(
            "token-admin",
            AuthenticatedPrincipal(
                userId = "admin-1",
                projectId = "PROJ-001",
                username = "admin",
                role = UserRole.ADMIN,
                principalType = PrincipalType.HUMAN,
                permissions = emptySet()
            )
        )

        securityContext.registerToken(
            "token-customer",
            AuthenticatedPrincipal(
                userId = "cust-1",
                projectId = "PROJ-001",
                username = "customer",
                role = UserRole.CUSTOMER,
                principalType = PrincipalType.HUMAN,
                permissions = emptySet()
            )
        )

        val mockConnProvider = object : PostgresConnectionProvider {
            override suspend fun acquireConnection(): Connection {
                val mockRs = Proxy.newProxyInstance(
                    ResultSet::class.java.classLoader,
                    arrayOf(ResultSet::class.java)
                ) { _, method, _ ->
                    if (method.name == "next") true
                    else if (method.name == "getInt") 1
                    else null
                } as ResultSet

                val mockStmt = Proxy.newProxyInstance(
                    Statement::class.java.classLoader,
                    arrayOf(Statement::class.java)
                ) { _, method, _ ->
                    if (method.name == "executeQuery") mockRs
                    else null
                } as Statement

                return Proxy.newProxyInstance(
                    Connection::class.java.classLoader,
                    arrayOf(Connection::class.java)
                ) { _, method, _ ->
                    if (method.name == "createStatement") mockStmt
                    else if (method.name == "isClosed") false
                    else null
                } as Connection
            }

            override suspend fun releaseConnection(connection: Connection) {}
            override fun close() {}
        }

        val healthChecker = DatabaseHealthChecker(mockConnProvider)

        router = BackendRouter(
            securityContext = securityContext,
            useCases = useCases,
            healthChecker = healthChecker
        )
    }

    @Test
    fun testGetExecutiveSummaryEndpoint() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/business-financial-reports/executive-summary",
            headers = mapOf("Authorization" to adminToken)
        )
        val resp = router.handleRequest(req)
        assertEquals(200, resp.statusCode)
        val data = (resp.body as ApiSuccessResponse<*>).data as BusinessExecutiveFinancialSummaryDto
        assertNotNull(data)
        assertEquals("PROJ-001", data.tenantId)
    }

    @Test
    fun testGetExpenseAnalyticsEndpoint() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/business-financial-reports/expenses?currency=BDT",
            headers = mapOf("Authorization" to adminToken)
        )
        val resp = router.handleRequest(req)
        assertEquals(200, resp.statusCode)
        val data = (resp.body as ApiSuccessResponse<*>).data as BusinessExpenseAnalyticsReportDto
        assertNotNull(data)
        assertEquals("BDT", data.currency)
    }

    @Test
    fun testGetVendorPayablesEndpoint() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/business-financial-reports/vendor-payables",
            headers = mapOf("Authorization" to adminToken)
        )
        val resp = router.handleRequest(req)
        assertEquals(200, resp.statusCode)
        val data = (resp.body as ApiSuccessResponse<*>).data as VendorPayableAnalyticsReportDto
        assertNotNull(data)
    }

    @Test
    fun testGetBusinessLedgerEndpoint() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/business-financial-reports/ledger",
            headers = mapOf("Authorization" to adminToken)
        )
        val resp = router.handleRequest(req)
        assertEquals(200, resp.statusCode)
        val data = (resp.body as ApiSuccessResponse<*>).data as BusinessLedgerReportDto
        assertNotNull(data)
    }

    @Test
    fun testCreateAndGetSnapshotEndpoints() = runBlocking {
        val createReq = HttpRequest(
            method = "POST",
            path = "/api/v1/business-financial-reports/snapshots",
            headers = mapOf("Authorization" to adminToken),
            body = CreateReportSnapshotRequestDto(
                reportType = "EXECUTIVE_SUMMARY",
                metricsJson = "{\"totalExpenses\": 1200}"
            )
        )
        val createResp = router.handleRequest(createReq)
        assertEquals(201, createResp.statusCode)
        val snapData = (createResp.body as ApiSuccessResponse<*>).data as BusinessFinancialReportSnapshotDto
        assertNotNull(snapData.snapshotId)

        val getReq = HttpRequest(
            method = "GET",
            path = "/api/v1/business-financial-reports/snapshots/${snapData.snapshotId}",
            headers = mapOf("Authorization" to adminToken)
        )
        val getResp = router.handleRequest(getReq)
        assertEquals(200, getResp.statusCode)
        val getSnapData = (getResp.body as ApiSuccessResponse<*>).data as BusinessFinancialReportSnapshotDto
        assertEquals(snapData.snapshotId, getSnapData.snapshotId)
    }

    @Test
    fun testExportReportEndpoint() = runBlocking {
        val exportReq = HttpRequest(
            method = "POST",
            path = "/api/v1/business-financial-reports/export",
            headers = mapOf("Authorization" to adminToken),
            body = ExportFinancialReportRequestDto(
                reportType = "EXECUTIVE_SUMMARY",
                format = "CSV"
            )
        )
        val exportResp = router.handleRequest(exportReq)
        assertEquals(200, exportResp.statusCode)
        val docData = (exportResp.body as ApiSuccessResponse<*>).data as BusinessFinancialExportDocumentDto
        assertEquals("CSV", docData.format)
        assertTrue(docData.fileName.endsWith(".csv"))
    }

    @Test
    fun testCustomerRoleReturns403OnReportingEndpoints() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/business-financial-reports/executive-summary",
            headers = mapOf("Authorization" to customerToken)
        )
        val resp = router.handleRequest(req)
        assertTrue(resp.statusCode == 403 || resp.statusCode == 401)
    }
}

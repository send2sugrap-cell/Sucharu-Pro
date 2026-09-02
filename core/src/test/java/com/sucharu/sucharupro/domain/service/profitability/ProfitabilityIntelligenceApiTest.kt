package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.profitability.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.datasource.profitability.FakeProfitabilityIntelligenceDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.profitability.ProfitabilityIntelligenceRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

class ProfitabilityIntelligenceApiTest {

    private lateinit var router: BackendRouter
    private val adminToken = "Bearer token-admin"
    private val managerToken = "Bearer token-manager"
    private val staffToken = "Bearer token-staff"
    private val customerToken = "Bearer token-customer"
    private val vendorToken = "Bearer token-vendor"

    @Before
    fun setUp() {
        val securityContext = BackendSecurityContext()

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
            "token-manager",
            AuthenticatedPrincipal(
                userId = "manager-1",
                projectId = "PROJ-001",
                username = "manager",
                role = UserRole.MANAGER,
                principalType = PrincipalType.HUMAN,
                permissions = emptySet()
            )
        )

        securityContext.registerToken(
            "token-staff",
            AuthenticatedPrincipal(
                userId = "staff-1",
                projectId = "PROJ-001",
                username = "staff",
                role = UserRole.STAFF,
                principalType = PrincipalType.HUMAN,
                permissions = emptySet()
            )
        )

        securityContext.registerToken(
            "token-customer",
            AuthenticatedPrincipal(
                userId = "customer-1",
                projectId = "PROJ-001",
                username = "customer",
                role = UserRole.CUSTOMER,
                principalType = PrincipalType.HUMAN,
                permissions = emptySet()
            )
        )

        securityContext.registerToken(
            "token-vendor",
            AuthenticatedPrincipal(
                userId = "vendor-1",
                projectId = "PROJ-001",
                username = "vendor",
                role = UserRole.VENDOR,
                principalType = PrincipalType.HUMAN,
                permissions = emptySet()
            )
        )

        val mockDb = MockPostgresEventDatabase()
        val mockConnProvider = object : PostgresConnectionProvider {
            override suspend fun acquireConnection(): Connection {
                val mockRs: ResultSet = Proxy.newProxyInstance(
                    ResultSet::class.java.classLoader,
                    arrayOf(ResultSet::class.java)
                ) { _, method, _ ->
                    if (method.name == "next") true
                    else if (method.name == "getInt") 1
                    else null
                } as ResultSet

                val mockStmt: Statement = Proxy.newProxyInstance(
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
                    else null
                } as Connection
            }

            override suspend fun releaseConnection(connection: Connection) {}
            override fun close() {}
        }


        val fakeDataSource = FakeProfitabilityIntelligenceDataSource()
        val repoFactory = object : PostgresRepositoryFactory(mockDb, defaultTenantId = "PROJ-001") {

            override fun createProfitabilityIntelligenceService(tenantId: String): ProfitabilityIntelligenceService {
                val repo = ProfitabilityIntelligenceRepositoryImpl(fakeDataSource)
                val collector = object : ProfitabilityIntelligenceSourceCollector {
                    override suspend fun collectSourceData(tenantId: String, projectId: String, periodId: String): DomainResult<CollectedIntelligenceSourceData> {
                        val dims = listOf(
                            DimensionInsight(
                                insightId = "dim-cust-001",
                                snapshotId = "",
                                tenantId = tenantId,
                                periodId = periodId,
                                dimensionType = ProfitabilityDimensionType.CUSTOMER,
                                dimensionId = "CUST-001",
                                dimensionLabel = "Acme Corp",
                                revenue = BigDecimal("100000.0000"),
                                cost = BigDecimal("60000.0000"),
                                grossProfit = BigDecimal("40000.0000"),
                                margin = BigDecimal("40.0000")
                            )
                        )
                        return DomainResult.Success(
                            CollectedIntelligenceSourceData(
                                overallRevenue = BigDecimal("100000.0000"),
                                overallCost = BigDecimal("60000.0000"),
                                overallProfit = BigDecimal("40000.0000"),
                                overallMargin = BigDecimal("40.0000"),
                                dimensions = dims,
                                relationships = emptyList(),
                                provenanceRecords = emptyList()
                            )
                        )
                    }
                }
                return ProfitabilityIntelligenceServiceImpl(
                    repository = repo,
                    sourceCollector = collector
                )
            }
        }

        val useCases = BackendUseCases(mockDb, repoFactory)
        val healthChecker = DatabaseHealthChecker(mockConnProvider)

        router = BackendRouter(
            securityContext = securityContext,
            useCases = useCases,
            healthChecker = healthChecker
        )
    }

    @Test
    fun testCalculateProfitabilityIntelligenceEndpoint() = runBlocking {
        val payload = mapOf(
            "scope" to "FULL_BUSINESS",
            "idempotencyKey" to "idemp-001"
        )

        val req = HttpRequest(
            method = "POST",
            path = "/api/v1/profit-cost-analysis/intelligence/2026-M09/calculate",
            headers = mapOf("Authorization" to adminToken),
            body = payload
        )
        val resp = router.handleRequest(req)
        assertEquals(201, resp.statusCode)
        val data = (resp.body as ApiSuccessResponse<*>).data as ProfitabilityIntelligenceSnapshotDto
        assertNotNull(data.snapshotId)
        assertEquals("2026-M09", data.analysisPeriodId)
        assertEquals(BigDecimal("100000.0000"), data.revenue)
        assertEquals(BigDecimal("60000.0000"), data.totalCost)
        assertEquals(BigDecimal("40000.0000"), data.grossProfit)
        assertEquals(BigDecimal("40.0000"), data.grossMargin)
        assertEquals("HIGHLY_PROFITABLE", data.profitabilityClassification)
    }

    @Test
    fun testGetDimensionsAndDriversEndpoints() = runBlocking {
        // Calculate first
        val calcReq = HttpRequest(
            method = "POST",
            path = "/api/v1/profit-cost-analysis/intelligence/2026-M09/calculate",
            headers = mapOf("Authorization" to managerToken),
            body = emptyMap<String, Any?>()
        )
        router.handleRequest(calcReq)

        // Get Dimensions
        val dimReq = HttpRequest(
            method = "GET",
            path = "/api/v1/profit-cost-analysis/intelligence/2026-M09/dimensions",
            headers = mapOf("Authorization" to staffToken)
        )
        val dimResp = router.handleRequest(dimReq)
        assertEquals(200, dimResp.statusCode)
        val dims = (dimResp.body as ApiSuccessResponse<*>).data as List<*>
        assertTrue(dims.isNotEmpty())

        // Get Drivers
        val drvReq = HttpRequest(
            method = "GET",
            path = "/api/v1/profit-cost-analysis/intelligence/2026-M09/drivers",
            headers = mapOf("Authorization" to staffToken)
        )
        val drvResp = router.handleRequest(drvReq)
        assertEquals(200, drvResp.statusCode)
    }

    @Test
    fun testHandoffContractExportEndpoint() = runBlocking {
        // Calculate first
        val calcReq = HttpRequest(
            method = "POST",
            path = "/api/v1/profit-cost-analysis/intelligence/2026-M09/calculate",
            headers = mapOf("Authorization" to adminToken),
            body = emptyMap<String, Any?>()
        )
        router.handleRequest(calcReq)

        val handoffReq = HttpRequest(
            method = "GET",
            path = "/api/v1/profit-cost-analysis/intelligence/2026-M09/handoff",
            headers = mapOf("Authorization" to managerToken)
        )
        val handoffResp = router.handleRequest(handoffReq)
        assertEquals(200, handoffResp.statusCode)
        val handoff = (handoffResp.body as ApiSuccessResponse<*>).data as Module16Step07ProfitabilityIntelligenceHandoffContractDto
        assertEquals("MODULE16_STEP07_V1", handoff.contractVersion)
        assertEquals("2026-M09", handoff.periodId)
        assertEquals(BigDecimal("100000.0000"), handoff.overallRevenue)
    }

    @Test
    fun testRbacAccessDeniedForCustomerAndVendor() = runBlocking {
        val calcReqCustomer = HttpRequest(
            method = "POST",
            path = "/api/v1/profit-cost-analysis/intelligence/2026-M09/calculate",
            headers = mapOf("Authorization" to customerToken),
            body = emptyMap<String, Any?>()
        )
        val respCustomer = router.handleRequest(calcReqCustomer)
        assertEquals(403, respCustomer.statusCode)

        val calcReqVendor = HttpRequest(
            method = "POST",
            path = "/api/v1/profit-cost-analysis/intelligence/2026-M09/calculate",
            headers = mapOf("Authorization" to vendorToken),
            body = emptyMap<String, Any?>()
        )
        val respVendor = router.handleRequest(calcReqVendor)
        assertEquals(403, respVendor.statusCode)
    }
}

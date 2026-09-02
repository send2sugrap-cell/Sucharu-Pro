package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.profitability.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.datasource.profitability.FakeVendorProfitabilityDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.profitability.VendorProfitabilityRepositoryImpl
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

class VendorProfitabilityApiTest {

    private lateinit var router: BackendRouter
    private val adminToken = "Bearer token-admin"
    private val managerToken = "Bearer token-manager"
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

        val fakeDs = FakeVendorProfitabilityDataSource()
        val repo = VendorProfitabilityRepositoryImpl(fakeDs)

        val repoFactory = object : PostgresRepositoryFactory(mockDb, defaultTenantId = "PROJ-001") {
            override fun createVendorProfitabilityRepository(tenantId: String): com.sucharu.sucharupro.data.repository.profitability.VendorProfitabilityRepository {
                return repo
            }

            override fun createVendorProfitabilityService(tenantId: String): VendorProfitabilityService {
                val collector = object : VendorProfitabilitySourceCollector {
                    override suspend fun collectVendorData(
                        tenantId: String,
                        projectId: String,
                        vendorId: String,
                        customCosts: List<VendorCostAttribution>?,
                        customRevenueContext: List<VendorRevenueContextAttribution>?,
                        periodStart: Long?,
                        periodEnd: Long?
                    ): DomainResult<VendorSourceCollectionResult> {
                        val c1 = VendorCostAttribution(
                            costAttributionId = "COST-API-1",
                            tenantId = tenantId,
                            projectId = projectId,
                            vendorId = vendorId,
                            workOrderId = "WO-1",
                            jobId = "JOB-1",
                            componentType = JobCostComponentType.VENDOR_OUTSOURCE_COST,
                            attributedAmount = BigDecimal("35000.0000"),
                            isPaid = true,
                            sourceEntityId = "WO-1"
                        )
                        return DomainResult.Success(
                            VendorSourceCollectionResult(
                                costAttributions = listOf(c1),
                                revenueContextAttributions = emptyList(),
                                unattributedItems = emptyList(),
                                totalVendorCost = BigDecimal("35000.0000"),
                                directVendorCost = BigDecimal("35000.0000"),
                                paidVendorCost = BigDecimal("35000.0000"),
                                outstandingExposure = BigDecimal.ZERO,
                                unbilledEstimateCost = BigDecimal.ZERO,
                                reworkCost = BigDecimal.ZERO,
                                attributedRevenueContext = BigDecimal.ZERO,
                                attributedTotalJobCost = BigDecimal("35000.0000"),
                                workOrderSummaries = listOf(
                                    VendorWorkOrderSummary(
                                        workOrderId = "WO-1",
                                        status = "COMPLETED",
                                        estimatedCost = BigDecimal("35000.0000"),
                                        actualCost = BigDecimal("35000.0000"),
                                        variance = BigDecimal.ZERO
                                    )
                                ),
                                jobSummaries = listOf(
                                    VendorJobSummary(
                                        jobId = "JOB-1",
                                        vendorCost = BigDecimal("35000.0000"),
                                        totalJobCost = BigDecimal("35000.0000"),
                                        vendorCostSharePercentage = BigDecimal("100.0000"),
                                        attributedRevenueContext = BigDecimal.ZERO
                                    )
                                ),
                                productSummaries = emptyList(),
                                customerSummaries = emptyList(),
                                costBreakdown = listOf(
                                    VendorCostBreakdownItem(
                                        componentType = JobCostComponentType.VENDOR_OUTSOURCE_COST,
                                        amount = BigDecimal("35000.0000"),
                                        percentageOfTotalCost = BigDecimal("100.0000")
                                    )
                                ),
                                totalQuantity = 0L,
                                qualityFailureCount = 0,
                                reworkCount = 0,
                                rejectionCount = 0,
                                disputeCount = 0,
                                provenanceFingerprints = listOf("FP-1"),
                                sourceReadiness = VendorSourceReadiness.READY,
                                warnings = emptyList()
                            )
                        )
                    }
                }
                val recon = VendorProfitabilityReconciliationServiceImpl()
                val rank = VendorProfitabilityRankingServiceImpl()
                return VendorProfitabilityServiceImpl(repo, collector, recon, rank)
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
    fun testCalculateVendorProfitabilityEndpoint() = runBlocking {
        val req = HttpRequest(
            method = "POST",
            path = "/api/v1/profit-cost-analysis/vendors/vend-1/profitability/calculate",
            headers = mapOf("Authorization" to adminToken),
            body = mapOf("vendorName" to "Precision Foil Printing", "serviceCategory" to "FOILING")
        )
        val resp = router.handleRequest(req)
        assertEquals(200, resp.statusCode)
        assertTrue(resp.body is ApiSuccessResponse<*>)
        val snapshotDto = (resp.body as ApiSuccessResponse<*>).data as VendorProfitabilitySnapshotDto
        assertEquals("vend-1", snapshotDto.vendorId)
        assertEquals("Precision Foil Printing", snapshotDto.vendorName)
        assertEquals(BigDecimal("35000.0000"), snapshotDto.totalVendorCost)
    }

    @Test
    fun testGetVendorCostBreakdownEndpoint() = runBlocking {
        // Calculate first
        val calcReq = HttpRequest(
            method = "POST",
            path = "/api/v1/profit-cost-analysis/vendors/vend-1/profitability/calculate",
            headers = mapOf("Authorization" to adminToken),
            body = mapOf("vendorName" to "Precision Foil Printing")
        )
        router.handleRequest(calcReq)

        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/profit-cost-analysis/vendors/vend-1/profitability/components",
            headers = mapOf("Authorization" to adminToken)
        )
        val resp = router.handleRequest(req)
        assertEquals(200, resp.statusCode)
        assertTrue(resp.body is ApiSuccessResponse<*>)
    }

    @Test
    fun testCustomerRoleDeniedAccess() = runBlocking {
        val req = HttpRequest(
            method = "POST",
            path = "/api/v1/profit-cost-analysis/vendors/vend-1/profitability/calculate",
            headers = mapOf("Authorization" to customerToken),
            body = emptyMap<String, Any>()
        )
        val resp = router.handleRequest(req)
        assertEquals(403, resp.statusCode)
    }

    @Test
    fun testVendorRoleDeniedAccess() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/profit-cost-analysis/vendors/vend-1/profitability",
            headers = mapOf("Authorization" to vendorToken)
        )
        val resp = router.handleRequest(req)
        assertEquals(403, resp.statusCode)
    }

    @Test
    fun testVendorRankingsEndpoint() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/profit-cost-analysis/vendors/rankings?criteria=TOTAL_COST",
            headers = mapOf("Authorization" to managerToken)
        )
        val resp = router.handleRequest(req)
        assertEquals(200, resp.statusCode)
        assertTrue(resp.body is ApiSuccessResponse<*>)
    }

    @Test
    fun testVendorConcentrationEndpoint() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/profit-cost-analysis/vendors/concentration",
            headers = mapOf("Authorization" to adminToken)
        )
        val resp = router.handleRequest(req)
        assertEquals(200, resp.statusCode)
        assertTrue(resp.body is ApiSuccessResponse<*>)
    }
}

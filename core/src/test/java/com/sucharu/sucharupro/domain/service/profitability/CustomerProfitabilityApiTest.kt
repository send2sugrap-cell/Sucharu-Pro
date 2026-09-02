package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.profitability.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.datasource.profitability.FakeCustomerProfitabilityDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.profitability.CustomerProfitabilityRepositoryImpl
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

class CustomerProfitabilityApiTest {

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

        val fakeDs = FakeCustomerProfitabilityDataSource()
        val repo = CustomerProfitabilityRepositoryImpl(fakeDs)

        val repoFactory = object : PostgresRepositoryFactory(mockDb, defaultTenantId = "PROJ-001") {
            override fun createCustomerProfitabilityRepository(tenantId: String): com.sucharu.sucharupro.data.repository.profitability.CustomerProfitabilityRepository {
                return repo
            }

            override fun createCustomerProfitabilityService(tenantId: String): CustomerProfitabilityService {
                val collector = object : CustomerProfitabilitySourceCollector {
                    override suspend fun collectCustomerData(
                        tenantId: String,
                        projectId: String,
                        customerId: String,
                        customRevenue: List<CustomerRevenueAttribution>?,
                        customCosts: List<CustomerCostAttribution>?,
                        periodStart: Long?,
                        periodEnd: Long?
                    ): DomainResult<CustomerSourceCollectionResult> {
                        val rev = CustomerRevenueAttribution(
                            revenueAttributionId = "REV-API-1",
                            tenantId = tenantId,
                            projectId = projectId,
                            customerId = customerId,
                            orderId = "ORD-1",
                            quantity = 100,
                            recognizedRevenue = BigDecimal("50000.0000"),
                            sourceEntityId = "INV-101"
                        )
                        val c1 = CustomerCostAttribution(
                            costAttributionId = "COST-API-1",
                            tenantId = tenantId,
                            projectId = projectId,
                            customerId = customerId,
                            orderId = "ORD-1",
                            jobId = "JOB-1",
                            componentType = JobCostComponentType.MATERIAL_COST,
                            attributedAmount = BigDecimal("20000.0000"),
                            sourceEntityId = "JOB-101"
                        )
                        return DomainResult.Success(
                            CustomerSourceCollectionResult(
                                revenueAttributions = listOf(rev),
                                costAttributions = listOf(c1),
                                unattributedItems = emptyList(),
                                totalRevenue = BigDecimal("50000.0000"),
                                totalCost = BigDecimal("20000.0000"),
                                variableCost = BigDecimal("20000.0000"),
                                fixedCost = BigDecimal.ZERO,
                                orderSummaries = emptyList(),
                                jobSummaries = emptyList(),
                                productSummaries = emptyList(),
                                costBreakdown = listOf(
                                    CustomerCostBreakdownItem(
                                        componentType = JobCostComponentType.MATERIAL_COST,
                                        amount = BigDecimal("20000.0000"),
                                        percentageOfTotalCost = BigDecimal("100.0000")
                                    )
                                ),
                                operationalMetrics = CustomerOperationalMetrics(orderCount = 1, totalQuantitySold = 100),
                                provenanceFingerprints = listOf("FP-1"),
                                sourceIntegrity = ProductSourceIntegrityStatus.VERIFIED,
                                warnings = emptyList()
                            )
                        )
                    }
                }
                val recon = CustomerProfitabilityReconciliationServiceImpl()
                val rank = CustomerProfitabilityRankingServiceImpl()
                return CustomerProfitabilityServiceImpl(repo, collector, recon, rank)
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
    fun testCalculateCustomerProfitabilityEndpoint() = runBlocking {
        val req = HttpRequest(
            method = "POST",
            path = "/api/v1/profit-cost-analysis/customers/cust-1/profitability/calculate",
            headers = mapOf("Authorization" to adminToken),
            body = mapOf("customerName" to "Acme Corp", "customerCode" to "ACME")
        )
        val resp = router.handleRequest(req)
        assertEquals(200, resp.statusCode)
        assertTrue(resp.body is ApiSuccessResponse<*>)
        val snapshotDto = (resp.body as ApiSuccessResponse<*>).data as CustomerProfitabilitySnapshotDto
        assertEquals("cust-1", snapshotDto.customerId)
        assertEquals("Acme Corp", snapshotDto.customerName)
    }

    @Test
    fun testGetCustomerProfitabilityComponentsEndpoint() = runBlocking {
        // First calculate
        val calcReq = HttpRequest(
            method = "POST",
            path = "/api/v1/profit-cost-analysis/customers/cust-1/profitability/calculate",
            headers = mapOf("Authorization" to adminToken),
            body = mapOf("customerName" to "Acme Corp")
        )
        router.handleRequest(calcReq)

        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/profit-cost-analysis/customers/cust-1/profitability/components",
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
            path = "/api/v1/profit-cost-analysis/customers/cust-1/profitability/calculate",
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
            path = "/api/v1/profit-cost-analysis/customers/cust-1/profitability",
            headers = mapOf("Authorization" to vendorToken)
        )
        val resp = router.handleRequest(req)
        assertEquals(403, resp.statusCode)
    }

    @Test
    fun testCustomerRankingEndpoint() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/profit-cost-analysis/customers/ranking?criteria=REVENUE",
            headers = mapOf("Authorization" to managerToken)
        )
        val resp = router.handleRequest(req)
        assertEquals(200, resp.statusCode)
        assertTrue(resp.body is ApiSuccessResponse<*>)
    }

    @Test
    fun testCustomerConcentrationEndpoint() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/profit-cost-analysis/customers/concentration",
            headers = mapOf("Authorization" to adminToken)
        )
        val resp = router.handleRequest(req)
        assertEquals(200, resp.statusCode)
        assertTrue(resp.body is ApiSuccessResponse<*>)
    }
}

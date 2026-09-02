package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.profitability.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.datasource.profitability.FakeProductProfitabilityDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.profitability.ProductProfitabilityRepositoryImpl
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

class ProductProfitabilityApiTest {

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
                    else if (method.name == "isClosed") false
                    else null
                } as Connection
            }

            override suspend fun releaseConnection(connection: Connection) {}
            override fun close() {}
        }

        val fakeDs = FakeProductProfitabilityDataSource()
        val repo = ProductProfitabilityRepositoryImpl(fakeDs)

        val repoFactory = object : PostgresRepositoryFactory(mockDb, defaultTenantId = "PROJ-001") {
            override fun createProductProfitabilityRepository(tenantId: String): com.sucharu.sucharupro.data.repository.profitability.ProductProfitabilityRepository {
                return repo
            }

            override fun createProductProfitabilityService(tenantId: String): ProductProfitabilityService {
                val collector = object : ProductProfitabilitySourceCollector {
                    override suspend fun collectProductData(
                        tenantId: String,
                        projectId: String,
                        productId: String,
                        customRevenue: List<ProductRevenueAttribution>?,
                        customCosts: List<ProductCostAttribution>?
                    ): DomainResult<ProductSourceCollectionResult> {
                        val rev = ProductRevenueAttribution(
                            revenueAttributionId = "REV-API-1",
                            tenantId = tenantId,
                            projectId = projectId,
                            productId = productId,
                            quantity = 500,
                            recognizedRevenue = BigDecimal("50000.0000"),
                            sourceEntityId = "INV-101"
                        )
                        val c1 = ProductCostAttribution(
                            costAttributionId = "COST-API-1",
                            tenantId = tenantId,
                            projectId = projectId,
                            productId = productId,
                            componentType = JobCostComponentType.MATERIAL_COST,
                            attributedAmount = BigDecimal("20000.0000"),
                            sourceEntityId = "JOB-101"
                        )
                        val c2 = ProductCostAttribution(
                            costAttributionId = "COST-API-2",
                            tenantId = tenantId,
                            projectId = projectId,
                            productId = productId,
                            componentType = JobCostComponentType.LABOUR_COST,
                            attributedAmount = BigDecimal("10000.0000"),
                            sourceEntityId = "JOB-101"
                        )
                        return DomainResult.Success(
                            ProductSourceCollectionResult(
                                revenueAttributions = listOf(rev),
                                costAttributions = listOf(c1, c2),
                                totalQuantity = 500,
                                totalRecognizedRevenue = BigDecimal("50000.0000"),
                                totalActualCost = BigDecimal("30000.0000"),
                                components = listOf(
                                    ProductCostBreakdownItem(componentType = JobCostComponentType.MATERIAL_COST, amount = BigDecimal("20000.0000")),
                                    ProductCostBreakdownItem(componentType = JobCostComponentType.LABOUR_COST, amount = BigDecimal("10000.0000"))
                                ),
                                provenanceFingerprints = listOf("FP1", "FP2"),
                                sourceIntegrity = ProductSourceIntegrityStatus.VERIFIED,
                                warnings = emptyList()
                            )
                        )
                    }
                }
                val recon = ProductProfitabilityReconciliationServiceImpl()
                return ProductProfitabilityServiceImpl(repo, collector, recon)
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
    fun testCalculateProductProfitabilityEndpoint() = runBlocking {
        val payload = mapOf(
            "productName" to "Deluxe Packaging Box",
            "customBaselineCost" to 28000.0
        )

        val req = HttpRequest(
            method = "POST",
            path = "/api/v1/profit-cost-analysis/products/PROD-2026-001/profitability/calculate",
            headers = mapOf("Authorization" to adminToken),
            body = payload
        )
        val resp = router.handleRequest(req)
        assertEquals(201, resp.statusCode)
        val data = (resp.body as ApiSuccessResponse<*>).data as ProductProfitabilitySnapshotDto
        assertNotNull(data.snapshotId)
        assertEquals("PROD-2026-001", data.productId)
        assertEquals(BigDecimal("50000.0000"), data.recognizedRevenue)
        assertEquals(BigDecimal("30000.0000"), data.totalActualCost)
        assertEquals(BigDecimal("20000.0000"), data.grossProfit)
        assertEquals(BigDecimal("40.0000"), data.grossMarginPercentage)
        assertEquals(500, data.totalQuantity)
        assertEquals(BigDecimal("100.0000"), data.unitEconomics.unitRevenue)
        assertEquals(BigDecimal("60.0000"), data.unitEconomics.unitActualCost)
        assertEquals(BigDecimal("40.0000"), data.unitEconomics.unitGrossProfit)
        assertEquals("HIGHLY_PROFITABLE", data.profitabilityClassification)
    }

    @Test
    fun testGetProductProfitabilityUnitEconomicsEndpoint() = runBlocking {
        // First calculate
        val calcReq = HttpRequest(
            method = "POST",
            path = "/api/v1/profit-cost-analysis/products/PROD-2026-002/profitability/calculate",
            headers = mapOf("Authorization" to managerToken),
            body = emptyMap<String, Any?>()
        )
        router.handleRequest(calcReq)

        // Then get unit economics
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/profit-cost-analysis/products/PROD-2026-002/profitability/unit-economics",
            headers = mapOf("Authorization" to managerToken)
        )
        val resp = router.handleRequest(req)
        assertEquals(200, resp.statusCode)
        val data = (resp.body as ApiSuccessResponse<*>).data as ProductUnitEconomicsDto
        assertEquals(500, data.quantity)
        assertEquals(BigDecimal("100.0000"), data.unitRevenue)
        assertEquals(BigDecimal("60.0000"), data.unitActualCost)
    }

    @Test
    fun testProductProfitabilityRbacDeniedForCustomer() = runBlocking {
        val req = HttpRequest(
            method = "POST",
            path = "/api/v1/profit-cost-analysis/products/PROD-2026-001/profitability/calculate",
            headers = mapOf("Authorization" to customerToken),
            body = emptyMap<String, Any?>()
        )
        val resp = router.handleRequest(req)
        assertEquals(403, resp.statusCode)
    }
}

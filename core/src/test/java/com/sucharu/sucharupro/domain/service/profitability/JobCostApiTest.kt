package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.profitability.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.datasource.profitability.FakeJobCostDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.profitability.JobCostRepositoryImpl
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

class JobCostApiTest {

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
                userId = "cust-1",
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
                username = "vendor_user",
                role = UserRole.VENDOR,
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

        val mockDb = MockPostgresEventDatabase()
        val fakeJobCostDs = FakeJobCostDataSource()

        val repoFactory = object : PostgresRepositoryFactory(mockDb, defaultTenantId = "PROJ-001") {
            override fun createJobCostCalculationService(
                tenantId: String
            ): JobCostCalculationService {
                val repo = JobCostRepositoryImpl(fakeJobCostDs)
                val collector = object : JobCostSourceCollector {
                    override suspend fun collectJobCosts(
                        tenantId: String,
                        projectId: String,
                        jobId: String,
                        customDirectCosts: List<JobCostComponent>?,
                        customIndirectCosts: List<JobCostAllocationDetail>?
                    ): DomainResult<JobCostCollectionResult> {
                        val c1 = JobCostComponent(
                            componentId = "COMP-1",
                            tenantId = tenantId,
                            projectId = projectId,
                            jobId = jobId,
                            componentType = JobCostComponentType.MATERIAL_COST,
                            directness = CostDirectness.DIRECT,
                            attributedAmount = BigDecimal("8000.0000")
                        )
                        val c2 = JobCostComponent(
                            componentId = "COMP-2",
                            tenantId = tenantId,
                            projectId = projectId,
                            jobId = jobId,
                            componentType = JobCostComponentType.ALLOCATED_INDIRECT_COST,
                            directness = CostDirectness.INDIRECT,
                            attributedAmount = BigDecimal("2000.0000")
                        )
                        return DomainResult.Success(
                            JobCostCollectionResult(
                                components = listOf(c1, c2),
                                provenances = emptyList(),
                                allocations = emptyList(),
                                duplicateCount = 0,
                                unresolvedCount = 0,
                                warnings = emptyList(),
                                readinessStatus = JobCostReadinessStatus.COMPLETE
                            )
                        )
                    }
                }
                val recon = JobCostReconciliationServiceImpl()
                return JobCostCalculationServiceImpl(repo, collector, recon)
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
    fun testCalculateJobActualCostEndpoint() = runBlocking {
        val payload = mapOf(
            "jobNumber" to "JOB-2026-001",
            "jobQuantity" to 1000,
            "customEstimatedCost" to 9500.0
        )

        val req = HttpRequest(
            method = "POST",
            path = "/api/v1/profit-cost-analysis/jobs/JOB-2026-001/actual-cost/calculate",
            headers = mapOf("Authorization" to adminToken),
            body = payload
        )
        val resp = router.handleRequest(req)
        assertEquals(201, resp.statusCode)
        val data = (resp.body as ApiSuccessResponse<*>).data as JobCostSnapshotDto
        assertNotNull(data.snapshotId)
        assertEquals("JOB-2026-001", data.jobId)
        assertEquals(BigDecimal("10000.0000"), data.totalActualCost)
        assertEquals(BigDecimal("8000.0000"), data.totalDirectCost)
        assertEquals(BigDecimal("2000.0000"), data.totalIndirectCost)
        assertEquals(BigDecimal("9500.0000"), data.estimatedCost)
        assertEquals(BigDecimal("500.0000"), data.costVariance)
        assertEquals("OVER_BUDGET", data.varianceClassification)
    }

    @Test
    fun testGetJobActualCostEndpoint() = runBlocking {
        // First calculate
        val postReq = HttpRequest(
            method = "POST",
            path = "/api/v1/profit-cost-analysis/jobs/JOB-100/actual-cost/calculate",
            headers = mapOf("Authorization" to adminToken),
            body = emptyMap<String, Any>()
        )
        router.handleRequest(postReq)

        // Then get
        val getReq = HttpRequest(
            method = "GET",
            path = "/api/v1/profit-cost-analysis/jobs/JOB-100/actual-cost",
            headers = mapOf("Authorization" to managerToken)
        )
        val resp = router.handleRequest(getReq)
        assertEquals(200, resp.statusCode)
        val data = (resp.body as ApiSuccessResponse<*>).data as JobCostSnapshotDto
        assertEquals("JOB-100", data.jobId)
    }

    @Test
    fun testGetJobCostComponentsEndpoint() = runBlocking {
        // Calculate
        val postReq = HttpRequest(
            method = "POST",
            path = "/api/v1/profit-cost-analysis/jobs/JOB-200/actual-cost/calculate",
            headers = mapOf("Authorization" to adminToken),
            body = emptyMap<String, Any>()
        )
        router.handleRequest(postReq)

        val getReq = HttpRequest(
            method = "GET",
            path = "/api/v1/profit-cost-analysis/jobs/JOB-200/actual-cost/components",
            headers = mapOf("Authorization" to adminToken)
        )
        val resp = router.handleRequest(getReq)
        assertEquals(200, resp.statusCode)
        val data = (resp.body as ApiSuccessResponse<*>).data as List<*>
        assertEquals(2, data.size)
    }

    @Test
    fun testUnauthorizedCustomerAccessForbidden() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/profit-cost-analysis/jobs/JOB-100/actual-cost",
            headers = mapOf("Authorization" to customerToken)
        )
        val resp = router.handleRequest(req)
        assertEquals(403, resp.statusCode)
    }

    @Test
    fun testUnauthorizedVendorAccessForbidden() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/profit-cost-analysis/jobs/JOB-100/actual-cost",
            headers = mapOf("Authorization" to vendorToken)
        )
        val resp = router.handleRequest(req)
        assertEquals(403, resp.statusCode)
    }
}

package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.profitability.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.datasource.profitability.FakeProfitabilityDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.profitability.ProfitabilityRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessintegrity.Module16FinancialHandoffContract
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

class ProfitabilityApiTest {

    private lateinit var router: BackendRouter
    private val adminToken = "Bearer token-admin"
    private val managerToken = "Bearer token-manager"
    private val customerToken = "Bearer token-customer"

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
        val fakeProfitabilityDs = FakeProfitabilityDataSource()

        val repoFactory = object : PostgresRepositoryFactory(mockDb, defaultTenantId = "PROJ-001") {
            override fun createProfitabilityFoundationService(
                tenantId: String
            ): ProfitabilityFoundationService {
                val repo = ProfitabilityRepositoryImpl(fakeProfitabilityDs)
                val fakeHandoff = object : Module16FinancialHandoffAdapter {
                    override suspend fun getVerifiedFinancialHandoff(
                        tenantId: String,
                        projectId: String,
                        periodId: String
                    ): DomainResult<ValidatedFinancialHandoff> {
                        return DomainResult.Success(
                            ValidatedFinancialHandoff(
                                contract = Module16FinancialHandoffContract(
                                    tenantId = tenantId,
                                    projectId = projectId,
                                    periodId = periodId,
                                    periodCode = "2026-M08",
                                    isPeriodClosed = false,
                                    closureCertificateChecksum = null,
                                    isLedgerBalanced = true
                                ),
                                integrityStatus = SourceIntegrityStatus.VERIFIED,
                                isLedgerBalanced = true,
                                isPeriodClosed = false,
                                hasValidClosureCertificate = false,
                                validationNotes = emptyList()
                            )
                        )
                    }

                    override suspend fun verifyPeriodIntegrityStatus(
                        tenantId: String,
                        projectId: String,
                        periodId: String
                    ): DomainResult<SourceIntegrityStatus> {
                        return DomainResult.Success(SourceIntegrityStatus.VERIFIED)
                    }
                }
                val registry = ProfitabilitySourceRegistryImpl(fakeHandoff)
                val recon = ProfitabilityReconciliationServiceImpl(fakeHandoff, registry)
                return ProfitabilityFoundationServiceImpl(repo, fakeHandoff, registry, recon)
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
    fun testGenerateProfitabilitySnapshotEndpoint() = runBlocking {
        val payload = mapOf(
            "scope" to "BUSINESS",
            "currency" to "BDT",
            "customRevenue" to 150000.0,
            "customDirectCost" to 90000.0,
            "customIndirectCost" to 10000.0
        )

        val req = HttpRequest(
            method = "POST",
            path = "/api/v1/profit-cost-analysis/snapshots",
            headers = mapOf("Authorization" to adminToken),
            body = payload
        )
        val resp = router.handleRequest(req)
        assertEquals(201, resp.statusCode)
        val data = (resp.body as ApiSuccessResponse<*>).data as ProfitabilitySnapshotDto
        assertNotNull(data.id)
        assertEquals("BUSINESS", data.scope)
        assertEquals(BigDecimal("150000.0000"), data.metrics.revenue)
        assertEquals(BigDecimal("100000.0000"), data.metrics.totalCost)
        assertEquals(BigDecimal("50000.0000"), data.metrics.grossProfit)
        assertEquals(BigDecimal("33.3333"), data.metrics.grossMarginPercentage)
    }

    @Test
    fun testGetSourceReadinessEndpoint() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/profit-cost-analysis/source-readiness",
            headers = mapOf("Authorization" to managerToken)
        )
        val resp = router.handleRequest(req)
        assertEquals(200, resp.statusCode)
        val data = (resp.body as ApiSuccessResponse<*>).data as ProfitabilitySourceReadinessDto
        assertNotNull(data)
        assertEquals("PROJ-001", data.projectId)
    }

    @Test
    fun testUnauthorizedCustomerAccessRejected() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/profit-cost-analysis/source-readiness",
            headers = mapOf("Authorization" to customerToken)
        )
        val resp = router.handleRequest(req)
        assertEquals(403, resp.statusCode)
    }
}

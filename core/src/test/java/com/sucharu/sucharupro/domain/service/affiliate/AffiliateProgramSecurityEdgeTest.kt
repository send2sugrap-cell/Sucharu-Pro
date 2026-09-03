package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.affiliate.CreateAffiliateProgramRequestDto
import com.sucharu.sucharupro.data.api.model.affiliate.EnrollAffiliateRequestDto
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateProgramDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateProgramRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

class AffiliateProgramSecurityEdgeTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var router: BackendRouter
    private lateinit var programDataSource: FakeAffiliateProgramDataSource
    private lateinit var affiliateDataSource: FakeAffiliateDataSource

    private val tenantAlpha = "TENANT-ALPHA"
    private val tenantBeta = "TENANT-BETA"

    private val adminToken = "Bearer token-admin"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "admin-1",
        username = "admin_user",
        role = UserRole.ADMIN,
        projectId = tenantAlpha
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "mgr-1",
        username = "manager_user",
        role = UserRole.MANAGER,
        projectId = tenantAlpha
    )

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff-1",
        username = "staff_user",
        role = UserRole.STAFF,
        projectId = tenantAlpha
    )

    private val affiliateA = AuthenticatedPrincipal(
        userId = "usr-aff-a",
        username = "aff_a",
        role = UserRole.AFFILIATE,
        projectId = tenantAlpha
    )

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "cust-1",
        username = "customer_user",
        role = UserRole.CUSTOMER,
        projectId = tenantAlpha
    )

    private val vendorPrincipal = AuthenticatedPrincipal(
        userId = "vend-1",
        username = "vendor_user",
        role = UserRole.VENDOR,
        projectId = tenantAlpha
    )

    private val aiAgentPrincipal = AuthenticatedPrincipal(
        userId = "ai-agent-1",
        username = "ai_agent",
        role = UserRole.AI_AGENT,
        principalType = PrincipalType.AI_AGENT,
        projectId = tenantAlpha
    )

    private val tenantBetaAdmin = AuthenticatedPrincipal(
        userId = "beta-admin",
        username = "beta_admin",
        role = UserRole.ADMIN,
        projectId = tenantBeta
    )

    @Before
    fun setup() {
        val securityContext = BackendSecurityContext()
        securityContext.registerToken("token-admin", adminPrincipal)
        securityContext.registerToken("token-manager", managerPrincipal)
        securityContext.registerToken("token-staff", staffPrincipal)
        securityContext.registerToken("token-aff-a", affiliateA)
        securityContext.registerToken("token-customer", customerPrincipal)
        securityContext.registerToken("token-vendor", vendorPrincipal)
        securityContext.registerToken("token-ai", aiAgentPrincipal)
        securityContext.registerToken("token-beta-admin", tenantBetaAdmin)

        programDataSource = FakeAffiliateProgramDataSource()
        affiliateDataSource = FakeAffiliateDataSource()

        val progRepo = AffiliateProgramRepositoryImpl(programDataSource)
        val affRepo = AffiliateRepositoryImpl(affiliateDataSource)

        val progService = AffiliateProgramServiceImpl(progRepo, affRepo)
        val affService = AffiliateServiceImpl(affRepo)

        val mockDb = MockPostgresEventDatabase()

        val factory = object : PostgresRepositoryFactory(
            transactionManager = mockDb,
            defaultTenantId = tenantAlpha
        ) {
            override fun createAffiliateDataSource(tenantId: String) = affiliateDataSource
            override fun createAffiliateRepository(tenantId: String) = affRepo
            override fun createAffiliateService(tenantId: String) = affService

            override fun createAffiliateProgramDataSource(tenantId: String) = programDataSource
            override fun createAffiliateProgramRepository(tenantId: String) = progRepo
            override fun createAffiliateProgramService(tenantId: String) = progService
        }

        useCases = BackendUseCases(mockDb, factory)

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

        val healthChecker = DatabaseHealthChecker(mockConnProvider)
        router = BackendRouter(securityContext, useCases, healthChecker)
    }

    @Test
    fun `test Customer and Vendor roles are rejected from creating or managing programs`() {
        val req = CreateAffiliateProgramRequestDto(
            programCode = "TEST_PROG",
            programName = "Test Program",
            startDate = 1000L
        )

        // Customer forbidden
        assertThrows(ForbiddenException::class.java) {
            runBlocking {
                useCases.createAffiliateProgram(customerPrincipal, req)
            }
        }

        // Vendor forbidden
        assertThrows(ForbiddenException::class.java) {
            runBlocking {
                useCases.createAffiliateProgram(vendorPrincipal, req)
            }
        }
    }

    @Test
    fun `test Cross-Tenant Program isolation between Alpha and Beta`() {
        runBlocking {
            val prog = useCases.createAffiliateProgram(
                managerPrincipal,
                CreateAffiliateProgramRequestDto(programCode = "ALPHA_PROG", programName = "Alpha Program", startDate = 1000L)
            )

            // Beta admin cannot access Alpha program (isolated tenant context)
            assertThrows(NotFoundException::class.java) {
                runBlocking {
                    useCases.getAffiliateProgramById(tenantBetaAdmin, prog.programId)
                }
            }
        }
    }

    @Test
    fun `test AI Agent cannot mutate program or enrollment state`() {
        runBlocking {
            val prog = useCases.createAffiliateProgram(
                managerPrincipal,
                CreateAffiliateProgramRequestDto(programCode = "AI_PROG", programName = "AI Prog", startDate = 1000L)
            )

            // AI Agent can read
            val readProg = useCases.getAffiliateProgramById(aiAgentPrincipal, prog.programId)
            assertEquals("AI_PROG", readProg.programCode)

            // AI Agent cannot activate
            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    useCases.activateAffiliateProgram(aiAgentPrincipal, prog.programId, "AI activate")
                }
            }
        }
    }

    @Test
    fun `test Staff cannot activate program or close program without Manager or Admin authority`() {
        runBlocking {
            val prog = useCases.createAffiliateProgram(
                managerPrincipal,
                CreateAffiliateProgramRequestDto(programCode = "STAFF_TEST_PROG", programName = "Staff Prog", startDate = 1000L)
            )

            // Staff activating fails
            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    useCases.activateAffiliateProgram(staffPrincipal, prog.programId, "Staff activate")
                }
            }

            // Manager activating succeeds
            val activated = useCases.activateAffiliateProgram(managerPrincipal, prog.programId, "Manager activate")
            assertEquals("ACTIVE", activated.status)

            // Staff archiving fails
            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    useCases.archiveAffiliateProgram(staffPrincipal, prog.programId, "Staff archive")
                }
            }

            // Manager cannot archive (Admin only)
            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    useCases.archiveAffiliateProgram(managerPrincipal, prog.programId, "Manager archive")
                }
            }

            // Admin archiving succeeds
            val closed = useCases.closeAffiliateProgram(managerPrincipal, prog.programId, "Closed")
            val archived = useCases.archiveAffiliateProgram(adminPrincipal, closed.programId, "Admin archive")
            assertEquals("ARCHIVED", archived.status)
        }
    }

    @Test
    fun `test REST endpoints for Affiliate Programs and Enrollments via Router`() {
        runBlocking {
            // 1. POST /api/v1/affiliate-programs
            val createReq = HttpRequest(
                method = "POST",
                path = "/api/v1/affiliate-programs",
                headers = mapOf("Authorization" to adminToken),
                body = mapOf(
                    "programCode" to "REST_PROG_01",
                    "programName" to "Rest Program 01",
                    "startDate" to 1000L
                )
            )
            val createResp = router.handleRequest(createReq)
            assertEquals(201, createResp.statusCode)

            // 2. GET /api/v1/affiliate-programs/overview
            val overviewReq = HttpRequest(
                method = "GET",
                path = "/api/v1/affiliate-programs/overview",
                headers = mapOf("Authorization" to adminToken)
            )
            val overviewResp = router.handleRequest(overviewReq)
            assertEquals(200, overviewResp.statusCode)

            // 3. GET /api/v1/affiliate-programs/code/REST_PROG_01
            val lookupReq = HttpRequest(
                method = "GET",
                path = "/api/v1/affiliate-programs/code/REST_PROG_01",
                headers = mapOf("Authorization" to adminToken)
            )
            val lookupResp = router.handleRequest(lookupReq)
            assertEquals(200, lookupResp.statusCode)
        }
    }
}

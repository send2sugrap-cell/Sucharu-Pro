package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.affiliate.CreateAffiliateRequestDto
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

class AffiliateSecurityEdgeTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var router: BackendRouter
    private lateinit var fakeDs: FakeAffiliateDataSource

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

    private val affiliateB = AuthenticatedPrincipal(
        userId = "usr-aff-b",
        username = "aff_b",
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

    private val guestPrincipal = AuthenticatedPrincipal(
        userId = "guest-1",
        username = "guest_user",
        role = UserRole.GUEST,
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
        securityContext.registerToken("token-aff-b", affiliateB)
        securityContext.registerToken("token-customer", customerPrincipal)
        securityContext.registerToken("token-vendor", vendorPrincipal)
        securityContext.registerToken("token-guest", guestPrincipal)
        securityContext.registerToken("token-ai", aiAgentPrincipal)
        securityContext.registerToken("token-beta-admin", tenantBetaAdmin)

        fakeDs = FakeAffiliateDataSource()
        val repo = AffiliateRepositoryImpl(fakeDs)
        val service = AffiliateServiceImpl(repo)

        val mockDb = MockPostgresEventDatabase()

        val factory = object : PostgresRepositoryFactory(
            transactionManager = mockDb,
            defaultTenantId = tenantAlpha
        ) {
            override fun createAffiliateDataSource(tenantId: String) = fakeDs
            override fun createAffiliateRepository(tenantId: String) = repo
            override fun createAffiliateService(tenantId: String) = service
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
    fun `test Customer and Vendor roles are rejected from managing affiliates`() {
        val req = CreateAffiliateRequestDto(
            userId = "target-user",
            displayName = "Target Affiliate"
        )

        // Vendor strictly forbidden
        assertThrows(ForbiddenException::class.java) {
            runBlocking {
                useCases.createAffiliate(vendorPrincipal, req)
            }
        }

        // Guest strictly forbidden
        assertThrows(ForbiddenException::class.java) {
            runBlocking {
                useCases.createAffiliate(guestPrincipal, req)
            }
        }
    }

    @Test
    fun `test Cross-Tenant access denial between Tenant Alpha and Tenant Beta`() {
        runBlocking {
            // Create affiliate in Tenant Alpha
            val created = useCases.createAffiliate(
                adminPrincipal,
                CreateAffiliateRequestDto(userId = "alpha-user", displayName = "Alpha Partner")
            )

            // Tenant Beta Admin attempting to view Tenant Alpha affiliate directly via service
            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    val service = AffiliateServiceImpl(AffiliateRepositoryImpl(fakeDs))
                    service.getAffiliateById(tenantAlpha, created.affiliateId, tenantBetaAdmin)
                }
            }
        }
    }

    @Test
    fun `test Affiliate A cannot view or mutate Affiliate B profile`() {
        runBlocking {
            val affAProfile = useCases.createAffiliate(
                affiliateA,
                CreateAffiliateRequestDto(userId = "usr-aff-a", displayName = "Partner A")
            )

            val affBProfile = useCases.createAffiliate(
                affiliateB,
                CreateAffiliateRequestDto(userId = "usr-aff-b", displayName = "Partner B")
            )

            // Affiliate A can read own profile
            val own = useCases.getAffiliateById(affiliateA, affAProfile.affiliateId)
            assertEquals("Partner A", own.displayName)

            // Affiliate A reading Affiliate B profile fails with ForbiddenException
            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    useCases.getAffiliateById(affiliateA, affBProfile.affiliateId)
                }
            }
        }
    }

    @Test
    fun `test AI Agent is strictly READ-ONLY and rejected from mutating or activating affiliate`() {
        runBlocking {
            val created = useCases.createAffiliate(
                adminPrincipal,
                CreateAffiliateRequestDto(userId = "target-user-2", displayName = "Test Partner")
            )

            // AI Agent can read / inspect
            val profile = useCases.getAffiliateById(aiAgentPrincipal, created.affiliateId)
            assertNotNull(profile)

            val handoff = useCases.getAffiliateHandoffContract(aiAgentPrincipal, created.affiliateId)
            assertTrue(handoff.isReadOnly)

            // AI Agent activating affiliate fails with ForbiddenException
            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    useCases.activateAffiliate(aiAgentPrincipal, created.affiliateId, "AI activation")
                }
            }
        }
    }

    @Test
    fun `test Staff cannot activate or suspend without Manager or Admin authority`() {
        runBlocking {
            val created = useCases.createAffiliate(
                staffPrincipal,
                CreateAffiliateRequestDto(userId = "target-user-3", displayName = "Test Partner 3")
            )

            // Staff activating fails
            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    useCases.activateAffiliate(staffPrincipal, created.affiliateId, "Staff trying to activate")
                }
            }

            // Manager activating succeeds
            val activated = useCases.activateAffiliate(managerPrincipal, created.affiliateId, "Manager approved")
            assertEquals("ACTIVE", activated.status)

            // Manager cannot terminate (Admin only)
            assertThrows(ForbiddenException::class.java) {
                runBlocking {
                    useCases.terminateAffiliate(managerPrincipal, created.affiliateId, "Manager trying to terminate")
                }
            }

            // Admin terminating succeeds
            val terminated = useCases.terminateAffiliate(adminPrincipal, created.affiliateId, "Admin terminated")
            assertEquals("TERMINATED", terminated.status)
        }
    }

    @Test
    fun `test REST endpoints via Router`() {
        runBlocking {
            // 1. Create affiliate via POST /api/v1/affiliates
            val createReq = HttpRequest(
                method = "POST",
                path = "/api/v1/affiliates",
                headers = mapOf("Authorization" to adminToken),
                body = mapOf(
                    "userId" to "router-user-01",
                    "displayName" to "Router Affiliate",
                    "affiliateCode" to "ROUTER_AFF_01",
                    "affiliateType" to "INDIVIDUAL"
                )
            )
            val createResp = router.handleRequest(createReq)
            assertEquals(201, createResp.statusCode)

            // 2. Overview via GET /api/v1/affiliates/overview
            val overviewReq = HttpRequest(
                method = "GET",
                path = "/api/v1/affiliates/overview",
                headers = mapOf("Authorization" to adminToken)
            )
            val overviewResp = router.handleRequest(overviewReq)
            assertEquals(200, overviewResp.statusCode)

            // 3. Lookup by code via GET /api/v1/affiliates/code/ROUTER_AFF_01
            val lookupReq = HttpRequest(
                method = "GET",
                path = "/api/v1/affiliates/code/ROUTER_AFF_01",
                headers = mapOf("Authorization" to adminToken)
            )
            val lookupResp = router.handleRequest(lookupReq)
            assertEquals(200, lookupResp.statusCode)
        }
    }
}

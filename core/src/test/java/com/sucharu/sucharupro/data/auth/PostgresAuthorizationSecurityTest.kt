package com.sucharu.sucharupro.data.auth

import com.sucharu.sucharupro.data.api.client.DirectBackendApiClient
import com.sucharu.sucharupro.data.api.client.InMemoryAuthTokenStorage
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.auth.authorization.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.auth.persistence.FakeAuthAccountDataSource
import com.sucharu.sucharupro.data.auth.persistence.FakeAuthAuditDataSource
import com.sucharu.sucharupro.data.auth.persistence.FakeAuthSessionDataSource
import com.sucharu.sucharupro.data.auth.security.AuthConfig
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.auth.service.AuthenticationService
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerCreditProfile
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.concurrent.ConcurrentHashMap

/**
 * Comprehensive Authorization, RBAC/ABAC, Capability Matrix & Security Validation Suite (INFRA-03 Step 02).
 */
class PostgresAuthorizationSecurityTest {

    private lateinit var mockProvider: MockAuthzConnectionProvider
    private lateinit var transactionManager: TransactionManager
    private lateinit var repositoryFactory: PostgresRepositoryFactory
    private lateinit var accountDataSource: FakeAuthAccountDataSource
    private lateinit var sessionDataSource: FakeAuthSessionDataSource
    private lateinit var auditDataSource: FakeAuthAuditDataSource
    private lateinit var authConfig: AuthConfig
    private lateinit var jwtProvider: JwtTokenProvider
    private lateinit var authService: AuthenticationService
    private lateinit var securityContext: BackendSecurityContext
    private lateinit var authorizationService: BackendAuthorizationService
    private lateinit var server: BackendApiServer
    private lateinit var client: DirectBackendApiClient
    private lateinit var tokenStorage: InMemoryAuthTokenStorage

    private lateinit var guestPrincipal: AuthenticatedPrincipal
    private lateinit var customerAPrincipal: AuthenticatedPrincipal
    private lateinit var customerBPrincipal: AuthenticatedPrincipal
    private lateinit var affiliateAPrincipal: AuthenticatedPrincipal
    private lateinit var affiliateBPrincipal: AuthenticatedPrincipal
    private lateinit var staffPrincipal: AuthenticatedPrincipal
    private lateinit var managerPrincipal: AuthenticatedPrincipal
    private lateinit var adminPrincipal: AuthenticatedPrincipal
    private lateinit var aiAgentPrincipal: AuthenticatedPrincipal

    @Before
    fun setUp() {
        runBlocking {
            mockProvider = MockAuthzConnectionProvider()
            transactionManager = DefaultPostgresTransactionManager(mockProvider)
            repositoryFactory = PostgresRepositoryFactory(transactionManager, defaultTenantId = "TENANT-001")

            accountDataSource = FakeAuthAccountDataSource()
            sessionDataSource = FakeAuthSessionDataSource()
            auditDataSource = FakeAuthAuditDataSource()

            authConfig = AuthConfig(
                accessTokenTtlSeconds = 300L,
                refreshTokenTtlSeconds = 3600L,
                jwtIssuer = "sucharu-authz-test",
                jwtAudience = "sucharu-authz-audience",
                jwtKeyId = "authz-kid-1",
                jwtSigningSecret = "sucharu_authz_super_secure_testing_secret_key_2026",
                maxLoginAttempts = 5,
                accountLockDurationSeconds = 900L
            )

            jwtProvider = JwtTokenProvider(authConfig)
            authService = AuthenticationService(
                accountDataSource = accountDataSource,
                sessionDataSource = sessionDataSource,
                auditDataSource = auditDataSource,
                jwtProvider = jwtProvider,
                config = authConfig
            )

            authorizationService = BackendAuthorizationService(auditDataSource = auditDataSource)
            securityContext = BackendSecurityContext(jwtTokenProvider = jwtProvider)

            server = BackendApiServer(
                connectionProvider = mockProvider,
                transactionManager = transactionManager,
                repositoryFactory = repositoryFactory,
                securityContext = securityContext,
                authService = authService
            )
            server.start()

            tokenStorage = InMemoryAuthTokenStorage()
            client = DirectBackendApiClient(server = server, tokenStorage = tokenStorage)

            // Setup Principals
            guestPrincipal = AuthenticatedPrincipal(
                userId = "GUEST-000",
                projectId = "TENANT-001",
                username = "guest_user",
                role = UserRole.GUEST,
                permissions = setOf(UserPermission.READ_PUBLIC)
            )

            customerAPrincipal = AuthenticatedPrincipal(
                userId = "CUST-100",
                projectId = "TENANT-001",
                username = "customer_alice",
                role = UserRole.CUSTOMER,
                customerId = "CUST-100",
                permissions = setOf(
                    UserPermission.READ_OWN_PROFILE,
                    UserPermission.READ_OWN_ORDERS,
                    UserPermission.CREATE_ORDER,
                    UserPermission.READ_OWN_INVOICES
                )
            )

            customerBPrincipal = AuthenticatedPrincipal(
                userId = "CUST-200",
                projectId = "TENANT-001",
                username = "customer_bob",
                role = UserRole.CUSTOMER,
                customerId = "CUST-200",
                permissions = setOf(UserPermission.READ_OWN_PROFILE, UserPermission.READ_OWN_ORDERS)
            )

            affiliateAPrincipal = AuthenticatedPrincipal(
                userId = "AFF-100",
                projectId = "TENANT-001",
                username = "affiliate_alpha",
                role = UserRole.AFFILIATE,
                affiliateId = "AFF-100",
                permissions = setOf(UserPermission.READ_OWN_PROFILE, UserPermission.READ_OWN_AFFILIATE)
            )

            affiliateBPrincipal = AuthenticatedPrincipal(
                userId = "AFF-200",
                projectId = "TENANT-001",
                username = "affiliate_beta",
                role = UserRole.AFFILIATE,
                affiliateId = "AFF-200",
                permissions = setOf(UserPermission.READ_OWN_PROFILE, UserPermission.READ_OWN_AFFILIATE)
            )

            staffPrincipal = AuthenticatedPrincipal(
                userId = "STAFF-100",
                projectId = "TENANT-001",
                username = "staff_member",
                role = UserRole.STAFF,
                staffId = "STAFF-100",
                permissions = setOf(
                    UserPermission.READ_OWN_PROFILE,
                    UserPermission.MANAGE_ORDERS,
                    UserPermission.MANAGE_CUSTOMERS
                )
            )

            managerPrincipal = AuthenticatedPrincipal(
                userId = "MGR-100",
                projectId = "TENANT-001",
                username = "operations_manager",
                role = UserRole.MANAGER,
                staffId = "MGR-100",
                permissions = setOf(
                    UserPermission.READ_OWN_PROFILE,
                    UserPermission.MANAGE_ORDERS,
                    UserPermission.MANAGE_CUSTOMERS,
                    UserPermission.MANAGE_FINANCE,
                    UserPermission.MANAGE_QC
                )
            )

            adminPrincipal = AuthenticatedPrincipal(
                userId = "ADMIN-001",
                projectId = "TENANT-001",
                username = "admin_master",
                role = UserRole.ADMIN,
                permissions = setOf(UserPermission.ADMIN_ALL)
            )

            aiAgentPrincipal = AuthenticatedPrincipal(
                userId = "AGENT-999",
                projectId = "TENANT-001",
                username = "ai_assistant_agent",
                role = UserRole.AI_AGENT,
                principalType = PrincipalType.AI_AGENT,
                agentId = "AGENT-999"
            )
        }
    }

    // =========================================================================
    // 1. PUBLIC ACCESS BOUNDARY TESTS
    // =========================================================================

    @Test
    fun test01_publicResourceAccess_allowedForGuest() {
        val context = AuthorizationContext(
            principal = guestPrincipal,
            requiredCapability = AuthorizationCapability.PUBLIC_READ_PRODUCTS,
            action = AuthorizationAction.READ,
            sensitivity = ActionSensitivity.PUBLIC
        )
        val decision = authorizationService.evaluate(context)
        assertTrue(decision.isAllowed)
    }

    @Test
    fun test02_privateResourceAccess_deniedForGuest() {
        val context = AuthorizationContext(
            principal = guestPrincipal,
            requiredCapability = AuthorizationCapability.READ_OWN_ORDERS,
            action = AuthorizationAction.READ
        )
        val decision = authorizationService.evaluate(context)
        assertTrue(decision.isDenied)
        val deny = decision as AuthorizationDecision.Deny
        assertEquals(DenialReasonCode.MISSING_CAPABILITY, deny.reasonCode)
    }

    // =========================================================================
    // 2. RBAC CAPABILITY MATRIX TESTS
    // =========================================================================

    @Test
    fun test03_roleCapabilityMatrix_evaluatesRoleMappings() {
        assertTrue(RoleCapabilityMatrix.hasCapability(UserRole.CUSTOMER, AuthorizationCapability.CREATE_ORDER))
        assertFalse(RoleCapabilityMatrix.hasCapability(UserRole.CUSTOMER, AuthorizationCapability.STAFF_UPDATE_INVENTORY))
        assertTrue(RoleCapabilityMatrix.hasCapability(UserRole.STAFF, AuthorizationCapability.STAFF_READ_CUSTOMERS))
        assertFalse(RoleCapabilityMatrix.hasCapability(UserRole.STAFF, AuthorizationCapability.ADMIN_MANAGE_USERS))
        assertTrue(RoleCapabilityMatrix.hasCapability(UserRole.MANAGER, AuthorizationCapability.MANAGER_APPROVE_ORDER))
        assertTrue(RoleCapabilityMatrix.hasCapability(UserRole.ADMIN, AuthorizationCapability.ADMIN_MANAGE_PERMISSIONS))
    }

    // =========================================================================
    // 3. HORIZONTAL CUSTOMER ISOLATION TESTS
    // =========================================================================

    @Test
    fun test04_customerOwnership_customerACannotAccessCustomerBData() {
        val context = AuthorizationContext(
            principal = customerAPrincipal,
            requiredCapability = AuthorizationCapability.READ_OWN_ORDERS,
            targetCustomerId = "CUST-200" // Customer B's ID
        )
        val decision = authorizationService.evaluate(context)
        assertTrue(decision.isDenied)
        val deny = decision as AuthorizationDecision.Deny
        assertEquals(DenialReasonCode.CUSTOMER_OWNERSHIP_VIOLATION, deny.reasonCode)
    }

    @Test
    fun test05_customerOwnership_customerCanAccessOwnData() {
        val context = AuthorizationContext(
            principal = customerAPrincipal,
            requiredCapability = AuthorizationCapability.READ_OWN_ORDERS,
            targetCustomerId = "CUST-100" // Customer A's own ID
        )
        val decision = authorizationService.evaluate(context)
        assertTrue(decision.isAllowed)
    }

    // =========================================================================
    // 4. HORIZONTAL AFFILIATE ISOLATION TESTS
    // =========================================================================

    @Test
    fun test06_affiliateOwnership_affiliateACannotAccessAffiliateBCommissions() {
        val context = AuthorizationContext(
            principal = affiliateAPrincipal,
            requiredCapability = AuthorizationCapability.READ_OWN_COMMISSIONS,
            targetAffiliateId = "AFF-200" // Affiliate B's ID
        )
        val decision = authorizationService.evaluate(context)
        assertTrue(decision.isDenied)
        val deny = decision as AuthorizationDecision.Deny
        assertEquals(DenialReasonCode.AFFILIATE_OWNERSHIP_VIOLATION, deny.reasonCode)
    }

    @Test
    fun test07_affiliateOwnership_affiliateCanAccessOwnCommissions() {
        val context = AuthorizationContext(
            principal = affiliateAPrincipal,
            requiredCapability = AuthorizationCapability.READ_OWN_COMMISSIONS,
            targetAffiliateId = "AFF-100"
        )
        val decision = authorizationService.evaluate(context)
        assertTrue(decision.isAllowed)
    }

    // =========================================================================
    // 5. VERTICAL PRIVILEGE ESCALATION DEFENSE TESTS
    // =========================================================================

    @Test
    fun test08_verticalEscalation_customerCannotExecuteStaffOrAdminOperations() {
        val staffContext = AuthorizationContext(
            principal = customerAPrincipal,
            requiredCapability = AuthorizationCapability.STAFF_UPDATE_ORDERS
        )
        val adminContext = AuthorizationContext(
            principal = customerAPrincipal,
            requiredCapability = AuthorizationCapability.ADMIN_MANAGE_USERS
        )
        assertTrue(authorizationService.evaluate(staffContext).isDenied)
        assertTrue(authorizationService.evaluate(adminContext).isDenied)
    }

    @Test
    fun test09_verticalEscalation_staffCannotExecuteManagerApprovalsWithoutRole() {
        val approvalContext = AuthorizationContext(
            principal = staffPrincipal,
            requiredCapability = AuthorizationCapability.MANAGER_APPROVE_PAYMENT,
            action = AuthorizationAction.APPROVE,
            isApprovalAction = true
        )
        val decision = authorizationService.evaluate(approvalContext)
        assertTrue(decision.isDenied)
    }

    @Test
    fun test10_verticalEscalation_managerCannotExecuteAdminSystemConfiguration() {
        val sysConfigContext = AuthorizationContext(
            principal = managerPrincipal,
            requiredCapability = AuthorizationCapability.ADMIN_MANAGE_SYSTEM_CONFIGURATION
        )
        val decision = authorizationService.evaluate(sysConfigContext)
        assertTrue(decision.isDenied)
    }

    // =========================================================================
    // 6. MULTI-TENANT ISOLATION TESTS
    // =========================================================================

    @Test
    fun test11_tenantIsolation_crossTenantOperationDenied() {
        val context = AuthorizationContext(
            principal = customerAPrincipal, // Belongs to TENANT-001
            requiredCapability = AuthorizationCapability.READ_OWN_ORDERS,
            targetProjectId = "TENANT-002" // Target is Tenant B
        )
        val decision = authorizationService.evaluate(context)
        assertTrue(decision.isDenied)
        val deny = decision as AuthorizationDecision.Deny
        assertEquals(DenialReasonCode.TENANT_MISMATCH, deny.reasonCode)
    }

    // =========================================================================
    // 7. AI AGENT TOOL AUTHORIZATION TESTS
    // =========================================================================

    @Test
    fun test12_aiAgent_explicitReadToolAllowed() {
        val context = AuthorizationContext(
            principal = aiAgentPrincipal,
            requiredCapability = AuthorizationCapability.AI_READ_ORDER_STATUS,
            action = AuthorizationAction.READ,
            sensitivity = ActionSensitivity.LOW
        )
        val decision = authorizationService.evaluate(context)
        assertTrue(decision.isAllowed)
    }

    @Test
    fun test13_aiAgent_unregisteredAdminToolDenied() {
        val context = AuthorizationContext(
            principal = aiAgentPrincipal,
            requiredCapability = AuthorizationCapability.ADMIN_MANAGE_USERS,
            action = AuthorizationAction.DELETE
        )
        val decision = authorizationService.evaluate(context)
        assertTrue(decision.isDenied)
    }

    @Test
    fun test14_aiAgent_criticalActionRequiresConfirmationDenied() {
        val context = AuthorizationContext(
            principal = aiAgentPrincipal,
            requiredCapability = AuthorizationCapability.AI_CREATE_ORDER,
            action = AuthorizationAction.CREATE,
            sensitivity = ActionSensitivity.CRITICAL
        )
        val decision = authorizationService.evaluate(context)
        assertTrue(decision.isDenied)
        val deny = decision as AuthorizationDecision.Deny
        assertEquals(DenialReasonCode.UNAUTHORIZED_AI_TOOL, deny.reasonCode)
    }

    // =========================================================================
    // 8. ANTI-SPOOFING TESTS
    // =========================================================================

    @Test
    fun test15_antiSpoofing_clientRoleClaimIgnoredInFavorOfServerPrincipal() {
        try {
            BackendAuthorizationPolicy.requireRole(customerAPrincipal, UserRole.ADMIN)
            fail("Expected ForbiddenException")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Access denied") == true)
        }
    }

    @Test
    fun test16_antiSpoofing_clientCustomerIdSpoofingBlocked() {
        try {
            BackendAuthorizationPolicy.enforceCustomerOwnership(customerAPrincipal, "CUST-BOB-999")
            fail("Expected ForbiddenException")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("customer 'CUST-BOB-999'") == true)
        }
    }

    @Test
    fun test17_antiSpoofing_clientAffiliateIdSpoofingBlocked() {
        try {
            BackendAuthorizationPolicy.enforceAffiliateOwnership(affiliateAPrincipal, "AFF-BETA-888")
            fail("Expected ForbiddenException")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("affiliate 'AFF-BETA-888'") == true)
        }
    }

    // =========================================================================
    // 9. AUDIT & ERROR CONTRACT TESTS
    // =========================================================================

    @Test
    fun test18_authorizationAudit_logsAllowAndDenyEvents() {
        runBlocking {
            val allowCtx = AuthorizationContext(
                principal = customerAPrincipal,
                requiredCapability = AuthorizationCapability.READ_OWN_ORDERS,
                targetCustomerId = "CUST-100"
            )
            val denyCtx = AuthorizationContext(
                principal = customerAPrincipal,
                requiredCapability = AuthorizationCapability.ADMIN_MANAGE_USERS
            )

            authorizationService.authorize(allowCtx)

            try {
                authorizationService.authorize(denyCtx)
                fail("Expected ForbiddenException")
            } catch (_: ForbiddenException) {}

            val auditEvents = auditDataSource.queryAuditEvents("TENANT-001", "CUST-100", 50)
            assertTrue(auditEvents.isNotEmpty())
        }
    }

    @Test
    fun test19_errorContract_sanitizedForbiddenAndUnauthenticatedExceptions() {
        try {
            authorizationService.requireRole(guestPrincipal, UserRole.CUSTOMER)
            fail("Expected ForbiddenException")
        } catch (e: ForbiddenException) {
            assertEquals("Access denied: Role 'GUEST' is not authorized for this resource.", e.message)
        }
    }

    @Test
    fun test20_propertyInvariant_denyByDefaultOnNullPrincipal() {
        val context = AuthorizationContext(
            principal = null,
            requiredCapability = AuthorizationCapability.READ_OWN_ORDERS
        )
        val decision = authorizationService.evaluate(context)
        assertTrue(decision.isDenied)
        val deny = decision as AuthorizationDecision.Deny
        assertEquals(DenialReasonCode.UNAUTHENTICATED, deny.reasonCode)
    }
}

/**
 * Mock Connection Provider for Authorization Integration Testing.
 */
class MockAuthzConnectionProvider : PostgresConnectionProvider {
    var isClosed = false

    override fun getActiveConnectionCount(): Int = 0
    override fun getIdleConnectionCount(): Int = 1
    override fun getTotalAcquisitions(): Long = 1L
    override fun getAcquisitionFailureCount(): Long = 0L

    override suspend fun shutdownGracefully(drainTimeoutMs: Long) {
        isClosed = true
    }

    override fun close() {
        isClosed = true
    }

    override suspend fun acquireConnection(): Connection {
        return java.lang.reflect.Proxy.newProxyInstance(
            Connection::class.java.classLoader,
            arrayOf(Connection::class.java),
            java.lang.reflect.InvocationHandler { _, method, args ->
                when (method.name) {
                    "prepareStatement" -> createMockPreparedStatement()
                    "setAutoCommit", "commit", "rollback", "close" -> null
                    "isClosed" -> isClosed
                    "isValid" -> true
                    else -> null
                }
            }
        ) as Connection
    }

    override suspend fun releaseConnection(connection: Connection) {}

    private fun createMockPreparedStatement(): PreparedStatement {
        return java.lang.reflect.Proxy.newProxyInstance(
            PreparedStatement::class.java.classLoader,
            arrayOf(PreparedStatement::class.java),
            java.lang.reflect.InvocationHandler { _, method, _ ->
                when (method.name) {
                    "setString", "setObject", "setBigDecimal", "setInt", "setLong", "setBoolean", "setTimestamp", "setNull" -> null
                    "execute", "executeUpdate" -> 1
                    "executeQuery" -> createMockResultSet()
                    "close" -> null
                    else -> null
                }
            }
        ) as PreparedStatement
    }

    private fun createMockResultSet(): ResultSet {
        return java.lang.reflect.Proxy.newProxyInstance(
            ResultSet::class.java.classLoader,
            arrayOf(ResultSet::class.java),
            java.lang.reflect.InvocationHandler { _, method, _ ->
                when (method.name) {
                    "next" -> false
                    "close" -> null
                    else -> null
                }
            }
        ) as ResultSet
    }
}

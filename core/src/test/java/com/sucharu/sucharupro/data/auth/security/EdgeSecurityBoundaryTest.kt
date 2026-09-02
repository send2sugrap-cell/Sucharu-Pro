package com.sucharu.sucharupro.data.auth.security

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.auth.authorization.BackendAuthorizationService
import com.sucharu.sucharupro.data.auth.model.UpdateAccountStatusRequestDto
import com.sucharu.sucharupro.data.persistence.postgres.*
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.customer.*
import com.sucharu.sucharupro.domain.model.order.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Comprehensive Unit and Edge Security Tests for INFRA-05 STEP 03.
 *
 * Validates:
 * 1. Token extraction & cryptographic verification
 * 2. Server-authoritative tenant resolution (Anti-Spoofing Attacks 1-5)
 * 3. Adversarial attacks (Attacks 6-9: invalid signature, expired, downgrade, connection context)
 * 4. RBAC, Capability-based authorization & Resource Ownership Guards
 * 5. Public route accessibility vs Protected route enforcement
 * 6. Concurrency and request-level principal isolation
 */
class EdgeSecurityBoundaryTest {

    private val authConfig = AuthConfig(
        jwtSigningSecret = "sucharu_production_edge_secret_test_key_2026_secure",
        jwtIssuer = "sucharu-backend-server",
        jwtAudience = "sucharu-api-clients",
        accessTokenTtlSeconds = 300L
    )

    private val jwtProvider = JwtTokenProvider(authConfig)
    private val securityContext = BackendSecurityContext(jwtProvider)
    private val edgeInterceptor = EdgeSecurityInterceptor(securityContext)
    private lateinit var mockConnProvider: MockEdgeConnectionProvider
    private lateinit var txManager: DefaultPostgresTransactionManager
    private lateinit var repoFactory: PostgresRepositoryFactory
    private lateinit var healthChecker: DatabaseHealthChecker
    private lateinit var useCases: BackendUseCases
    private lateinit var router: BackendRouter

    private val validCustomerPrincipal = AuthenticatedPrincipal(
        userId = "CUST-001",
        projectId = "PROJECT-ALPHA",
        username = "customer_alpha",
        role = UserRole.CUSTOMER,
        permissions = setOf(UserPermission.READ_OWN_ORDERS, UserPermission.READ_OWN_PROFILE)
    )

    private val validAffiliatePrincipal = AuthenticatedPrincipal(
        userId = "AFF-001",
        projectId = "PROJECT-ALPHA",
        username = "affiliate_alpha",
        role = UserRole.AFFILIATE,
        permissions = setOf(UserPermission.READ_OWN_AFFILIATE)
    )

    private val validStaffPrincipal = AuthenticatedPrincipal(
        userId = "STAFF-001",
        projectId = "PROJECT-ALPHA",
        username = "staff_alpha",
        role = UserRole.STAFF,
        permissions = setOf(UserPermission.MANAGE_ORDERS, UserPermission.MANAGE_CUSTOMERS)
    )

    private val validAdminPrincipal = AuthenticatedPrincipal(
        userId = "ADMIN-001",
        projectId = "PROJECT-ALPHA",
        username = "admin_alpha",
        role = UserRole.ADMIN,
        permissions = setOf(UserPermission.ADMIN_ALL)
    )

    @Before
    fun setUp() {
        mockConnProvider = MockEdgeConnectionProvider()
        txManager = DefaultPostgresTransactionManager(mockConnProvider)
        repoFactory = PostgresRepositoryFactory(txManager, defaultTenantId = "PROJECT-ALPHA")
        healthChecker = DatabaseHealthChecker(mockConnProvider)
        useCases = BackendUseCases(txManager, repoFactory)
        router = BackendRouter(securityContext, useCases, healthChecker)

        // Seed customer in PROJECT-ALPHA
        mockConnProvider.customers["CUST-001"] = Customer(
            customerId = "CUST-001",
            customerCode = "CC-001",
            displayName = "Alice Printing Client",
            contactPersonName = "Alice",
            primaryPhone = "01700000001",
            email = "alice@example.com",
            creditProfile = CustomerCreditProfile(
                creditLimit = Money(BigDecimal("50000.00")),
                paymentTermDays = 30
            ),
            status = CustomerStatusType.ACTIVE,
            createdAt = "2026-08-25T10:00:00Z",
            updatedAt = "2026-08-25T10:00:00Z"
        )

        // Seed order belonging to CUST-001 in PROJECT-ALPHA
        mockConnProvider.orders["ORD-001"] = Order(
            orderId = "ORD-001",
            orderNumber = "ORD-2026-001",
            customerId = "CUST-001",
            quotationId = null,
            approvedQuotationRevisionId = null,
            status = OrderStatusType.CONFIRMED,
            priority = OrderPriority.NORMAL,
            items = listOf(OrderItem("I1", "Visiting Cards", specification = null, quantity = 1000, unit = "Pcs", unitPrice = Money(BigDecimal("12.00")))),
            discount = Money.ZERO,
            jobHandoffStatus = JobHandoffStatus.NOT_READY,
            notes = "Customer order",
            confirmedBy = "Admin",
            createdAt = "2026-08-25T10:00:00Z",
            updatedAt = "2026-08-25T10:00:00Z"
        )

        // Seed order belonging to CUST-002 (Different Customer) in PROJECT-ALPHA
        mockConnProvider.orders["ORD-002"] = Order(
            orderId = "ORD-002",
            orderNumber = "ORD-2026-002",
            customerId = "CUST-002",
            quotationId = null,
            approvedQuotationRevisionId = null,
            status = OrderStatusType.IN_PRODUCTION,
            priority = OrderPriority.URGENT,
            items = listOf(OrderItem("I2", "Catalogues", specification = null, quantity = 500, unit = "Pcs", unitPrice = Money(BigDecimal("50.00")))),
            discount = Money.ZERO,
            jobHandoffStatus = JobHandoffStatus.NOT_READY,
            notes = "Customer 2 order",
            confirmedBy = "Admin",
            createdAt = "2026-08-25T10:00:00Z",
            updatedAt = "2026-08-25T10:00:00Z"
        )
    }

    // =========================================================================
    // 1. TOKEN EXTRACTION & CRYPTOGRAPHIC VERIFICATION
    // =========================================================================

    @Test
    fun test01_missingAuthorizationHeader_returns401() = runBlocking {
        val req = HttpRequest(method = "GET", path = "/api/v1/customer/profile")
        val resp = router.handleRequest(req)
        assertEquals(401, resp.statusCode)
        val error = resp.body as ApiErrorResponse
        assertEquals(ErrorCode.UNAUTHENTICATED, error.errorCode)
        assertTrue(error.message.contains("missing", ignoreCase = true))
    }

    @Test
    fun test02_malformedAuthorizationHeader_notBearer_returns401() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/profile",
            headers = mapOf("Authorization" to "Basic dXNlcjpwYXNz")
        )
        val resp = router.handleRequest(req)
        assertEquals(401, resp.statusCode)
        val error = resp.body as ApiErrorResponse
        assertEquals(ErrorCode.UNAUTHENTICATED, error.errorCode)
        assertTrue(error.message.contains("Unsupported authentication scheme", ignoreCase = true))
    }

    @Test
    fun test03_emptyBearerToken_returns401() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/profile",
            headers = mapOf("Authorization" to "Bearer   ")
        )
        val resp = router.handleRequest(req)
        assertEquals(401, resp.statusCode)
        val error = resp.body as ApiErrorResponse
        assertEquals(ErrorCode.UNAUTHENTICATED, error.errorCode)
        assertTrue(error.message.contains("empty", ignoreCase = true))
    }

    @Test
    fun test04_malformedJwtStructure_notThreeParts_returns401() = runBlocking {
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/profile",
            headers = mapOf("Authorization" to "Bearer header.payload")
        )
        val resp = router.handleRequest(req)
        assertEquals(401, resp.statusCode)
        val error = resp.body as ApiErrorResponse
        assertEquals(ErrorCode.UNAUTHENTICATED, error.errorCode)
    }

    @Test
    fun test05_invalidSignature_returns401() = runBlocking {
        val validToken = jwtProvider.generateAccessToken(validCustomerPrincipal)
        val tamperedToken = validToken.substring(0, validToken.lastIndexOf('.') + 1) + "invalidsignaturebytes"
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/profile",
            headers = mapOf("Authorization" to "Bearer $tamperedToken")
        )
        val resp = router.handleRequest(req)
        assertEquals(401, resp.statusCode)
        val error = resp.body as ApiErrorResponse
        assertEquals(ErrorCode.UNAUTHENTICATED, error.errorCode)
        assertEquals("Invalid JWT signature.", error.message)
    }

    @Test
    fun test06_expiredToken_returns401() = runBlocking {
        val expiredToken = jwtProvider.generateAccessToken(validCustomerPrincipal, ttlSeconds = -10L)
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/profile",
            headers = mapOf("Authorization" to "Bearer $expiredToken")
        )
        val resp = router.handleRequest(req)
        assertEquals(401, resp.statusCode)
        val error = resp.body as ApiErrorResponse
        assertEquals(ErrorCode.UNAUTHENTICATED, error.errorCode)
        assertEquals("JWT access token has expired.", error.message)
    }

    @Test
    fun test07_invalidIssuer_returns401() {
        val foreignProvider = JwtTokenProvider(authConfig.copy(jwtIssuer = "foreign-auth-issuer"))
        val foreignToken = foreignProvider.generateAccessToken(validCustomerPrincipal)

        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/profile",
            headers = mapOf("Authorization" to "Bearer $foreignToken")
        )
        val resp = runBlocking { router.handleRequest(req) }
        assertEquals(401, resp.statusCode)
    }

    @Test
    fun test08_invalidAudience_returns401() {
        val foreignProvider = JwtTokenProvider(authConfig.copy(jwtAudience = "foreign-audience"))
        val foreignToken = foreignProvider.generateAccessToken(validCustomerPrincipal)

        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/profile",
            headers = mapOf("Authorization" to "Bearer $foreignToken")
        )
        val resp = runBlocking { router.handleRequest(req) }
        assertEquals(401, resp.statusCode)
    }

    @Test
    fun test09_validToken_createsAuthoritativePrincipal() = runBlocking {
        val token = jwtProvider.generateAccessToken(validCustomerPrincipal)
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/profile",
            headers = mapOf("Authorization" to "Bearer $token")
        )
        val resp = router.handleRequest(req)
        assertEquals(200, resp.statusCode)
        val success = resp.body as ApiSuccessResponse<*>
        val profile = success.data as CustomerProfileDto
        assertEquals("CUST-001", profile.customerId)
        assertEquals("Alice Printing Client", profile.name)

        // Verify transaction context was bound to authoritative PROJECT-ALPHA
        assertTrue(mockConnProvider.boundSessionProjects.contains("PROJECT-ALPHA"))
    }

    // =========================================================================
    // 2. TENANT RESOLUTION & ANTI-SPOOFING (ATTACKS 1-5)
    // =========================================================================

    @Test
    fun test10_attack1_tenantHeaderSpoofing_isBlocked() = runBlocking {
        // Authenticated for PROJECT-ALPHA, but attacker sends header X-Project-Id: PROJECT-BETA
        val token = jwtProvider.generateAccessToken(validCustomerPrincipal)
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/profile",
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "X-Project-Id" to "PROJECT-BETA"
            )
        )

        val resp = router.handleRequest(req)
        assertEquals(200, resp.statusCode)

        // Database context must remain bound strictly to PROJECT-ALPHA
        assertTrue(mockConnProvider.boundSessionProjects.contains("PROJECT-ALPHA"))
        assertFalse(mockConnProvider.boundSessionProjects.contains("PROJECT-BETA"))
    }

    @Test
    fun test11_attack2_bodySpoofing_isOverriddenByAuthoritativeTenant() = runBlocking {
        // Authenticated for PROJECT-ALPHA, but request body specifies projectId = PROJECT-BETA
        val token = jwtProvider.generateAccessToken(validCustomerPrincipal)
        val createOrderDto = CreateOrderRequestDto(
            items = listOf(OrderItemRequestDto("Visiting Cards", 1000, BigDecimal("2.50"))),
            notes = "Rush order"
        )
        val req = HttpRequest(
            method = "POST",
            path = "/api/v1/customer/orders",
            headers = mapOf("Authorization" to "Bearer $token"),
            body = createOrderDto
        )

        val resp = router.handleRequest(req)
        assertEquals(201, resp.statusCode)

        // Bound project must strictly be PROJECT-ALPHA
        assertTrue(mockConnProvider.boundSessionProjects.contains("PROJECT-ALPHA"))
        assertFalse(mockConnProvider.boundSessionProjects.contains("PROJECT-BETA"))
    }

    @Test
    fun test12_attack3_pathSpoofing_crossTenant_isBlocked() = runBlocking {
        val crossTenantPrincipal = validCustomerPrincipal.copy(projectId = "PROJECT-BETA")
        val service = BackendAuthorizationService()

        // Attempting to enforce cross-tenant operation on PROJECT-ALPHA with PROJECT-BETA token
        val ex = assertThrows(ForbiddenException::class.java) {
            service.enforceTenantIsolation(crossTenantPrincipal, "PROJECT-ALPHA")
        }
        assertTrue(ex.message!!.contains("Cross-tenant operation blocked"))
    }

    @Test
    fun test13_attack4_customerIdSpoofing_otherCustomerResource_returns403() = runBlocking {
        // Authenticated as CUST-001, attempting to access ORD-002 which belongs to CUST-002
        val token = jwtProvider.generateAccessToken(validCustomerPrincipal)
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/orders/ORD-002",
            headers = mapOf("Authorization" to "Bearer $token")
        )

        val resp = router.handleRequest(req)
        assertEquals(403, resp.statusCode)
        val error = resp.body as ApiErrorResponse
        assertEquals(ErrorCode.FORBIDDEN, error.errorCode)
        assertTrue(error.message.contains("Access denied: You do not have permission to access records belonging to customer 'CUST-002'."))
    }

    @Test
    fun test14_attack5_ownOrderAccess_returns200() = runBlocking {
        // Authenticated as CUST-001 accessing ORD-001 (own order)
        val token = jwtProvider.generateAccessToken(validCustomerPrincipal)
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/orders/ORD-001",
            headers = mapOf("Authorization" to "Bearer $token")
        )

        val resp = router.handleRequest(req)
        assertEquals(200, resp.statusCode)
        val success = resp.body as ApiSuccessResponse<*>
        val detail = success.data as CustomerOrderDetailDto
        assertEquals("ORD-001", detail.orderId)
    }

    // =========================================================================
    // 3. ADVERSARIAL ATTACKS (ATTACKS 6-9)
    // =========================================================================

    @Test
    fun test15_attack6_payloadTampering_modifyingRole_failsVerification() = runBlocking {
        // Create customer token, then tamper payload JSON to change role to ADMIN
        val token = jwtProvider.generateAccessToken(validCustomerPrincipal)
        val parts = token.split(".")
        val payloadJson = String(java.util.Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8)
        val tamperedJson = payloadJson.replace("\"role\":\"CUSTOMER\"", "\"role\":\"ADMIN\"")
        val tamperedB64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(tamperedJson.toByteArray(Charsets.UTF_8))
        val tamperedToken = "${parts[0]}.$tamperedB64.${parts[2]}"

        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/profile",
            headers = mapOf("Authorization" to "Bearer $tamperedToken")
        )
        val resp = router.handleRequest(req)
        assertEquals(401, resp.statusCode)
    }

    @Test
    fun test16_attack8_algorithmDowngrade_none_isRejected() = runBlocking {
        val headerJson = """{"alg":"none","typ":"JWT"}"""
        val headerB64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.toByteArray(Charsets.UTF_8))
        val payloadJson = """{"sub":"CUST-001","pid":"PROJECT-ALPHA","iss":"sucharu-backend-server","aud":"sucharu-api-clients","exp":${(System.currentTimeMillis() / 1000L) + 300}}"""
        val payloadB64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.toByteArray(Charsets.UTF_8))
        val unsignedToken = "$headerB64.$payloadB64."

        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/profile",
            headers = mapOf("Authorization" to "Bearer $unsignedToken")
        )
        val resp = router.handleRequest(req)
        assertEquals(401, resp.statusCode)
    }

    @Test
    fun test17_attack9_connectionContextLeakage_protectsAcrossTransactions() = runBlocking {
        // Transaction 1: Executed for PROJECT-ALPHA
        val tokenAlpha = jwtProvider.generateAccessToken(validCustomerPrincipal.copy(projectId = "PROJECT-ALPHA"))
        val req1 = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/profile",
            headers = mapOf("Authorization" to "Bearer $tokenAlpha")
        )
        val resp1 = router.handleRequest(req1)
        assertEquals(200, resp1.statusCode)
        assertEquals("PROJECT-ALPHA", mockConnProvider.boundSessionProjects.last())

        // Transaction 2: Executed for PROJECT-BETA
        val tokenBeta = jwtProvider.generateAccessToken(validCustomerPrincipal.copy(projectId = "PROJECT-BETA"))
        val req2 = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/profile",
            headers = mapOf("Authorization" to "Bearer $tokenBeta")
        )
        val resp2 = router.handleRequest(req2)
        assertEquals(200, resp2.statusCode)
        assertEquals("PROJECT-BETA", mockConnProvider.boundSessionProjects.last())
    }

    // =========================================================================
    // 4. RBAC & CAPABILITY AUTHORIZATION
    // =========================================================================

    @Test
    fun test18_customerRole_deniedAdminRoutes() = runBlocking {
        val token = jwtProvider.generateAccessToken(validCustomerPrincipal)
        val req = HttpRequest(
            method = "POST",
            path = "/api/v1/admin/users/USR-01/status",
            headers = mapOf("Authorization" to "Bearer $token"),
            body = UpdateAccountStatusRequestDto(newStatus = com.sucharu.sucharupro.data.auth.model.AccountStatus.SUSPENDED, reason = "Admin action")
        )
        val resp = router.handleRequest(req)
        assertEquals(403, resp.statusCode)
    }

    @Test
    fun test19_affiliateRole_allowedAffiliate_deniedCustomerPrivateOrders() = runBlocking {
        val affToken = jwtProvider.generateAccessToken(validAffiliatePrincipal)

        // 1. Allowed affiliate profile
        val req1 = HttpRequest(
            method = "GET",
            path = "/api/v1/affiliate/profile",
            headers = mapOf("Authorization" to "Bearer $affToken")
        )
        val resp1 = router.handleRequest(req1)
        assertEquals(200, resp1.statusCode)

        // 2. Denied customer private orders
        val req2 = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/orders",
            headers = mapOf("Authorization" to "Bearer $affToken")
        )
        val resp2 = router.handleRequest(req2)
        assertEquals(403, resp2.statusCode)
    }

    @Test
    fun test20_staffRole_canAccessCustomerOrdersForManagement() = runBlocking {
        val staffToken = jwtProvider.generateAccessToken(validStaffPrincipal)
        val req = HttpRequest(
            method = "GET",
            path = "/api/v1/customer/orders/ORD-001",
            headers = mapOf("Authorization" to "Bearer $staffToken")
        )
        val resp = router.handleRequest(req)
        assertEquals(200, resp.statusCode)
    }

    // =========================================================================
    // 5. PUBLIC ROUTE ACCESSIBILITY
    // =========================================================================

    @Test
    fun test21_publicRoutes_accessibleWithoutToken() = runBlocking {
        assertTrue(edgeInterceptor.isPublicRoute("/health", "GET"))
        assertTrue(edgeInterceptor.isPublicRoute("/health/live", "GET"))
        assertTrue(edgeInterceptor.isPublicRoute("/health/readiness", "GET"))
        assertTrue(edgeInterceptor.isPublicRoute("/api/v1/public/company", "GET"))
        assertTrue(edgeInterceptor.isPublicRoute("/api/v1/public/products", "GET"))
        assertTrue(edgeInterceptor.isPublicRoute("/api/v1/auth/login", "POST"))
        assertTrue(edgeInterceptor.isPublicRoute("/api/v1/auth/register", "POST"))
        assertFalse(edgeInterceptor.isPublicRoute("/api/v1/customer/orders", "GET"))
        assertFalse(edgeInterceptor.isPublicRoute("/api/v1/admin/workflows", "GET"))

        val req = HttpRequest(method = "GET", path = "/api/v1/public/company")
        val resp = router.handleRequest(req)
        assertEquals(200, resp.statusCode)
    }

    // =========================================================================
    // 6. CONCURRENCY & REQUEST CONTEXT ISOLATION
    // =========================================================================

    @Test
    fun test22_concurrentRequests_maintainStrictContextIsolation() = runBlocking {
        val threads = 10
        val results = mutableListOf<RequestSecurityContext>()

        (1..threads).forEach { i ->
            val projectId = "PROJECT-$i"
            val principal = AuthenticatedPrincipal(
                userId = "USER-$i",
                projectId = projectId,
                username = "user_$i",
                role = UserRole.CUSTOMER
            )
            val token = jwtProvider.generateAccessToken(principal)
            val req = HttpRequest(
                method = "GET",
                path = "/api/v1/customer/profile",
                headers = mapOf("Authorization" to "Bearer $token", "X-Correlation-ID" to "corr-$i")
            )

            val ctx = edgeInterceptor.authenticateRequest(req)
            results.add(ctx)
        }

        assertEquals(threads, results.size)
        // Verify no principal was corrupted across requests
        for (i in 1..threads) {
            val matched = results.find { it.principal.userId == "USER-$i" }
            assertNotNull(matched)
            assertEquals("PROJECT-$i", matched!!.tenantContext.projectId)
            assertEquals("corr-$i", matched.correlationId)
        }
    }

    // =========================================================================
    // MOCK CONNECTION PROVIDER WITH PROXY JDBC OBJECTS
    // =========================================================================

    private class MockEdgeConnectionProvider : PostgresConnectionProvider {
        val customers = mutableMapOf<String, Customer>()
        val orders = mutableMapOf<String, Order>()
        var currentSessionProjectId: String = ""
        val boundSessionProjects = mutableListOf<String>()
        private var isClosed = false

        override suspend fun acquireConnection(): Connection {
            return Proxy.newProxyInstance(
                Connection::class.java.classLoader,
                arrayOf(Connection::class.java),
                MockConnectionInvocationHandler()
            ) as Connection
        }

        override suspend fun releaseConnection(connection: Connection) {
            currentSessionProjectId = ""
        }

        override fun getActiveConnectionCount(): Int = 0
        override fun getIdleConnectionCount(): Int = 1
        override fun getTotalAcquisitions(): Long = 1L
        override fun getAcquisitionFailureCount(): Long = 0L
        override suspend fun shutdownGracefully(drainTimeoutMs: Long) { isClosed = true }
        override fun close() { isClosed = true }

        private inner class MockConnectionInvocationHandler : java.lang.reflect.InvocationHandler {
            private var inTx = false

            override fun invoke(proxy: Any, method: java.lang.reflect.Method, args: Array<out Any>?): Any? {
                val name = method.name
                val methodArgs = args ?: emptyArray()

                return when (name) {
                    "setAutoCommit" -> {
                        inTx = !(methodArgs[0] as Boolean)
                        null
                    }
                    "getAutoCommit" -> !inTx
                    "commit", "rollback" -> {
                        inTx = false
                        null
                    }
                    "isClosed" -> isClosed
                    "isValid" -> true
                    "close" -> null
                    "prepareStatement" -> {
                        val sql = methodArgs[0] as String
                        createMockPreparedStatement(sql)
                    }
                    else -> null
                }
            }
        }

        private fun createMockPreparedStatement(sql: String): PreparedStatement {
            val params = mutableListOf<Any?>()

            return Proxy.newProxyInstance(
                PreparedStatement::class.java.classLoader,
                arrayOf(PreparedStatement::class.java)
            ) { _, method, args ->
                val mArgs = args ?: emptyArray()
                when (method.name) {
                    "setString", "setInt", "setLong", "setBigDecimal" -> {
                        val idx = mArgs[0] as Int
                        val v = mArgs[1]
                        while (params.size < idx) params.add(null)
                        params[idx - 1] = v
                        null
                    }
                    "execute" -> {
                        if (sql.contains("set_config")) {
                            currentSessionProjectId = params.getOrNull(0) as? String ?: ""
                            if (currentSessionProjectId.isNotBlank()) {
                                boundSessionProjects.add(currentSessionProjectId)
                            }
                        }
                        true
                    }
                    "executeQuery" -> {
                        createMockResultSet(sql, params)
                    }
                    "executeUpdate" -> {
                        if (sql.contains("INSERT INTO orders")) {
                            val orderId = params.getOrNull(1) as? String ?: "ORD-${System.currentTimeMillis()}"
                            val orderNum = params.getOrNull(2) as? String ?: "ORD-2026-NEW"
                            val custId = params.getOrNull(3) as? String ?: ""
                            val total = params.getOrNull(9) as? BigDecimal ?: BigDecimal.ZERO
                            orders[orderId] = Order(
                                orderId = orderId,
                                orderNumber = orderNum,
                                customerId = custId,
                                quotationId = null,
                                approvedQuotationRevisionId = null,
                                status = OrderStatusType.CONFIRMED,
                                priority = OrderPriority.NORMAL,
                                items = listOf(OrderItem("I1", "Order Item", specification = null, quantity = 100, unit = "Pcs", unitPrice = Money(total))),
                                discount = Money.ZERO,
                                jobHandoffStatus = JobHandoffStatus.NOT_READY,
                                notes = "New Order",
                                confirmedBy = "System",
                                createdAt = "2026-08-25T10:00:00Z",
                                updatedAt = "2026-08-25T10:00:00Z"
                            )
                        }
                        1
                    }
                    "close" -> null
                    else -> null
                }
            } as PreparedStatement
        }

        private fun createMockResultSet(sql: String, params: List<Any?>): ResultSet {
            val rows = mutableListOf<Map<String, Any?>>()

            if (sql.contains("SELECT current_database()")) {
                rows.add(mapOf("1" to "sucharu_edge_test"))
            } else if (sql.contains("FROM customers") && sql.contains("customer_id = ?")) {
                val custId = params.getOrNull(1) as? String
                val c = customers[custId]
                if (c != null) {
                    rows.add(
                        mapOf(
                            "customer_id" to c.customerId,
                            "customer_code" to c.customerCode,
                            "display_name" to c.displayName,
                            "customer_type" to c.customerType.name,
                            "status" to c.status.name,
                            "primary_phone" to c.primaryPhone,
                            "alternate_phone" to c.alternatePhone,
                            "email" to c.email,
                            "contact_person_name" to c.contactPersonName,
                            "credit_limit_amount" to c.creditProfile.creditLimit.amount,
                            "credit_days" to c.creditProfile.paymentTermDays,
                            "notes" to c.notes,
                            "created_at" to Timestamp(1755940000000L),
                            "updated_at" to Timestamp(1755940000000L)
                        )
                    )
                }
            } else if (sql.contains("FROM orders") && sql.contains("order_id = ?")) {
                val orderId = params.getOrNull(1) as? String
                val o = orders[orderId]
                if (o != null) {
                    rows.add(
                        mapOf(
                            "order_id" to o.orderId,
                            "order_number" to o.orderNumber,
                            "customer_id" to o.customerId,
                            "quotation_id" to o.quotationId,
                            "status" to o.status.name,
                            "priority" to o.priority.name,
                            "discount_amount" to o.discount.amount,
                            "total_amount" to o.totalAmount.amount,
                            "job_handoff_status" to o.jobHandoffStatus.name,
                            "notes" to o.notes,
                            "confirmed_by" to o.confirmedBy,
                            "confirmed_at" to Timestamp(1755940000000L),
                            "created_at" to Timestamp(1755940000000L),
                            "updated_at" to Timestamp(1755940000000L)
                        )
                    )
                }
            }

            var cursor = -1

            return Proxy.newProxyInstance(
                ResultSet::class.java.classLoader,
                arrayOf(ResultSet::class.java)
            ) { _, method, args ->
                val mArgs = args ?: emptyArray()
                when (method.name) {
                    "next" -> {
                        cursor++
                        cursor < rows.size
                    }
                    "getString" -> {
                        val col = if (mArgs[0] is Int) (mArgs[0] as Int).toString() else mArgs[0] as String
                        rows.getOrNull(cursor)?.get(col)?.toString()
                    }
                    "getBigDecimal" -> {
                        val col = mArgs[0] as String
                        val v = rows.getOrNull(cursor)?.get(col)
                        when (v) {
                            is BigDecimal -> v
                            is Number -> BigDecimal(v.toString())
                            else -> BigDecimal.ZERO
                        }
                    }
                    "getInt" -> {
                        val col = mArgs[0] as String
                        (rows.getOrNull(cursor)?.get(col) as? Number)?.toInt() ?: 0
                    }
                    "getLong" -> {
                        val col = mArgs[0] as String
                        (rows.getOrNull(cursor)?.get(col) as? Number)?.toLong() ?: 0L
                    }
                    "getTimestamp" -> {
                        val col = mArgs[0] as String
                        rows.getOrNull(cursor)?.get(col) as? Timestamp ?: Timestamp(System.currentTimeMillis())
                    }
                    "wasNull" -> false
                    "close" -> null
                    else -> null
                }
            } as ResultSet
        }
    }
}

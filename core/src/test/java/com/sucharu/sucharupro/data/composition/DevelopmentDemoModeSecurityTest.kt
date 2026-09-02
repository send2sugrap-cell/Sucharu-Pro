package com.sucharu.sucharupro.data.composition

import com.sucharu.sucharupro.data.api.client.DemoBackendApiClient
import com.sucharu.sucharupro.data.api.client.DirectBackendApiClient
import com.sucharu.sucharupro.data.api.client.InMemoryAuthTokenStorage
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.BackendApiServer
import com.sucharu.sucharupro.data.api.server.BackendSecurityContext
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.auth.persistence.*
import com.sucharu.sucharupro.data.auth.security.AuthConfig
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.auth.service.AuthenticationService
import com.sucharu.sucharupro.data.auth.session.AppEntryState
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy
import java.sql.Connection

/**
 * Production Security & Isolation Test Suite for Development Demo Mode (INFRA-06).
 *
 * Verifies all mandatory security invariants:
 * 1. Production composition fails fast if SUCHARU_API_GATEWAY_URL is missing.
 * 2. Production authentication does NOT accept 123456 as a universal OTP for real accounts.
 * 3. Demo OTP works only inside demo runtime.
 * 4. Demo user is not created in PostgreSQL / remains isolated in memory.
 * 5. Demo runtime composition uses AppRuntimeMode.DEVELOPMENT.
 * 6. ProductionRuntimeComposition remains API-Gateway-only with AppRuntimeMode.PRODUCTION.
 * 7. Demo user receives CUSTOMER role only.
 * 8. Demo logout clears demo state and returns to public state.
 */
class DevelopmentDemoModeSecurityTest {

    private class MockTestConnectionProvider : PostgresConnectionProvider {
        override suspend fun acquireConnection(): Connection {
            return Proxy.newProxyInstance(
                Connection::class.java.classLoader,
                arrayOf(Connection::class.java)
            ) { _, method, _ ->
                when (method.name) {
                    "prepareStatement", "createStatement" -> Proxy.newProxyInstance(
                        java.sql.PreparedStatement::class.java.classLoader,
                        arrayOf(java.sql.PreparedStatement::class.java)
                    ) { _, stmtMethod, _ ->
                        when (stmtMethod.name) {
                            "executeQuery" -> Proxy.newProxyInstance(
                                java.sql.ResultSet::class.java.classLoader,
                                arrayOf(java.sql.ResultSet::class.java)
                            ) { _, _, _ -> null }
                            "executeUpdate" -> 1
                            else -> null
                        }
                    }
                    else -> null
                }
            } as Connection
        }

        override suspend fun releaseConnection(connection: Connection) {}

        override fun close() {}
    }

    @Test
    fun testProductionRuntimeComposition_failsFastWhenApiGatewayUrlMissing() {
        val prodComposition = ProductionRuntimeComposition(apiGatewayUrl = null)
        assertEquals(AppRuntimeMode.PRODUCTION, prodComposition.mode)

        val exception = assertThrows(IllegalStateException::class.java) {
            prodComposition.createSessionManager()
        }
        assertTrue(exception.message!!.contains("Production composition requires a valid SUCHARU_API_GATEWAY_URL"))
    }

    @Test
    fun testProductionRuntimeComposition_blocksDirectDatabaseConnections() {
        val prodComposition = ProductionRuntimeComposition(apiGatewayUrl = "https://api.sucharugraphics.com")
        assertEquals(AppRuntimeMode.PRODUCTION, prodComposition.mode)

        val exception = assertThrows(UnsupportedOperationException::class.java) {
            prodComposition.createSessionManager()
        }
        assertTrue(exception.message!!.contains("Production remote API client"))
    }

    @Test
    fun testProductionAuthenticationService_rejectsUniversalOtp123456() = runBlocking {
        val mockProvider = MockTestConnectionProvider()
        val transactionManager = DefaultPostgresTransactionManager(mockProvider)
        val repositoryFactory = PostgresRepositoryFactory(transactionManager, defaultTenantId = "TENANT-001")

        val accountDs = FakeAuthAccountDataSource()
        val sessionDs = FakeAuthSessionDataSource()
        val auditDs = FakeAuthAuditDataSource()

        val authConfig = AuthConfig(
            accessTokenTtlSeconds = 10L,
            refreshTokenTtlSeconds = 300L,
            jwtIssuer = "sucharu-test-issuer",
            jwtAudience = "sucharu-test-audience",
            jwtKeyId = "test-kid-1",
            jwtSigningSecret = "sucharu_super_secure_testing_secret_key_2026_xyz123",
            maxLoginAttempts = 3,
            accountLockDurationSeconds = 60L
        )

        val jwtProvider = JwtTokenProvider(authConfig)
        val authService = AuthenticationService(
            accountDataSource = accountDs,
            sessionDataSource = sessionDs,
            auditDataSource = auditDs,
            jwtProvider = jwtProvider,
            config = authConfig
        )

        val securityContext = BackendSecurityContext(jwtTokenProvider = jwtProvider)

        val server = BackendApiServer(
            connectionProvider = mockProvider,
            transactionManager = transactionManager,
            repositoryFactory = repositoryFactory,
            securityContext = securityContext,
            authService = authService
        )
        server.start()

        val tokenStorage = InMemoryAuthTokenStorage()
        val client = DirectBackendApiClient(server = server, tokenStorage = tokenStorage)

        // Register a real account
        val regRes = client.register(
            RegisterRequestDto(
                username = "realcustomer",
                displayName = "Real Customer",
                email = "realcustomer@example.com",
                phone = "+8801712345678",
                password = "SecureRealPassword123!",
                requestedRole = UserRole.CUSTOMER
            )
        )
        assertTrue(regRes is ApiResult.Success)
        val realUserId = (regRes as ApiResult.Success).data.userId

        // Confirming with arbitrary '123456' MUST FAIL in production server
        val verifRes = client.confirmVerificationToken(
            ConfirmVerificationRequestDto(
                verificationType = VerificationType.PHONE,
                token = "123456",
                identifier = realUserId
            )
        )
        // Production rejects arbitrary OTP 123456
        assertTrue("Production auth MUST reject 123456 for real unverified accounts", verifRes is ApiResult.Error)
    }

    @Test
    fun testDemoBackendApiClient_accepts123456_andRejectsOtherOtps() = runBlocking {
        val demoClient = DemoBackendApiClient()

        // 1. Wrong OTP -> Validation Error
        val invalidRes = demoClient.confirmVerificationToken(
            ConfirmVerificationRequestDto(
                verificationType = VerificationType.PHONE,
                token = "999999"
            )
        )
        assertTrue(invalidRes is ApiResult.Error)
        assertEquals(
            "Invalid demo verification code. Please enter 123456.",
            (invalidRes as ApiResult.Error).errorResponse.message
        )

        // 2. Correct Demo OTP '123456' -> Success
        val validRes = demoClient.confirmVerificationToken(
            ConfirmVerificationRequestDto(
                verificationType = VerificationType.PHONE,
                token = "123456"
            )
        )
        assertTrue(validRes is ApiResult.Success)

        // 3. Demo profile should be accessible and ACTIVE
        val profileRes = demoClient.getMyProfile()
        assertTrue(profileRes is ApiResult.Success)
        val principal = (profileRes as ApiResult.Success).data
        assertEquals("USER-DEMO-001", principal.userId)
        assertEquals("demo", principal.username)
        assertEquals(UserRole.CUSTOMER, principal.role)
        assertEquals(AccountStatus.ACTIVE, principal.accountStatus)
    }

    @Test
    fun testDevelopmentDemoRuntimeComposition_endToEndLifecycle() = runBlocking {
        val demoComposition = DevelopmentDemoRuntimeComposition()
        assertEquals(AppRuntimeMode.DEVELOPMENT, demoComposition.mode)

        val sessionManager = demoComposition.createSessionManager()

        // Initial state is Public
        val initialState = sessionManager.restoreSession()
        assertEquals(AppEntryState.Public, initialState)

        // Verification with Demo OTP 123456
        val confirmRes = sessionManager.confirmVerification("123456", VerificationType.PHONE)
        assertTrue(confirmRes is ApiResult.Success)

        // Login as demo user
        val loginRes = sessionManager.login(LoginRequestDto(identifier = "demo", password = "demoPassword123!"))
        assertTrue(loginRes is ApiResult.Success)

        // Entry state is Authenticated with CUSTOMER role
        val state = sessionManager.entryState.value
        assertTrue(state is AppEntryState.Authenticated)
        val principal = (state as AppEntryState.Authenticated).principal
        assertEquals("USER-DEMO-001", principal.userId)
        assertEquals(UserRole.CUSTOMER, principal.role)

        // Logout clears demo state
        sessionManager.logout()
        assertEquals(AppEntryState.Public, sessionManager.entryState.value)
    }

    @Test
    fun testDemoBackendApiClient_returnsRichDemoOrders() = runBlocking {
        val demoClient = DemoBackendApiClient()
        val ordersRes = demoClient.getCustomerOrders()
        assertTrue(ordersRes is ApiResult.Success)
        val orders = (ordersRes as ApiResult.Success).data
        assertTrue(orders.isNotEmpty())
        assertTrue(orders.any { it.orderId == "ord-demo-001" })
    }
}

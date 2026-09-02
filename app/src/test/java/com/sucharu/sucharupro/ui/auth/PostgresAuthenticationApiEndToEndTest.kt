package com.sucharu.sucharupro.data.auth

import com.sucharu.sucharupro.data.api.client.DirectBackendApiClient
import com.sucharu.sucharupro.data.api.client.InMemoryAuthTokenStorage
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.auth.authorization.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.auth.persistence.*
import com.sucharu.sucharupro.data.auth.security.*
import com.sucharu.sucharupro.data.auth.service.AuthenticationService
import com.sucharu.sucharupro.data.auth.service.UserIdentityService
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.ui.features.auth.AuthDestination
import com.sucharu.sucharupro.ui.features.auth.PostLoginRouter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * End-to-End Authentication REST API & Client Integration Security Test Suite (INFRA-03 Step 04).
 */
class PostgresAuthenticationApiEndToEndTest {

    private lateinit var mockProvider: MockIdentityConnectionProvider
    private lateinit var transactionManager: DefaultPostgresTransactionManager
    private lateinit var repositoryFactory: PostgresRepositoryFactory
    private lateinit var accountDataSource: FakeAuthAccountDataSource
    private lateinit var profileDataSource: FakeAuthProfileDataSource
    private lateinit var verificationDataSource: FakeAuthVerificationDataSource
    private lateinit var passwordHistoryDataSource: FakeAuthPasswordHistoryDataSource
    private lateinit var sessionDataSource: FakeAuthSessionDataSource
    private lateinit var auditDataSource: FakeAuthAuditDataSource
    private lateinit var notificationProvider: FakeVerificationNotificationProvider
    private lateinit var authConfig: AuthConfig
    private lateinit var jwtProvider: JwtTokenProvider
    private lateinit var authService: AuthenticationService
    private lateinit var identityService: UserIdentityService
    private lateinit var securityContext: BackendSecurityContext
    private lateinit var server: BackendApiServer
    private lateinit var client: DirectBackendApiClient

    @Before
    fun setUp() {
        mockProvider = MockIdentityConnectionProvider()
        transactionManager = DefaultPostgresTransactionManager(mockProvider)
        repositoryFactory = PostgresRepositoryFactory(transactionManager, defaultTenantId = "TENANT-001")

        accountDataSource = FakeAuthAccountDataSource()
        profileDataSource = FakeAuthProfileDataSource()
        verificationDataSource = FakeAuthVerificationDataSource()
        passwordHistoryDataSource = FakeAuthPasswordHistoryDataSource()
        sessionDataSource = FakeAuthSessionDataSource()
        auditDataSource = FakeAuthAuditDataSource()
        notificationProvider = FakeVerificationNotificationProvider()

        authConfig = AuthConfig(
            accessTokenTtlSeconds = 300L,
            refreshTokenTtlSeconds = 3600L,
            jwtIssuer = "sucharu-e2e-test",
            jwtAudience = "sucharu-e2e-audience",
            jwtKeyId = "e2e-kid-1",
            jwtSigningSecret = "sucharu_e2e_super_secure_testing_secret_key_2026",
            maxLoginAttempts = 5,
            accountLockDurationSeconds = 900L
        )
        jwtProvider = JwtTokenProvider(authConfig)

        authService = AuthenticationService(
            accountDataSource = accountDataSource,
            sessionDataSource = sessionDataSource,
            auditDataSource = auditDataSource,
            jwtProvider = jwtProvider,
            config = authConfig,
            profileDataSource = profileDataSource,
            verificationDataSource = verificationDataSource,
            passwordHistoryDataSource = passwordHistoryDataSource,
            notificationProvider = notificationProvider
        )

        identityService = UserIdentityService(
            accountDataSource = accountDataSource,
            profileDataSource = profileDataSource,
            verificationDataSource = verificationDataSource,
            passwordHistoryDataSource = passwordHistoryDataSource,
            sessionDataSource = sessionDataSource,
            auditDataSource = auditDataSource
        )

        securityContext = BackendSecurityContext(jwtTokenProvider = jwtProvider)

        server = BackendApiServer(
            connectionProvider = mockProvider,
            transactionManager = transactionManager,
            repositoryFactory = repositoryFactory,
            securityContext = securityContext,
            authService = authService,
            userIdentityService = identityService
        )
        server.start()

        client = DirectBackendApiClient(server = server, tokenStorage = InMemoryAuthTokenStorage())
    }

    @Test
    fun test01_endToEndRegistrationLoginAndSessionManagementFlow(): Unit = runBlocking {
        // 1. Public Registration
        val regReq = RegisterRequestDto(displayName = "E2E User", email = "e2e@example.com", password = "SecurePassword123!")
        val regRes = client.register(regReq)
        assertTrue(regRes is ApiResult.Success)

        val regData = (regRes as ApiResult.Success).data
        assertEquals("e2e@example.com", regData.email)
        assertEquals(AccountStatus.PENDING, regData.accountStatus)

        // 2. Activate Account via Verification Token
        val notifications = notificationProvider.getSentNotifications()
        val rawToken = notifications[0].rawToken

        // Confirm token
        val confirmRes = identityService.confirmVerificationToken("TENANT-001", regData.userId, VerificationType.EMAIL, rawToken)
        assertTrue(confirmRes)

        // Manually activate account status for test login
        accountDataSource.updateAccountStatus("TENANT-001", regData.userId, AccountStatus.ACTIVE)

        // 3. Login
        val loginRes = client.login(LoginRequestDto(identifier = "e2e@example.com", password = "SecurePassword123!", requestedProjectId = "TENANT-001"))
        assertTrue(loginRes is ApiResult.Success)

        val loginData = (loginRes as ApiResult.Success).data
        assertNotNull(loginData.accessToken)

        // 4. Post-Login Router Validation
        val principal = securityContext.authenticate("Bearer ${loginData.accessToken}")
        val dest = PostLoginRouter.resolveWorkspaceDestination(principal)
        assertEquals(AuthDestination.CustomerWorkspace, dest)

        // 5. Logout
        val logoutRes = client.logout()
        assertTrue(logoutRes is ApiResult.Success)
    }

    @Test
    fun test02_endToEndPasswordRecoveryFlow(): Unit = runBlocking {
        // Register & activate account
        val regReq = RegisterRequestDto(displayName = "Reset User", email = "reset@example.com", password = "OldPassword123!")
        client.register(regReq)

        // Request recovery
        val recRes = client.requestPasswordRecovery(PasswordRecoveryRequestDto(identifier = "reset@example.com"))
        assertTrue(recRes is ApiResult.Success)

        val recData = (recRes as ApiResult.Success).data
        assertEquals("If the account exists, recovery instructions have been sent.", recData.message)

        // Fetch dispatched reset token
        val notifications = notificationProvider.getSentNotifications()
        val resetToken = notifications.last().rawToken

        // Confirm Reset
        val resetRes = client.confirmPasswordReset(PasswordRecoveryConfirmDto(token = resetToken, newPassword = "NewPassword123!"))
        assertTrue(resetRes is ApiResult.Success)

        // Verify login with new password succeeds
        val account = accountDataSource.getAccount("TENANT-001", "reset@example.com")
        accountDataSource.updateAccountStatus("TENANT-001", account!!.userId, AccountStatus.ACTIVE)
        val loginRes = client.login(LoginRequestDto(identifier = "reset@example.com", password = "NewPassword123!", requestedProjectId = "TENANT-001"))
        assertTrue(loginRes is ApiResult.Success)
    }

    @Test
    fun test03_endToEndPostLoginRoleRouting(): Unit = runBlocking {
        val adminPrincipal = AuthenticatedPrincipal(userId = "admin-1", projectId = "P1", username = "admin", role = UserRole.ADMIN, permissions = emptySet(), email = null, tokenExpiresAt = System.currentTimeMillis() + 10000)
        val staffPrincipal = AuthenticatedPrincipal(userId = "staff-1", projectId = "P1", username = "staff", role = UserRole.STAFF, permissions = emptySet(), email = null, tokenExpiresAt = System.currentTimeMillis() + 10000)
        val affiliatePrincipal = AuthenticatedPrincipal(userId = "aff-1", projectId = "P1", username = "aff", role = UserRole.AFFILIATE, permissions = emptySet(), email = null, tokenExpiresAt = System.currentTimeMillis() + 10000)

        assertEquals(AuthDestination.AdminWorkspace, PostLoginRouter.resolveWorkspaceDestination(adminPrincipal))
        assertEquals(AuthDestination.StaffWorkspace, PostLoginRouter.resolveWorkspaceDestination(staffPrincipal))
        assertEquals(AuthDestination.AffiliateWorkspace, PostLoginRouter.resolveWorkspaceDestination(affiliatePrincipal))
    }

    @Test
    fun test04_aiAgentRoleBoundary_cannotUsePublicRegistrationFlow(): Unit = runBlocking {
        val regReq = RegisterRequestDto(displayName = "AI Agent", email = "agent@example.com", password = "SecurePass123!", requestedRole = UserRole.AI_AGENT)
        val res = client.register(regReq)
        assertTrue(res is ApiResult.Error)

        val err = (res as ApiResult.Error).errorResponse
        assertEquals(ErrorCode.VALIDATION_ERROR, err.errorCode)
        assertTrue(err.message.contains("Public registration cannot assign privileged role"))
    }
}

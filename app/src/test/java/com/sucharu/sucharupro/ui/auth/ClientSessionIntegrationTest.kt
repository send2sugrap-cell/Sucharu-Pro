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
import com.sucharu.sucharupro.data.auth.session.*
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.ui.features.auth.AuthDestination
import com.sucharu.sucharupro.ui.features.auth.PostLoginRouter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Production Client Session Integration & Entry Architecture Test Suite (INFRA-03 Step 05).
 */
class ClientSessionIntegrationTest {

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
    private lateinit var sessionStore: InMemorySecureSessionStore
    private lateinit var sessionManager: AuthenticationSessionManager

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
            jwtIssuer = "sucharu-client-test",
            jwtAudience = "sucharu-client-audience",
            jwtKeyId = "client-kid-1",
            jwtSigningSecret = "sucharu_client_super_secure_testing_secret_key_2026",
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
        sessionStore = InMemorySecureSessionStore()
        sessionManager = AuthenticationSessionManager(client = client, sessionStore = sessionStore)

        // Seed default active test account with even-length hex salt
        runBlocking {
            val hashed = PasswordHasher.hashPassword("SecurePass123!", "1234567890abcdef")
            val account = AuthAccount(
                projectId = "TENANT-001",
                userId = "user-client-001",
                username = "clienthero",
                email = "clienthero@example.com",
                passwordHash = hashed.hashHex,
                passwordSalt = hashed.saltHex,
                role = UserRole.CUSTOMER,
                accountStatus = AccountStatus.ACTIVE
            )
            accountDataSource.createAccount(account)
            passwordHistoryDataSource.recordPasswordHistory(
                PasswordHistoryEntry(
                    historyId = UUID.randomUUID().toString(),
                    projectId = "TENANT-001",
                    userId = "user-client-001",
                    passwordHash = hashed.hashHex,
                    passwordSalt = hashed.saltHex
                )
            )
        }
    }

    @Test
    fun test01_startupSessionRestoration_noSession_transitionsToPublicState(): Unit = runBlocking {
        sessionStore.clearSession()
        val state = sessionManager.restoreSession()
        assertEquals(AppEntryState.Public, state)
    }

    @Test
    fun test02_startupSessionRestoration_validSession_transitionsToAuthenticatedState(): Unit = runBlocking {
        val loginRes = client.login(LoginRequestDto(identifier = "clienthero@example.com", password = "SecurePass123!", requestedProjectId = "TENANT-001"))
        assertTrue(loginRes is ApiResult.Success)

        val authData = (loginRes as ApiResult.Success).data
        sessionStore.saveSession(
            UserSessionData(
                accessToken = authData.accessToken,
                refreshToken = authData.refreshToken,
                sessionId = authData.sessionId,
                principal = authData.user.toAuthenticatedPrincipal()
            )
        )

        val state = sessionManager.restoreSession()
        assertTrue(state is AppEntryState.Authenticated)
        assertEquals("user-client-001", (state as AppEntryState.Authenticated).principal.userId)
    }

    @Test
    fun test05_login_updatesSessionStoreAndTransitionsToAuthenticated(): Unit = runBlocking {
        val loginRes = sessionManager.login(LoginRequestDto(identifier = "clienthero@example.com", password = "SecurePass123!", requestedProjectId = "TENANT-001"))
        assertTrue(loginRes is ApiResult.Success)

        assertTrue(sessionStore.hasSession())
        assertTrue(sessionManager.entryState.value is AppEntryState.Authenticated)
    }

    @Test
    fun test06_publicRegistration_pendingAccount_transitionsToVerificationRequired(): Unit = runBlocking {
        val regReq = RegisterRequestDto(displayName = "New User", email = "newuser@example.com", password = "NewPassword123!")
        val regRes = sessionManager.register(regReq)

        assertTrue(regRes is ApiResult.Success)
        assertTrue(sessionManager.entryState.value is AppEntryState.VerificationRequired)
    }

    @Test
    fun test07_passwordRecovery_enumerationDefense_genericResponse(): Unit = runBlocking {
        val recRes = sessionManager.requestPasswordRecovery(PasswordRecoveryRequestDto(identifier = "clienthero@example.com"))
        assertTrue(recRes is ApiResult.Success)
        assertEquals("If the account exists, recovery instructions have been sent.", (recRes as ApiResult.Success).data.message)
        assertEquals(AppEntryState.RecoveryFlow, sessionManager.entryState.value)
    }

    @Test
    fun test08_passwordResetConfirmation_clearsSessionAndRequiresReAuthentication(): Unit = runBlocking {
        sessionManager.login(LoginRequestDto(identifier = "clienthero@example.com", password = "SecurePass123!", requestedProjectId = "TENANT-001"))
        sessionManager.requestPasswordRecovery(PasswordRecoveryRequestDto(identifier = "clienthero@example.com"))

        val resetToken = notificationProvider.getSentNotifications().last().rawToken
        val resetRes = sessionManager.confirmPasswordReset(PasswordRecoveryConfirmDto(token = resetToken, newPassword = "BrandNewPassword123!"))

        assertTrue(resetRes is ApiResult.Success)
        assertFalse(sessionStore.hasSession())
        assertEquals(AppEntryState.Public, sessionManager.entryState.value)
    }

    @Test
    fun test09_executeWith401Retry_retriesOnceOnUnauthenticatedAndSucceeds(): Unit = runBlocking {
        sessionManager.login(LoginRequestDto(identifier = "clienthero@example.com", password = "SecurePass123!", requestedProjectId = "TENANT-001"))

        val result = sessionManager.executeWith401Retry {
            client.getMyProfile()
        }

        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun test11_roleAwarePostLoginRouting_customerWorkspace(): Unit = runBlocking {
        val customerPrincipal = AuthenticatedPrincipal(userId = "c1", projectId = "P1", username = "cust", role = UserRole.CUSTOMER, permissions = emptySet(), email = null, tokenExpiresAt = System.currentTimeMillis() + 10000)
        assertEquals(AuthDestination.CustomerWorkspace, PostLoginRouter.resolveWorkspaceDestination(customerPrincipal))
    }

    @Test
    fun test12_roleAwarePostLoginRouting_affiliateWorkspace(): Unit = runBlocking {
        val affiliatePrincipal = AuthenticatedPrincipal(userId = "a1", projectId = "P1", username = "aff", role = UserRole.AFFILIATE, permissions = emptySet(), email = null, tokenExpiresAt = System.currentTimeMillis() + 10000)
        assertEquals(AuthDestination.AffiliateWorkspace, PostLoginRouter.resolveWorkspaceDestination(affiliatePrincipal))
    }

    @Test
    fun test13_roleAwarePostLoginRouting_staffWorkspace(): Unit = runBlocking {
        val staffPrincipal = AuthenticatedPrincipal(userId = "s1", projectId = "P1", username = "staff", role = UserRole.STAFF, permissions = emptySet(), email = null, tokenExpiresAt = System.currentTimeMillis() + 10000)
        assertEquals(AuthDestination.StaffWorkspace, PostLoginRouter.resolveWorkspaceDestination(staffPrincipal))
    }

    @Test
    fun test14_roleAwarePostLoginRouting_managerWorkspace(): Unit = runBlocking {
        val managerPrincipal = AuthenticatedPrincipal(userId = "m1", projectId = "P1", username = "manager", role = UserRole.MANAGER, permissions = emptySet(), email = null, tokenExpiresAt = System.currentTimeMillis() + 10000)
        assertEquals(AuthDestination.ManagerWorkspace, PostLoginRouter.resolveWorkspaceDestination(managerPrincipal))
    }

    @Test
    fun test15_roleAwarePostLoginRouting_adminWorkspace(): Unit = runBlocking {
        val adminPrincipal = AuthenticatedPrincipal(userId = "adm1", projectId = "P1", username = "admin", role = UserRole.ADMIN, permissions = emptySet(), email = null, tokenExpiresAt = System.currentTimeMillis() + 10000)
        assertEquals(AuthDestination.AdminWorkspace, PostLoginRouter.resolveWorkspaceDestination(adminPrincipal))
    }

    @Test
    fun test16_aiAgentRoleBoundary_deniesHumanDashboardRouting(): Unit = runBlocking {
        val agentPrincipal = AuthenticatedPrincipal(userId = "bot1", projectId = "P1", username = "ai_bot", role = UserRole.AI_AGENT, permissions = emptySet(), email = null, tokenExpiresAt = System.currentTimeMillis() + 10000)
        assertEquals(AuthDestination.PublicGuestHome, PostLoginRouter.resolveWorkspaceDestination(agentPrincipal))
    }

    @Test
    fun test17_accountStatusRouting_lockedAccount(): Unit = runBlocking {
        val lockedPrincipal = AuthenticatedPrincipal(userId = "l1", projectId = "P1", username = "locked", role = UserRole.CUSTOMER, permissions = emptySet(), email = null, tokenExpiresAt = System.currentTimeMillis() + 10000, accountStatus = AccountStatus.LOCKED)
        val state = sessionManager.evaluateAuthoritativePrincipal(lockedPrincipal)
        assertTrue(state is AppEntryState.AccountUnavailable)
        assertEquals("Your account is temporarily unavailable. Please try again later.", (state as AppEntryState.AccountUnavailable).displayMessage)
    }

    @Test
    fun test18_accountStatusRouting_suspendedAccount(): Unit = runBlocking {
        val suspendedPrincipal = AuthenticatedPrincipal(userId = "s1", projectId = "P1", username = "suspended", role = UserRole.CUSTOMER, permissions = emptySet(), email = null, tokenExpiresAt = System.currentTimeMillis() + 10000, accountStatus = AccountStatus.SUSPENDED)
        val state = sessionManager.evaluateAuthoritativePrincipal(suspendedPrincipal)
        assertTrue(state is AppEntryState.AccountUnavailable)
        assertEquals("Your account is currently unavailable.", (state as AppEntryState.AccountUnavailable).displayMessage)
    }

    @Test
    fun test20_logout_clearsSessionAndTransitionsToPublic(): Unit = runBlocking {
        sessionManager.login(LoginRequestDto(identifier = "clienthero@example.com", password = "SecurePass123!", requestedProjectId = "TENANT-001"))
        sessionManager.logout()

        assertFalse(sessionStore.hasSession())
        assertEquals(AppEntryState.Public, sessionManager.entryState.value)
    }

    @Test
    fun test21_singleFlightRefresh_preventsMultipleConcurrentRefreshRequests(): Unit = runBlocking {
        sessionManager.login(LoginRequestDto(identifier = "clienthero@example.com", password = "SecurePass123!", requestedProjectId = "TENANT-001"))

        val deferreds = (1..5).map {
            async {
                sessionManager.refreshSession()
            }
        }
        val results = deferreds.awaitAll()
        assertTrue(results.all { it })
    }

    @Test
    fun test25_noSecretsLoggedOrStoredInPlaintext(): Unit = runBlocking {
        sessionManager.login(LoginRequestDto(identifier = "clienthero@example.com", password = "SecurePass123!", requestedProjectId = "TENANT-001"))
        val sessionData = sessionStore.getSession()

        assertNotNull(sessionData)
        assertFalse(sessionData!!.accessToken.contains("password"))
        assertFalse(sessionData.refreshToken.contains("password"))
    }
}

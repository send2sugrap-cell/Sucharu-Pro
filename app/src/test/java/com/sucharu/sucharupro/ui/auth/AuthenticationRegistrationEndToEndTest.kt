package com.sucharu.sucharupro.ui.auth

import com.sucharu.sucharupro.data.api.client.DirectBackendApiClient
import com.sucharu.sucharupro.data.api.client.InMemoryAuthTokenStorage
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.auth.MockIdentityConnectionProvider
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
 * End-to-End Authentication & Registration Functional Gap-Fix Verification Test Suite.
 */
class AuthenticationRegistrationEndToEndTest {

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
            jwtIssuer = "sucharu-reg-test",
            jwtAudience = "sucharu-reg-audience",
            jwtKeyId = "reg-kid-1",
            jwtSigningSecret = "sucharu_reg_super_secure_testing_secret_key_2026",
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
    fun test01_successfulMobileRegistrationAndLoginFlow(): Unit = runBlocking {
        // 1. Mobile Registration
        val regReq = RegisterRequestDto(
            displayName = "Rahim Ahmed",
            phone = "01711234567",
            password = "SecurePassword123!"
        )
        val regRes = client.register(regReq)
        assertTrue(regRes is ApiResult.Success)

        val regData = (regRes as ApiResult.Success).data
        assertEquals("01711234567", regData.phone)
        assertEquals(UserRole.CUSTOMER, regData.role)
        assertEquals(AccountStatus.PENDING, regData.accountStatus)
        assertTrue(regData.verificationRequired)

        // 2. Profile Verification
        val profile = profileDataSource.getProfile("TENANT-001", regData.userId)
        assertNotNull(profile)
        assertEquals("Rahim Ahmed", profile!!.displayName)
        assertEquals("01711234567", profile.phone)

        // 3. Attempt Login Before Verification (Must be rejected with pending verification notice)
        val prematureLogin = client.login(
            LoginRequestDto(
                identifier = "01711234567",
                password = "SecurePassword123!",
                requestedProjectId = "TENANT-001"
            )
        )
        assertTrue(prematureLogin is ApiResult.Error)
        val prematureError = (prematureLogin as ApiResult.Error).errorResponse
        assertTrue(prematureError.message.contains("Account pending verification"))

        // 4. Confirm Verification via Single-Use Token
        val notifications = notificationProvider.getSentNotifications()
        assertTrue(notifications.isNotEmpty())
        val sentNotification = notifications.first { it.recipient == "01711234567" }
        assertNotNull(sentNotification.rawToken)

        val verifRes = client.confirmVerificationToken(
            ConfirmVerificationRequestDto(
                verificationType = VerificationType.PHONE,
                token = sentNotification.rawToken
            )
        )
        assertTrue(verifRes is ApiResult.Success)

        // 5. Account is now ACTIVE in Database
        val activeAccount = accountDataSource.getAccountById("TENANT-001", regData.userId)
        assertEquals(AccountStatus.ACTIVE, activeAccount?.accountStatus)

        // 6. Login with formatted/international mobile number (+880 1711-234567)
        val loginRes = client.login(
            LoginRequestDto(
                identifier = "+880 1711-234567",
                password = "SecurePassword123!",
                requestedProjectId = "TENANT-001"
            )
        )
        assertTrue(loginRes is ApiResult.Success)

        val loginData = (loginRes as ApiResult.Success).data
        assertNotNull(loginData.accessToken)

        val principal = securityContext.authenticate("Bearer ${loginData.accessToken}")
        assertEquals(UserRole.CUSTOMER, principal.role)
        assertEquals(AuthDestination.CustomerWorkspace, PostLoginRouter.resolveWorkspaceDestination(principal))
    }

    @Test
    fun test02_duplicateMobileOrEmailRegistrationIsBlocked(): Unit = runBlocking {
        // Register initial user with phone and email
        val regReq = RegisterRequestDto(
            displayName = "User One",
            email = "user1@example.com",
            phone = "01812345678",
            password = "SecurePassword123!"
        )
        val regRes1 = client.register(regReq)
        assertTrue(regRes1 is ApiResult.Success)

        // Attempt registering second user with same email
        val regReqDuplicateEmail = RegisterRequestDto(
            displayName = "User Two",
            email = "user1@example.com",
            phone = "01999999999",
            password = "SecurePassword123!"
        )
        val regRes2 = client.register(regReqDuplicateEmail)
        assertTrue(regRes2 is ApiResult.Error)
        assertEquals(ErrorCode.CONFLICT, (regRes2 as ApiResult.Error).errorResponse.errorCode)

        // Attempt registering third user with same phone in +880 format
        val regReqDuplicatePhone = RegisterRequestDto(
            displayName = "User Three",
            email = "user3@example.com",
            phone = "+880 1812-345678",
            password = "SecurePassword123!"
        )
        val regRes3 = client.register(regReqDuplicatePhone)
        assertTrue(regRes3 is ApiResult.Error)
        assertEquals(ErrorCode.CONFLICT, (regRes3 as ApiResult.Error).errorResponse.errorCode)
    }

    @Test
    fun test03_registrationWithAffiliateReferralCodeAssignsAffiliateRole(): Unit = runBlocking {
        val regReq = RegisterRequestDto(
            displayName = "Partner User",
            email = "partner@sucharu.com",
            phone = "01611223344",
            password = "SecurePassword123!",
            affiliateReferralCode = "AFF-9999"
        )
        val regRes = client.register(regReq)
        assertTrue(regRes is ApiResult.Success)

        val regData = (regRes as ApiResult.Success).data
        assertEquals(UserRole.AFFILIATE, regData.role)

        val sentNotification = notificationProvider.getSentNotifications().first { it.recipient == "partner@sucharu.com" }
        val verifRes = client.confirmVerificationToken(
            ConfirmVerificationRequestDto(
                verificationType = VerificationType.EMAIL,
                token = sentNotification.rawToken
            )
        )
        assertTrue(verifRes is ApiResult.Success)

        val loginRes = client.login(LoginRequestDto(identifier = "partner@sucharu.com", password = "SecurePassword123!", requestedProjectId = "TENANT-001"))
        assertTrue(loginRes is ApiResult.Success)

        val principal = securityContext.authenticate("Bearer ${(loginRes as ApiResult.Success).data.accessToken}")
        assertEquals(UserRole.AFFILIATE, principal.role)
        assertEquals(AuthDestination.AffiliateWorkspace, PostLoginRouter.resolveWorkspaceDestination(principal))
    }

    @Test
    fun test04_privilegedRoleEscalationAttemptsAreRejected(): Unit = runBlocking {
        val rolesToTest = listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.AI_AGENT)
        for (role in rolesToTest) {
            val regReq = RegisterRequestDto(
                displayName = "Hacker User",
                email = "hacker_${role.name.lowercase()}@example.com",
                password = "SecurePassword123!",
                requestedRole = role
            )
            val regRes = client.register(regReq)
            assertTrue("Expected failure for requested role: $role", regRes is ApiResult.Error)
            val err = (regRes as ApiResult.Error).errorResponse
            assertEquals(ErrorCode.VALIDATION_ERROR, err.errorCode)
            assertTrue(err.message.contains("Public registration cannot assign privileged role"))
        }
    }

    @Test
    fun test05_shortPasswordValidationRejection(): Unit = runBlocking {
        val regReq = RegisterRequestDto(
            displayName = "Short Pwd",
            email = "short@example.com",
            password = "123"
        )
        val regRes = client.register(regReq)
        assertTrue(regRes is ApiResult.Error)
        val err = (regRes as ApiResult.Error).errorResponse
        assertEquals(ErrorCode.VALIDATION_ERROR, err.errorCode)
        assertTrue(err.message.contains("at least 8 characters"))
    }
}

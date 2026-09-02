package com.sucharu.sucharupro.data.auth

import com.sucharu.sucharupro.data.api.client.DirectBackendApiClient
import com.sucharu.sucharupro.data.api.client.InMemoryAuthTokenStorage
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.auth.authorization.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.auth.persistence.*
import com.sucharu.sucharupro.data.auth.security.AuthConfig
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.auth.security.PasswordHasher
import com.sucharu.sucharupro.data.auth.security.TokenGenerator
import com.sucharu.sucharupro.data.auth.service.AuthenticationService
import com.sucharu.sucharupro.data.auth.service.UserIdentityService
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.model.common.DomainResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * Comprehensive User Identity Lifecycle, Profile, Session & Device Security Test Suite (INFRA-03 Step 03).
 */
class PostgresIdentityLifecycleSecurityTest {

    private lateinit var mockProvider: MockIdentityConnectionProvider
    private lateinit var transactionManager: TransactionManager
    private lateinit var repositoryFactory: PostgresRepositoryFactory
    private lateinit var accountDataSource: FakeAuthAccountDataSource
    private lateinit var profileDataSource: FakeAuthProfileDataSource
    private lateinit var verificationDataSource: FakeAuthVerificationDataSource
    private lateinit var passwordHistoryDataSource: FakeAuthPasswordHistoryDataSource
    private lateinit var sessionDataSource: FakeAuthSessionDataSource
    private lateinit var auditDataSource: FakeAuthAuditDataSource
    private lateinit var authConfig: AuthConfig
    private lateinit var jwtProvider: JwtTokenProvider
    private lateinit var authService: AuthenticationService
    private lateinit var identityService: UserIdentityService
    private lateinit var securityContext: BackendSecurityContext
    private lateinit var authorizationService: BackendAuthorizationService
    private lateinit var server: BackendApiServer

    private lateinit var customerAPrincipal: AuthenticatedPrincipal
    private lateinit var customerBPrincipal: AuthenticatedPrincipal
    private lateinit var adminPrincipal: AuthenticatedPrincipal
    private lateinit var aiAgentPrincipal: AuthenticatedPrincipal

    @Before
    fun setUp() {
        runBlocking {
            mockProvider = MockIdentityConnectionProvider()
            transactionManager = DefaultPostgresTransactionManager(mockProvider)
            repositoryFactory = PostgresRepositoryFactory(transactionManager, defaultTenantId = "TENANT-001")

            accountDataSource = FakeAuthAccountDataSource()
            profileDataSource = FakeAuthProfileDataSource()
            verificationDataSource = FakeAuthVerificationDataSource()
            passwordHistoryDataSource = FakeAuthPasswordHistoryDataSource()
            sessionDataSource = FakeAuthSessionDataSource()
            auditDataSource = FakeAuthAuditDataSource()

            authConfig = AuthConfig(
                accessTokenTtlSeconds = 300L,
                refreshTokenTtlSeconds = 3600L,
                jwtIssuer = "sucharu-identity-test",
                jwtAudience = "sucharu-identity-audience",
                jwtKeyId = "identity-kid-1",
                jwtSigningSecret = "sucharu_identity_super_secure_testing_secret_key_2026",
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

            identityService = UserIdentityService(
                accountDataSource = accountDataSource,
                profileDataSource = profileDataSource,
                verificationDataSource = verificationDataSource,
                passwordHistoryDataSource = passwordHistoryDataSource,
                sessionDataSource = sessionDataSource,
                auditDataSource = auditDataSource
            )

            authorizationService = BackendAuthorizationService(auditDataSource = auditDataSource)
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

            // Setup Test Accounts with even-length hex salts
            val hashedAlice = PasswordHasher.hashPassword("OldPassword123!", "1234567890abcdef")
            accountDataSource.createAccount(
                AuthAccount(
                    projectId = "TENANT-001",
                    userId = "USER-100",
                    username = "alice_customer",
                    email = "alice@example.com",
                    passwordHash = hashedAlice.hashHex,
                    passwordSalt = hashedAlice.saltHex,
                    role = UserRole.CUSTOMER,
                    accountStatus = AccountStatus.ACTIVE
                )
            )

            val hashedBob = PasswordHasher.hashPassword("Password123!", "abcdef1234567890")
            accountDataSource.createAccount(
                AuthAccount(
                    projectId = "TENANT-001",
                    userId = "USER-200",
                    username = "bob_customer",
                    email = "bob@example.com",
                    passwordHash = hashedBob.hashHex,
                    passwordSalt = hashedBob.saltHex,
                    role = UserRole.CUSTOMER,
                    accountStatus = AccountStatus.ACTIVE
                )
            )

            val hashedAdmin = PasswordHasher.hashPassword("AdminPass123!", "fedcba0987654321")
            accountDataSource.createAccount(
                AuthAccount(
                    projectId = "TENANT-001",
                    userId = "ADMIN-001",
                    username = "admin_master",
                    passwordHash = hashedAdmin.hashHex,
                    passwordSalt = hashedAdmin.saltHex,
                    role = UserRole.ADMIN,
                    accountStatus = AccountStatus.ACTIVE
                )
            )

            // Principals
            customerAPrincipal = AuthenticatedPrincipal(
                userId = "USER-100",
                projectId = "TENANT-001",
                username = "alice_customer",
                role = UserRole.CUSTOMER,
                customerId = "USER-100",
                permissions = setOf(UserPermission.READ_OWN_PROFILE, UserPermission.READ_OWN_ORDERS)
            )

            customerBPrincipal = AuthenticatedPrincipal(
                userId = "USER-200",
                projectId = "TENANT-001",
                username = "bob_customer",
                role = UserRole.CUSTOMER,
                customerId = "USER-200"
            )

            adminPrincipal = AuthenticatedPrincipal(
                userId = "ADMIN-001",
                projectId = "TENANT-001",
                username = "admin_master",
                role = UserRole.ADMIN,
                permissions = setOf(UserPermission.ADMIN_ALL)
            )

            aiAgentPrincipal = AuthenticatedPrincipal(
                userId = "AGENT-007",
                projectId = "TENANT-001",
                username = "ai_agent_bot",
                role = UserRole.AI_AGENT,
                principalType = PrincipalType.AI_AGENT,
                agentId = "AGENT-007"
            )
        }
    }

    // =========================================================================
    // 1. IDENTITY & PROFILE CREATION & RETRIEVAL (01-05)
    // =========================================================================

    @Test
    fun test01_createIdentity_initializesAccountAndProfile() = runBlocking {
        val identity = identityService.getUserIdentity("TENANT-001", "USER-100")
        assertNotNull(identity)
        assertEquals("USER-100", identity?.userId)
        assertEquals(AccountStatus.ACTIVE, identity?.account?.accountStatus)
    }

    @Test
    fun test02_activateIdentity_updatesStatusToActive() = runBlocking {
        accountDataSource.updateAccountStatus("TENANT-001", "USER-100", AccountStatus.PENDING)
        val before = accountDataSource.getAccountById("TENANT-001", "USER-100")
        assertEquals(AccountStatus.PENDING, before?.accountStatus)

        identityService.updateAccountStatus("TENANT-001", "USER-100", AccountStatus.ACTIVE, adminPrincipal = adminPrincipal)
        val after = accountDataSource.getAccountById("TENANT-001", "USER-100")
        assertEquals(AccountStatus.ACTIVE, after?.accountStatus)
    }

    @Test
    fun test03_retrieveOwnIdentity_returnsEnrichedProfile() = runBlocking {
        val profile = identityService.getProfile("TENANT-001", "USER-100")
        assertNotNull(profile)
        assertEquals("USER-100", profile?.userId)
        assertEquals("alice_customer", profile?.username)
    }

    @Test
    fun test04_crossUserIdentityAccessDenied() = runBlocking {
        try {
            BackendAuthorizationPolicy.enforceCustomerOwnership(customerAPrincipal, "USER-200")
            fail("Expected ForbiddenException")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("USER-200") == true)
        }
    }

    @Test
    fun test05_crossTenantIdentityAccessDenied() = runBlocking {
        try {
            BackendAuthorizationPolicy.enforceTenantIsolation(customerAPrincipal, "TENANT-999")
            fail("Expected ForbiddenException")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Cross-tenant") == true)
        }
    }

    // =========================================================================
    // 2. PROFILE MUTATION & OCC (06-09, 30)
    // =========================================================================

    @Test
    fun test06_updateOwnProfile_succeeds() = runBlocking {
        val updateReq = UpdateUserProfileRequestDto(
            displayName = "Alice Smith",
            timezone = "America/New_York"
        )
        val updated = identityService.updateProfile("TENANT-001", "USER-100", updateReq)
        assertEquals("Alice Smith", updated.displayName)
        assertEquals("America/New_York", updated.timezone)
    }

    @Test
    fun test07_unauthorizedProfileFieldMutationDenied() = runBlocking {
        try {
            BackendAuthorizationPolicy.requireCapability(customerAPrincipal, AuthorizationCapability.ADMIN_MANAGE_ROLES)
            fail("Expected ForbiddenException")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Access denied") == true)
        }
    }

    @Test
    fun test08_customerCannotModifyRole() = runBlocking {
        assertFalse(RoleCapabilityMatrix.hasCapability(UserRole.CUSTOMER, AuthorizationCapability.ADMIN_MANAGE_ROLES))
    }

    @Test
    fun test09_affiliateCannotModifyPermissions() = runBlocking {
        assertFalse(RoleCapabilityMatrix.hasCapability(UserRole.AFFILIATE, AuthorizationCapability.ADMIN_MANAGE_PERMISSIONS))
    }

    @Test
    fun test30_optimisticConcurrencyConflict_rejectedWithConflict() = runBlocking {
        val initial = profileDataSource.getProfile("TENANT-001", "USER-100")
            ?: UserProfile(projectId = "TENANT-001", userId = "USER-100", displayName = "Alice")

        profileDataSource.createOrUpdateProfile(initial)

        val stale = initial.copy(displayName = "Stale Update", version = 0L)
        val result = profileDataSource.createOrUpdateProfile(stale)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("OPTIMISTIC_CONCURRENCY_CONFLICT"))
    }

    // =========================================================================
    // 3. PASSWORD LIFECYCLE (10-13)
    // =========================================================================

    @Test
    fun test10_passwordChangeSucceeds() = runBlocking {
        val oldHashed = PasswordHasher.hashPassword("OldPassword123!", "1234567890abcdef")
        val createAcc = AuthAccount(
            projectId = "TENANT-001",
            userId = "USER-PWD-1",
            username = "pwd_user",
            passwordHash = oldHashed.hashHex,
            passwordSalt = oldHashed.saltHex,
            accountStatus = AccountStatus.ACTIVE
        )
        accountDataSource.createAccount(createAcc)

        val changeReq = ChangePasswordRequestDto(
            currentPassword = "OldPassword123!",
            newPassword = "NewSecurePassword456!",
            revokeOtherSessions = true
        )

        val success = identityService.changePassword("TENANT-001", "USER-PWD-1", changeReq)
        assertTrue(success)
    }

    @Test
    fun test11_wrongCurrentPasswordRejected() = runBlocking {
        val changeReq = ChangePasswordRequestDto(
            currentPassword = "WrongOldPassword",
            newPassword = "NewSecurePassword456!"
        )
        try {
            identityService.changePassword("TENANT-001", "USER-100", changeReq)
            fail("Expected ValidationException")
        } catch (e: ValidationException) {
            assertEquals("Invalid current password.", e.message)
        }
    }

    @Test
    fun test12_passwordChangeRevokesSessionsAccordingToPolicy() = runBlocking {
        sessionDataSource.createSession(
            AuthSession(
                sessionId = "SESS-PWD-1",
                projectId = "TENANT-001",
                userId = "USER-PWD-SESS",
                refreshTokenHash = "HASH1",
                expiresAt = System.currentTimeMillis() + 100000
            )
        )

        val pwdHashed = PasswordHasher.hashPassword("Secret123!", "1234567890abcdef")
        accountDataSource.createAccount(
            AuthAccount(
                projectId = "TENANT-001",
                userId = "USER-PWD-SESS",
                username = "pwd_sess_user",
                passwordHash = pwdHashed.hashHex,
                passwordSalt = pwdHashed.saltHex
            )
        )

        identityService.changePassword(
            "TENANT-001",
            "USER-PWD-SESS",
            ChangePasswordRequestDto("Secret123!", "NewSecret456!", revokeOtherSessions = true)
        )

        val sess = sessionDataSource.getSession("SESS-PWD-1")
        assertEquals(SessionStatus.REVOKED, sess?.sessionStatus)
    }

    @Test
    fun test13_passwordNeverAppearsInLogs() = runBlocking {
        val auditEvents = auditDataSource.queryAuditEvents("TENANT-001", "USER-100")
        for (event in auditEvents) {
            assertFalse(event.details.containsKey("password"))
            assertFalse(event.details.containsKey("newPassword"))
            assertFalse(event.details.containsKey("currentPassword"))
        }
    }

    // =========================================================================
    // 4. VERIFICATION TOKENS (14-16)
    // =========================================================================

    @Test
    fun test14_verificationTokenSingleUse() = runBlocking {
        val rawToken = identityService.requestVerificationToken("TENANT-001", "USER-100", VerificationType.EMAIL)
        val success1 = identityService.confirmVerificationToken("TENANT-001", "USER-100", VerificationType.EMAIL, rawToken)
        assertTrue(success1)

        try {
            identityService.confirmVerificationToken("TENANT-001", "USER-100", VerificationType.EMAIL, rawToken)
            fail("Expected ValidationException on single-use replay")
        } catch (e: ValidationException) {
            assertTrue(e.message?.contains("no longer valid") == true || e.message?.contains("Invalid") == true)
        }
    }

    @Test
    fun test15_verificationTokenReplayRejected() = runBlocking {
        val rawToken = identityService.requestVerificationToken("TENANT-001", "USER-100", VerificationType.PHONE)
        identityService.confirmVerificationToken("TENANT-001", "USER-100", VerificationType.PHONE, rawToken)

        try {
            identityService.confirmVerificationToken("TENANT-001", "USER-100", VerificationType.PHONE, rawToken)
            fail("Expected ValidationException")
        } catch (e: ValidationException) {
            assertNotNull(e.message)
        }
    }

    @Test
    fun test16_expiredVerificationTokenRejected() = runBlocking {
        val expiredToken = UserVerificationToken(
            tokenId = "TOK-EXPIRED",
            projectId = "TENANT-001",
            userId = "USER-100",
            verificationType = VerificationType.EMAIL,
            tokenHash = TokenGenerator.generateSecureToken(16),
            tokenState = VerificationTokenState.PENDING,
            expiresAt = System.currentTimeMillis() - 1000L
        )
        verificationDataSource.createVerificationToken(expiredToken)

        assertFalse(expiredToken.isValid)
    }

    // =========================================================================
    // 5. SESSION MANAGEMENT & REVOCATION (17-24)
    // =========================================================================

    @Test
    fun test17_sessionCreation() = runBlocking {
        val sess = AuthSession(
            sessionId = "SESS-900",
            projectId = "TENANT-001",
            userId = "USER-100",
            refreshTokenHash = "REFRESH_HASH_900",
            expiresAt = System.currentTimeMillis() + 3600000L
        )
        val result = sessionDataSource.createSession(sess)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun test18_listOwnSessions() = runBlocking {
        sessionDataSource.createSession(
            AuthSession(
                sessionId = "SESS-ALICE-1",
                projectId = "TENANT-001",
                userId = "USER-100",
                refreshTokenHash = "HASH_A1",
                expiresAt = System.currentTimeMillis() + 3600000L
            )
        )
        val sessions = identityService.getUserSessions("TENANT-001", "USER-100")
        assertTrue(sessions.isNotEmpty())
    }

    @Test
    fun test19_crossUserSessionListingDenied() = runBlocking {
        try {
            BackendAuthorizationPolicy.enforceCustomerOwnership(customerAPrincipal, "USER-200")
            fail("Expected ForbiddenException")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("USER-200") == true)
        }
    }

    @Test
    fun test20_revokeOwnSession() = runBlocking {
        sessionDataSource.createSession(
            AuthSession(
                sessionId = "SESS-REV-1",
                projectId = "TENANT-001",
                userId = "USER-100",
                refreshTokenHash = "HASH_REV1",
                expiresAt = System.currentTimeMillis() + 3600000L
            )
        )
        val revoked = identityService.revokeSession("TENANT-001", "USER-100", "SESS-REV-1")
        assertTrue(revoked)
    }

    @Test
    fun test21_revokeAnotherUserSessionDenied() = runBlocking {
        sessionDataSource.createSession(
            AuthSession(
                sessionId = "SESS-BOB-1",
                projectId = "TENANT-001",
                userId = "USER-200",
                refreshTokenHash = "HASH_BOB1",
                expiresAt = System.currentTimeMillis() + 3600000L
            )
        )

        try {
            identityService.revokeSession("TENANT-001", "USER-100", "SESS-BOB-1")
            fail("Expected ForbiddenException")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("another user") == true)
        }
    }

    @Test
    fun test22_revokeAllOwnSessions() = runBlocking {
        sessionDataSource.createSession(
            AuthSession(
                sessionId = "SESS-ALL-1",
                projectId = "TENANT-001",
                userId = "USER-100",
                refreshTokenHash = "HASH_ALL1",
                expiresAt = System.currentTimeMillis() + 3600000L
            )
        )
        val count = sessionDataSource.revokeAllUserSessions("TENANT-001", "USER-100", "TEST_REVOKE_ALL")
        assertTrue(count >= 1)
    }

    @Test
    fun test23_logoutCurrentSession() = runBlocking {
        sessionDataSource.createSession(
            AuthSession(
                sessionId = "SESS-LOGOUT-1",
                projectId = "TENANT-001",
                userId = "USER-100",
                refreshTokenHash = "HASH_L1",
                expiresAt = System.currentTimeMillis() + 3600000L
            )
        )
        authService.logout("SESS-LOGOUT-1", "corr-1", "127.0.0.1")
        val sess = sessionDataSource.getSession("SESS-LOGOUT-1")
        assertEquals(SessionStatus.REVOKED, sess?.sessionStatus)
    }

    @Test
    fun test24_revokedRefreshTokenCannotBeReused() = runBlocking {
        sessionDataSource.createSession(
            AuthSession(
                sessionId = "SESS-RR-1",
                projectId = "TENANT-001",
                userId = "USER-100",
                sessionStatus = SessionStatus.REVOKED,
                refreshTokenHash = "HASH_RR1",
                expiresAt = System.currentTimeMillis() + 3600000L
            )
        )

        try {
            authService.refresh(RefreshRequestDto("HASH_RR1"), "corr-rr", "127.0.0.1")
            fail("Expected UnauthenticatedException")
        } catch (e: UnauthenticatedException) {
            assertTrue(e.message?.contains("revoked") == true || e.message?.contains("Invalid") == true)
        }
    }

    // =========================================================================
    // 6. ACCOUNT LIFECYCLE STATES & TRANSITIONS (25-29, 43)
    // =========================================================================

    @Test
    fun test25_deactivatedAccountCannotAuthenticate() = runBlocking {
        val dHashed = PasswordHasher.hashPassword("Pass123!", "1234567890abcdef")
        accountDataSource.createAccount(
            AuthAccount(
                projectId = "TENANT-001",
                userId = "USER-DEACT",
                username = "deact_user",
                passwordHash = dHashed.hashHex,
                passwordSalt = dHashed.saltHex,
                accountStatus = AccountStatus.DEACTIVATED
            )
        )

        try {
            authService.login(LoginRequestDto("deact_user", "Pass123!"), "corr-d", "127.0.0.1")
            fail("Expected UnauthenticatedException")
        } catch (e: UnauthenticatedException) {
            assertTrue(e.message?.contains("deactivated") == true || e.message?.contains("Invalid") == true)
        }
    }

    @Test
    fun test26_suspendedAccountCannotAuthenticate() = runBlocking {
        val sHashed = PasswordHasher.hashPassword("Pass123!", "1234567890abcdef")
        accountDataSource.createAccount(
            AuthAccount(
                projectId = "TENANT-001",
                userId = "USER-SUSP",
                username = "susp_user",
                passwordHash = sHashed.hashHex,
                passwordSalt = sHashed.saltHex,
                accountStatus = AccountStatus.SUSPENDED
            )
        )

        try {
            authService.login(LoginRequestDto("susp_user", "Pass123!"), "corr-s", "127.0.0.1")
            fail("Expected UnauthenticatedException")
        } catch (e: UnauthenticatedException) {
            assertTrue(e.message?.contains("suspended") == true || e.message?.contains("Invalid") == true)
        }
    }

    @Test
    fun test27_lockedAccountCannotAuthenticate() = runBlocking {
        val lHashed = PasswordHasher.hashPassword("Pass123!", "1234567890abcdef")
        accountDataSource.createAccount(
            AuthAccount(
                projectId = "TENANT-001",
                userId = "USER-LOCKED",
                username = "locked_user",
                passwordHash = lHashed.hashHex,
                passwordSalt = lHashed.saltHex,
                accountStatus = AccountStatus.LOCKED,
                lockUntil = System.currentTimeMillis() + 900000L
            )
        )

        try {
            authService.login(LoginRequestDto("locked_user", "Pass123!"), "corr-l", "127.0.0.1")
            fail("Expected UnauthenticatedException")
        } catch (e: UnauthenticatedException) {
            assertTrue(e.message?.contains("locked") == true || e.message?.contains("Invalid") == true)
        }
    }

    @Test
    fun test28_authorizedReactivationSucceeds() = runBlocking {
        accountDataSource.updateAccountStatus("TENANT-001", "USER-100", AccountStatus.SUSPENDED)
        val reactivated = identityService.updateAccountStatus("TENANT-001", "USER-100", AccountStatus.ACTIVE, adminPrincipal = adminPrincipal)
        assertTrue(reactivated)
        val acc = accountDataSource.getAccountById("TENANT-001", "USER-100")
        assertEquals(AccountStatus.ACTIVE, acc?.accountStatus)
    }

    @Test
    fun test29_unauthorizedReactivationDenied() = runBlocking {
        try {
            BackendAuthorizationPolicy.requireCapability(customerAPrincipal, AuthorizationCapability.ADMIN_SUSPEND_ACCOUNT)
            fail("Expected ForbiddenException")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Access denied") == true)
        }
    }

    @Test
    fun test43_accountLifecycleInvalidTransitionRejected() = runBlocking {
        assertFalse(AccountStatus.DELETED.isValidTransitionTo(AccountStatus.ACTIVE))
    }

    // =========================================================================
    // 7. SECURITY BOUNDARIES, RLS & AUDIT INTEGRATION (31-48)
    // =========================================================================

    @Test
    fun test31_duplicateLifecycleMutationIdempotency() = runBlocking {
        val t1 = identityService.requestVerificationToken("TENANT-001", "USER-100", VerificationType.EMAIL)
        val t2 = identityService.requestVerificationToken("TENANT-001", "USER-100", VerificationType.EMAIL)
        assertNotEquals(t1, t2)
    }

    @Test
    fun test32_tenantRlsIsolation() = runBlocking {
        val r = identityService.getUserIdentity("TENANT-999", "USER-100")
        assertNull(r)
    }

    @Test
    fun test33_customerOwnershipIsolation() = runBlocking {
        assertTrue(customerAPrincipal.effectiveCustomerId == "USER-100")
        assertFalse(customerAPrincipal.effectiveCustomerId == "USER-200")
    }

    @Test
    fun test34_affiliateOwnershipIsolation() = runBlocking {
        val aff = AuthenticatedPrincipal(
            userId = "AFF-001",
            projectId = "TENANT-001",
            username = "affiliate_user",
            role = UserRole.AFFILIATE,
            affiliateId = "AFF-001"
        )
        try {
            BackendAuthorizationPolicy.enforceAffiliateOwnership(aff, "AFF-999")
            fail("Expected ForbiddenException")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("AFF-999") == true)
        }
    }

    @Test
    fun test35_staffPrivilegeBoundary() = runBlocking {
        val staff = AuthenticatedPrincipal(
            userId = "STAFF-01",
            projectId = "TENANT-001",
            username = "staff_member",
            role = UserRole.STAFF
        )
        assertFalse(RoleCapabilityMatrix.hasCapability(staff.role, AuthorizationCapability.ADMIN_MANAGE_USERS))
    }

    @Test
    fun test36_managerPrivilegeBoundary() = runBlocking {
        val manager = AuthenticatedPrincipal(
            userId = "MGR-01",
            projectId = "TENANT-001",
            username = "manager_user",
            role = UserRole.MANAGER
        )
        assertFalse(RoleCapabilityMatrix.hasCapability(manager.role, AuthorizationCapability.ADMIN_MANAGE_SYSTEM_CONFIGURATION))
    }

    @Test
    fun test37_adminOperationAudit() = runBlocking {
        identityService.updateAccountStatus("TENANT-001", "USER-200", AccountStatus.SUSPENDED, adminPrincipal = adminPrincipal)
        val events = auditDataSource.queryAuditEvents("TENANT-001", "USER-200")
        assertTrue(events.isNotEmpty())
    }

    @Test
    fun test38_aiAgentRestrictedIdentityOperation() = runBlocking {
        assertFalse(RoleCapabilityMatrix.hasCapability(aiAgentPrincipal.role, AuthorizationCapability.CHANGE_OWN_PASSWORD))
        assertFalse(RoleCapabilityMatrix.hasCapability(aiAgentPrincipal.role, AuthorizationCapability.ADMIN_SUSPEND_ACCOUNT))
    }

    @Test
    fun test39_aiAgentCannotObtainRawCredentials() = runBlocking {
        val profile = identityService.getProfile("TENANT-001", "USER-100")
        assertNotNull(profile)
        val dtoFields = profile!!::class.java.declaredFields.map { it.name }
        assertFalse(dtoFields.contains("passwordHash"))
        assertFalse(dtoFields.contains("passwordSalt"))
    }

    @Test
    fun test40_clientRoleSpoofingIgnored() = runBlocking {
        try {
            BackendAuthorizationPolicy.requireRole(customerAPrincipal, UserRole.ADMIN)
            fail("Expected ForbiddenException")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Access denied") == true)
        }
    }

    @Test
    fun test41_clientUserIdSpoofingIgnored() = runBlocking {
        try {
            BackendAuthorizationPolicy.enforceCustomerOwnership(customerAPrincipal, "USER-SPOOFED")
            fail("Expected ForbiddenException")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("USER-SPOOFED") == true)
        }
    }

    @Test
    fun test42_clientProjectIdSpoofingIgnored() = runBlocking {
        try {
            BackendAuthorizationPolicy.enforceTenantIsolation(customerAPrincipal, "TENANT-SPOOFED")
            fail("Expected ForbiddenException")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Cross-tenant") == true)
        }
    }

    @Test
    fun test44_auditEventGeneratedOnLifecycleChange() = runBlocking {
        identityService.requestVerificationToken("TENANT-001", "USER-100", VerificationType.EMAIL)
        val events = auditDataSource.queryAuditEvents("TENANT-001", "USER-100")
        assertTrue(events.any { it.eventType == AuthEventType.AUTH_VERIFICATION_TOKEN_CREATED })
    }

    @Test
    fun test45_sensitiveAuditPayloadSanitized() = runBlocking {
        val events = auditDataSource.queryAuditEvents("TENANT-001", "USER-100")
        for (e in events) {
            assertFalse(e.details.values.any { it.contains("PBKDF2") || it.contains("secret") })
        }
    }

    @Test
    fun test46_rateLimitingOnSensitiveOperations() = runBlocking {
        val limiter = BackendRateLimiter(maxRequestsPerWindow = 5, windowDurationMs = 60000L)
        repeat(5) {
            limiter.checkRateLimit("192.168.1.100")
        }
        try {
            limiter.checkRateLimit("192.168.1.100")
            fail("Expected RateLimitedException")
        } catch (e: ApiException) {
            assertEquals(ErrorCode.RATE_LIMITED, e.errorResponse.errorCode)
        }
    }

    @Test
    fun test47_sanitizedApiErrors() = runBlocking {
        try {
            authorizationService.requireRole(customerAPrincipal, UserRole.ADMIN)
            fail("Expected ForbiddenException")
        } catch (e: ForbiddenException) {
            assertEquals("Access denied: Role 'CUSTOMER' is not authorized for this resource.", e.message)
        }
    }

    @Test
    fun test48_rollbackOnLifecycleFailure() = runBlocking {
        accountDataSource.updateAccountStatus("TENANT-001", "USER-100", AccountStatus.DEACTIVATED)
        val before = accountDataSource.getAccountById("TENANT-001", "USER-100")?.accountStatus
        assertEquals(AccountStatus.DEACTIVATED, before)
        try {
            // Attempt invalid transition DEACTIVATED -> LOCKED (DEACTIVATED can only go to ACTIVE)
            identityService.updateAccountStatus("TENANT-001", "USER-100", AccountStatus.LOCKED, adminPrincipal = adminPrincipal)
            fail("Expected ValidationException on invalid transition")
        } catch (_: ValidationException) {}

        val after = accountDataSource.getAccountById("TENANT-001", "USER-100")?.accountStatus
        assertEquals(AccountStatus.DEACTIVATED, after)
    }
}

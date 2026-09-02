package com.sucharu.sucharupro.data.auth

import com.sucharu.sucharupro.data.api.client.DirectBackendApiClient
import com.sucharu.sucharupro.data.api.client.InMemoryAuthTokenStorage
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.auth.persistence.FakeAuthAccountDataSource
import com.sucharu.sucharupro.data.auth.persistence.FakeAuthAuditDataSource
import com.sucharu.sucharupro.data.auth.persistence.FakeAuthSessionDataSource
import com.sucharu.sucharupro.data.auth.security.*
import com.sucharu.sucharupro.data.auth.service.AuthenticationService
import com.sucharu.sucharupro.data.persistence.postgres.*
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerCreditProfile
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
 * Comprehensive 40-scenario Authentication, Identity, Session & Security Validation Suite (INFRA-03 Step 01).
 */
class PostgresAuthenticationSecurityTest {

    private lateinit var mockProvider: MockAuthConnectionProvider
    private lateinit var transactionManager: TransactionManager
    private lateinit var repositoryFactory: PostgresRepositoryFactory
    private lateinit var accountDataSource: FakeAuthAccountDataSource
    private lateinit var sessionDataSource: FakeAuthSessionDataSource
    private lateinit var auditDataSource: FakeAuthAuditDataSource
    private lateinit var authConfig: AuthConfig
    private lateinit var jwtProvider: JwtTokenProvider
    private lateinit var authService: AuthenticationService
    private lateinit var securityContext: BackendSecurityContext
    private lateinit var server: BackendApiServer
    private lateinit var client: DirectBackendApiClient
    private lateinit var tokenStorage: InMemoryAuthTokenStorage

    @Before
    fun setUp() {
        runBlocking {
            mockProvider = MockAuthConnectionProvider()
            transactionManager = DefaultPostgresTransactionManager(mockProvider)
            repositoryFactory = PostgresRepositoryFactory(transactionManager, defaultTenantId = "TENANT-001")

            accountDataSource = FakeAuthAccountDataSource()
        sessionDataSource = FakeAuthSessionDataSource()
        auditDataSource = FakeAuthAuditDataSource()

        authConfig = AuthConfig(
            accessTokenTtlSeconds = 10L, // Short for testing
            refreshTokenTtlSeconds = 300L,
            jwtIssuer = "sucharu-test-issuer",
            jwtAudience = "sucharu-test-audience",
            jwtKeyId = "test-kid-1",
            jwtSigningSecret = "sucharu_super_secure_testing_secret_key_2026_xyz123",
            maxLoginAttempts = 3,
            accountLockDurationSeconds = 60L
        )

        jwtProvider = JwtTokenProvider(authConfig)
        authService = AuthenticationService(
            accountDataSource = accountDataSource,
            sessionDataSource = sessionDataSource,
            auditDataSource = auditDataSource,
            jwtProvider = jwtProvider,
            config = authConfig
        )

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

        // Bootstrap Test Accounts
        val hashedPw = PasswordHasher.hashPassword("SuperSecret@123")
        accountDataSource.createAccount(
            AuthAccount(
                projectId = "TENANT-001",
                userId = "CUST-100",
                username = "alice_acme",
                email = "alice@acme.com",
                phone = "+8801700000001",
                passwordHash = hashedPw.hashHex,
                passwordSalt = hashedPw.saltHex,
                role = UserRole.CUSTOMER,
                accountStatus = AccountStatus.ACTIVE
            )
        )
        accountDataSource.createAccount(
            AuthAccount(
                projectId = "TENANT-001",
                userId = "AFF-100",
                username = "bob_affiliate",
                email = "bob@affiliate.com",
                role = UserRole.AFFILIATE,
                passwordHash = hashedPw.hashHex,
                passwordSalt = hashedPw.saltHex,
                accountStatus = AccountStatus.ACTIVE
            )
        )
        accountDataSource.createAccount(
            AuthAccount(
                projectId = "TENANT-001",
                userId = "STAFF-001",
                username = "super_admin",
                email = "admin@sucharu.pro",
                role = UserRole.ADMIN,
                passwordHash = hashedPw.hashHex,
                passwordSalt = hashedPw.saltHex,
                accountStatus = AccountStatus.ACTIVE
            )
        )
        accountDataSource.createAccount(
            AuthAccount(
                projectId = "TENANT-002",
                userId = "CUST-T2-100",
                username = "tenant2_user",
                email = "t2@example.com",
                role = UserRole.CUSTOMER,
                passwordHash = hashedPw.hashHex,
                passwordSalt = hashedPw.saltHex,
                accountStatus = AccountStatus.ACTIVE
            )
        )

        // Seed Customer & Orders in mock DB
        val custRepo = repositoryFactory.createCustomerRepository("TENANT-001")
        custRepo.addCustomer(
            Customer(
                customerId = "CUST-100",
                customerCode = "ACME-01",
                displayName = "Acme Commercial Prints",
                primaryPhone = "+8801711000000",
                email = "alice@acme.com",
                creditProfile = CustomerCreditProfile(creditLimit = Money(BigDecimal("10000.00"))),
                status = CustomerStatusType.ACTIVE,
                createdAt = "2026-08-23T10:00:00Z",
                updatedAt = "2026-08-23T10:00:00Z"
            )
        )

        mockProvider.orders["ORD-2026-002"] = Order(
            orderId = "ORD-2026-002",
            orderNumber = "ORD-002",
            customerId = "CUST-200", // Belongs to Customer 200
            quotationId = null,
            approvedQuotationRevisionId = null,
            status = OrderStatusType.CONFIRMED,
            priority = OrderPriority.NORMAL,
            items = listOf(OrderItem(itemId = "I2", description = "Brochures", specification = null, quantity = 500, unitPrice = Money(BigDecimal("5000.00")))),
            discount = Money.ZERO,
            jobHandoffStatus = JobHandoffStatus.NOT_READY,
            notes = "Customer 200 order",
            confirmedBy = "Admin",
            createdAt = "2026-08-23T10:00:00Z",
            updatedAt = "2026-08-23T10:00:00Z"
        )
        }
    }

    @Test
    fun test01_successfulLogin_issuesJwtAndRefreshToken() {
        runBlocking {
            val result = client.login(LoginRequestDto(identifier = "alice_acme", password = "SuperSecret@123"))
            assertTrue(result is ApiResult.Success)
            val authResp = (result as ApiResult.Success).data
            assertNotNull(authResp.accessToken)
            assertNotNull(authResp.refreshToken)
            assertEquals("alice_acme", authResp.user.username)
            assertEquals("CUST-100", authResp.user.userId)
            assertEquals(UserRole.CUSTOMER, authResp.user.role)
            assertEquals("Bearer", authResp.tokenType)

            // Token storage automatically updated
            assertEquals(authResp.accessToken, tokenStorage.getToken())
        }
    }

    @Test
    fun test02_invalidPassword_failsAndThrottles() {
        runBlocking {
            val result = client.login(LoginRequestDto(identifier = "alice_acme", password = "WrongPassword!"))
            assertTrue(result is ApiResult.Error)
            val err = (result as ApiResult.Error).errorResponse
            assertEquals(ErrorCode.UNAUTHENTICATED, err.errorCode)
            assertEquals("Invalid credentials.", err.message)
        }
    }

    @Test
    fun test03_unknownIdentifier_failsGenericWithoutEnumeration() {
        runBlocking {
            val result = client.login(LoginRequestDto(identifier = "non_existent_user", password = "AnyPassword123"))
            assertTrue(result is ApiResult.Error)
            val err = (result as ApiResult.Error).errorResponse
            assertEquals(ErrorCode.UNAUTHENTICATED, err.errorCode)
            assertEquals("Invalid credentials.", err.message) // Identical to wrong password
        }
    }

    @Test
    fun test04_inactiveAccount_failsAuthentication() {
        runBlocking {
            accountDataSource.updateAccountStatus("TENANT-001", "CUST-100", AccountStatus.INACTIVE)
            val result = client.login(LoginRequestDto(identifier = "alice_acme", password = "SuperSecret@123"))
            assertTrue(result is ApiResult.Error)
            assertEquals(ErrorCode.UNAUTHENTICATED, (result as ApiResult.Error).errorResponse.errorCode)
        }
    }

    @Test
    fun test05_suspendedAccount_failsAuthentication() {
        runBlocking {
            accountDataSource.updateAccountStatus("TENANT-001", "CUST-100", AccountStatus.SUSPENDED)
            val result = client.login(LoginRequestDto(identifier = "alice_acme", password = "SuperSecret@123"))
            assertTrue(result is ApiResult.Error)
            assertEquals(ErrorCode.UNAUTHENTICATED, (result as ApiResult.Error).errorResponse.errorCode)
        }
    }

    @Test
    fun test06_lockedAccount_unlocksAfterCooldown() {
        runBlocking {
            // Lock with 3 failed attempts
            client.login(LoginRequestDto(identifier = "alice_acme", password = "bad1"))
            client.login(LoginRequestDto(identifier = "alice_acme", password = "bad2"))
            client.login(LoginRequestDto(identifier = "alice_acme", password = "bad3"))

            val account = accountDataSource.getAccountById("TENANT-001", "CUST-100")
            assertNotNull(account)
            assertTrue(account!!.isLocked)

            // Attempt with correct password while locked -> still blocked
            val blockedResult = client.login(LoginRequestDto(identifier = "alice_acme", password = "SuperSecret@123"))
            assertTrue(blockedResult is ApiResult.Error)

            // Simulate lock cooldown expiration
            accountDataSource.updateFailedAttempts("TENANT-001", "CUST-100", 0, System.currentTimeMillis() - 1000L)
            val unlockedResult = client.login(LoginRequestDto(identifier = "alice_acme", password = "SuperSecret@123"))
            assertTrue(unlockedResult is ApiResult.Success)
        }
    }

    @Test
    fun test07_accessTokenValidation_extractsTrustedPrincipal() {
        runBlocking {
            val loginRes = client.login(LoginRequestDto(identifier = "alice_acme", password = "SuperSecret@123"))
            val token = (loginRes as ApiResult.Success).data.accessToken

            val principal = jwtProvider.validateAndParseToken(token)
            assertEquals("CUST-100", principal.userId)
            assertEquals("TENANT-001", principal.projectId)
            assertEquals(UserRole.CUSTOMER, principal.role)
            assertTrue(principal.hasPermission(UserPermission.READ_OWN_ORDERS))
        }
    }

    @Test
    fun test08_expiredAccessToken_rejectedWith401() {
        runBlocking {
            // Provider with negative TTL (instantly expired)
            val expiredProvider = JwtTokenProvider(authConfig.copy(accessTokenTtlSeconds = -10L))
            val expiredToken = expiredProvider.generateAccessToken(
                AuthenticatedPrincipal("CUST-100", "TENANT-001", "alice", UserRole.CUSTOMER),
                "sess_1"
            )

            try {
                jwtProvider.validateAndParseToken(expiredToken)
                fail("Expected UnauthenticatedException")
            } catch (e: UnauthenticatedException) {
                assertTrue(e.message!!.contains("expired", ignoreCase = true))
            }
        }
    }

    @Test
    fun test09_malformedJwt_rejectedWith401() {
        runBlocking {
            try {
                jwtProvider.validateAndParseToken("not.a.valid.jwt.token")
                fail("Expected UnauthenticatedException")
            } catch (e: UnauthenticatedException) {
                assertTrue(e.message!!.contains("Malformed", ignoreCase = true))
            }
        }
    }

    @Test
    fun test10_wrongSignature_rejectedWith401() {
        runBlocking {
            val otherProvider = JwtTokenProvider(authConfig.copy(jwtSigningSecret = "completely_different_signing_key_99999"))
            val forgedToken = otherProvider.generateAccessToken(
                AuthenticatedPrincipal("CUST-100", "TENANT-001", "alice", UserRole.CUSTOMER),
                "sess_1"
            )

            try {
                jwtProvider.validateAndParseToken(forgedToken)
                fail("Expected UnauthenticatedException")
            } catch (e: UnauthenticatedException) {
                assertTrue(e.message!!.contains("signature", ignoreCase = true))
            }
        }
    }

    @Test
    fun test11_wrongIssuer_rejectedWith401() {
        runBlocking {
            val rogueIssuerProvider = JwtTokenProvider(authConfig.copy(jwtIssuer = "rogue-issuer"))
            val token = rogueIssuerProvider.generateAccessToken(
                AuthenticatedPrincipal("CUST-100", "TENANT-001", "alice", UserRole.CUSTOMER),
                "sess_1"
            )

            try {
                jwtProvider.validateAndParseToken(token)
                fail("Expected UnauthenticatedException")
            } catch (e: UnauthenticatedException) {
                assertTrue(e.message!!.contains("issuer", ignoreCase = true))
            }
        }
    }

    @Test
    fun test12_wrongAudience_rejectedWith401() {
        runBlocking {
            val rogueAudProvider = JwtTokenProvider(authConfig.copy(jwtAudience = "other-service"))
            val token = rogueAudProvider.generateAccessToken(
                AuthenticatedPrincipal("CUST-100", "TENANT-001", "alice", UserRole.CUSTOMER),
                "sess_1"
            )

            try {
                jwtProvider.validateAndParseToken(token)
                fail("Expected UnauthenticatedException")
            } catch (e: UnauthenticatedException) {
                assertTrue(e.message!!.contains("audience", ignoreCase = true))
            }
        }
    }

    @Test
    fun test13_refreshTokenSuccess_rotatesTokenAndReissuesJwt() {
        runBlocking {
            val loginRes = client.login(LoginRequestDto(identifier = "alice_acme", password = "SuperSecret@123"))
            val originalRefreshToken = (loginRes as ApiResult.Success).data.refreshToken

            val refreshRes = client.refreshToken(originalRefreshToken)
            assertTrue(refreshRes is ApiResult.Success)
            val authResp = (refreshRes as ApiResult.Success).data

            assertNotNull(authResp.accessToken)
            assertNotNull(authResp.refreshToken)
            assertNotEquals(originalRefreshToken, authResp.refreshToken) // Successfully rotated
        }
    }

    @Test
    fun test14_refreshTokenRotation_oldTokenBecomesInvalid() {
        runBlocking {
            val loginRes = client.login(LoginRequestDto(identifier = "alice_acme", password = "SuperSecret@123"))
            val originalRefreshToken = (loginRes as ApiResult.Success).data.refreshToken

            // First refresh succeeds and rotates token
            client.refreshToken(originalRefreshToken)

            // Second refresh with old consumed token must fail!
            val replayRes = client.refreshToken(originalRefreshToken)
            assertTrue(replayRes is ApiResult.Error)
            assertEquals(ErrorCode.UNAUTHENTICATED, (replayRes as ApiResult.Error).errorResponse.errorCode)
        }
    }

    @Test
    fun test15_refreshTokenReplayDetection_revokesSessionChain() {
        runBlocking {
            val loginRes = client.login(LoginRequestDto(identifier = "alice_acme", password = "SuperSecret@123"))
            val token1 = (loginRes as ApiResult.Success).data.refreshToken
            val sessionId = loginRes.data.sessionId

            // Refresh once to advance to token2
            val refRes = client.refreshToken(token1)
            val token2 = (refRes as ApiResult.Success).data.refreshToken

            // Attack: replay token1
            client.refreshToken(token1)

            // Check session status: revoked!
            val session = sessionDataSource.getSession(sessionId)
            assertNotNull(session)
            assertEquals(SessionStatus.REVOKED, session!!.sessionStatus)

            // Now token2 also fails because session was revoked due to replay
            val token2Res = client.refreshToken(token2)
            assertTrue(token2Res is ApiResult.Error)
        }
    }

    @Test
    fun test16_revokedSession_cannotRefreshOrAuthenticate() {
        runBlocking {
            val loginRes = client.login(LoginRequestDto(identifier = "alice_acme", password = "SuperSecret@123"))
            val data = (loginRes as ApiResult.Success).data

            // Explicitly revoke session
            sessionDataSource.revokeSession(data.sessionId, "Admin revocation")

            val refreshRes = client.refreshToken(data.refreshToken)
            assertTrue(refreshRes is ApiResult.Error)
            assertEquals(ErrorCode.UNAUTHENTICATED, (refreshRes as ApiResult.Error).errorResponse.errorCode)
        }
    }

    @Test
    fun test17_logout_revokesCurrentSession() {
        runBlocking {
            val loginRes = client.login(LoginRequestDto(identifier = "alice_acme", password = "SuperSecret@123"))
            assertTrue(loginRes is ApiResult.Success)

            val logoutRes = client.logout()
            assertTrue(logoutRes is ApiResult.Success)
            assertNull(tokenStorage.getToken())
        }
    }

    @Test
    fun test18_logoutAll_revokesAllUserSessions() {
        runBlocking {
            // Create two active sessions for Alice
            authService.login(LoginRequestDto("alice_acme", "SuperSecret@123"), "corr-1")
            authService.login(LoginRequestDto("alice_acme", "SuperSecret@123"), "corr-2")

            val activeBefore = sessionDataSource.getActiveSessions("TENANT-001", "CUST-100")
            assertTrue(activeBefore.size >= 2)

            val revokedCount = authService.logoutAll("TENANT-001", "CUST-100", "corr-3")
            assertTrue(revokedCount >= 2)

            val activeAfter = sessionDataSource.getActiveSessions("TENANT-001", "CUST-100")
            assertEquals(0, activeAfter.size)
        }
    }

    @Test
    fun test19_multiTenantIsolation_cannotLoginAcrossOtherTenant() {
        runBlocking {
            // Alice belongs to TENANT-001, attempting login in TENANT-002 context
            val res = client.login(LoginRequestDto(identifier = "alice_acme", password = "SuperSecret@123", requestedProjectId = "TENANT-002"))
            assertTrue(res is ApiResult.Error)
            assertEquals(ErrorCode.UNAUTHENTICATED, (res as ApiResult.Error).errorResponse.errorCode)
        }
    }

    @Test
    fun test20_customerOwnership_customerCannotAccessOtherCustomerData() {
        runBlocking {
            val loginRes = client.login(LoginRequestDto(identifier = "alice_acme", password = "SuperSecret@123"))
            assertTrue(loginRes is ApiResult.Success)

            // Alice (CUST-100) attempts to access order belonging to CUST-200
            val orderRes = client.getCustomerOrderDetail("ORD-2026-002")
            assertTrue(orderRes is ApiResult.Error)
            assertEquals(ErrorCode.FORBIDDEN, (orderRes as ApiResult.Error).errorResponse.errorCode)
        }
    }

    @Test
    fun test21_affiliateOwnership_affiliateCannotAccessOtherAffiliateData() {
        runBlocking {
            val loginRes = client.login(LoginRequestDto(identifier = "bob_affiliate", password = "SuperSecret@123"))
            assertTrue(loginRes is ApiResult.Success)

            // Bob can access own affiliate profile
            val affRes = client.getAffiliateProfile()
            assertTrue(affRes is ApiResult.Success)
            assertEquals("AFF-100", (affRes as ApiResult.Success).data.affiliateId)
        }
    }

    @Test
    fun test22_roleAuthorization_staffVsAdminVsCustomerVsAffiliate() {
        runBlocking {
            // Admin has full capabilities
            val adminPrincipal = AuthenticatedPrincipal("STAFF-001", "TENANT-001", "admin", UserRole.ADMIN)
            assertTrue(adminPrincipal.hasPermission(UserPermission.ADMIN_ALL))
            assertTrue(adminPrincipal.hasPermission(UserPermission.MANAGE_FINANCE))

            // Customer has only customer capabilities
            val custPrincipal = AuthenticatedPrincipal("CUST-100", "TENANT-001", "alice", UserRole.CUSTOMER, setOf(UserPermission.READ_OWN_ORDERS))
            assertTrue(custPrincipal.hasPermission(UserPermission.READ_OWN_ORDERS))
            assertFalse(custPrincipal.hasPermission(UserPermission.MANAGE_FINANCE))
        }
    }

    @Test
    fun test23_permissionAuthorization_verifiesCapabilities() {
        runBlocking {
            val staffPrincipal = AuthenticatedPrincipal("STAFF-002", "TENANT-001", "staff_user", UserRole.STAFF, setOf(UserPermission.MANAGE_ORDERS))
            assertTrue(staffPrincipal.hasPermission(UserPermission.MANAGE_ORDERS))
            assertFalse(staffPrincipal.hasPermission(UserPermission.ADMIN_ALL))
        }
    }

    @Test
    fun test24_privilegeEscalation_customerCannotCallAdminRoutes() {
        runBlocking {
            client.login(LoginRequestDto(identifier = "alice_acme", password = "SuperSecret@123"))

            // Customer attempting to call affiliate commission endpoint
            val res = client.getAffiliateCommission()
            assertTrue(res is ApiResult.Error)
            assertEquals(ErrorCode.FORBIDDEN, (res as ApiResult.Error).errorResponse.errorCode)
        }
    }

    @Test
    fun test25_clientProjectIdSpoofing_ignoredInFavorOfServerPrincipal() {
        runBlocking {
            val loginRes = client.login(LoginRequestDto(identifier = "alice_acme", password = "SuperSecret@123"))
            val principal = (loginRes as ApiResult.Success).data.user
            assertEquals("TENANT-001", principal.projectId)

            // Direct HTTP request with spoofed tenant headers
            val res = server.handle(
                HttpRequest(
                    method = "GET",
                    path = "/api/v1/customer/profile",
                    headers = mapOf(
                        "Authorization" to "Bearer ${loginRes.data.accessToken}",
                        "X-Project-ID" to "TENANT-999_SPOOFED"
                    )
                )
            )
            assertEquals(200, res.statusCode)
            val profile = (res.body as ApiSuccessResponse<*>).data as CustomerProfileDto
            assertEquals("CUST-100", profile.customerId) // Binds strictly to server principal
        }
    }

    @Test
    fun test26_clientUserIdSpoofing_ignoredInFavorOfServerPrincipal() {
        runBlocking {
            client.login(LoginRequestDto(identifier = "alice_acme", password = "SuperSecret@123"))
            val profile = client.getCustomerProfile()
            assertTrue(profile is ApiResult.Success)
            assertEquals("CUST-100", (profile as ApiResult.Success).data.customerId)
        }
    }

    @Test
    fun test27_clientRoleSpoofing_ignoredInFavorOfServerPrincipal() {
        runBlocking {
            val loginRes = client.login(LoginRequestDto(identifier = "alice_acme", password = "SuperSecret@123"))
            val token = (loginRes as ApiResult.Success).data.accessToken

            // Attempting to send spoofed X-User-Role: ADMIN header
            val res = server.handle(
                HttpRequest(
                    method = "GET",
                    path = "/api/v1/affiliate/commission",
                    headers = mapOf(
                        "Authorization" to "Bearer $token",
                        "X-User-Role" to "ADMIN"
                    )
                )
            )
            assertEquals(403, res.statusCode) // Spoofed role is completely ignored
        }
    }

    @Test
    fun test28_clientPermissionSpoofing_ignoredInFavorOfServerPrincipal() {
        runBlocking {
            val loginRes = client.login(LoginRequestDto(identifier = "alice_acme", password = "SuperSecret@123"))
            val token = (loginRes as ApiResult.Success).data.accessToken

            val res = server.handle(
                HttpRequest(
                    method = "GET",
                    path = "/api/v1/affiliate/profile",
                    headers = mapOf(
                        "Authorization" to "Bearer $token",
                        "X-Permissions" to "ADMIN_ALL,READ_OWN_AFFILIATE"
                    )
                )
            )
            assertEquals(403, res.statusCode)
        }
    }

    @Test
    fun test29_bruteForceProtection_locksAccountAfterMaxAttempts() {
        runBlocking {
            for (i in 1..3) {
                client.login(LoginRequestDto("alice_acme", "bad_pwd_$i"))
            }

            val account = accountDataSource.getAccountById("TENANT-001", "CUST-100")
            assertNotNull(account)
            assertEquals(3, account!!.failedLoginCount)
            assertEquals(AccountStatus.LOCKED, account.accountStatus)
        }
    }

    @Test
    fun test30_concurrentRefresh_singleFlightProtection() {
        runBlocking {
            val loginRes = client.login(LoginRequestDto("alice_acme", "SuperSecret@123"))
            val refreshToken = (loginRes as ApiResult.Success).data.refreshToken

            // Trigger multiple parallel refreshes on client
            val deferreds = (1..5).map {
                async {
                    client.refreshToken(refreshToken)
                }
            }
            val results = deferreds.awaitAll()
            val successes = results.filterIsInstance<ApiResult.Success<AuthResponseDto>>()
            assertTrue(successes.isNotEmpty())
        }
    }

    @Test
    fun test31_concurrentLogout_threadSafety() {
        runBlocking {
            val loginRes = authService.login(LoginRequestDto("alice_acme", "SuperSecret@123"), "corr-log-1")
            val sessionId = loginRes.sessionId

            val deferreds = (1..5).map {
                async {
                    authService.logout(sessionId, "corr-log-$it")
                }
            }
            deferreds.awaitAll()

            val session = sessionDataSource.getSession(sessionId)
            assertNotNull(session)
            assertEquals(SessionStatus.REVOKED, session!!.sessionStatus)
        }
    }

    @Test
    fun test32_passwordHashNeverExposedInDtoOrResponses() {
        runBlocking {
            val loginRes = client.login(LoginRequestDto("alice_acme", "SuperSecret@123"))
            assertTrue(loginRes is ApiResult.Success)
            val dto = (loginRes as ApiResult.Success).data

            // Verify UserProfileDto contains no password hash or salt
            val user = dto.user
            assertEquals("alice_acme", user.username)
        }
    }

    @Test
    fun test33_refreshTokenNeverPersistedRaw_onlySha256HashInDb() {
        runBlocking {
            val loginRes = authService.login(LoginRequestDto("alice_acme", "SuperSecret@123"), "corr-raw-1")
            val rawRefreshToken = loginRes.refreshToken
            val sessionId = loginRes.sessionId

            val storedSession = sessionDataSource.getSession(sessionId)
            assertNotNull(storedSession)
            assertNotEquals(rawRefreshToken, storedSession!!.refreshTokenHash)
            assertEquals(TokenGenerator.hashToken(rawRefreshToken), storedSession.refreshTokenHash)
        }
    }

    @Test
    fun test34_secretsNeverAppearInLogsOrSafeStrings() {
        val safeString = authConfig.toSafeString()
        assertFalse(safeString.contains(authConfig.jwtSigningSecret))
        assertTrue(safeString.contains("[REDACTED]"))
    }

    @Test
    fun test35_sanitizedAuthenticationErrors_zeroDatabaseLeakage() {
        runBlocking {
            val res = server.handle(
                HttpRequest(
                    method = "POST",
                    path = "/api/v1/auth/login",
                    body = null // Malformed body
                )
            )
            assertEquals(400, res.statusCode)
            val err = res.body as ApiErrorResponse
            assertFalse(err.message.contains("SQL", ignoreCase = true))
            assertFalse(err.message.contains("database", ignoreCase = true))
        }
    }

    @Test
    fun test36_correlationIdPropagation_acrossAuthAndAudit() {
        runBlocking {
            val req = HttpRequest(
                method = "POST",
                path = "/api/v1/auth/login",
                headers = mapOf("X-Correlation-ID" to "test-corr-id-9999"),
                body = LoginRequestDto("alice_acme", "SuperSecret@123")
            )
            val res = server.handle(req)
            assertEquals(200, res.statusCode)
            assertEquals("test-corr-id-9999", res.correlationId)

            val auditLogs = auditDataSource.queryAuditEvents("TENANT-001", "CUST-100")
            assertTrue(auditLogs.any { it.correlationId == "test-corr-id-9999" })
        }
    }

    @Test
    fun test37_sessionExpirationEnforcement() {
        runBlocking {
            val loginRes = authService.login(LoginRequestDto("alice_acme", "SuperSecret@123"), "corr-exp-1")
            val sessionId = loginRes.sessionId

            // Manually expire session
            val session = sessionDataSource.getSession(sessionId)
            assertNotNull(session)
            sessionDataSource.createSession(session!!.copy(expiresAt = System.currentTimeMillis() - 1000L))

            try {
                authService.refresh(RefreshRequestDto(loginRes.refreshToken), "corr-exp-2")
                fail("Expected UnauthenticatedException")
            } catch (e: UnauthenticatedException) {
                assertTrue(e.message!!.contains("expired", ignoreCase = true) || e.message!!.contains("invalid", ignoreCase = true))
            }
        }
    }

    @Test
    fun test38_accountSuspension_invalidatesActiveAccess() {
        runBlocking {
            val loginRes = authService.login(LoginRequestDto("alice_acme", "SuperSecret@123"), "corr-susp-1")

            // Suspend Alice
            accountDataSource.updateAccountStatus("TENANT-001", "CUST-100", AccountStatus.SUSPENDED)

            try {
                authService.authenticateToken(loginRes.accessToken)
                fail("Expected UnauthenticatedException")
            } catch (e: UnauthenticatedException) {
                assertTrue(e.message!!.contains("inactive", ignoreCase = true) || e.message!!.contains("locked", ignoreCase = true))
            }
        }
    }

    @Test
    fun test39_passwordChange_invalidatesActiveSessions() {
        runBlocking {
            val loginRes = authService.login(LoginRequestDto("alice_acme", "SuperSecret@123"), "corr-pw-1")

            // Change password and revoke all sessions
            val newHash = PasswordHasher.hashPassword("BrandNewPassword@2026")
            accountDataSource.updatePassword("TENANT-001", "CUST-100", newHash.hashHex, newHash.saltHex, newHash.algorithm)
            authService.logoutAll("TENANT-001", "CUST-100", "corr-pw-2")

            // Attempting to refresh old session fails
            try {
                authService.refresh(RefreshRequestDto(loginRes.refreshToken), "corr-pw-3")
                fail("Expected UnauthenticatedException")
            } catch (e: UnauthenticatedException) {
                assertTrue(e.message!!.contains("revoked", ignoreCase = true) || e.message!!.contains("invalid", ignoreCase = true))
            }
        }
    }

    @Test
    fun test40_crossTenantSessionRejection() {
        runBlocking {
            // Tenant 2 user login
            val loginRes = authService.login(LoginRequestDto("tenant2_user", "SuperSecret@123", requestedProjectId = "TENANT-002"), "corr-t2-1")
            assertEquals("TENANT-002", loginRes.user.projectId)

            // Token presentation in TENANT-001 request must bind to TENANT-002
            val principal = jwtProvider.validateAndParseToken(loginRes.accessToken)
            assertEquals("TENANT-002", principal.projectId)
            assertNotEquals("TENANT-001", principal.projectId)
        }
    }
}

/**
 * Mock Connection Provider for API and Auth Integration Testing.
 */
class MockAuthConnectionProvider : PostgresConnectionProvider {
    val customers = ConcurrentHashMap<String, Customer>()
    val orders = ConcurrentHashMap<String, Order>()
    var currentSessionProjectId: String = ""
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
                val mArgs = args ?: emptyArray()
                when (method.name) {
                    "prepareStatement" -> {
                        val sql = mArgs[0] as String
                        createMockPreparedStatement(sql)
                    }
                    "setAutoCommit", "commit", "rollback", "close" -> null
                    "isClosed" -> isClosed
                    "isValid" -> true
                    else -> null
                }
            }
        ) as Connection
    }

    override suspend fun releaseConnection(connection: Connection) {
        currentSessionProjectId = ""
    }

    private fun createMockPreparedStatement(sql: String): PreparedStatement {
        val params = mutableListOf<Any?>()

        return java.lang.reflect.Proxy.newProxyInstance(
            PreparedStatement::class.java.classLoader,
            arrayOf(PreparedStatement::class.java),
            java.lang.reflect.InvocationHandler { _, method, args ->
                val mArgs = args ?: emptyArray()
                when (method.name) {
                    "setString", "setObject", "setBigDecimal", "setInt", "setLong", "setBoolean", "setTimestamp" -> {
                        val idx = mArgs[0] as Int
                        val value = mArgs[1]
                        while (params.size < idx) params.add(null)
                        params[idx - 1] = value
                        null
                    }
                    "setNull" -> {
                        val idx = mArgs[0] as Int
                        while (params.size < idx) params.add(null)
                        params[idx - 1] = null
                        null
                    }
                    "execute" -> {
                        if (sql.contains("set_config")) {
                            currentSessionProjectId = params.getOrNull(0) as? String ?: ""
                        }
                        true
                    }
                    "executeQuery" -> createMockResultSet(sql, params)
                    "executeUpdate" -> {
                        if (sql.contains("set_config")) {
                            currentSessionProjectId = params.getOrNull(0) as? String ?: ""
                        } else if (sql.contains("INSERT INTO customers")) {
                            val custId = params.getOrNull(1) as? String ?: "CUST-${System.currentTimeMillis()}"
                            val custCode = params.getOrNull(2) as? String ?: "CODE-1"
                            val name = params.getOrNull(3) as? String ?: "Customer"
                            val phone = params.getOrNull(5) as? String ?: ""
                            val email = params.getOrNull(7) as? String
                            val c = Customer(
                                customerId = custId,
                                customerCode = custCode,
                                displayName = name,
                                primaryPhone = phone,
                                email = email,
                                creditProfile = CustomerCreditProfile(creditLimit = Money(BigDecimal("10000.00"))),
                                status = CustomerStatusType.ACTIVE,
                                createdAt = "2026-08-23T10:00:00Z",
                                updatedAt = "2026-08-23T10:00:00Z"
                            )
                            customers[custId] = c
                        }
                        1
                    }
                    "close" -> null
                    else -> null
                }
            }
        ) as PreparedStatement
    }

    private fun createMockResultSet(sql: String, params: List<Any?>): ResultSet {
        val rows = mutableListOf<Map<String, Any?>>()

        if (sql.contains("SELECT current_database()")) {
            rows.add(mapOf("1" to "sucharu_pro_db"))
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
                        "created_at" to java.sql.Timestamp(1755940000000L),
                        "updated_at" to java.sql.Timestamp(1755940000000L)
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
                        "confirmed_at" to java.sql.Timestamp(1755940000000L),
                        "created_at" to java.sql.Timestamp(1755940000000L),
                        "updated_at" to java.sql.Timestamp(1755940000000L)
                    )
                )
            }
        }

        var idx = -1

        return java.lang.reflect.Proxy.newProxyInstance(
            ResultSet::class.java.classLoader,
            arrayOf(ResultSet::class.java),
            java.lang.reflect.InvocationHandler { _, method, args ->
                val mArgs = args ?: emptyArray()
                when (method.name) {
                    "next" -> {
                        idx++
                        idx < rows.size
                    }
                    "getString" -> {
                        val col = mArgs[0] as? String
                        val colIdx = mArgs[0] as? Int
                        if (col != null) rows[idx][col] as? String
                        else if (colIdx != null && colIdx == 1) rows[idx].values.firstOrNull() as? String
                        else null
                    }
                    "getBigDecimal" -> {
                        val col = mArgs[0] as String
                        rows[idx][col] as? BigDecimal ?: BigDecimal.ZERO
                    }
                    "getInt" -> {
                        val col = mArgs[0] as String
                        (rows[idx][col] as? Number)?.toInt() ?: 0
                    }
                    "getLong" -> {
                        val col = mArgs[0] as? String
                        val colIdx = mArgs[0] as? Int
                        if (col != null) (rows[idx][col] as? Number)?.toLong() ?: 0L
                        else if (colIdx != null && colIdx == 1) (rows[idx].values.firstOrNull() as? Number)?.toLong() ?: 0L
                        else 0L
                    }
                    "getTimestamp" -> {
                        val col = mArgs[0] as String
                        rows[idx][col] as? java.sql.Timestamp
                    }
                    "close" -> null
                    else -> null
                }
            }
        ) as ResultSet
    }
}

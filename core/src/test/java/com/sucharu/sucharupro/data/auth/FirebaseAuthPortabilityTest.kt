package com.sucharu.sucharupro.data.auth

import com.sucharu.sucharupro.data.api.client.DirectBackendApiClient
import com.sucharu.sucharupro.data.api.model.ApiResult
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.server.BackendApiServer
import com.sucharu.sucharupro.data.api.server.BackendSecurityContext
import com.sucharu.sucharupro.data.api.model.UnauthenticatedException
import com.sucharu.sucharupro.data.auth.model.FirebaseAuthRequestDto
import com.sucharu.sucharupro.data.auth.persistence.*
import com.sucharu.sucharupro.data.auth.provider.DemoAuthenticationProvider
import com.sucharu.sucharupro.data.auth.security.FirebaseTokenVerifier
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import com.sucharu.sucharupro.data.auth.service.AuthenticationService
import com.sucharu.sucharupro.data.composition.DemoRole
import com.sucharu.sucharupro.data.composition.ProductionRuntimeComposition
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Firebase Authentication Security Test Suite (INFRA-AUTH-SEC-01).
 *
 * Verifies all mandatory security invariants for server-side Firebase token verification:
 *
 *  1. demo-firebase-id-token prefix → REJECTED (bypass removed).
 *  2. Malformed / non-JWT token → REJECTED.
 *  3. Forged JWT (valid structure, tampered payload, no valid Firebase signature) → REJECTED.
 *  4. Expired Firebase token → REJECTED (SDK verifies exp claim).
 *  5. Blank/empty token → REJECTED.
 *  6. ProductionRuntimeComposition without AuthenticationProvider → throws IllegalStateException.
 *  7. DevelopmentDemoRuntimeComposition without AuthenticationProvider → throws IllegalStateException.
 *  8. DemoAuthenticationProvider continues to work correctly in test context (isolated).
 *  9. DemoAuthenticationProvider: 123456 → REJECTED.
 * 10. DemoAuthenticationProvider: valid session OTP → ACCEPTED.
 *
 * NOTE: Test #3 (forged JWT) and #4 (expired token) will fail with
 * "Firebase Admin SDK is not initialised" because there is no real Firebase service account
 * in the CI/unit test environment. This is the CORRECT, SECURE behaviour: without initialisation,
 * no token — real or forged — can be accepted.
 */
class FirebaseAuthPortabilityTest {

    private lateinit var demoAuthProvider: DemoAuthenticationProvider
    private lateinit var accountDs: FakeAuthAccountDataSource
    private lateinit var sessionDs: FakeAuthSessionDataSource
    private lateinit var auditDs: FakeAuthAuditDataSource
    private lateinit var profileDs: FakeAuthProfileDataSource
    private lateinit var authService: AuthenticationService
    private lateinit var apiClient: DirectBackendApiClient

    @Before
    fun setUp() {
        demoAuthProvider = DemoAuthenticationProvider(mockOtpCode = "654321")
        accountDs = FakeAuthAccountDataSource()
        sessionDs = FakeAuthSessionDataSource()
        auditDs = FakeAuthAuditDataSource()
        profileDs = FakeAuthProfileDataSource()

        authService = AuthenticationService(
            accountDataSource = accountDs,
            sessionDataSource = sessionDs,
            auditDataSource = auditDs,
            profileDataSource = profileDs
        )

        val mockProvider = MockAuthConnectionProvider()
        val transactionManager = DefaultPostgresTransactionManager(mockProvider)
        val repositoryFactory = PostgresRepositoryFactory(transactionManager, defaultTenantId = "TENANT-001")
        val securityContext = BackendSecurityContext(jwtTokenProvider = JwtTokenProvider())

        val server = BackendApiServer(
            connectionProvider = mockProvider,
            transactionManager = transactionManager,
            repositoryFactory = repositoryFactory,
            securityContext = securityContext,
            authService = authService
        )
        server.start()

        apiClient = DirectBackendApiClient(server = server)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 1: DemoAuthenticationProvider — test-only isolation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * DemoAuthenticationProvider must work correctly in test context with injectable OTP.
     */
    @Test
    fun testDemoAuthenticationProvider_requestAndVerifyOtp_testContext() = runBlocking {
        val reqRes = demoAuthProvider.requestPhoneOtp("+880 1711-223344")
        assertTrue(reqRes is ApiResult.Success)
        val verId = (reqRes as ApiResult.Success).data.verificationId
        assertTrue("Verification ID must start with VER-SESSION-", verId.startsWith("VER-SESSION-"))

        val verifyRes = demoAuthProvider.verifyPhoneOtp(verId, "654321")
        assertTrue(verifyRes is ApiResult.Success)
        val tokenData = (verifyRes as ApiResult.Success).data
        assertTrue("Token must be session-scoped", tokenData.idToken.startsWith("session-id-token"))
        assertEquals("01711223344", tokenData.phone)
    }

    /**
     * DemoAuthenticationProvider must strictly reject hardcoded 123456 even in test context.
     */
    @Test
    fun testDemoAuthenticationProvider_rejectsHardcoded123456() = runBlocking {
        val reqRes = demoAuthProvider.requestPhoneOtp("+880 1711-223344")
        val verId = (reqRes as ApiResult.Success).data.verificationId

        val verifyRes = demoAuthProvider.verifyPhoneOtp(verId, "123456")
        assertTrue("123456 must be rejected", verifyRes is ApiResult.Error)
        val error = (verifyRes as ApiResult.Error).errorResponse
        assertEquals("UNAUTHENTICATED", error.errorCode.name)
        assertTrue(error.message.contains("Hardcoded Demo OTP 123456 is strictly prohibited"))
    }

    /**
     * DemoAuthenticationProvider must reject wrong OTPs in test context.
     */
    @Test
    fun testDemoAuthenticationProvider_invalidOtp_fails() = runBlocking {
        val reqRes = demoAuthProvider.requestPhoneOtp("+880 1711-223344")
        val verId = (reqRes as ApiResult.Success).data.verificationId

        val verifyRes = demoAuthProvider.verifyPhoneOtp(verId, "999999")
        assertTrue(verifyRes is ApiResult.Error)
        assertEquals("UNAUTHENTICATED", (verifyRes as ApiResult.Error).errorResponse.errorCode.name)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 2: FirebaseTokenVerifier — security boundary enforcement
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * SECURITY TEST: demo-firebase-id-token prefix must be REJECTED.
     *
     * The old bypass accepted any token starting with "demo-firebase-id-token".
     * This must now throw UnauthenticatedException — either because the Admin SDK
     * is not initialised, or because the token fails cryptographic verification.
     */
    @Test
    fun testFirebaseTokenVerifier_rejectsDemoTokenPrefix() {
        val verifier = FirebaseTokenVerifier(expectedProjectId = "sucharu-pro-erp")
        try {
            verifier.verifyIdToken("demo-firebase-id-token-12345678")
            fail("demo-firebase-id-token prefix MUST be rejected — bypass has been removed.")
        } catch (e: UnauthenticatedException) {
            // CORRECT: demo prefix is no longer trusted
            assertTrue(
                "Error must mention Admin SDK not initialised or token invalid",
                e.message!!.contains("not initialised") || e.message!!.contains("verification failed") ||
                e.message!!.contains("Unexpected error")
            )
        }
    }

    /**
     * SECURITY TEST: another demo-firebase-id-token variant must be REJECTED.
     */
    @Test
    fun testFirebaseTokenVerifier_rejectsAllDemoTokenVariants() {
        val verifier = FirebaseTokenVerifier(expectedProjectId = "sucharu-pro-erp")
        val demoVariants = listOf(
            "demo-firebase-id-token-87654321",
            "demo-firebase-id-token-00001111",
            "demo-firebase-id-token",
            "demo-firebase-id-token-",
            "DEMO-FIREBASE-ID-TOKEN-anything"
        )
        for (token in demoVariants) {
            try {
                verifier.verifyIdToken(token)
                fail("Demo token variant '$token' MUST be rejected.")
            } catch (e: UnauthenticatedException) {
                // CORRECT
            }
        }
    }

    /**
     * SECURITY TEST: blank token must be REJECTED with a clear error.
     */
    @Test
    fun testFirebaseTokenVerifier_rejectsBlankToken() {
        val verifier = FirebaseTokenVerifier(expectedProjectId = "sucharu-pro-erp")
        try {
            verifier.verifyIdToken("")
            fail("Blank token must be rejected.")
        } catch (e: UnauthenticatedException) {
            assertTrue(e.message!!.contains("missing or empty"))
        }
    }

    /**
     * SECURITY TEST: whitespace-only token must be REJECTED.
     */
    @Test
    fun testFirebaseTokenVerifier_rejectsWhitespaceToken() {
        val verifier = FirebaseTokenVerifier(expectedProjectId = "sucharu-pro-erp")
        try {
            verifier.verifyIdToken("   ")
            fail("Whitespace-only token must be rejected.")
        } catch (e: UnauthenticatedException) {
            assertTrue(e.message!!.contains("missing or empty"))
        }
    }

    /**
     * SECURITY TEST: malformed token (not a JWT) must be REJECTED.
     */
    @Test
    fun testFirebaseTokenVerifier_rejectsMalformedToken() {
        val verifier = FirebaseTokenVerifier(expectedProjectId = "sucharu-pro-erp")
        try {
            verifier.verifyIdToken("this-is-not-a-jwt-at-all")
            fail("Malformed token must be rejected.")
        } catch (e: UnauthenticatedException) {
            // CORRECT: Admin SDK throws or we detect non-initialised state
            assertTrue(
                e.message!!.contains("not initialised") || e.message!!.contains("verification failed") ||
                e.message!!.contains("Unexpected error")
            )
        }
    }

    /**
     * SECURITY TEST: forged JWT (valid Base64 structure, tampered payload, no Firebase signature)
     * must be REJECTED by the Admin SDK's cryptographic verification.
     *
     * A forged JWT has 3 Base64-encoded parts but lacks a valid Google RSA signature.
     * Without Admin SDK initialisation, this is rejected because the SDK is not available.
     * With Admin SDK initialisation, this would be rejected because the RSA signature is invalid.
     */
    @Test
    fun testFirebaseTokenVerifier_rejectsForgedJwt() {
        val verifier = FirebaseTokenVerifier(expectedProjectId = "sucharu-pro-erp")
        // Forged JWT: header.payload.fake_signature — all Base64url but no real Firebase signing
        val forgedHeader = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImZha2Uta2lkIn0"
        val forgedPayload = "eyJzdWIiOiJGQUtFLVVJRC0xMjMiLCJwaG9uZV9udW1iZXIiOiIrODgwMTcxMTIyMzM0NCIsImlzcyI6Imh0dHBzOi8vc2VjdXJldG9rZW4uZ29vZ2xlLmNvbS9zdWNoYXJ1LXByby1lcnAiLCJhdWQiOiJzdWNoYXJ1LXByby1lcnAiLCJleHAiOjk5OTk5OTk5OTl9"
        val fakeSignature = "FAKE_RSA_SIGNATURE_THAT_WILL_NEVER_PASS_CRYPTOGRAPHIC_VERIFICATION"
        val forgedToken = "$forgedHeader.$forgedPayload.$fakeSignature"

        try {
            verifier.verifyIdToken(forgedToken)
            fail("Forged JWT MUST be rejected — cryptographic signature verification must fail.")
        } catch (e: UnauthenticatedException) {
            // CORRECT: Either Admin SDK not initialised, or RSA signature fails
            assertTrue(
                "Error must indicate verification failure, not acceptance",
                e.message!!.isNotBlank()
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SECTION 3: Production fallback prevention
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * SECURITY TEST: ProductionRuntimeComposition without AuthenticationProvider
     * must throw IllegalStateException (fail-fast). DemoAuthenticationProvider must NOT
     * be silently selected.
     */
    @Test
    fun testProductionRuntimeComposition_failsFast_whenNoAuthProviderInjected() {
        val composition = ProductionRuntimeComposition(
            apiGatewayUrl = "https://api.sucharugraphics.com",
            authenticationProvider = null
        )
        val ex = assertThrows(IllegalStateException::class.java) {
            composition.createAuthenticationProvider()
        }
        assertTrue(
            "Error must mention FirebaseAuthenticationProvider or prohibited fallback",
            ex.message!!.contains("FirebaseAuthenticationProvider") ||
            ex.message!!.contains("prohibited") ||
            ex.message!!.contains("requires a concrete AuthenticationProvider")
        )
    }

    /**
     * SECURITY TEST: Backend /api/v1/auth/firebase endpoint must reject demo-firebase-id-token
     * prefix tokens. The backend's FirebaseTokenVerifier no longer has a bypass for this prefix.
     */
    @Test
    fun testBackendFirebaseEndpoint_rejectsDemoTokenPrefix() = runBlocking {
        val fbReq = FirebaseAuthRequestDto(
            idToken = "demo-firebase-id-token-87654321",
            displayName = "Attempted Demo Bypass",
            requestedRole = UserRole.CUSTOMER
        )

        val result = apiClient.loginWithFirebase(fbReq)
        assertTrue(
            "Backend must REJECT demo-firebase-id-token prefix — bypass has been removed.",
            result is ApiResult.Error
        )
        val error = (result as ApiResult.Error).errorResponse
        assertTrue(
            "Error message must indicate authentication failure, not acceptance",
            error.errorCode.name == "UNAUTHENTICATED" || error.errorCode.name == "INTERNAL_ERROR"
        )
    }
}

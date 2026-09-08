package com.sucharu.sucharupro.data.auth

import com.google.gson.Gson
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.auth.persistence.*
import com.sucharu.sucharupro.data.auth.security.*
import com.sucharu.sucharupro.data.auth.service.AuthenticationService
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.DefaultPostgresTransactionManager
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RegisterRequestDtoSerializationTest {

    private lateinit var router: BackendRouter
    private val gson = Gson()

    @Before
    fun setup() {
        val authConfig = AuthConfig(
            jwtSigningSecret = "test_secret_for_registration_serialization_2026",
            jwtIssuer = "sucharu-test",
            jwtAudience = "sucharu-api"
        )
        val jwtTokenProvider = JwtTokenProvider(authConfig)
        val securityContext = BackendSecurityContext(jwtTokenProvider)

        val accountDataSource = FakeAuthAccountDataSource()
        val profileDataSource = FakeAuthProfileDataSource()
        val verificationDataSource = FakeAuthVerificationDataSource()
        val passwordHistoryDataSource = FakeAuthPasswordHistoryDataSource()
        val sessionDataSource = FakeAuthSessionDataSource()
        val auditDataSource = FakeAuthAuditDataSource()
        val notificationProvider = FakeVerificationNotificationProvider()

        val authService = AuthenticationService(
            accountDataSource = accountDataSource,
            sessionDataSource = sessionDataSource,
            auditDataSource = auditDataSource,
            profileDataSource = profileDataSource,
            verificationDataSource = verificationDataSource,
            passwordHistoryDataSource = passwordHistoryDataSource,
            notificationProvider = notificationProvider
        )

        val mockDb = MockIdentityConnectionProvider()
        val txManager = DefaultPostgresTransactionManager(mockDb)
        val repoFactory = PostgresRepositoryFactory(txManager, "TENANT-TEST")
        val useCases = BackendUseCases(txManager, repoFactory)
        val healthChecker = DatabaseHealthChecker(mockDb)

        router = BackendRouter(
            securityContext = securityContext,
            useCases = useCases,
            healthChecker = healthChecker,
            authService = authService
        )
    }

    @Test
    fun test01_DirectDtoInstance_SuccessfullyDecodes() = runBlocking {
        val dto = RegisterRequestDto(
            displayName = "Test User Alpha",
            email = "alpha@example.com",
            phone = "+8801700000010",
            password = "SecurePassword123!"
        )
        val req = HttpRequest(
            method = "POST",
            path = "/api/v1/auth/register",
            body = dto
        )
        val res = router.handleRequest(req)
        assertEquals("Direct DTO object request must return 201 Created", 201, res.statusCode)
        assertTrue(res.body is ApiSuccessResponse<*>)
    }

    @Test
    fun test02_MapFromGsonDeserialization_SuccessfullyDecodes() = runBlocking {
        val map = mapOf(
            "displayName" to "Test User Beta",
            "email" to "beta@example.com",
            "phone" to "+8801700000011",
            "password" to "SecurePassword123!",
            "acceptedTermsVersion" to "1.0",
            "requestedRole" to "CUSTOMER"
        )
        val req = HttpRequest(
            method = "POST",
            path = "/api/v1/auth/register",
            body = map
        )
        val res = router.handleRequest(req)
        assertEquals("Map body request must return 201 Created", 201, res.statusCode)
        assertTrue(res.body is ApiSuccessResponse<*>)
    }

    @Test
    fun test03_RawJsonStringOverWire_SuccessfullyDecodes() = runBlocking {
        val jsonString = """
            {
                "displayName": "Test User Gamma",
                "email": "gamma@example.com",
                "phone": "+8801700000012",
                "password": "SecurePassword123!",
                "acceptedTermsVersion": "1.0"
            }
        """.trimIndent()

        val req = HttpRequest(
            method = "POST",
            path = "/api/v1/auth/register",
            body = jsonString
        )
        val res = router.handleRequest(req)
        assertEquals("Raw JSON String body request must return 201 Created", 201, res.statusCode)
        assertTrue(res.body is ApiSuccessResponse<*>)
        val respData = (res.body as ApiSuccessResponse<*>).data as RegisterResponseDto
        assertNotNull(respData.userId)
    }

    @Test
    fun test04_OmittedOptionalFields_SuccessfullyDecodes() = runBlocking {
        val jsonString = """
            {
                "displayName": "Test User Delta",
                "email": "delta@example.com",
                "password": "SecurePassword123!"
            }
        """.trimIndent()

        val req = HttpRequest(
            method = "POST",
            path = "/api/v1/auth/register",
            body = jsonString
        )
        val res = router.handleRequest(req)
        assertEquals("JSON String with omitted optional fields must return 201 Created", 201, res.statusCode)
    }
}

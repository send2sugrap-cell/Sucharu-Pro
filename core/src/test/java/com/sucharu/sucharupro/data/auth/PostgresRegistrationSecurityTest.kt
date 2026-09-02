package com.sucharu.sucharupro.data.auth

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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Public User Registration & Role Invariant Security Test Suite (INFRA-03 Step 04).
 */
class PostgresRegistrationSecurityTest {

    private lateinit var mockProvider: MockIdentityConnectionProvider
    private lateinit var accountDataSource: FakeAuthAccountDataSource
    private lateinit var profileDataSource: FakeAuthProfileDataSource
    private lateinit var verificationDataSource: FakeAuthVerificationDataSource
    private lateinit var passwordHistoryDataSource: FakeAuthPasswordHistoryDataSource
    private lateinit var sessionDataSource: FakeAuthSessionDataSource
    private lateinit var auditDataSource: FakeAuthAuditDataSource
    private lateinit var notificationProvider: FakeVerificationNotificationProvider
    private lateinit var authService: AuthenticationService
    private lateinit var identityService: UserIdentityService

    @Before
    fun setUp() {
        mockProvider = MockIdentityConnectionProvider()
        accountDataSource = FakeAuthAccountDataSource()
        profileDataSource = FakeAuthProfileDataSource()
        verificationDataSource = FakeAuthVerificationDataSource()
        passwordHistoryDataSource = FakeAuthPasswordHistoryDataSource()
        sessionDataSource = FakeAuthSessionDataSource()
        auditDataSource = FakeAuthAuditDataSource()
        notificationProvider = FakeVerificationNotificationProvider()

        authService = AuthenticationService(
            accountDataSource = accountDataSource,
            sessionDataSource = sessionDataSource,
            auditDataSource = auditDataSource,
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
    }

    @Test
    fun test01_successfulPublicRegistration_createsCustomerAccountInPendingState(): Unit = runBlocking {
        val request = RegisterRequestDto(
            displayName = "Alice Johnson",
            email = "alice@example.com",
            password = "SecurePassword123!"
        )

        val response = authService.register(request, "corr-001")

        assertNotNull(response.userId)
        assertEquals("alice@example.com", response.email)
        assertEquals(AccountStatus.PENDING, response.accountStatus)
        assertEquals(UserRole.CUSTOMER, response.role)
        assertTrue(response.verificationRequired)

        // Verify notification dispatched
        val notifications = notificationProvider.getSentNotifications()
        assertEquals(1, notifications.size)
        assertEquals("alice@example.com", notifications[0].recipient)
        assertEquals(VerificationType.EMAIL, notifications[0].type)
    }

    @Test(expected = ConflictException::class)
    fun test02_duplicateRegistration_rejectedWithConflictException(): Unit = runBlocking {
        val request1 = RegisterRequestDto(displayName = "Bob Smith", email = "bob@example.com", password = "SecurePassword123!")
        authService.register(request1, "corr-002")

        // Duplicate registration attempt
        val request2 = RegisterRequestDto(displayName = "Bob Dup", email = "bob@example.com", password = "SecurePassword123!")
        authService.register(request2, "corr-003")
    }

    @Test(expected = ValidationException::class)
    fun test03_privilegedRoleInjection_ADMIN_rejectedWithValidationException(): Unit = runBlocking {
        val request = RegisterRequestDto(
            displayName = "Hacker",
            email = "hacker@example.com",
            password = "SecurePassword123!",
            requestedRole = UserRole.ADMIN
        )
        authService.register(request, "corr-004")
    }

    @Test(expected = ValidationException::class)
    fun test04_privilegedRoleInjection_MANAGER_rejectedWithValidationException(): Unit = runBlocking {
        val request = RegisterRequestDto(
            displayName = "Attacker",
            email = "attacker@example.com",
            password = "SecurePassword123!",
            requestedRole = UserRole.MANAGER
        )
        authService.register(request, "corr-005")
    }

    @Test(expected = ValidationException::class)
    fun test05_privilegedRoleInjection_STAFF_rejectedWithValidationException(): Unit = runBlocking {
        val request = RegisterRequestDto(
            displayName = "Imposter",
            email = "imposter@example.com",
            password = "SecurePassword123!",
            requestedRole = UserRole.STAFF
        )
        authService.register(request, "corr-006")
    }

    @Test(expected = ValidationException::class)
    fun test06_privilegedRoleInjection_AI_AGENT_rejectedWithValidationException(): Unit = runBlocking {
        val request = RegisterRequestDto(
            displayName = "Bot",
            email = "bot@example.com",
            password = "SecurePassword123!",
            requestedRole = UserRole.AI_AGENT
        )
        authService.register(request, "corr-007")
    }

    @Test
    fun test07_affiliateReferralRegistration_createsAffiliateRoleAccount(): Unit = runBlocking {
        val request = RegisterRequestDto(
            displayName = "Charlie Partner",
            email = "charlie@example.com",
            password = "SecurePassword123!",
            affiliateReferralCode = "REF-PARTNER-123"
        )

        val response = authService.register(request, "corr-008")
        assertEquals(UserRole.AFFILIATE, response.role)
        assertEquals(AccountStatus.PENDING, response.accountStatus)
    }

    @Test(expected = ValidationException::class)
    fun test08_registrationWithWeakPassword_rejectedWithValidationException(): Unit = runBlocking {
        val request = RegisterRequestDto(displayName = "Short Pwd", email = "short@example.com", password = "123")
        authService.register(request, "corr-009")
    }

    @Test(expected = ValidationException::class)
    fun test09_registrationWithoutEmailAndPhone_rejectedWithValidationException(): Unit = runBlocking {
        val request = RegisterRequestDto(displayName = "No Contact", email = null, phone = null, password = "SecurePassword123!")
        authService.register(request, "corr-010")
    }

    @Test
    fun test10_registrationEmitsSuccessAuditEvent(): Unit = runBlocking {
        val request = RegisterRequestDto(displayName = "Dave Miller", email = "dave@example.com", password = "SecurePassword123!")
        val resp = authService.register(request, "corr-011")

        val events = auditDataSource.queryAuditEvents("TENANT-001", resp.userId)
        assertTrue(events.any { it.eventType == AuthEventType.AUTH_REGISTER_SUCCESS && it.outcome == AuthEventOutcome.SUCCESS })
    }
}

package com.sucharu.sucharupro.data.auth

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.auth.authorization.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.auth.persistence.*
import com.sucharu.sucharupro.data.auth.security.*
import com.sucharu.sucharupro.data.auth.service.AuthenticationService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Production Login Flow & Brute-Force Throttling Security Test Suite (INFRA-03 Step 04).
 */
class PostgresLoginFlowSecurityTest {

    private lateinit var accountDataSource: FakeAuthAccountDataSource
    private lateinit var sessionDataSource: FakeAuthSessionDataSource
    private lateinit var auditDataSource: FakeAuthAuditDataSource
    private lateinit var authService: AuthenticationService

    @Before
    fun setUp() {
        accountDataSource = FakeAuthAccountDataSource()
        sessionDataSource = FakeAuthSessionDataSource()
        auditDataSource = FakeAuthAuditDataSource()

        authService = AuthenticationService(
            accountDataSource = accountDataSource,
            sessionDataSource = sessionDataSource,
            auditDataSource = auditDataSource,
            config = AuthConfig(maxLoginAttempts = 3, accountLockDurationSeconds = 600)
        )

        // Seed test active account with even-length hex salt
        runBlocking {
            val hashed = PasswordHasher.hashPassword("ValidPass123!", "1234567890abcdef")
            val account = AuthAccount(
                projectId = "TENANT-001",
                userId = "user-login-001",
                username = "logintesthero",
                email = "hero@example.com",
                passwordHash = hashed.hashHex,
                passwordSalt = hashed.saltHex,
                role = UserRole.CUSTOMER,
                accountStatus = AccountStatus.ACTIVE
            )
            accountDataSource.createAccount(account)
        }
    }

    @Test
    fun test01_successfulLogin_resetsFailedAttemptsAndIssuesTokens(): Unit = runBlocking {
        val request = LoginRequestDto(identifier = "hero@example.com", password = "ValidPass123!", requestedProjectId = "TENANT-001")
        val response = authService.login(request, "corr-login-001")

        assertNotNull(response.accessToken)
        assertNotNull(response.refreshToken)
        assertEquals("Bearer", response.tokenType)
        assertEquals("user-login-001", response.user.userId)

        val updatedAccount = accountDataSource.getAccountById("TENANT-001", "user-login-001")
        assertEquals(0, updatedAccount?.failedLoginCount)
    }

    @Test(expected = UnauthenticatedException::class)
    fun test02_loginWithUnknownIdentifier_returnsGenericUnauthenticatedException(): Unit = runBlocking {
        val request = LoginRequestDto(identifier = "nonexistent@example.com", password = "SomePassword123!")
        authService.login(request, "corr-login-002")
    }

    @Test(expected = UnauthenticatedException::class)
    fun test03_loginWithWrongPassword_returnsGenericUnauthenticatedException(): Unit = runBlocking {
        val request = LoginRequestDto(identifier = "hero@example.com", password = "WrongPassword123!")
        authService.login(request, "corr-login-003")
    }

    @Test
    fun test05_failedLoginCounter_incrementsOnFailedAttempt(): Unit = runBlocking {
        try {
            authService.login(LoginRequestDto(identifier = "hero@example.com", password = "WrongPassword!"), "corr-login-005")
        } catch (_: UnauthenticatedException) {}

        val account = accountDataSource.getAccountById("TENANT-001", "user-login-001")
        assertEquals(1, account?.failedLoginCount)
    }

    @Test
    fun test06_exceedingMaxLoginAttempts_locksAccount(): Unit = runBlocking {
        for (i in 1..3) {
            try {
                authService.login(LoginRequestDto(identifier = "hero@example.com", password = "WrongPassword!"), "corr-login-006-$i")
            } catch (_: UnauthenticatedException) {}
        }

        val account = accountDataSource.getAccountById("TENANT-001", "user-login-001")
        assertEquals(3, account?.failedLoginCount)
        assertTrue(account!!.isLocked)
    }

    @Test
    fun test08_pendingAccount_cannotAuthenticateBeforeActivation(): Unit = runBlocking {
        val hashed = PasswordHasher.hashPassword("PendingPass123!", "1234567890abcdef")
        val pendingAccount = AuthAccount(
            projectId = "TENANT-001",
            userId = "user-pending-001",
            username = "pendinguser",
            email = "pending@example.com",
            passwordHash = hashed.hashHex,
            passwordSalt = hashed.saltHex,
            role = UserRole.CUSTOMER,
            accountStatus = AccountStatus.PENDING
        )
        accountDataSource.createAccount(pendingAccount)

        try {
            authService.login(LoginRequestDto(identifier = "pending@example.com", password = "PendingPass123!"), "corr-login-008")
            fail("Expected UnauthenticatedException for pending account")
        } catch (e: UnauthenticatedException) {
            assertTrue(e.message!!.contains("Account pending verification"))
        }
    }

    @Test
    fun test09_refreshTokenNeverStoredInPlaintext_onlyHash(): Unit = runBlocking {
        val request = LoginRequestDto(identifier = "hero@example.com", password = "ValidPass123!", requestedProjectId = "TENANT-001")
        val response = authService.login(request, "corr-login-009")

        val session = sessionDataSource.getSession(response.sessionId)
        assertNotNull(session)
        assertNotEquals(response.refreshToken, session?.refreshTokenHash)
        assertEquals(TokenGenerator.hashToken(response.refreshToken), session?.refreshTokenHash)
    }

    @Test
    fun test10_loginEmitsAuditEvents(): Unit = runBlocking {
        val request = LoginRequestDto(identifier = "hero@example.com", password = "ValidPass123!", requestedProjectId = "TENANT-001")
        val response = authService.login(request, "corr-login-010")

        val events = auditDataSource.queryAuditEvents("TENANT-001", response.user.userId)
        assertTrue(events.any { it.eventType == AuthEventType.AUTH_LOGIN_SUCCESS && it.outcome == AuthEventOutcome.SUCCESS })
    }

    @Test
    fun test11_phoneNormalization_allowsLoginWithVariousBDFormats(): Unit = runBlocking {
        val hashed = PasswordHasher.hashPassword("PhonePass123!", "1234567890abcdef")
        val phoneAccount = AuthAccount(
            projectId = "TENANT-001",
            userId = "user-phone-001",
            username = "phoneuser",
            email = "phone@example.com",
            phone = "01712553809", // normalized in DB
            passwordHash = hashed.hashHex,
            passwordSalt = hashed.saltHex,
            role = UserRole.CUSTOMER,
            accountStatus = AccountStatus.ACTIVE
        )
        accountDataSource.createAccount(phoneAccount)

        // Login using +8801712553809
        val resp1 = authService.login(LoginRequestDto(identifier = "+8801712553809", password = "PhonePass123!", requestedProjectId = "TENANT-001"), "corr-p1")
        assertEquals("user-phone-001", resp1.user.userId)

        // Login using 8801712553809
        val resp2 = authService.login(LoginRequestDto(identifier = "8801712553809", password = "PhonePass123!", requestedProjectId = "TENANT-001"), "corr-p2")
        assertEquals("user-phone-001", resp2.user.userId)

        // Login using 01712553809
        val resp3 = authService.login(LoginRequestDto(identifier = "01712553809", password = "PhonePass123!", requestedProjectId = "TENANT-001"), "corr-p3")
        assertEquals("user-phone-001", resp3.user.userId)
    }
}

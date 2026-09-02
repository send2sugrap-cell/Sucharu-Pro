package com.sucharu.sucharupro.data.auth

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.auth.persistence.*
import com.sucharu.sucharupro.data.auth.security.*
import com.sucharu.sucharupro.data.auth.service.AuthenticationService
import com.sucharu.sucharupro.data.auth.service.UserIdentityService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Session Listing & Remote Device Security Integration Test Suite (INFRA-03 Step 04).
 */
class PostgresSessionSecurityIntegrationTest {

    private lateinit var accountDataSource: FakeAuthAccountDataSource
    private lateinit var profileDataSource: FakeAuthProfileDataSource
    private lateinit var verificationDataSource: FakeAuthVerificationDataSource
    private lateinit var passwordHistoryDataSource: FakeAuthPasswordHistoryDataSource
    private lateinit var sessionDataSource: FakeAuthSessionDataSource
    private lateinit var auditDataSource: FakeAuthAuditDataSource
    private lateinit var authService: AuthenticationService
    private lateinit var identityService: UserIdentityService

    @Before
    fun setUp() {
        accountDataSource = FakeAuthAccountDataSource()
        profileDataSource = FakeAuthProfileDataSource()
        verificationDataSource = FakeAuthVerificationDataSource()
        passwordHistoryDataSource = FakeAuthPasswordHistoryDataSource()
        sessionDataSource = FakeAuthSessionDataSource()
        auditDataSource = FakeAuthAuditDataSource()

        authService = AuthenticationService(
            accountDataSource = accountDataSource,
            sessionDataSource = sessionDataSource,
            auditDataSource = auditDataSource
        )

        identityService = UserIdentityService(
            accountDataSource = accountDataSource,
            profileDataSource = profileDataSource,
            verificationDataSource = verificationDataSource,
            passwordHistoryDataSource = passwordHistoryDataSource,
            sessionDataSource = sessionDataSource,
            auditDataSource = auditDataSource
        )

        runBlocking {
            val hashed = PasswordHasher.hashPassword("SessionPass123!", "1234567890abcdef")
            val account = AuthAccount(
                projectId = "TENANT-001",
                userId = "user-sess-001",
                username = "sessionuser",
                email = "session@example.com",
                passwordHash = hashed.hashHex,
                passwordSalt = hashed.saltHex,
                role = UserRole.CUSTOMER,
                accountStatus = AccountStatus.ACTIVE
            )
            accountDataSource.createAccount(account)
        }
    }

    @Test
    fun test01_listActiveSessions_returnsOnlyUserSessionsWithCurrentFlag(): Unit = runBlocking {
        val resp1 = authService.login(LoginRequestDto("session@example.com", "SessionPass123!", deviceName = "Pixel 8"), "corr-sess-001")
        val resp2 = authService.login(LoginRequestDto("session@example.com", "SessionPass123!", deviceName = "MacBook Pro"), "corr-sess-002")

        val sessions = identityService.getUserSessions("TENANT-001", "user-sess-001", currentSessionId = resp2.sessionId)

        assertEquals(2, sessions.size)
        val currentSession = sessions.find { it.sessionId == resp2.sessionId }
        val otherSession = sessions.find { it.sessionId == resp1.sessionId }

        assertNotNull(currentSession)
        assertTrue(currentSession!!.isCurrent)

        assertNotNull(otherSession)
        assertFalse(otherSession!!.isCurrent)
    }

    @Test
    fun test02_revokeSingleSession_invalidatesTargetSession(): Unit = runBlocking {
        val resp1 = authService.login(LoginRequestDto("session@example.com", "SessionPass123!", deviceName = "Phone"), "corr-sess-003")
        val resp2 = authService.login(LoginRequestDto("session@example.com", "SessionPass123!", deviceName = "Tablet"), "corr-sess-004")

        val revoked = identityService.revokeSession("TENANT-001", "user-sess-001", targetSessionId = resp1.sessionId)
        assertTrue(revoked)

        val s1 = sessionDataSource.getSession(resp1.sessionId)
        val s2 = sessionDataSource.getSession(resp2.sessionId)

        assertEquals(SessionStatus.REVOKED, s1?.sessionStatus)
        assertEquals(SessionStatus.ACTIVE, s2?.sessionStatus)
    }

    @Test
    fun test03_revokeAllSessions_invalidatesAllUserSessions(): Unit = runBlocking {
        authService.login(LoginRequestDto("session@example.com", "SessionPass123!", deviceName = "Device 1"), "corr-sess-005")
        authService.login(LoginRequestDto("session@example.com", "SessionPass123!", deviceName = "Device 2"), "corr-sess-006")

        val revokedCount = authService.logoutAll("TENANT-001", "user-sess-001", "corr-sess-logoutall")
        assertEquals(2, revokedCount)

        val activeSessions = sessionDataSource.getAllSessionsForUser("TENANT-001", "user-sess-001")
        assertTrue(activeSessions.all { it.sessionStatus == SessionStatus.REVOKED })
    }

    @Test(expected = ForbiddenException::class)
    fun test04_crossUserSessionRevocation_rejectedWithForbiddenException(): Unit = runBlocking {
        val resp = authService.login(LoginRequestDto("session@example.com", "SessionPass123!"), "corr-sess-007")

        // Attempting to revoke session belonging to another user
        identityService.revokeSession("TENANT-001", "user-OTHER-999", targetSessionId = resp.sessionId)
    }

    @Test
    fun test05_sessionRevocationEmitsAuditEvents(): Unit = runBlocking {
        val resp = authService.login(LoginRequestDto("session@example.com", "SessionPass123!"), "corr-sess-008")
        identityService.revokeSession("TENANT-001", "user-sess-001", targetSessionId = resp.sessionId)

        val events = auditDataSource.queryAuditEvents("TENANT-001", "user-sess-001")
        assertTrue(events.any { it.eventType == AuthEventType.AUTH_SESSION_REVOKED && it.outcome == AuthEventOutcome.SUCCESS })
    }
}

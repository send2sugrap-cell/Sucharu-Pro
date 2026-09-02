package com.sucharu.sucharupro.data.auth

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.auth.persistence.*
import com.sucharu.sucharupro.data.auth.security.*
import com.sucharu.sucharupro.data.auth.service.AuthenticationService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Account Enumeration Safe Password Recovery & Reset Security Test Suite (INFRA-03 Step 04).
 */
class PostgresPasswordRecoverySecurityTest {

    private lateinit var accountDataSource: FakeAuthAccountDataSource
    private lateinit var profileDataSource: FakeAuthProfileDataSource
    private lateinit var verificationDataSource: FakeAuthVerificationDataSource
    private lateinit var passwordHistoryDataSource: FakeAuthPasswordHistoryDataSource
    private lateinit var sessionDataSource: FakeAuthSessionDataSource
    private lateinit var auditDataSource: FakeAuthAuditDataSource
    private lateinit var notificationProvider: FakeVerificationNotificationProvider
    private lateinit var authService: AuthenticationService

    @Before
    fun setUp() {
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

        runBlocking {
            val hashed = PasswordHasher.hashPassword("OriginalPass123!", "1234567890abcdef")
            val account = AuthAccount(
                projectId = "TENANT-001",
                userId = "user-rec-001",
                username = "recoveryuser",
                email = "recovery@example.com",
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
                    userId = "user-rec-001",
                    passwordHash = hashed.hashHex,
                    passwordSalt = hashed.saltHex
                )
            )
        }
    }

    @Test
    fun test01_passwordRecoveryRequest_existingAccount_returnsGenericResponse(): Unit = runBlocking {
        val request = PasswordRecoveryRequestDto(identifier = "recovery@example.com")
        val response = authService.requestPasswordRecovery(request, "corr-rec-001")

        assertEquals("If the account exists, recovery instructions have been sent.", response.message)

        val notifications = notificationProvider.getSentNotifications()
        assertEquals(1, notifications.size)
        assertEquals("recovery@example.com", notifications[0].recipient)
        assertEquals(VerificationType.PASSWORD_RESET, notifications[0].type)
    }

    @Test
    fun test02_passwordRecoveryRequest_nonExistentAccount_returnsIdenticalGenericResponse(): Unit = runBlocking {
        val request = PasswordRecoveryRequestDto(identifier = "nonexistent@example.com")
        val response = authService.requestPasswordRecovery(request, "corr-rec-002")

        assertEquals("If the account exists, recovery instructions have been sent.", response.message)

        // Notification should NOT be dispatched for non-existent account
        val notifications = notificationProvider.getSentNotifications()
        assertEquals(0, notifications.size)
    }

    @Test
    fun test04_passwordResetConfirmation_validToken_updatesPassword(): Unit = runBlocking {
        authService.requestPasswordRecovery(PasswordRecoveryRequestDto("recovery@example.com"), "corr-rec-004")
        val notifications = notificationProvider.getSentNotifications()
        val rawToken = notifications[0].rawToken

        val confirmReq = PasswordRecoveryConfirmDto(token = rawToken, newPassword = "BrandNewPassword123!")
        val success = authService.confirmPasswordReset(confirmReq, "corr-rec-004-confirm")

        assertTrue(success)

        // Verify login succeeds with new password
        val loginResp = authService.login(LoginRequestDto("recovery@example.com", "BrandNewPassword123!"), "corr-rec-004-login")
        assertNotNull(loginResp.accessToken)
    }

    @Test(expected = ValidationException::class)
    fun test05_passwordResetConfirmation_reusedRecentPassword_rejectedWithValidationException(): Unit = runBlocking {
        authService.requestPasswordRecovery(PasswordRecoveryRequestDto("recovery@example.com"), "corr-rec-005")
        val rawToken = notificationProvider.getSentNotifications()[0].rawToken

        // Attempting to reuse original password
        val confirmReq = PasswordRecoveryConfirmDto(token = rawToken, newPassword = "OriginalPass123!")
        authService.confirmPasswordReset(confirmReq, "corr-rec-005-confirm")
    }

    @Test(expected = ValidationException::class)
    fun test06_passwordResetConfirmation_expiredToken_rejectedWithValidationException(): Unit = runBlocking {
        val tokenHash = TokenGenerator.hashToken("expired-raw-token")
        val expiredToken = UserVerificationToken(
            tokenId = "tok-expired-001",
            projectId = "TENANT-001",
            userId = "user-rec-001",
            verificationType = VerificationType.PASSWORD_RESET,
            tokenHash = tokenHash,
            tokenState = VerificationTokenState.PENDING,
            expiresAt = System.currentTimeMillis() - 1000L // Already expired
        )
        verificationDataSource.createVerificationToken(expiredToken)

        val confirmReq = PasswordRecoveryConfirmDto(token = "expired-raw-token", newPassword = "NewPassword123!")
        authService.confirmPasswordReset(confirmReq, "corr-rec-006")
    }

    @Test
    fun test08_passwordResetConfirmation_revokesActiveSessions(): Unit = runBlocking {
        // Log in to establish active session
        val loginResp = authService.login(LoginRequestDto("recovery@example.com", "OriginalPass123!"), "corr-rec-008-login")

        // Initiate recovery and reset password
        authService.requestPasswordRecovery(PasswordRecoveryRequestDto("recovery@example.com"), "corr-rec-008")
        val rawToken = notificationProvider.getSentNotifications()[0].rawToken

        authService.confirmPasswordReset(PasswordRecoveryConfirmDto(token = rawToken, newPassword = "NewPassword123!", revokeSessions = true), "corr-rec-008-reset")

        // Session should now be revoked
        val session = sessionDataSource.getSession(loginResp.sessionId)
        assertEquals(SessionStatus.REVOKED, session?.sessionStatus)
    }

    @Test
    fun test09_passwordResetConfirmation_emitsAuditEvents(): Unit = runBlocking {
        authService.requestPasswordRecovery(PasswordRecoveryRequestDto("recovery@example.com"), "corr-rec-009")
        val rawToken = notificationProvider.getSentNotifications()[0].rawToken

        authService.confirmPasswordReset(PasswordRecoveryConfirmDto(token = rawToken, newPassword = "NewPassword123!"), "corr-rec-009-reset")

        val events = auditDataSource.queryAuditEvents("TENANT-001", "user-rec-001")
        assertTrue(events.any { it.eventType == AuthEventType.AUTH_PASSWORD_RESET_COMPLETED && it.outcome == AuthEventOutcome.SUCCESS })
    }
}

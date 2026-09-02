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
 * Single-Use Verification Token & Contact Activation Security Test Suite (INFRA-03 Step 04).
 */
class PostgresVerificationSecurityTest {

    private lateinit var accountDataSource: FakeAuthAccountDataSource
    private lateinit var profileDataSource: FakeAuthProfileDataSource
    private lateinit var verificationDataSource: FakeAuthVerificationDataSource
    private lateinit var passwordHistoryDataSource: FakeAuthPasswordHistoryDataSource
    private lateinit var sessionDataSource: FakeAuthSessionDataSource
    private lateinit var auditDataSource: FakeAuthAuditDataSource
    private lateinit var notificationProvider: FakeVerificationNotificationProvider
    private lateinit var identityService: UserIdentityService
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

        identityService = UserIdentityService(
            accountDataSource = accountDataSource,
            profileDataSource = profileDataSource,
            verificationDataSource = verificationDataSource,
            passwordHistoryDataSource = passwordHistoryDataSource,
            sessionDataSource = sessionDataSource,
            auditDataSource = auditDataSource
        )

        runBlocking {
            val hashed = PasswordHasher.hashPassword("SecurePass123!", "1234567890abcdef")
            val account = AuthAccount(
                projectId = "TENANT-001",
                userId = "user-verif-001",
                username = "verifuser",
                email = "verif@example.com",
                phone = "01711000111",
                passwordHash = hashed.hashHex,
                passwordSalt = hashed.saltHex,
                role = UserRole.CUSTOMER,
                accountStatus = AccountStatus.PENDING
            )
            accountDataSource.createAccount(account)

            val profile = UserProfile(
                projectId = "TENANT-001",
                userId = "user-verif-001",
                displayName = "Verif User",
                email = "verif@example.com",
                phone = "01711000111"
            )
            profileDataSource.createOrUpdateProfile(profile)
        }
    }

    @Test
    fun test01_requestVerificationToken_issuesPendingToken(): Unit = runBlocking {
        val rawToken = identityService.requestVerificationToken(
            projectId = "TENANT-001",
            userId = "user-verif-001",
            type = VerificationType.EMAIL
        )

        assertNotNull(rawToken)
        assertTrue(rawToken.isNotBlank())
    }

    @Test
    fun test02_confirmVerificationToken_email_updatesEmailVerifiedAtAndActivatesAccount(): Unit = runBlocking {
        val rawToken = identityService.requestVerificationToken("TENANT-001", "user-verif-001", VerificationType.EMAIL)
        val success = identityService.confirmVerificationToken("TENANT-001", "user-verif-001", VerificationType.EMAIL, rawToken)

        assertTrue(success)

        val profile = profileDataSource.getProfile("TENANT-001", "user-verif-001")
        assertNotNull(profile?.emailVerifiedAt)

        val account = accountDataSource.getAccountById("TENANT-001", "user-verif-001")
        assertEquals(AccountStatus.ACTIVE, account?.accountStatus)
    }

    @Test(expected = ValidationException::class)
    fun test03_confirmVerificationToken_singleUse_reusedTokenRejected(): Unit = runBlocking {
        val rawToken = identityService.requestVerificationToken("TENANT-001", "user-verif-001", VerificationType.EMAIL)
        identityService.confirmVerificationToken("TENANT-001", "user-verif-001", VerificationType.EMAIL, rawToken)

        // Second consumption attempt must be rejected
        identityService.confirmVerificationToken("TENANT-001", "user-verif-001", VerificationType.EMAIL, rawToken)
    }

    @Test(expected = ValidationException::class)
    fun test04_confirmVerificationToken_expiredToken_rejectedWithValidationException(): Unit = runBlocking {
        val tokenHash = TokenGenerator.hashToken("expired-token-val")
        val expiredToken = UserVerificationToken(
            tokenId = "tok-expired-verif",
            projectId = "TENANT-001",
            userId = "user-verif-001",
            verificationType = VerificationType.EMAIL,
            tokenHash = tokenHash,
            tokenState = VerificationTokenState.PENDING,
            expiresAt = System.currentTimeMillis() - 1000L
        )
        verificationDataSource.createVerificationToken(expiredToken)

        identityService.confirmVerificationToken("TENANT-001", "user-verif-001", VerificationType.EMAIL, "expired-token-val")
    }

    @Test(expected = ForbiddenException::class)
    fun test05_confirmVerificationToken_crossUserToken_rejectedWithForbiddenException(): Unit = runBlocking {
        val rawToken = identityService.requestVerificationToken("TENANT-001", "user-verif-001", VerificationType.EMAIL)

        // Attempting to consume token using another user's identity
        identityService.confirmVerificationToken("TENANT-001", "user-verif-OTHER", VerificationType.EMAIL, rawToken)
    }

    @Test
    fun test06_authService_verifyAccount_activatesPendingAccount(): Unit = runBlocking {
        val rawToken = identityService.requestVerificationToken("TENANT-001", "user-verif-001", VerificationType.PHONE)
        val resp = authService.verifyAccount(ConfirmVerificationRequestDto(token = rawToken, verificationType = VerificationType.PHONE), "corr-test-06")

        assertEquals(true, resp["verified"])

        val account = accountDataSource.getAccountById("TENANT-001", "user-verif-001")
        assertEquals(AccountStatus.ACTIVE, account?.accountStatus)
        assertTrue(account!!.canAuthenticate)
    }

    @Test
    fun test07_authService_resendVerificationToken_generatesNewToken(): Unit = runBlocking {
        val resp = authService.resendVerificationToken("01711000111", "corr-test-07")
        assertTrue(resp.success)
        assertEquals("DELIVERY_ACCEPTED", resp.deliveryStatus)
        assertEquals("A new verification code has been sent.", resp.message)

        val notifications = notificationProvider.getSentNotifications()
        assertTrue(notifications.any { it.recipient == "01711000111" })
    }

    @Test
    fun test08_phoneVerificationUses6DigitNumericOtp(): Unit = runBlocking {
        val rawToken = identityService.requestVerificationToken(
            projectId = "TENANT-001",
            userId = "user-verif-001",
            type = VerificationType.PHONE
        )

        assertNotNull(rawToken)
        assertEquals(6, rawToken.length)
        assertTrue(rawToken.all { it.isDigit() })
    }

    @Test
    fun test09_resendVerification_deliveryFailure_reportsHonestError(): Unit = runBlocking {
        notificationProvider.setSimulateSuccess(false)
        val resp = authService.resendVerificationToken("01711000111", "corr-test-09")
        assertFalse(resp.success)
        assertEquals("DELIVERY_FAILED", resp.deliveryStatus)
        assertEquals("We couldn't send the verification code right now. Please try again shortly.", resp.message)
    }

    @Test
    fun test10_resendVerification_revokesPreviousPendingTokens(): Unit = runBlocking {
        val initialToken = identityService.requestVerificationToken("TENANT-001", "user-verif-001", VerificationType.PHONE)
        val resendResp = authService.resendVerificationToken("01711000111", "corr-test-10")
        assertTrue(resendResp.success)

        val latestToken = notificationProvider.getLatestTokenForRecipient("01711000111")
        assertNotNull(latestToken)
        assertNotEquals(initialToken, latestToken)

        // Attempt to confirm old token must fail because it was revoked
        val confirmOld = runCatching {
            authService.verifyAccount(ConfirmVerificationRequestDto(token = initialToken, verificationType = VerificationType.PHONE), "corr-test-10-old")
        }
        assertTrue(confirmOld.isFailure)
    }
}


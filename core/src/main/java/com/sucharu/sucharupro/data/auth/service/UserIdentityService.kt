package com.sucharu.sucharupro.data.auth.service

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.ValidationException
import com.sucharu.sucharupro.data.auth.datasource.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.auth.security.PasswordHasher
import com.sucharu.sucharupro.data.auth.security.TokenGenerator
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.security.MessageDigest
import java.util.UUID

/**
 * Production-grade Service managing User Identity Lifecycle, Profiles, Verification,
 * Sessions, Device Security, and Account Transitions (INFRA-03 Step 03).
 */
class UserIdentityService(
    private val accountDataSource: AuthAccountDataSource,
    private val profileDataSource: AuthProfileDataSource,
    private val verificationDataSource: AuthVerificationDataSource,
    private val passwordHistoryDataSource: AuthPasswordHistoryDataSource,
    private val sessionDataSource: AuthSessionDataSource,
    private val auditDataSource: AuthAuditDataSource
) {

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    /**
     * Retrieves canonical identity aggregation for a user.
     */
    suspend fun getUserIdentity(projectId: String, userId: String): UserIdentity? {
        val account = accountDataSource.getAccountById(projectId, userId) ?: return null
        val profile = profileDataSource.getProfile(projectId, userId)
        return UserIdentity(account = account, profile = profile)
    }

    /**
     * Retrieves user profile DTO.
     */
    suspend fun getProfile(projectId: String, userId: String): UserProfileDto? {
        val account = accountDataSource.getAccountById(projectId, userId) ?: return null
        val profile = profileDataSource.getProfile(projectId, userId)
            ?: UserProfile(projectId = projectId, userId = userId, displayName = account.username, email = account.email, phone = account.phone)

        return UserProfileDto(
            userId = account.userId,
            projectId = account.projectId,
            username = account.username,
            displayName = profile.displayName,
            email = account.email ?: profile.email,
            phone = account.phone ?: profile.phone,
            avatarUrl = profile.avatarUrl,
            preferredLanguage = profile.preferredLanguage,
            timezone = profile.timezone,
            emailVerified = profile.emailVerifiedAt != null,
            phoneVerified = profile.phoneVerifiedAt != null,
            role = account.role,
            permissions = emptySet(),
            accountStatus = account.accountStatus
        )
    }

    /**
     * Updates user profile with optimistic concurrency control.
     */
    suspend fun updateProfile(
        projectId: String,
        userId: String,
        request: UpdateUserProfileRequestDto,
        correlationId: String? = null,
        clientIp: String? = null
    ): UserProfileDto {
        val account = accountDataSource.getAccountById(projectId, userId)
            ?: throw ValidationException("User account '$userId' not found.")

        val existingProfile = profileDataSource.getProfile(projectId, userId)
            ?: UserProfile(projectId = projectId, userId = userId, displayName = account.username, email = account.email, phone = account.phone)

        val updated = existingProfile.copy(
            displayName = request.displayName ?: existingProfile.displayName,
            legalName = request.legalName ?: existingProfile.legalName,
            phone = request.phone ?: existingProfile.phone,
            avatarUrl = request.avatarUrl ?: existingProfile.avatarUrl,
            preferredLanguage = request.preferredLanguage ?: existingProfile.preferredLanguage,
            timezone = request.timezone ?: existingProfile.timezone,
            contactPreferences = request.contactPreferences ?: existingProfile.contactPreferences
        )

        return when (val res = profileDataSource.createOrUpdateProfile(updated)) {
            is DomainResult.Success -> {
                auditDataSource.recordAuditEvent(
                    AuthAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        projectId = projectId,
                        userId = userId,
                        eventType = AuthEventType.AUTH_PROFILE_UPDATED,
                        outcome = AuthEventOutcome.SUCCESS,
                        ipAddress = clientIp,
                        correlationId = correlationId,
                        details = mapOf("action" to "UPDATE_PROFILE")
                    )
                )
                getProfile(projectId, userId)!!
            }
            is DomainResult.Error -> throw IllegalStateException(res.message)
            else -> throw IllegalStateException("Profile update failed.")
        }
    }

    /**
     * Changes user password, checks password history, updates credential, and revokes sessions according to policy.
     */
    suspend fun changePassword(
        projectId: String,
        userId: String,
        request: ChangePasswordRequestDto,
        correlationId: String? = null,
        clientIp: String? = null
    ): Boolean {
        val account = accountDataSource.getAccountById(projectId, userId)
            ?: throw ValidationException("User account not found.")

        // 1. Verify current password
        val currentValid = PasswordHasher.verifyPassword(
            plaintext = request.currentPassword,
            saltHex = account.passwordSalt,
            expectedHashHex = account.passwordHash
        )

        if (!currentValid) {
            auditDataSource.recordAuditEvent(
                AuthAuditEvent(
                    eventId = UUID.randomUUID().toString(),
                    projectId = projectId,
                    userId = userId,
                    eventType = AuthEventType.AUTH_PASSWORD_CHANGED,
                    outcome = AuthEventOutcome.FAILURE,
                    ipAddress = clientIp,
                    correlationId = correlationId,
                    details = mapOf("reason" to "Invalid current password")
                )
            )
            throw ValidationException("Invalid current password.")
        }

        // 2. Validate new password strength
        if (request.newPassword.length < 8) {
            throw ValidationException("Password must be at least 8 characters long.")
        }

        // 3. Check password history (prevent re-using recent passwords)
        val history = passwordHistoryDataSource.getRecentPasswordHistory(projectId, userId, limit = 5)
        for (past in history) {
            if (PasswordHasher.verifyPassword(request.newPassword, past.passwordSalt, past.passwordHash)) {
                throw ValidationException("New password cannot be one of the last 5 previous passwords.")
            }
        }

        // 4. Hash new password
        val newHashed = PasswordHasher.hashPassword(request.newPassword)

        // 5. Save history of old password
        passwordHistoryDataSource.recordPasswordHistory(
            PasswordHistoryEntry(
                historyId = UUID.randomUUID().toString(),
                projectId = projectId,
                userId = userId,
                passwordHash = account.passwordHash,
                passwordSalt = account.passwordSalt
            )
        )

        // 6. Update password in database
        accountDataSource.updatePassword(projectId, userId, newHashed.hashHex, newHashed.saltHex, newHashed.algorithm)

        // 7. Revoke other sessions if requested
        if (request.revokeOtherSessions) {
            sessionDataSource.revokeAllUserSessions(projectId, userId, "PASSWORD_CHANGED")
        }

        // 8. Record audit event
        auditDataSource.recordAuditEvent(
            AuthAuditEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                userId = userId,
                eventType = AuthEventType.AUTH_PASSWORD_CHANGED,
                outcome = AuthEventOutcome.SUCCESS,
                ipAddress = clientIp,
                correlationId = correlationId
            )
        )

        return true
    }

    /**
     * Issues a single-use verification token for Email, Phone, or Password Reset.
     */
    suspend fun requestVerificationToken(
        projectId: String,
        userId: String,
        type: VerificationType,
        ttlSeconds: Long = 900L,
        correlationId: String? = null,
        clientIp: String? = null
    ): String {
        // Revoke existing pending tokens for this type
        verificationDataSource.revokeUserTokens(projectId, userId, type)

        val rawToken = if (type == VerificationType.PHONE) {
            TokenGenerator.generateNumericOtp(6)
        } else {
            TokenGenerator.generateSecureToken(32)
        }
        val tokenHash = hashToken(rawToken)

        val token = UserVerificationToken(
            tokenId = UUID.randomUUID().toString(),
            projectId = projectId,
            userId = userId,
            verificationType = type,
            tokenHash = tokenHash,
            tokenState = VerificationTokenState.PENDING,
            expiresAt = System.currentTimeMillis() + (ttlSeconds * 1000L)
        )

        verificationDataSource.createVerificationToken(token)

        auditDataSource.recordAuditEvent(
            AuthAuditEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                userId = userId,
                eventType = AuthEventType.AUTH_VERIFICATION_TOKEN_CREATED,
                outcome = AuthEventOutcome.SUCCESS,
                ipAddress = clientIp,
                correlationId = correlationId,
                details = mapOf("type" to type.name)
            )
        )

        return rawToken
    }

    /**
     * Confirms and consumes a verification token.
     */
    suspend fun confirmVerificationToken(
        projectId: String,
        userId: String,
        type: VerificationType,
        rawToken: String,
        correlationId: String? = null,
        clientIp: String? = null
    ): Boolean {
        val tHash = hashToken(rawToken)
        val token = verificationDataSource.getVerificationTokenByHash(tHash)
            ?: throw ValidationException("Invalid or expired verification token.")

        if (token.projectId != projectId || token.userId != userId || token.verificationType != type) {
            throw ForbiddenException("Verification token does not belong to this user or channel.")
        }

        if (!token.isValid) {
            throw ValidationException("Verification token is no longer valid or has expired.")
        }

        val consumed = verificationDataSource.consumeVerificationToken(token.tokenId, System.currentTimeMillis())
        if (!consumed) {
            throw ValidationException("Verification token could not be processed.")
        }

        when (type) {
            VerificationType.EMAIL -> profileDataSource.updateVerificationTimestamps(projectId, userId, System.currentTimeMillis(), null)
            VerificationType.PHONE -> profileDataSource.updateVerificationTimestamps(projectId, userId, null, System.currentTimeMillis())
            VerificationType.PASSWORD_RESET -> { /* Handled separately during reset completion */ }
        }

        // Transition pending account to active upon successful contact verification
        if (type == VerificationType.EMAIL || type == VerificationType.PHONE) {
            val currentAccount = accountDataSource.getAccountById(projectId, userId)
            if (currentAccount != null && currentAccount.accountStatus == AccountStatus.PENDING) {
                accountDataSource.updateAccountStatus(projectId, userId, AccountStatus.ACTIVE)
                auditDataSource.recordAuditEvent(
                    AuthAuditEvent(
                        eventId = UUID.randomUUID().toString(),
                        projectId = projectId,
                        userId = userId,
                        eventType = AuthEventType.AUTH_ACCOUNT_ACTIVATED,
                        outcome = AuthEventOutcome.SUCCESS,
                        ipAddress = clientIp,
                        correlationId = correlationId,
                        details = mapOf("reason" to "contact_verified", "channel" to type.name)
                    )
                )
            }
        }

        val eventType = when (type) {
            VerificationType.EMAIL -> AuthEventType.AUTH_EMAIL_VERIFIED
            VerificationType.PHONE -> AuthEventType.AUTH_PHONE_VERIFIED
            VerificationType.PASSWORD_RESET -> AuthEventType.AUTH_PASSWORD_RESET_COMPLETED
        }

        auditDataSource.recordAuditEvent(
            AuthAuditEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                userId = userId,
                eventType = eventType,
                outcome = AuthEventOutcome.SUCCESS,
                ipAddress = clientIp,
                correlationId = correlationId
            )
        )

        return true
    }

    /**
     * Lists all active/historic sessions for a user.
     */
    suspend fun getUserSessions(
        projectId: String,
        userId: String,
        currentSessionId: String? = null
    ): List<SessionSummaryDto> {
        val sessions = sessionDataSource.getAllSessionsForUser(projectId, userId)
        return sessions.map { s ->
            SessionSummaryDto(
                sessionId = s.sessionId,
                deviceName = s.deviceName ?: "Unknown Device",
                clientIp = s.clientIp,
                createdAt = s.createdAt,
                lastSeenAt = s.lastSeenAt,
                expiresAt = s.expiresAt,
                isCurrent = (currentSessionId != null && s.sessionId == currentSessionId),
                sessionStatus = s.sessionStatus
            )
        }
    }

    /**
     * Revokes a specific session.
     */
    suspend fun revokeSession(
        projectId: String,
        userId: String,
        targetSessionId: String,
        reason: String = "User requested revocation",
        correlationId: String? = null,
        clientIp: String? = null
    ): Boolean {
        val session = sessionDataSource.getSession(targetSessionId)
            ?: throw ValidationException("Session '$targetSessionId' not found.")

        if (session.projectId != projectId || session.userId != userId) {
            throw ForbiddenException("Access denied: Cannot revoke session belonging to another user or tenant.")
        }

        val revoked = sessionDataSource.revokeSession(targetSessionId, reason)

        if (revoked) {
            auditDataSource.recordAuditEvent(
                AuthAuditEvent(
                    eventId = UUID.randomUUID().toString(),
                    projectId = projectId,
                    userId = userId,
                    sessionId = targetSessionId,
                    eventType = AuthEventType.AUTH_SESSION_REVOKED,
                    outcome = AuthEventOutcome.SUCCESS,
                    ipAddress = clientIp,
                    correlationId = correlationId,
                    details = mapOf("reason" to reason)
                )
            )
        }

        return revoked
    }

    /**
     * Administrative account status transition (e.g. SUSPEND, REACTIVATE, DEACTIVATE, LOCK).
     */
    suspend fun updateAccountStatus(
        projectId: String,
        targetUserId: String,
        newStatus: AccountStatus,
        reason: String? = null,
        adminPrincipal: AuthenticatedPrincipal,
        correlationId: String? = null,
        clientIp: String? = null
    ): Boolean {
        val account = accountDataSource.getAccountById(projectId, targetUserId)
            ?: throw ValidationException("User account '$targetUserId' not found.")

        val currentStatus = account.accountStatus
        if (!currentStatus.isValidTransitionTo(newStatus)) {
            throw ValidationException("Invalid account status transition from '$currentStatus' to '$newStatus'.")
        }

        accountDataSource.updateAccountStatus(projectId, targetUserId, newStatus)

        // Revoke active sessions if account is suspended or deactivated
        if (newStatus in setOf(AccountStatus.SUSPENDED, AccountStatus.DEACTIVATED, AccountStatus.LOCKED)) {
            sessionDataSource.revokeAllUserSessions(projectId, targetUserId, "ACCOUNT_STATUS_CHANGED_$newStatus")
        }

        val eventType = when (newStatus) {
            AccountStatus.ACTIVE -> if (currentStatus == AccountStatus.SUSPENDED) AuthEventType.AUTH_ACCOUNT_REACTIVATED else AuthEventType.AUTH_ACCOUNT_ACTIVATED
            AccountStatus.SUSPENDED -> AuthEventType.AUTH_ACCOUNT_SUSPENDED
            AccountStatus.DEACTIVATED -> AuthEventType.AUTH_ACCOUNT_DEACTIVATED
            AccountStatus.LOCKED -> AuthEventType.AUTH_ACCOUNT_LOCKED
            else -> AuthEventType.AUTH_ACCOUNT_ACTIVATED
        }

        auditDataSource.recordAuditEvent(
            AuthAuditEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                userId = targetUserId,
                eventType = eventType,
                outcome = AuthEventOutcome.SUCCESS,
                ipAddress = clientIp,
                correlationId = correlationId,
                details = mapOf("by" to adminPrincipal.userId, "from" to currentStatus.name, "to" to newStatus.name, "reason" to (reason ?: "Admin action"))
            )
        )

        return true
    }
}

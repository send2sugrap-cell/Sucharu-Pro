package com.sucharu.sucharupro.data.auth.datasource

import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * DataSource contract for Account identity persistence (INFRA-03 Step 01).
 */
interface AuthAccountDataSource {
    suspend fun getAccount(projectId: String, identifier: String): AuthAccount?
    suspend fun getAccountById(projectId: String, userId: String): AuthAccount?
    suspend fun createAccount(account: AuthAccount): DomainResult<AuthAccount>
    suspend fun updateFailedAttempts(projectId: String, userId: String, failedCount: Int, lockUntil: Long?)
    suspend fun recordSuccessfulLogin(projectId: String, userId: String, loginTime: Long)
    suspend fun updatePassword(projectId: String, userId: String, passwordHash: String, salt: String, algorithm: String)
    suspend fun updateAccountStatus(projectId: String, userId: String, status: AccountStatus)
}

/**
 * DataSource contract for User Profile persistence (INFRA-03 Step 03).
 */
interface AuthProfileDataSource {
    suspend fun getProfile(projectId: String, userId: String): UserProfile?
    suspend fun createOrUpdateProfile(profile: UserProfile): DomainResult<UserProfile>
    suspend fun updateVerificationTimestamps(projectId: String, userId: String, emailVerifiedAt: Long?, phoneVerifiedAt: Long?): Boolean
}

/**
 * DataSource contract for Contact & Password Reset Verification Tokens (INFRA-03 Step 03).
 */
interface AuthVerificationDataSource {
    suspend fun createVerificationToken(token: UserVerificationToken): DomainResult<UserVerificationToken>
    suspend fun getVerificationTokenByHash(tokenHash: String): UserVerificationToken?
    suspend fun consumeVerificationToken(tokenId: String, consumedAt: Long): Boolean
    suspend fun revokeUserTokens(projectId: String, userId: String, verificationType: VerificationType): Int
}

/**
 * DataSource contract for Password History security compliance (INFRA-03 Step 03).
 */
interface AuthPasswordHistoryDataSource {
    suspend fun recordPasswordHistory(entry: PasswordHistoryEntry)
    suspend fun getRecentPasswordHistory(projectId: String, userId: String, limit: Int = 5): List<PasswordHistoryEntry>
}

/**
 * DataSource contract for Session and Refresh Token persistence (INFRA-03 Step 01).
 */
interface AuthSessionDataSource {
    suspend fun createSession(session: AuthSession): DomainResult<AuthSession>
    suspend fun getSession(sessionId: String): AuthSession?
    suspend fun getSessionByRefreshTokenHash(refreshTokenHash: String): AuthSession?
    suspend fun updateSessionSeen(sessionId: String, seenAt: Long)
    suspend fun rotateRefreshToken(sessionId: String, oldTokenHash: String, newTokenHash: String, newExpiresAt: Long): Boolean
    suspend fun revokeSession(sessionId: String, reason: String): Boolean
    suspend fun revokeAllUserSessions(projectId: String, userId: String, reason: String): Int
    suspend fun getActiveSessions(projectId: String, userId: String): List<AuthSession>
    suspend fun getAllSessionsForUser(projectId: String, userId: String): List<AuthSession>
}

/**
 * DataSource contract for append-only security audit event logging (INFRA-03 Step 01).
 */
interface AuthAuditDataSource {
    suspend fun recordAuditEvent(event: AuthAuditEvent)
    suspend fun queryAuditEvents(projectId: String, userId: String? = null, limit: Int = 100): List<AuthAuditEvent>
}

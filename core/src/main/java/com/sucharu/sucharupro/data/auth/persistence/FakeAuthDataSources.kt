package com.sucharu.sucharupro.data.auth.persistence

import com.sucharu.sucharupro.data.auth.datasource.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory thread-safe fake implementation of [AuthAccountDataSource] for testing (INFRA-03 Step 01).
 */
class FakeAuthAccountDataSource : AuthAccountDataSource {

    private val accounts = ConcurrentHashMap<String, AuthAccount>() // key: "projectId:userId"

    private fun key(projectId: String, userId: String) = "$projectId:$userId"

    override suspend fun getAccount(projectId: String, identifier: String): AuthAccount? {
        val trimmed = identifier.trim()
        val normalizedPhone = com.sucharu.sucharupro.core.validation.CustomerValidation.normalizePhoneNumber(trimmed)
        return accounts.values.firstOrNull {
            it.projectId == projectId && (
                it.username.equals(trimmed, ignoreCase = true) ||
                it.email.equals(trimmed, ignoreCase = true) ||
                it.phone == trimmed ||
                (normalizedPhone.isNotBlank() && it.phone == normalizedPhone)
            )
        }
    }

    override suspend fun getAccountById(projectId: String, userId: String): AuthAccount? {
        return accounts[key(projectId, userId)]
    }

    override suspend fun createAccount(account: AuthAccount): DomainResult<AuthAccount> {
        val k = key(account.projectId, account.userId)
        if (accounts.containsKey(k)) {
            return DomainResult.Error(message = "Account with userId '${account.userId}' already exists.")
        }
        accounts[k] = account
        return DomainResult.Success(account)
    }

    override suspend fun updateFailedAttempts(projectId: String, userId: String, failedCount: Int, lockUntil: Long?) {
        val existing = accounts[key(projectId, userId)] ?: return
        val status = if (lockUntil != null && lockUntil > System.currentTimeMillis()) {
            AccountStatus.LOCKED
        } else if (existing.accountStatus == AccountStatus.LOCKED && (lockUntil == null || lockUntil <= System.currentTimeMillis())) {
            AccountStatus.ACTIVE
        } else {
            existing.accountStatus
        }
        accounts[key(projectId, userId)] = existing.copy(
            failedLoginCount = failedCount,
            lockUntil = lockUntil,
            accountStatus = status,
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1
        )
    }

    override suspend fun recordSuccessfulLogin(projectId: String, userId: String, loginTime: Long) {
        val existing = accounts[key(projectId, userId)] ?: return
        accounts[key(projectId, userId)] = existing.copy(
            failedLoginCount = 0,
            lockUntil = null,
            lastLoginAt = loginTime,
            accountStatus = AccountStatus.ACTIVE,
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1
        )
    }

    override suspend fun updatePassword(projectId: String, userId: String, passwordHash: String, salt: String, algorithm: String) {
        val existing = accounts[key(projectId, userId)] ?: return
        accounts[key(projectId, userId)] = existing.copy(
            passwordHash = passwordHash,
            passwordSalt = salt,
            passwordAlgorithm = algorithm,
            passwordChangedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1
        )
    }

    override suspend fun updateAccountStatus(projectId: String, userId: String, status: AccountStatus) {
        val existing = accounts[key(projectId, userId)] ?: return
        accounts[key(projectId, userId)] = existing.copy(
            accountStatus = status,
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1
        )
    }
}

/**
 * In-memory thread-safe fake implementation of [AuthProfileDataSource] for testing (INFRA-03 Step 03).
 */
class FakeAuthProfileDataSource : AuthProfileDataSource {
    private val profiles = ConcurrentHashMap<String, UserProfile>() // key: "projectId:userId"

    private fun key(projectId: String, userId: String) = "$projectId:$userId"

    override suspend fun getProfile(projectId: String, userId: String): UserProfile? {
        return profiles[key(projectId, userId)]
    }

    override suspend fun createOrUpdateProfile(profile: UserProfile): DomainResult<UserProfile> {
        val k = key(profile.projectId, profile.userId)
        val existing = profiles[k]
        if (existing != null && profile.version != existing.version) {
            return DomainResult.Error(message = "OPTIMISTIC_CONCURRENCY_CONFLICT: Profile version mismatch (expected ${existing.version}, got ${profile.version})")
        }
        val updated = profile.copy(updatedAt = System.currentTimeMillis(), version = (existing?.version ?: 0L) + 1)
        profiles[k] = updated
        return DomainResult.Success(updated)
    }

    override suspend fun updateVerificationTimestamps(
        projectId: String,
        userId: String,
        emailVerifiedAt: Long?,
        phoneVerifiedAt: Long?
    ): Boolean {
        val k = key(projectId, userId)
        val existing = profiles[k] ?: return false
        profiles[k] = existing.copy(
            emailVerifiedAt = emailVerifiedAt ?: existing.emailVerifiedAt,
            phoneVerifiedAt = phoneVerifiedAt ?: existing.phoneVerifiedAt,
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1
        )
        return true
    }
}

/**
 * In-memory thread-safe fake implementation of [AuthVerificationDataSource] for testing (INFRA-03 Step 03).
 */
class FakeAuthVerificationDataSource : AuthVerificationDataSource {
    private val tokens = ConcurrentHashMap<String, UserVerificationToken>() // key: tokenId

    override suspend fun createVerificationToken(token: UserVerificationToken): DomainResult<UserVerificationToken> {
        tokens[token.tokenId] = token
        return DomainResult.Success(token)
    }

    override suspend fun getVerificationTokenByHash(tokenHash: String): UserVerificationToken? {
        return tokens.values.firstOrNull { it.tokenHash == tokenHash }
    }

    override suspend fun consumeVerificationToken(tokenId: String, consumedAt: Long): Boolean {
        val existing = tokens[tokenId] ?: return false
        if (existing.tokenState != VerificationTokenState.PENDING || existing.expiresAt <= System.currentTimeMillis()) {
            return false
        }
        tokens[tokenId] = existing.copy(tokenState = VerificationTokenState.USED, usedAt = consumedAt, version = existing.version + 1)
        return true
    }

    override suspend fun revokeUserTokens(projectId: String, userId: String, verificationType: VerificationType): Int {
        var count = 0
        tokens.values.filter { it.projectId == projectId && it.userId == userId && it.verificationType == verificationType && it.tokenState == VerificationTokenState.PENDING }
            .forEach {
                tokens[it.tokenId] = it.copy(tokenState = VerificationTokenState.REVOKED, version = it.version + 1)
                count++
            }
        return count
    }
}

/**
 * In-memory thread-safe fake implementation of [AuthPasswordHistoryDataSource] for testing (INFRA-03 Step 03).
 */
class FakeAuthPasswordHistoryDataSource : AuthPasswordHistoryDataSource {
    private val history = CopyOnWriteArrayList<PasswordHistoryEntry>()

    override suspend fun recordPasswordHistory(entry: PasswordHistoryEntry) {
        history.add(entry)
    }

    override suspend fun getRecentPasswordHistory(projectId: String, userId: String, limit: Int): List<PasswordHistoryEntry> {
        return history.filter { it.projectId == projectId && it.userId == userId }
            .sortedByDescending { it.createdAt }
            .take(limit)
    }
}

/**
 * In-memory thread-safe fake implementation of [AuthSessionDataSource] for testing (INFRA-03 Step 01 & Step 03).
 */
class FakeAuthSessionDataSource : AuthSessionDataSource {

    private val sessions = ConcurrentHashMap<String, AuthSession>() // key: sessionId

    override suspend fun createSession(session: AuthSession): DomainResult<AuthSession> {
        sessions[session.sessionId] = session
        return DomainResult.Success(session)
    }

    override suspend fun getSession(sessionId: String): AuthSession? {
        return sessions[sessionId]
    }

    override suspend fun getSessionByRefreshTokenHash(refreshTokenHash: String): AuthSession? {
        return sessions.values.firstOrNull { it.refreshTokenHash == refreshTokenHash || it.previousRefreshTokenHashes.contains(refreshTokenHash) }
    }

    override suspend fun updateSessionSeen(sessionId: String, seenAt: Long) {
        val existing = sessions[sessionId] ?: return
        sessions[sessionId] = existing.copy(lastSeenAt = seenAt, version = existing.version + 1)
    }

    override suspend fun rotateRefreshToken(
        sessionId: String,
        oldTokenHash: String,
        newTokenHash: String,
        newExpiresAt: Long
    ): Boolean {
        val existing = sessions[sessionId] ?: return false
        if (existing.refreshTokenHash != oldTokenHash || existing.sessionStatus != SessionStatus.ACTIVE) {
            return false
        }
        val updatedPrevious = existing.previousRefreshTokenHashes + oldTokenHash
        sessions[sessionId] = existing.copy(
            refreshTokenHash = newTokenHash,
            previousRefreshTokenHashes = updatedPrevious,
            expiresAt = newExpiresAt,
            lastSeenAt = System.currentTimeMillis(),
            version = existing.version + 1
        )
        return true
    }

    override suspend fun revokeSession(sessionId: String, reason: String): Boolean {
        val existing = sessions[sessionId] ?: return false
        if (existing.sessionStatus != SessionStatus.ACTIVE) return false
        sessions[sessionId] = existing.copy(
            sessionStatus = SessionStatus.REVOKED,
            revokedAt = System.currentTimeMillis(),
            revocationReason = reason,
            version = existing.version + 1
        )
        return true
    }

    override suspend fun revokeAllUserSessions(projectId: String, userId: String, reason: String): Int {
        var count = 0
        sessions.values.filter { it.projectId == projectId && it.userId == userId && it.sessionStatus == SessionStatus.ACTIVE }
            .forEach {
                sessions[it.sessionId] = it.copy(
                    sessionStatus = SessionStatus.REVOKED,
                    revokedAt = System.currentTimeMillis(),
                    revocationReason = reason,
                    version = it.version + 1
                )
                count++
            }
        return count
    }

    override suspend fun getActiveSessions(projectId: String, userId: String): List<AuthSession> {
        return sessions.values.filter {
            it.projectId == projectId && it.userId == userId && it.sessionStatus == SessionStatus.ACTIVE && it.expiresAt > System.currentTimeMillis()
        }.sortedByDescending { it.createdAt }
    }

    override suspend fun getAllSessionsForUser(projectId: String, userId: String): List<AuthSession> {
        return sessions.values.filter {
            it.projectId == projectId && it.userId == userId
        }.sortedByDescending { it.createdAt }
    }
}

/**
 * In-memory thread-safe fake implementation of [AuthAuditDataSource] for testing (INFRA-03 Step 01).
 */
class FakeAuthAuditDataSource : AuthAuditDataSource {

    private val auditLogs = CopyOnWriteArrayList<AuthAuditEvent>()

    override suspend fun recordAuditEvent(event: AuthAuditEvent) {
        auditLogs.add(event)
    }

    override suspend fun queryAuditEvents(projectId: String, userId: String?, limit: Int): List<AuthAuditEvent> {
        return auditLogs.filter {
            it.projectId == projectId && (userId == null || it.userId == userId)
        }.sortedByDescending { it.occurredAt }.take(limit)
    }
}

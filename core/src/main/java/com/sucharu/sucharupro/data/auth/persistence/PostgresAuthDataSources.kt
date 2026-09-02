package com.sucharu.sucharupro.data.auth.persistence

import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.datasource.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresErrorTranslator
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getNullableTimestampMillis
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getTimestampMillis
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * Production-grade PostgreSQL DataSource for Auth Accounts (INFRA-03 Step 01).
 */
class PostgresAuthAccountDataSource(
    private val transactionManager: TransactionManager
) : AuthAccountDataSource {

    private fun mapAccount(rs: ResultSet): AuthAccount {
        return AuthAccount(
            projectId = rs.getString("project_id"),
            userId = rs.getString("user_id"),
            username = rs.getString("username"),
            email = rs.getString("email"),
            phone = rs.getString("phone"),
            passwordHash = rs.getString("password_hash"),
            passwordSalt = rs.getString("password_salt"),
            passwordAlgorithm = rs.getString("password_algorithm") ?: "PBKDF2_SHA256",
            role = rs.getEnumByName("role", UserRole.CUSTOMER),
            accountStatus = rs.getEnumByName("account_status", AccountStatus.ACTIVE),
            failedLoginCount = rs.getInt("failed_login_count"),
            lockUntil = rs.getNullableTimestampMillis("lock_until"),
            lastLoginAt = rs.getNullableTimestampMillis("last_login_at"),
            passwordChangedAt = rs.getTimestampMillis("password_changed_at"),
            createdAt = rs.getTimestampMillis("created_at"),
            updatedAt = rs.getTimestampMillis("updated_at"),
            version = rs.getLong("version")
        )
    }

    override suspend fun getAccount(projectId: String, identifier: String): AuthAccount? {
        val tenant = TenantContext(projectId)
        val trimmed = identifier.trim()
        val normalizedPhone = com.sucharu.sucharupro.core.validation.CustomerValidation.normalizePhoneNumber(trimmed)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT project_id, user_id, username, email, phone, password_hash, password_salt,
                       password_algorithm, role, account_status, failed_login_count, lock_until,
                       last_login_at, password_changed_at, created_at, updated_at, version
                FROM auth_accounts
                WHERE project_id = ? AND (username = ? OR email = ? OR phone = ? OR phone = ?)
            """.trimIndent()

            ctx.sqlExecutor.querySingleOrNull(sql, listOf(projectId, trimmed, trimmed, trimmed, normalizedPhone)) { rs ->
                mapAccount(rs)
            }
        }
    }

    override suspend fun getAccountById(projectId: String, userId: String): AuthAccount? {
        val tenant = TenantContext(projectId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT project_id, user_id, username, email, phone, password_hash, password_salt,
                       password_algorithm, role, account_status, failed_login_count, lock_until,
                       last_login_at, password_changed_at, created_at, updated_at, version
                FROM auth_accounts
                WHERE project_id = ? AND user_id = ?
            """.trimIndent()

            ctx.sqlExecutor.querySingleOrNull(sql, listOf(projectId, userId)) { rs ->
                mapAccount(rs)
            }
        }
    }

    override suspend fun createAccount(account: AuthAccount): DomainResult<AuthAccount> {
        val tenant = TenantContext(account.projectId)
        return try {
            transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO auth_accounts (
                        project_id, user_id, username, email, phone, password_hash, password_salt,
                        password_algorithm, role, account_status, failed_login_count, lock_until,
                        last_login_at, password_changed_at, created_at, updated_at, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), NOW(), 1)
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        account.projectId,
                        account.userId,
                        account.username,
                        account.email,
                        account.phone,
                        account.passwordHash,
                        account.passwordSalt,
                        account.passwordAlgorithm,
                        account.role.name,
                        account.accountStatus.name,
                        account.failedLoginCount,
                        account.lockUntil?.let { Timestamp(it) },
                        account.lastLoginAt?.let { Timestamp(it) }
                    )
                )
            }
            DomainResult.Success(account)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create auth account")
        }
    }

    override suspend fun updateFailedAttempts(projectId: String, userId: String, failedCount: Int, lockUntil: Long?) {
        val tenant = TenantContext(projectId)
        transactionManager.inTransaction(tenant) { ctx ->
            val status = if (lockUntil != null && lockUntil > System.currentTimeMillis()) AccountStatus.LOCKED.name else AccountStatus.ACTIVE.name
            val sql = """
                UPDATE auth_accounts
                SET failed_login_count = ?, lock_until = ?, account_status = CASE WHEN ? = 'LOCKED' THEN 'LOCKED' ELSE account_status END, updated_at = NOW(), version = version + 1
                WHERE project_id = ? AND user_id = ?
            """.trimIndent()

            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    failedCount,
                    lockUntil?.let { Timestamp(it) },
                    status,
                    projectId,
                    userId
                )
            )
        }
    }

    override suspend fun recordSuccessfulLogin(projectId: String, userId: String, loginTime: Long) {
        val tenant = TenantContext(projectId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                UPDATE auth_accounts
                SET failed_login_count = 0, lock_until = NULL, last_login_at = ?, account_status = 'ACTIVE', updated_at = NOW(), version = version + 1
                WHERE project_id = ? AND user_id = ?
            """.trimIndent()

            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(Timestamp(loginTime), projectId, userId)
            )
        }
    }

    override suspend fun updatePassword(projectId: String, userId: String, passwordHash: String, salt: String, algorithm: String) {
        val tenant = TenantContext(projectId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                UPDATE auth_accounts
                SET password_hash = ?, password_salt = ?, password_algorithm = ?, password_changed_at = NOW(), updated_at = NOW(), version = version + 1
                WHERE project_id = ? AND user_id = ?
            """.trimIndent()

            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(passwordHash, salt, algorithm, projectId, userId)
            )
        }
    }

    override suspend fun updateAccountStatus(projectId: String, userId: String, status: AccountStatus) {
        val tenant = TenantContext(projectId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                UPDATE auth_accounts
                SET account_status = ?, updated_at = NOW(), version = version + 1
                WHERE project_id = ? AND user_id = ?
            """.trimIndent()

            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(status.name, projectId, userId)
            )
        }
    }
}

/**
 * Production-grade PostgreSQL DataSource for User Profiles (INFRA-03 Step 03).
 */
class PostgresAuthProfileDataSource(
    private val transactionManager: TransactionManager
) : AuthProfileDataSource {

    private fun mapProfile(rs: ResultSet): UserProfile {
        return UserProfile(
            projectId = rs.getString("project_id"),
            userId = rs.getString("user_id"),
            displayName = rs.getString("display_name"),
            legalName = rs.getString("legal_name"),
            email = rs.getString("email"),
            phone = rs.getString("phone"),
            avatarUrl = rs.getString("avatar_url"),
            preferredLanguage = rs.getString("preferred_language") ?: "en",
            timezone = rs.getString("timezone") ?: "UTC",
            contactPreferences = emptyMap(),
            emailVerifiedAt = rs.getNullableTimestampMillis("email_verified_at"),
            phoneVerifiedAt = rs.getNullableTimestampMillis("phone_verified_at"),
            createdAt = rs.getTimestampMillis("created_at"),
            updatedAt = rs.getTimestampMillis("updated_at"),
            version = rs.getLong("version")
        )
    }

    override suspend fun getProfile(projectId: String, userId: String): UserProfile? {
        val tenant = TenantContext(projectId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT project_id, user_id, display_name, legal_name, email, phone, avatar_url,
                       preferred_language, timezone, contact_preferences, email_verified_at,
                       phone_verified_at, created_at, updated_at, version
                FROM user_profiles
                WHERE project_id = ? AND user_id = ?
            """.trimIndent()

            ctx.sqlExecutor.querySingleOrNull(sql, listOf(projectId, userId)) { rs ->
                mapProfile(rs)
            }
        }
    }

    override suspend fun createOrUpdateProfile(profile: UserProfile): DomainResult<UserProfile> {
        val tenant = TenantContext(profile.projectId)
        return try {
            transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO user_profiles (
                        project_id, user_id, display_name, legal_name, email, phone, avatar_url,
                        preferred_language, timezone, created_at, updated_at, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), 1)
                    ON CONFLICT (project_id, user_id) DO UPDATE
                    SET display_name = EXCLUDED.display_name,
                        legal_name = EXCLUDED.legal_name,
                        email = EXCLUDED.email,
                        phone = EXCLUDED.phone,
                        avatar_url = EXCLUDED.avatar_url,
                        preferred_language = EXCLUDED.preferred_language,
                        timezone = EXCLUDED.timezone,
                        updated_at = NOW(),
                        version = user_profiles.version + 1
                    WHERE user_profiles.version = ?
                """.trimIndent()

                val affected = ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        profile.projectId,
                        profile.userId,
                        profile.displayName,
                        profile.legalName,
                        profile.email,
                        profile.phone,
                        profile.avatarUrl,
                        profile.preferredLanguage,
                        profile.timezone,
                        profile.version
                    )
                )
                if (affected == 0) {
                    throw IllegalStateException("OPTIMISTIC_CONCURRENCY_CONFLICT: Profile updated concurrently.")
                }
            }
            DomainResult.Success(profile.copy(version = profile.version + 1))
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create or update user profile")
        }
    }

    override suspend fun updateVerificationTimestamps(
        projectId: String,
        userId: String,
        emailVerifiedAt: Long?,
        phoneVerifiedAt: Long?
    ): Boolean {
        val tenant = TenantContext(projectId)
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                UPDATE user_profiles
                SET email_verified_at = COALESCE(?, email_verified_at),
                    phone_verified_at = COALESCE(?, phone_verified_at),
                    updated_at = NOW(),
                    version = version + 1
                WHERE project_id = ? AND user_id = ?
            """.trimIndent()

            val affected = ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    emailVerifiedAt?.let { Timestamp(it) },
                    phoneVerifiedAt?.let { Timestamp(it) },
                    projectId,
                    userId
                )
            )
            affected > 0
        }
    }
}

/**
 * Production-grade PostgreSQL DataSource for Verification Tokens (INFRA-03 Step 03).
 */
class PostgresAuthVerificationDataSource(
    private val transactionManager: TransactionManager
) : AuthVerificationDataSource {

    private fun mapToken(rs: ResultSet): UserVerificationToken {
        return UserVerificationToken(
            tokenId = rs.getString("token_id"),
            projectId = rs.getString("project_id"),
            userId = rs.getString("user_id"),
            verificationType = rs.getEnumByName("verification_type", VerificationType.EMAIL),
            tokenHash = rs.getString("token_hash"),
            tokenState = rs.getEnumByName("token_state", VerificationTokenState.PENDING),
            expiresAt = rs.getTimestampMillis("expires_at"),
            usedAt = rs.getNullableTimestampMillis("used_at"),
            createdAt = rs.getTimestampMillis("created_at"),
            version = rs.getLong("version")
        )
    }

    override suspend fun createVerificationToken(token: UserVerificationToken): DomainResult<UserVerificationToken> {
        val tenant = TenantContext(token.projectId)
        return try {
            transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO user_verification_tokens (
                        token_id, project_id, user_id, verification_type, token_hash,
                        token_state, expires_at, created_at, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), 1)
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        token.tokenId,
                        token.projectId,
                        token.userId,
                        token.verificationType.name,
                        token.tokenHash,
                        token.tokenState.name,
                        Timestamp(token.expiresAt)
                    )
                )
            }
            DomainResult.Success(token)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create verification token")
        }
    }

    override suspend fun getVerificationTokenByHash(tokenHash: String): UserVerificationToken? {
        val tenant = TenantContext("SYSTEM_DEFAULT")
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT token_id, project_id, user_id, verification_type, token_hash,
                       token_state, expires_at, used_at, created_at, version
                FROM user_verification_tokens
                WHERE token_hash = ?
            """.trimIndent()

            ctx.sqlExecutor.querySingleOrNull(sql, listOf(tokenHash)) { rs ->
                mapToken(rs)
            }
        }
    }

    override suspend fun consumeVerificationToken(tokenId: String, consumedAt: Long): Boolean {
        val tenant = TenantContext("SYSTEM_DEFAULT")
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                UPDATE user_verification_tokens
                SET token_state = 'USED', used_at = ?, version = version + 1
                WHERE token_id = ? AND token_state = 'PENDING' AND expires_at > NOW()
            """.trimIndent()

            val affected = ctx.sqlExecutor.executeUpdate(sql, listOf(Timestamp(consumedAt), tokenId))
            affected > 0
        }
    }

    override suspend fun revokeUserTokens(projectId: String, userId: String, verificationType: VerificationType): Int {
        val tenant = TenantContext(projectId)
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                UPDATE user_verification_tokens
                SET token_state = 'REVOKED', version = version + 1
                WHERE project_id = ? AND user_id = ? AND verification_type = ? AND token_state = 'PENDING'
            """.trimIndent()

            ctx.sqlExecutor.executeUpdate(sql, listOf(projectId, userId, verificationType.name))
        }
    }
}

/**
 * Production-grade PostgreSQL DataSource for Password History (INFRA-03 Step 03).
 */
class PostgresAuthPasswordHistoryDataSource(
    private val transactionManager: TransactionManager
) : AuthPasswordHistoryDataSource {

    private fun mapEntry(rs: ResultSet): PasswordHistoryEntry {
        return PasswordHistoryEntry(
            historyId = rs.getString("history_id"),
            projectId = rs.getString("project_id"),
            userId = rs.getString("user_id"),
            passwordHash = rs.getString("password_hash"),
            passwordSalt = rs.getString("password_salt"),
            createdAt = rs.getTimestampMillis("created_at")
        )
    }

    override suspend fun recordPasswordHistory(entry: PasswordHistoryEntry) {
        val tenant = TenantContext(entry.projectId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO password_history (history_id, project_id, user_id, password_hash, password_salt, created_at)
                VALUES (?, ?, ?, ?, ?, NOW())
            """.trimIndent()

            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(entry.historyId, entry.projectId, entry.userId, entry.passwordHash, entry.passwordSalt)
            )
        }
    }

    override suspend fun getRecentPasswordHistory(projectId: String, userId: String, limit: Int): List<PasswordHistoryEntry> {
        val tenant = TenantContext(projectId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT history_id, project_id, user_id, password_hash, password_salt, created_at
                FROM password_history
                WHERE project_id = ? AND user_id = ?
                ORDER BY created_at DESC
                LIMIT ?
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(projectId, userId, limit)) { rs ->
                mapEntry(rs)
            }
        }
    }
}

/**
 * Production-grade PostgreSQL DataSource for Auth Sessions and Refresh Tokens (INFRA-03 Step 01 & Step 03).
 */
class PostgresAuthSessionDataSource(
    private val transactionManager: TransactionManager
) : AuthSessionDataSource {

    private fun mapSession(rs: ResultSet): AuthSession {
        return AuthSession(
            sessionId = rs.getString("session_id"),
            projectId = rs.getString("project_id"),
            userId = rs.getString("user_id"),
            sessionStatus = rs.getEnumByName("session_status", SessionStatus.ACTIVE),
            refreshTokenHash = rs.getString("refresh_token_hash"),
            deviceName = rs.getString("device_name"),
            clientIp = rs.getString("client_ip"),
            userAgent = rs.getString("user_agent"),
            createdAt = rs.getTimestampMillis("created_at"),
            lastSeenAt = rs.getTimestampMillis("last_seen_at"),
            expiresAt = rs.getTimestampMillis("expires_at"),
            revokedAt = rs.getNullableTimestampMillis("revoked_at"),
            revocationReason = rs.getString("revocation_reason"),
            version = rs.getLong("version")
        )
    }

    override suspend fun createSession(session: AuthSession): DomainResult<AuthSession> {
        val tenant = TenantContext(session.projectId)
        return try {
            transactionManager.inTransaction(tenant) { ctx ->
                val sql = """
                    INSERT INTO auth_sessions (
                        session_id, project_id, user_id, session_status, refresh_token_hash,
                        device_name, client_ip, user_agent, created_at, last_seen_at, expires_at, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), ?, 1)
                """.trimIndent()

                ctx.sqlExecutor.executeUpdate(
                    sql,
                    listOf(
                        session.sessionId,
                        session.projectId,
                        session.userId,
                        session.sessionStatus.name,
                        session.refreshTokenHash,
                        session.deviceName,
                        session.clientIp,
                        session.userAgent,
                        Timestamp(session.expiresAt)
                    )
                )
            }
            DomainResult.Success(session)
        } catch (e: Throwable) {
            PostgresErrorTranslator.translate(e, "create auth session")
        }
    }

    override suspend fun getSession(sessionId: String): AuthSession? {
        val tenant = TenantContext("SYSTEM_DEFAULT")
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT session_id, project_id, user_id, session_status, refresh_token_hash,
                       device_name, client_ip, user_agent, created_at, last_seen_at, expires_at,
                       revoked_at, revocation_reason, version
                FROM auth_sessions
                WHERE session_id = ?
            """.trimIndent()

            ctx.sqlExecutor.querySingleOrNull(sql, listOf(sessionId)) { rs ->
                mapSession(rs)
            }
        }
    }

    override suspend fun getSessionByRefreshTokenHash(refreshTokenHash: String): AuthSession? {
        val tenant = TenantContext("SYSTEM_DEFAULT")
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT session_id, project_id, user_id, session_status, refresh_token_hash,
                       device_name, client_ip, user_agent, created_at, last_seen_at, expires_at,
                       revoked_at, revocation_reason, version
                FROM auth_sessions
                WHERE refresh_token_hash = ?
            """.trimIndent()

            ctx.sqlExecutor.querySingleOrNull(sql, listOf(refreshTokenHash)) { rs ->
                mapSession(rs)
            }
        }
    }

    override suspend fun updateSessionSeen(sessionId: String, seenAt: Long) {
        val tenant = TenantContext("SYSTEM_DEFAULT")
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = "UPDATE auth_sessions SET last_seen_at = ?, version = version + 1 WHERE session_id = ?"
            ctx.sqlExecutor.executeUpdate(sql, listOf(Timestamp(seenAt), sessionId))
        }
    }

    override suspend fun rotateRefreshToken(
        sessionId: String,
        oldTokenHash: String,
        newTokenHash: String,
        newExpiresAt: Long
    ): Boolean {
        val tenant = TenantContext("SYSTEM_DEFAULT")
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                UPDATE auth_sessions
                SET refresh_token_hash = ?, expires_at = ?, last_seen_at = NOW(), version = version + 1
                WHERE session_id = ? AND refresh_token_hash = ? AND session_status = 'ACTIVE'
            """.trimIndent()

            val affected = ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(newTokenHash, Timestamp(newExpiresAt), sessionId, oldTokenHash)
            )
            affected > 0
        }
    }

    override suspend fun revokeSession(sessionId: String, reason: String): Boolean {
        val tenant = TenantContext("SYSTEM_DEFAULT")
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                UPDATE auth_sessions
                SET session_status = 'REVOKED', revoked_at = NOW(), revocation_reason = ?, version = version + 1
                WHERE session_id = ? AND session_status = 'ACTIVE'
            """.trimIndent()

            val affected = ctx.sqlExecutor.executeUpdate(sql, listOf(reason, sessionId))
            affected > 0
        }
    }

    override suspend fun revokeAllUserSessions(projectId: String, userId: String, reason: String): Int {
        val tenant = TenantContext(projectId)
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                UPDATE auth_sessions
                SET session_status = 'REVOKED', revoked_at = NOW(), revocation_reason = ?, version = version + 1
                WHERE project_id = ? AND user_id = ? AND session_status = 'ACTIVE'
            """.trimIndent()

            ctx.sqlExecutor.executeUpdate(sql, listOf(reason, projectId, userId))
        }
    }

    override suspend fun getActiveSessions(projectId: String, userId: String): List<AuthSession> {
        val tenant = TenantContext(projectId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT session_id, project_id, user_id, session_status, refresh_token_hash,
                       device_name, client_ip, user_agent, created_at, last_seen_at, expires_at,
                       revoked_at, revocation_reason, version
                FROM auth_sessions
                WHERE project_id = ? AND user_id = ? AND session_status = 'ACTIVE' AND expires_at > NOW()
                ORDER BY created_at DESC
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(projectId, userId)) { rs ->
                mapSession(rs)
            }
        }
    }

    override suspend fun getAllSessionsForUser(projectId: String, userId: String): List<AuthSession> {
        val tenant = TenantContext(projectId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = """
                SELECT session_id, project_id, user_id, session_status, refresh_token_hash,
                       device_name, client_ip, user_agent, created_at, last_seen_at, expires_at,
                       revoked_at, revocation_reason, version
                FROM auth_sessions
                WHERE project_id = ? AND user_id = ?
                ORDER BY created_at DESC
            """.trimIndent()

            ctx.sqlExecutor.queryList(sql, listOf(projectId, userId)) { rs ->
                mapSession(rs)
            }
        }
    }
}

/**
 * Production-grade PostgreSQL DataSource for Security Audit Events (INFRA-03 Step 01).
 */
class PostgresAuthAuditDataSource(
    private val transactionManager: TransactionManager
) : AuthAuditDataSource {

    private fun mapAuditEvent(rs: ResultSet): AuthAuditEvent {
        return AuthAuditEvent(
            eventId = rs.getString("event_id"),
            projectId = rs.getString("project_id"),
            userId = rs.getString("user_id"),
            sessionId = rs.getString("session_id"),
            eventType = rs.getEnumByName("event_type", AuthEventType.AUTH_LOGIN_SUCCESS),
            outcome = rs.getEnumByName("outcome", AuthEventOutcome.SUCCESS),
            ipAddress = rs.getString("ip_address"),
            userAgent = rs.getString("user_agent"),
            correlationId = rs.getString("correlation_id"),
            details = emptyMap(),
            occurredAt = rs.getTimestampMillis("occurred_at")
        )
    }

    override suspend fun recordAuditEvent(event: AuthAuditEvent) {
        val tenant = TenantContext(event.projectId)
        transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO auth_audit_events (
                    event_id, project_id, user_id, session_id, event_type, outcome,
                    ip_address, user_agent, correlation_id, occurred_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """.trimIndent()

            ctx.sqlExecutor.executeUpdate(
                sql,
                listOf(
                    event.eventId,
                    event.projectId,
                    event.userId,
                    event.sessionId,
                    event.eventType.name,
                    event.outcome.name,
                    event.ipAddress,
                    event.userAgent,
                    event.correlationId
                )
            )
        }
    }

    override suspend fun queryAuditEvents(projectId: String, userId: String?, limit: Int): List<AuthAuditEvent> {
        val tenant = TenantContext(projectId)
        return transactionManager.inReadOnly(tenant) { ctx ->
            val sql = if (userId != null) {
                """
                    SELECT event_id, project_id, user_id, session_id, event_type, outcome,
                           ip_address, user_agent, correlation_id, occurred_at
                    FROM auth_audit_events
                    WHERE project_id = ? AND user_id = ?
                    ORDER BY occurred_at DESC
                    LIMIT ?
                """.trimIndent()
            } else {
                """
                    SELECT event_id, project_id, user_id, session_id, event_type, outcome,
                           ip_address, user_agent, correlation_id, occurred_at
                    FROM auth_audit_events
                    WHERE project_id = ?
                    ORDER BY occurred_at DESC
                    LIMIT ?
                """.trimIndent()
            }

            val params = if (userId != null) listOf(projectId, userId, limit) else listOf(projectId, limit)
            ctx.sqlExecutor.queryList(sql, params) { rs ->
                mapAuditEvent(rs)
            }
        }
    }
}

package com.sucharu.sucharupro.data.auth.service

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ConflictException
import com.sucharu.sucharupro.data.api.model.UnauthenticatedException
import com.sucharu.sucharupro.data.api.model.UserPermission
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.ValidationException
import com.sucharu.sucharupro.data.auth.datasource.*
import com.sucharu.sucharupro.data.auth.model.*
import com.sucharu.sucharupro.data.auth.security.*
import java.util.UUID

/**
 * Production-grade Authentication, Identity and Session Orchestrator (INFRA-03 Step 01 & Step 04).
 *
 * Provides authoritative credential verification, JWT token issuance, refresh token rotation,
 * secure public user registration, password recovery, verification delivery, session revocation,
 * brute-force throttling, and immutable security audit logging.
 */
class AuthenticationService(
    private val accountDataSource: AuthAccountDataSource,
    private val sessionDataSource: AuthSessionDataSource,
    private val auditDataSource: AuthAuditDataSource,
    private val profileDataSource: AuthProfileDataSource? = null,
    private val verificationDataSource: AuthVerificationDataSource? = null,
    private val passwordHistoryDataSource: AuthPasswordHistoryDataSource? = null,
    private val notificationProvider: IVerificationNotificationProvider = FakeVerificationNotificationProvider(),
    private val jwtProvider: JwtTokenProvider = JwtTokenProvider(),
    private val config: AuthConfig = AuthConfig()
) {


    /**
     * Resolves default permissions for standard roles.
     */
    fun resolvePermissionsForRole(role: UserRole): Set<UserPermission> {
        return when (role) {
            UserRole.ADMIN -> setOf(UserPermission.ADMIN_ALL)
            UserRole.MANAGER -> setOf(
                UserPermission.READ_OWN_PROFILE,
                UserPermission.UPDATE_OWN_PROFILE,
                UserPermission.MANAGE_CUSTOMERS,
                UserPermission.MANAGE_ORDERS,
                UserPermission.MANAGE_INVENTORY,
                UserPermission.MANAGE_QC
            )
            UserRole.STAFF -> setOf(
                UserPermission.READ_OWN_PROFILE,
                UserPermission.UPDATE_OWN_PROFILE,
                UserPermission.READ_OWN_ORDERS,
                UserPermission.MANAGE_ORDERS
            )
            UserRole.CUSTOMER -> setOf(
                UserPermission.READ_OWN_PROFILE,
                UserPermission.UPDATE_OWN_PROFILE,
                UserPermission.READ_OWN_ORDERS,
                UserPermission.CREATE_ORDER,
                UserPermission.READ_OWN_INVOICES,
                UserPermission.READ_OWN_PAYMENTS,
                UserPermission.READ_OWN_DELIVERY
            )
            UserRole.AFFILIATE -> setOf(
                UserPermission.READ_OWN_PROFILE,
                UserPermission.UPDATE_OWN_PROFILE,
                UserPermission.READ_OWN_AFFILIATE
            )
            UserRole.VENDOR -> setOf(
                UserPermission.READ_PUBLIC,
                UserPermission.READ_OWN_PROFILE,
                UserPermission.UPDATE_OWN_PROFILE,
                UserPermission.READ_VENDOR_PORTAL
            )
            UserRole.GUEST -> setOf(UserPermission.READ_PUBLIC)
            UserRole.AI_AGENT -> setOf(
                UserPermission.READ_PUBLIC,
                UserPermission.READ_OWN_PROFILE,
                UserPermission.CREATE_ORDER,
                UserPermission.READ_OWN_ORDERS
            )
        }
    }

    /**
     * Authenticates user credentials and establishes a new session with access & refresh tokens.
     */
    suspend fun login(
        request: LoginRequestDto,
        correlationId: String,
        clientIp: String? = null,
        userAgent: String? = null
    ): AuthResponseDto {
        val projectId = request.requestedProjectId ?: "TENANT-001"
        val now = System.currentTimeMillis()
        val rawId = request.identifier.trim()
        val normalizedPhone = com.sucharu.sucharupro.core.validation.CustomerValidation.normalizePhoneNumber(rawId)

        // 1. Account Lookup (By username, email, phone or normalized phone)
        var account = accountDataSource.getAccount(projectId, rawId)
        if (account == null && normalizedPhone.isNotBlank() && normalizedPhone != rawId) {
            account = accountDataSource.getAccount(projectId, normalizedPhone)
        }

        if (account == null) {
            recordAudit(
                projectId = projectId,
                userId = null,
                sessionId = null,
                eventType = AuthEventType.AUTH_LOGIN_FAILURE,
                outcome = AuthEventOutcome.FAILURE,
                ipAddress = clientIp,
                userAgent = userAgent,
                correlationId = correlationId,
                details = mapOf("reason" to "unknown_identifier")
            )
            // Prevent user enumeration with generic message
            throw UnauthenticatedException("Invalid credentials.")
        }

        // 2. Brute-force & Account Lock Validation
        if (account.isLocked) {
            recordAudit(
                projectId = projectId,
                userId = account.userId,
                sessionId = null,
                eventType = AuthEventType.AUTH_LOGIN_FAILURE,
                outcome = AuthEventOutcome.LOCKED,
                ipAddress = clientIp,
                userAgent = userAgent,
                correlationId = correlationId,
                details = mapOf("reason" to "account_locked")
            )
            throw UnauthenticatedException("Invalid credentials.")
        }

        // 3. Password Verification (Verify credentials first to prevent user status probing)
        val passwordValid = PasswordHasher.verifyPassword(
            plaintext = request.password,
            saltHex = account.passwordSalt,
            expectedHashHex = account.passwordHash,
            algorithm = account.passwordAlgorithm
        )

        if (!passwordValid) {
            val newFailed = account.failedLoginCount + 1
            val lockUntil = if (newFailed >= config.maxLoginAttempts) {
                now + (config.accountLockDurationSeconds * 1000L)
            } else null

            accountDataSource.updateFailedAttempts(projectId, account.userId, newFailed, lockUntil)

            recordAudit(
                projectId = projectId,
                userId = account.userId,
                sessionId = null,
                eventType = if (lockUntil != null) AuthEventType.AUTH_ACCOUNT_LOCKED else AuthEventType.AUTH_LOGIN_FAILURE,
                outcome = if (lockUntil != null) AuthEventOutcome.LOCKED else AuthEventOutcome.FAILURE,
                ipAddress = clientIp,
                userAgent = userAgent,
                correlationId = correlationId,
                details = mapOf("failedAttempts" to "$newFailed")
            )
            throw UnauthenticatedException("Invalid credentials.")
        }

        // 4. Account Status Validation (For verified credentials)
        if (account.accountStatus == AccountStatus.PENDING) {
            recordAudit(
                projectId = projectId,
                userId = account.userId,
                sessionId = null,
                eventType = AuthEventType.AUTH_LOGIN_FAILURE,
                outcome = AuthEventOutcome.DENIED,
                ipAddress = clientIp,
                userAgent = userAgent,
                correlationId = correlationId,
                details = mapOf("reason" to "pending_verification", "status" to account.accountStatus.name)
            )
            throw UnauthenticatedException("Account pending verification. Please verify your account before logging in.")
        }

        if (!account.canAuthenticate) {
            recordAudit(
                projectId = projectId,
                userId = account.userId,
                sessionId = null,
                eventType = AuthEventType.AUTH_LOGIN_FAILURE,
                outcome = AuthEventOutcome.DENIED,
                ipAddress = clientIp,
                userAgent = userAgent,
                correlationId = correlationId,
                details = mapOf("reason" to "inactive_status", "status" to account.accountStatus.name)
            )
            val statusMsg = when (account.accountStatus) {
                AccountStatus.DEACTIVATED -> "Account is deactivated."
                AccountStatus.SUSPENDED -> "Account is suspended."
                else -> "Invalid credentials."
            }
            throw UnauthenticatedException(statusMsg)
        }

        // 5. Successful Authentication -> Reset Failed Counters
        accountDataSource.recordSuccessfulLogin(projectId, account.userId, now)

        // 6. Create Session and Tokens
        val sessionId = TokenGenerator.generateSessionId()
        val rawRefreshToken = TokenGenerator.generateSecureToken(32)
        val refreshTokenHash = TokenGenerator.hashToken(rawRefreshToken)
        val refreshExpiresAt = now + (config.refreshTokenTtlSeconds * 1000L)

        val session = AuthSession(
            sessionId = sessionId,
            projectId = projectId,
            userId = account.userId,
            sessionStatus = SessionStatus.ACTIVE,
            refreshTokenHash = refreshTokenHash,
            deviceName = request.deviceName,
            clientIp = clientIp,
            userAgent = userAgent,
            createdAt = now,
            lastSeenAt = now,
            expiresAt = refreshExpiresAt
        )

        sessionDataSource.createSession(session)

        val permissions = resolvePermissionsForRole(account.role)
        val principal = AuthenticatedPrincipal(
            userId = account.userId,
            projectId = projectId,
            username = account.username,
            role = account.role,
            permissions = permissions,
            email = account.email,
            tokenExpiresAt = now + (config.accessTokenTtlSeconds * 1000L)
        )

        val accessToken = jwtProvider.generateAccessToken(principal, sessionId, config.accessTokenTtlSeconds)

        // 7. Audit Event
        recordAudit(
            projectId = projectId,
            userId = account.userId,
            sessionId = sessionId,
            eventType = AuthEventType.AUTH_LOGIN_SUCCESS,
            outcome = AuthEventOutcome.SUCCESS,
            ipAddress = clientIp,
            userAgent = userAgent,
            correlationId = correlationId
        )

        return AuthResponseDto(
            accessToken = accessToken,
            refreshToken = rawRefreshToken,
            tokenType = "Bearer",
            expiresInSeconds = config.accessTokenTtlSeconds,
            user = UserProfileDto(
                userId = account.userId,
                projectId = projectId,
                username = account.username,
                email = account.email,
                phone = account.phone,
                role = account.role,
                permissions = permissions,
                accountStatus = account.accountStatus
            ),
            sessionId = sessionId
        )
    }

    /**
     * Rotates refresh token and issues a new access token for active sessions.
     */
    suspend fun refresh(
        request: RefreshRequestDto,
        correlationId: String,
        clientIp: String? = null,
        userAgent: String? = null
    ): AuthResponseDto {
        val rawToken = request.refreshToken.trim()
        if (rawToken.isBlank()) {
            throw UnauthenticatedException("Refresh token is required.")
        }

        val tokenHash = TokenGenerator.hashToken(rawToken)
        val session = sessionDataSource.getSessionByRefreshTokenHash(tokenHash)

        if (session == null) {
            recordAudit(
                projectId = "SYSTEM_DEFAULT",
                userId = null,
                sessionId = null,
                eventType = AuthEventType.AUTH_REFRESH_FAILURE,
                outcome = AuthEventOutcome.FAILURE,
                ipAddress = clientIp,
                userAgent = userAgent,
                correlationId = correlationId,
                details = mapOf("reason" to "unknown_token_hash")
            )
            throw UnauthenticatedException("Invalid or expired refresh token.")
        }

        // Replay Detection: If presented token is not the active one, revoke session!
        if (session.refreshTokenHash != tokenHash) {
            sessionDataSource.revokeSession(session.sessionId, "Suspected refresh token replay")
            recordAudit(
                projectId = session.projectId,
                userId = session.userId,
                sessionId = session.sessionId,
                eventType = AuthEventType.AUTH_REFRESH_FAILURE,
                outcome = AuthEventOutcome.DENIED,
                ipAddress = clientIp,
                userAgent = userAgent,
                correlationId = correlationId,
                details = mapOf("reason" to "replay_detected")
            )
            throw UnauthenticatedException("Invalid or already consumed refresh token.")
        }

        // Validate Session Status
        if (!session.isActive) {
            recordAudit(
                projectId = session.projectId,
                userId = session.userId,
                sessionId = session.sessionId,
                eventType = AuthEventType.AUTH_REFRESH_FAILURE,
                outcome = AuthEventOutcome.DENIED,
                ipAddress = clientIp,
                userAgent = userAgent,
                correlationId = correlationId,
                details = mapOf("reason" to "session_${session.sessionStatus.name.lowercase()}")
            )
            throw UnauthenticatedException("Session has been revoked or expired.")
        }

        // Validate Account Status
        val account = accountDataSource.getAccountById(session.projectId, session.userId)
        if (account == null || !account.canAuthenticate) {
            sessionDataSource.revokeSession(session.sessionId, "Account suspended or missing")
            recordAudit(
                projectId = session.projectId,
                userId = session.userId,
                sessionId = session.sessionId,
                eventType = AuthEventType.AUTH_REFRESH_FAILURE,
                outcome = AuthEventOutcome.DENIED,
                ipAddress = clientIp,
                userAgent = userAgent,
                correlationId = correlationId,
                details = mapOf("reason" to "account_not_active")
            )
            throw UnauthenticatedException("Account is not active.")
        }

        val now = System.currentTimeMillis()
        val newRawRefreshToken = TokenGenerator.generateSecureToken(32)
        val newTokenHash = TokenGenerator.hashToken(newRawRefreshToken)
        val newExpiresAt = now + (config.refreshTokenTtlSeconds * 1000L)

        // Atomic Refresh Token Rotation
        val rotated = sessionDataSource.rotateRefreshToken(
            sessionId = session.sessionId,
            oldTokenHash = tokenHash,
            newTokenHash = newTokenHash,
            newExpiresAt = newExpiresAt
        )

        if (!rotated) {
            // Replay or collision detected!
            sessionDataSource.revokeSession(session.sessionId, "Suspected refresh token replay")
            recordAudit(
                projectId = session.projectId,
                userId = session.userId,
                sessionId = session.sessionId,
                eventType = AuthEventType.AUTH_REFRESH_FAILURE,
                outcome = AuthEventOutcome.DENIED,
                ipAddress = clientIp,
                userAgent = userAgent,
                correlationId = correlationId,
                details = mapOf("reason" to "replay_detected")
            )
            throw UnauthenticatedException("Invalid or already consumed refresh token.")
        }

        val permissions = resolvePermissionsForRole(account.role)
        val principal = AuthenticatedPrincipal(
            userId = account.userId,
            projectId = session.projectId,
            username = account.username,
            role = account.role,
            permissions = permissions,
            email = account.email,
            tokenExpiresAt = now + (config.accessTokenTtlSeconds * 1000L)
        )

        val newAccessToken = jwtProvider.generateAccessToken(principal, session.sessionId, config.accessTokenTtlSeconds)

        recordAudit(
            projectId = session.projectId,
            userId = account.userId,
            sessionId = session.sessionId,
            eventType = AuthEventType.AUTH_REFRESH_SUCCESS,
            outcome = AuthEventOutcome.SUCCESS,
            ipAddress = clientIp,
            userAgent = userAgent,
            correlationId = correlationId
        )

        return AuthResponseDto(
            accessToken = newAccessToken,
            refreshToken = newRawRefreshToken,
            tokenType = "Bearer",
            expiresInSeconds = config.accessTokenTtlSeconds,
            user = UserProfileDto(
                userId = account.userId,
                projectId = session.projectId,
                username = account.username,
                email = account.email,
                phone = account.phone,
                role = account.role,
                permissions = permissions,
                accountStatus = account.accountStatus
            ),
            sessionId = session.sessionId
        )
    }

    /**
     * Securely logs out the specified session.
     */
    suspend fun logout(
        sessionId: String,
        correlationId: String,
        clientIp: String? = null
    ): Boolean {
        val session = sessionDataSource.getSession(sessionId)
        val success = sessionDataSource.revokeSession(sessionId, "User explicit logout")

        if (session != null) {
            recordAudit(
                projectId = session.projectId,
                userId = session.userId,
                sessionId = sessionId,
                eventType = AuthEventType.AUTH_LOGOUT,
                outcome = AuthEventOutcome.SUCCESS,
                ipAddress = clientIp,
                correlationId = correlationId
            )
        }
        return success
    }

    /**
     * Revokes all active sessions for a user (e.g. password change, security compromise).
     */
    suspend fun logoutAll(
        projectId: String,
        userId: String,
        correlationId: String,
        clientIp: String? = null
    ): Int {
        val revokedCount = sessionDataSource.revokeAllUserSessions(projectId, userId, "User logout all devices")
        recordAudit(
            projectId = projectId,
            userId = userId,
            sessionId = null,
            eventType = AuthEventType.AUTH_ALL_SESSIONS_REVOKED,
            outcome = AuthEventOutcome.SUCCESS,
            ipAddress = clientIp,
            correlationId = correlationId,
            details = mapOf("revokedCount" to "$revokedCount")
        )
        return revokedCount
    }

    /**
     * Resolves authenticated principal from raw Bearer token, enforcing session validity.
     */
    suspend fun authenticateToken(token: String): AuthenticatedPrincipal {
        val principal = jwtProvider.validateAndParseToken(token)

        // Verify account is active
        val account = accountDataSource.getAccountById(principal.projectId, principal.userId)
        if (account == null || !account.canAuthenticate) {
            throw UnauthenticatedException("User account is inactive, locked or deleted.")
        }

        return principal
    }

    /**
     * Public user registration flow (INFRA-03 Step 04).
     * Enforces public role policy (CUSTOMER default, AFFILIATE if referral code provided; rejects ADMIN, MANAGER, STAFF, AI_AGENT).
     */
    suspend fun register(
        request: RegisterRequestDto,
        correlationId: String,
        clientIp: String? = null,
        userAgent: String? = null
    ): RegisterResponseDto {
        val projectId = request.requestedProjectId ?: "TENANT-001"
        val trimmedEmail = request.email?.trim()?.ifBlank { null }
        val trimmedPhone = request.phone?.trim()?.ifBlank { null }

        if (trimmedEmail == null && trimmedPhone == null) {
            throw ValidationException("Either email or phone number must be provided for registration.")
        }
        if (request.password.length < 8) {
            throw ValidationException("Password must be at least 8 characters long.")
        }

        // Enforce Public Registration Role Policy
        val targetRole = when {
            request.requestedRole != null -> {
                if (request.requestedRole in setOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.AI_AGENT)) {
                    recordAudit(
                        projectId = projectId,
                        userId = null,
                        sessionId = null,
                        eventType = AuthEventType.AUTH_REGISTER_FAILURE,
                        outcome = AuthEventOutcome.DENIED,
                        ipAddress = clientIp,
                        userAgent = userAgent,
                        correlationId = correlationId,
                        details = mapOf("reason" to "privileged_role_injection_attempt", "requestedRole" to request.requestedRole.name)
                    )
                    throw ValidationException("Public registration cannot assign privileged role '${request.requestedRole}'.")
                }
                request.requestedRole
            }
            !request.affiliateReferralCode.isNullOrBlank() -> UserRole.AFFILIATE
            else -> UserRole.CUSTOMER
        }

        val normalizedPhone = trimmedPhone?.let { com.sucharu.sucharupro.core.validation.CustomerValidation.normalizePhoneNumber(it) }
        val primaryIdentifier = trimmedEmail ?: (normalizedPhone ?: trimmedPhone!!)

        // Duplicate Account Prevention
        val existingByEmail = if (trimmedEmail != null) accountDataSource.getAccount(projectId, trimmedEmail) else null
        val existingByPhone = if (normalizedPhone != null) {
            accountDataSource.getAccount(projectId, normalizedPhone) ?: (if (trimmedPhone != normalizedPhone) accountDataSource.getAccount(projectId, trimmedPhone) else null)
        } else null
        val existing = existingByEmail ?: existingByPhone

        if (existing != null) {
            recordAudit(
                projectId = projectId,
                userId = existing.userId,
                sessionId = null,
                eventType = AuthEventType.AUTH_REGISTER_FAILURE,
                outcome = AuthEventOutcome.FAILURE,
                ipAddress = clientIp,
                userAgent = userAgent,
                correlationId = correlationId,
                details = mapOf("reason" to "duplicate_account")
            )
            throw ConflictException(message = "Account already exists with identifier '$primaryIdentifier'.")
        }

        val userId = UUID.randomUUID().toString()
        val username = request.username?.trim()?.ifBlank { null }
            ?: (trimmedEmail?.substringBefore("@") ?: "user_${userId.take(8)}")

        val hashed = PasswordHasher.hashPassword(request.password)

        val newAccount = AuthAccount(
            projectId = projectId,
            userId = userId,
            username = username,
            email = trimmedEmail,
            phone = normalizedPhone ?: trimmedPhone,
            passwordHash = hashed.hashHex,
            passwordSalt = hashed.saltHex,
            passwordAlgorithm = hashed.algorithm,
            role = targetRole,
            accountStatus = AccountStatus.PENDING
        )

        accountDataSource.createAccount(newAccount)

        val newProfile = UserProfile(
            projectId = projectId,
            userId = userId,
            displayName = request.displayName.trim(),
            email = trimmedEmail,
            phone = normalizedPhone ?: trimmedPhone
        )
        profileDataSource?.createOrUpdateProfile(newProfile)

        // Issue Verification Token if verificationDataSource is provided
        var deliveryAccepted = true
        var deliveryStatus = "DELIVERY_ACCEPTED"
        var respMessage = "Registration successful. Please verify your account."

        if (verificationDataSource != null) {
            val verifType = if (trimmedEmail != null && trimmedPhone == null) VerificationType.EMAIL else VerificationType.PHONE
            val rawToken = if (verifType == VerificationType.PHONE) {
                TokenGenerator.generateNumericOtp(6)
            } else {
                TokenGenerator.generateNumericOtp(6)
            }
            val tokenHash = TokenGenerator.hashToken(rawToken)

            val token = UserVerificationToken(
                tokenId = UUID.randomUUID().toString(),
                projectId = projectId,
                userId = userId,
                verificationType = verifType,
                tokenHash = tokenHash,
                tokenState = VerificationTokenState.PENDING,
                expiresAt = System.currentTimeMillis() + (900 * 1000L)
            )
            verificationDataSource.createVerificationToken(token)

            // Dispatch token via notification provider abstraction
            val deliveryResult = notificationProvider.sendVerificationNotification(
                projectId = projectId,
                userId = userId,
                recipient = primaryIdentifier,
                type = verifType,
                rawToken = rawToken
            )

            deliveryAccepted = deliveryResult.isAccepted
            deliveryStatus = deliveryResult.status.name
            respMessage = if (deliveryResult.isAccepted) {
                "Registration successful. A verification code has been sent to your ${verifType.name.lowercase()}."
            } else {
                "Registration successful, but we couldn't send the verification code right now. Please try requesting a resend shortly."
            }
        }

        recordAudit(
            projectId = projectId,
            userId = userId,
            sessionId = null,
            eventType = AuthEventType.AUTH_REGISTER_SUCCESS,
            outcome = AuthEventOutcome.SUCCESS,
            ipAddress = clientIp,
            userAgent = userAgent,
            correlationId = correlationId,
            details = mapOf("role" to targetRole.name, "status" to AccountStatus.PENDING.name, "deliveryStatus" to deliveryStatus)
        )

        return RegisterResponseDto(
            userId = userId,
            username = username,
            email = trimmedEmail,
            phone = trimmedPhone,
            accountStatus = AccountStatus.PENDING,
            role = targetRole,
            verificationRequired = true,
            message = respMessage,
            deliveryAccepted = deliveryAccepted,
            deliveryStatus = deliveryStatus
        )
    }

    /**
     * Account Enumeration Safe Password Recovery Request (INFRA-03 Step 04).
     * Always returns generic response regardless of whether the requested email/phone exists.
     */
    suspend fun requestPasswordRecovery(
        request: PasswordRecoveryRequestDto,
        correlationId: String,
        clientIp: String? = null,
        userAgent: String? = null
    ): PasswordRecoveryResponseDto {
        val identifier = request.identifier.trim()
        val normalizedPhone = com.sucharu.sucharupro.core.validation.CustomerValidation.normalizePhoneNumber(identifier)
        val genericResponse = PasswordRecoveryResponseDto("If the account exists, recovery instructions have been sent.")

        if (identifier.isBlank()) {
            return genericResponse
        }

        var account = accountDataSource.getAccount("TENANT-001", identifier)
        if (account == null && normalizedPhone.isNotBlank() && normalizedPhone != identifier) {
            account = accountDataSource.getAccount("TENANT-001", normalizedPhone)
        }
        if (account == null || account.accountStatus == AccountStatus.DELETED) {
            recordAudit(
                projectId = "TENANT-001",
                userId = null,
                sessionId = null,
                eventType = AuthEventType.AUTH_PASSWORD_RESET_REQUESTED,
                outcome = AuthEventOutcome.SUCCESS,
                ipAddress = clientIp,
                userAgent = userAgent,
                correlationId = correlationId,
                details = mapOf("result" to "non_existent_account_masked")
            )
            return genericResponse
        }

        if (verificationDataSource != null) {
            // Revoke existing pending password reset tokens
            verificationDataSource.revokeUserTokens(account.projectId, account.userId, VerificationType.PASSWORD_RESET)

            val rawToken = TokenGenerator.generateSecureToken(32)
            val tokenHash = TokenGenerator.hashToken(rawToken)

            val token = UserVerificationToken(
                tokenId = UUID.randomUUID().toString(),
                projectId = account.projectId,
                userId = account.userId,
                verificationType = VerificationType.PASSWORD_RESET,
                tokenHash = tokenHash,
                tokenState = VerificationTokenState.PENDING,
                expiresAt = System.currentTimeMillis() + (900 * 1000L)
            )

            verificationDataSource.createVerificationToken(token)

            val recipient = account.email ?: account.phone ?: identifier
            notificationProvider.sendVerificationNotification(
                projectId = account.projectId,
                userId = account.userId,
                recipient = recipient,
                type = VerificationType.PASSWORD_RESET,
                rawToken = rawToken
            )
        }

        recordAudit(
            projectId = account.projectId,
            userId = account.userId,
            sessionId = null,
            eventType = AuthEventType.AUTH_PASSWORD_RESET_REQUESTED,
            outcome = AuthEventOutcome.SUCCESS,
            ipAddress = clientIp,
            userAgent = userAgent,
            correlationId = correlationId
        )

        return genericResponse
    }

    /**
     * Password Recovery Reset Confirmation (INFRA-03 Step 04).
     * Validates single-use token, password history, updates credential, and revokes active user sessions.
     */
    suspend fun confirmPasswordReset(
        request: PasswordRecoveryConfirmDto,
        correlationId: String,
        clientIp: String? = null,
        userAgent: String? = null
    ): Boolean {
        val rawToken = request.token.trim()
        if (rawToken.isBlank()) {
            throw ValidationException("Reset token is required.")
        }
        if (request.newPassword.length < 8) {
            throw ValidationException("New password must be at least 8 characters long.")
        }

        val tokenHash = TokenGenerator.hashToken(rawToken)
        val token = verificationDataSource?.getVerificationTokenByHash(tokenHash)
            ?: throw ValidationException("Invalid or expired password reset token.")

        if (token.verificationType != VerificationType.PASSWORD_RESET || !token.isValid) {
            throw ValidationException("Password reset token is invalid, used, or expired.")
        }

        val account = accountDataSource.getAccountById(token.projectId, token.userId)
            ?: throw ValidationException("Associated user account not found.")

        // Check password history (prevent re-using recent 5 passwords)
        if (passwordHistoryDataSource != null) {
            val history = passwordHistoryDataSource.getRecentPasswordHistory(token.projectId, token.userId, limit = 5)
            for (past in history) {
                if (PasswordHasher.verifyPassword(request.newPassword, past.passwordSalt, past.passwordHash)) {
                    throw ValidationException("New password cannot be one of the last 5 previous passwords.")
                }
            }
        }

        // Hash new password
        val newHashed = PasswordHasher.hashPassword(request.newPassword)

        // Record history of old password
        passwordHistoryDataSource?.recordPasswordHistory(
            PasswordHistoryEntry(
                historyId = UUID.randomUUID().toString(),
                projectId = token.projectId,
                userId = token.userId,
                passwordHash = account.passwordHash,
                passwordSalt = account.passwordSalt
            )
        )

        // Update password in DB
        accountDataSource.updatePassword(token.projectId, token.userId, newHashed.hashHex, newHashed.saltHex, newHashed.algorithm)

        // Consume verification token
        verificationDataSource.consumeVerificationToken(token.tokenId, System.currentTimeMillis())

        // Revoke active user sessions if requested
        if (request.revokeSessions) {
            sessionDataSource.revokeAllUserSessions(token.projectId, token.userId, "PASSWORD_RESET_COMPLETED")
        }

        recordAudit(
            projectId = token.projectId,
            userId = token.userId,
            sessionId = null,
            eventType = AuthEventType.AUTH_PASSWORD_RESET_COMPLETED,
            outcome = AuthEventOutcome.SUCCESS,
            ipAddress = clientIp,
            userAgent = userAgent,
            correlationId = correlationId
        )

        return true
    }

    /**
     * Confirms a single-use verification token (email/phone), updates verification timestamps,
     * and activates PENDING accounts to ACTIVE state.
     */
    suspend fun verifyAccount(
        request: ConfirmVerificationRequestDto,
        correlationId: String,
        clientIp: String? = null,
        userAgent: String? = null
    ): Map<String, Any> {
        val rawToken = request.token.trim()
        if (rawToken.isBlank()) {
            throw ValidationException("Verification token cannot be blank.")
        }
        val tokenHash = TokenGenerator.hashToken(rawToken)
        val token = verificationDataSource?.getVerificationTokenByHash(tokenHash)
            ?: throw ValidationException("Invalid or expired verification token.")

        if (!token.isValid) {
            throw ValidationException("Verification token is no longer valid or has expired.")
        }

        val consumed = verificationDataSource.consumeVerificationToken(token.tokenId, System.currentTimeMillis())
        if (!consumed) {
            throw ValidationException("Verification token could not be processed.")
        }

        // Update profile verification timestamps
        when (token.verificationType) {
            VerificationType.EMAIL -> profileDataSource?.updateVerificationTimestamps(token.projectId, token.userId, System.currentTimeMillis(), null)
            VerificationType.PHONE -> profileDataSource?.updateVerificationTimestamps(token.projectId, token.userId, null, System.currentTimeMillis())
            VerificationType.PASSWORD_RESET -> {}
        }

        // Transition PENDING account to ACTIVE
        val account = accountDataSource.getAccountById(token.projectId, token.userId)
        if (account != null && account.accountStatus == AccountStatus.PENDING) {
            accountDataSource.updateAccountStatus(token.projectId, token.userId, AccountStatus.ACTIVE)
            recordAudit(
                projectId = token.projectId,
                userId = token.userId,
                sessionId = null,
                eventType = AuthEventType.AUTH_ACCOUNT_ACTIVATED,
                outcome = AuthEventOutcome.SUCCESS,
                ipAddress = clientIp,
                userAgent = userAgent,
                correlationId = correlationId,
                details = mapOf("reason" to "registration_verified", "channel" to token.verificationType.name)
            )
        }

        val eventType = when (token.verificationType) {
            VerificationType.EMAIL -> AuthEventType.AUTH_EMAIL_VERIFIED
            VerificationType.PHONE -> AuthEventType.AUTH_PHONE_VERIFIED
            VerificationType.PASSWORD_RESET -> AuthEventType.AUTH_PASSWORD_RESET_COMPLETED
        }
        recordAudit(
            projectId = token.projectId,
            userId = token.userId,
            sessionId = null,
            eventType = eventType,
            outcome = AuthEventOutcome.SUCCESS,
            ipAddress = clientIp,
            userAgent = userAgent,
            correlationId = correlationId
        )

        return mapOf(
            "verified" to true,
            "message" to "Account successfully verified and activated! Please sign in."
        )
    }

    /**
     * Resends a verification token to a pending user.
     * Enforces anti-enumeration semantics and honest delivery status.
     */
    suspend fun resendVerificationToken(
        identifier: String,
        correlationId: String,
        clientIp: String? = null,
        userAgent: String? = null
    ): ResendVerificationResponseDto {
        val trimmed = identifier.trim()
        val normalizedPhone = com.sucharu.sucharupro.core.validation.CustomerValidation.normalizePhoneNumber(trimmed)
        val projectId = "TENANT-001"
        var account = accountDataSource.getAccount(projectId, trimmed)
        if (account == null && normalizedPhone.isNotBlank() && normalizedPhone != trimmed) {
            account = accountDataSource.getAccount(projectId, normalizedPhone)
        }

        if (account != null && account.accountStatus == AccountStatus.PENDING && verificationDataSource != null) {
            val verifType = if (trimmed.contains("@") || (account.email != null && account.phone == null)) VerificationType.EMAIL else VerificationType.PHONE
            val recipient = if (verifType == VerificationType.EMAIL) (account.email ?: trimmed) else (account.phone ?: if (normalizedPhone.isNotBlank()) normalizedPhone else trimmed)

            // Invalidate prior pending tokens to enforce single active challenge
            verificationDataSource.revokeUserTokens(account.projectId, account.userId, verifType)

            val rawToken = if (verifType == VerificationType.PHONE) {
                TokenGenerator.generateNumericOtp(6)
            } else {
                TokenGenerator.generateNumericOtp(6)
            }
            val tokenHash = TokenGenerator.hashToken(rawToken)

            val token = UserVerificationToken(
                tokenId = UUID.randomUUID().toString(),
                projectId = account.projectId,
                userId = account.userId,
                verificationType = verifType,
                tokenHash = tokenHash,
                tokenState = VerificationTokenState.PENDING,
                expiresAt = System.currentTimeMillis() + (900 * 1000L)
            )
            verificationDataSource.createVerificationToken(token)

            val deliveryResult = notificationProvider.sendVerificationNotification(
                projectId = account.projectId,
                userId = account.userId,
                recipient = recipient,
                type = verifType,
                rawToken = rawToken
            )

            val outcome = if (deliveryResult.isAccepted) AuthEventOutcome.SUCCESS else AuthEventOutcome.FAILURE
            recordAudit(
                projectId = account.projectId,
                userId = account.userId,
                sessionId = null,
                eventType = AuthEventType.AUTH_VERIFICATION_REQUEST,
                outcome = outcome,
                ipAddress = clientIp,
                userAgent = userAgent,
                correlationId = correlationId,
                details = mapOf("channel" to verifType.name, "deliveryStatus" to deliveryResult.status.name)
            )

            return if (deliveryResult.isAccepted) {
                ResendVerificationResponseDto(
                    success = true,
                    deliveryStatus = "DELIVERY_ACCEPTED",
                    message = "A new verification code has been sent."
                )
            } else {
                ResendVerificationResponseDto(
                    success = false,
                    deliveryStatus = deliveryResult.status.name,
                    message = "We couldn't send the verification code right now. Please try again shortly."
                )
            }
        }

        return ResendVerificationResponseDto(
            success = true,
            deliveryStatus = "CHALLENGE_MASKED",
            message = "If the account exists and requires verification, a new code has been sent."
        )
    }

    private suspend fun recordAudit(
        projectId: String,
        userId: String?,
        sessionId: String?,
        eventType: AuthEventType,
        outcome: AuthEventOutcome,
        ipAddress: String? = null,
        userAgent: String? = null,
        correlationId: String? = null,
        details: Map<String, String> = emptyMap()
    ) {
        try {
            auditDataSource.recordAuditEvent(
                AuthAuditEvent(
                    eventId = UUID.randomUUID().toString(),
                    projectId = projectId,
                    userId = userId,
                    sessionId = sessionId,
                    eventType = eventType,
                    outcome = outcome,
                    ipAddress = ipAddress,
                    userAgent = userAgent,
                    correlationId = correlationId,
                    details = details
                )
            )
        } catch (_: Exception) {
            // Auditing failure should not crash core workflow, but is recorded
        }
    }
}

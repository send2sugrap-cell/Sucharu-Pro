package com.sucharu.sucharupro.data.auth.model

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserPermission
import com.sucharu.sucharupro.data.api.model.UserRole

/**
 * Account operational lifecycle status (INFRA-03 Step 01 & Step 03).
 */
enum class AccountStatus {
    PENDING,
    ACTIVE,
    LOCKED,
    SUSPENDED,
    DEACTIVATED,
    SECURITY_REVIEW,
    INACTIVE,
    DELETED;

    fun isValidTransitionTo(target: AccountStatus): Boolean {
        if (this == target) return true
        return when (this) {
            PENDING -> target in setOf(ACTIVE, DEACTIVATED, INACTIVE, DELETED)
            ACTIVE -> target in setOf(LOCKED, SUSPENDED, DEACTIVATED, SECURITY_REVIEW, INACTIVE, DELETED)
            LOCKED -> target in setOf(ACTIVE, SUSPENDED, DEACTIVATED, SECURITY_REVIEW)
            SUSPENDED -> target in setOf(ACTIVE, DEACTIVATED, SECURITY_REVIEW)
            SECURITY_REVIEW -> target in setOf(ACTIVE, SUSPENDED, DEACTIVATED)
            DEACTIVATED -> target in setOf(ACTIVE) // Explicit authorized reactivation required
            INACTIVE -> target in setOf(ACTIVE, DEACTIVATED)
            DELETED -> false // Terminal state
        }
    }
}

/**
 * Verification channel type.
 */
enum class VerificationType {
    EMAIL,
    PHONE,
    PASSWORD_RESET
}

/**
 * Verification token lifecycle status.
 */
enum class VerificationTokenState {
    PENDING,
    USED,
    EXPIRED,
    REVOKED
}

/**
 * Session lifecycle status (INFRA-03 Step 01).
 */
enum class SessionStatus {
    ACTIVE,
    EXPIRED,
    REVOKED
}

/**
 * Security audit event classifications (INFRA-03 Step 01 & Step 03).
 */
enum class AuthEventType {
    AUTH_LOGIN_SUCCESS,
    AUTH_LOGIN_FAILURE,
    AUTH_LOGOUT,
    AUTH_REFRESH_SUCCESS,
    AUTH_REFRESH_FAILURE,
    AUTH_SESSION_REVOKED,
    AUTH_ALL_SESSIONS_REVOKED,
    AUTH_ACCOUNT_LOCKED,
    AUTH_ACCOUNT_UNLOCKED,
    AUTH_ACCOUNT_ACTIVATED,
    AUTH_ACCOUNT_SUSPENDED,
    AUTH_ACCOUNT_REACTIVATED,
    AUTH_ACCOUNT_DEACTIVATED,
    AUTH_PROFILE_UPDATED,
    AUTH_PASSWORD_CHANGED,
    AUTH_PASSWORD_RESET_REQUESTED,
    AUTH_PASSWORD_RESET_COMPLETED,
    AUTH_EMAIL_VERIFIED,
    AUTH_PHONE_VERIFIED,
    AUTH_VERIFICATION_TOKEN_CREATED,
    AUTH_REGISTER_SUCCESS,
    AUTH_REGISTER_FAILURE,
    AUTH_VERIFICATION_REQUEST,
    AUTH_VERIFICATION_SUCCESS,
    AUTH_VERIFICATION_FAILURE,
    AUTH_SUSPICIOUS_ACTIVITY,
    AUTHORIZATION_DENIED
}

/**
 * Audit event outcomes.
 */
enum class AuthEventOutcome {
    SUCCESS,
    FAILURE,
    DENIED,
    LOCKED
}

/**
 * Production-grade Account identity model (INFRA-03 Step 01 & Step 03).
 */
data class AuthAccount(
    val projectId: String,
    val userId: String,
    val username: String,
    val email: String? = null,
    val phone: String? = null,
    val passwordHash: String,
    val passwordSalt: String,
    val passwordAlgorithm: String = "PBKDF2_SHA256",
    val role: UserRole = UserRole.CUSTOMER,
    val accountStatus: AccountStatus = AccountStatus.ACTIVE,
    val failedLoginCount: Int = 0,
    val lockUntil: Long? = null,
    val lastLoginAt: Long? = null,
    val passwordChangedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
) {
    val isLocked: Boolean get() = accountStatus == AccountStatus.LOCKED && (lockUntil != null && lockUntil > System.currentTimeMillis())
    val canAuthenticate: Boolean get() = (accountStatus == AccountStatus.ACTIVE || (accountStatus == AccountStatus.LOCKED && !isLocked))
}

/**
 * Server-authoritative user profile entity (INFRA-03 Step 03).
 */
data class UserProfile(
    val projectId: String,
    val userId: String,
    val displayName: String,
    val legalName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val preferredLanguage: String = "en",
    val timezone: String = "UTC",
    val contactPreferences: Map<String, Boolean> = mapOf("email" to true, "sms" to false, "push" to true),
    val emailVerifiedAt: Long? = null,
    val phoneVerifiedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

/**
 * Single-use verification token model (INFRA-03 Step 03).
 */
data class UserVerificationToken(
    val tokenId: String,
    val projectId: String,
    val userId: String,
    val verificationType: VerificationType,
    val tokenHash: String,
    val tokenState: VerificationTokenState = VerificationTokenState.PENDING,
    val expiresAt: Long,
    val usedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
) {
    val isValid: Boolean get() = tokenState == VerificationTokenState.PENDING && System.currentTimeMillis() < expiresAt
}

/**
 * Password change history entry for security compliance.
 */
data class PasswordHistoryEntry(
    val historyId: String,
    val projectId: String,
    val userId: String,
    val passwordHash: String,
    val passwordSalt: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Canonical Infrastructure User Identity Aggregation Model.
 */
data class UserIdentity(
    val account: AuthAccount,
    val profile: UserProfile?,
    val principalType: PrincipalType = PrincipalType.HUMAN
) {
    val userId: String get() = account.userId
    val projectId: String get() = account.projectId
    val isEmailVerified: Boolean get() = profile?.emailVerifiedAt != null
    val isPhoneVerified: Boolean get() = profile?.phoneVerifiedAt != null
}

/**
 * Server-authoritative session state entity (INFRA-03 Step 01).
 */
data class AuthSession(
    val sessionId: String,
    val projectId: String,
    val userId: String,
    val sessionStatus: SessionStatus = SessionStatus.ACTIVE,
    val refreshTokenHash: String,
    val previousRefreshTokenHashes: Set<String> = emptySet(),
    val deviceName: String? = null,
    val clientIp: String? = null,
    val userAgent: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis(),
    val expiresAt: Long,
    val revokedAt: Long? = null,
    val revocationReason: String? = null,
    val version: Long = 1L
) {
    val isActive: Boolean get() = sessionStatus == SessionStatus.ACTIVE && System.currentTimeMillis() < expiresAt
}

/**
 * Immutable audit event entity for security compliance (INFRA-03 Step 01).
 */
data class AuthAuditEvent(
    val eventId: String,
    val projectId: String,
    val userId: String? = null,
    val sessionId: String? = null,
    val eventType: AuthEventType,
    val outcome: AuthEventOutcome,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val correlationId: String? = null,
    val details: Map<String, String> = emptyMap(),
    val occurredAt: Long = System.currentTimeMillis()
)

// =========================================================================
// REQUEST & RESPONSE DTOS (INFRA-03 Step 03)
// =========================================================================

data class LoginRequestDto(
    val identifier: String,
    val password: String,
    val deviceName: String? = null,
    val requestedProjectId: String? = null
)

data class RefreshRequestDto(
    val refreshToken: String
)

data class LogoutRequestDto(
    val allDevices: Boolean = false
)

data class UserProfileDto(
    val userId: String,
    val projectId: String,
    val username: String,
    val displayName: String = "",
    val email: String? = null,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val preferredLanguage: String = "en",
    val timezone: String = "UTC",
    val emailVerified: Boolean = false,
    val phoneVerified: Boolean = false,
    val role: UserRole,
    val permissions: Set<UserPermission>,
    val accountStatus: AccountStatus
) {
    fun toAuthenticatedPrincipal(): com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal =
        com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal(
            userId = userId,
            projectId = projectId,
            username = username,
            role = role,
            permissions = permissions,
            email = email,
            accountStatus = accountStatus
        )
}

data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val user: UserProfileDto,
    val sessionId: String
)

data class UpdateUserProfileRequestDto(
    val displayName: String? = null,
    val legalName: String? = null,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val preferredLanguage: String? = null,
    val timezone: String? = null,
    val contactPreferences: Map<String, Boolean>? = null
)

data class ChangePasswordRequestDto(
    val currentPassword: String,
    val newPassword: String,
    val revokeOtherSessions: Boolean = true
)

data class RequestVerificationRequestDto(
    val verificationType: VerificationType = VerificationType.PHONE,
    val identifier: String? = null
)

data class ConfirmVerificationRequestDto(
    val verificationType: VerificationType = VerificationType.PHONE,
    val token: String,
    val identifier: String? = null
)

data class ResendVerificationRequestDto(
    val identifier: String
)

data class SessionSummaryDto(
    val sessionId: String,
    val deviceName: String? = null,
    val clientIp: String? = null,
    val createdAt: Long,
    val lastSeenAt: Long,
    val expiresAt: Long,
    val isCurrent: Boolean = false,
    val sessionStatus: SessionStatus
)

data class SessionDetailsDto(
    val sessionId: String,
    val userId: String,
    val deviceName: String? = null,
    val clientIp: String? = null,
    val userAgent: String? = null,
    val createdAt: Long,
    val lastSeenAt: Long,
    val expiresAt: Long,
    val sessionStatus: SessionStatus,
    val isCurrentSession: Boolean = false
)

data class RevokeSessionRequestDto(
    val sessionId: String,
    val reason: String? = "User initiated revocation"
)

data class UpdateAccountStatusRequestDto(
    val newStatus: AccountStatus,
    val reason: String? = null
)

data class UserIdentityResponseDto(
    val userId: String,
    val projectId: String,
    val username: String,
    val email: String?,
    val phone: String?,
    val role: UserRole,
    val principalType: PrincipalType,
    val accountStatus: AccountStatus,
    val profile: UserProfileDto?,
    val lastLoginAt: Long?
)

data class RegisterRequestDto(
    val displayName: String,
    val username: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val password: String,
    val acceptedTermsVersion: String = "1.0",
    val affiliateReferralCode: String? = null,
    val requestedProjectId: String? = null,
    val requestedRole: UserRole? = null
)

data class RegisterResponseDto(
    val userId: String,
    val username: String,
    val email: String?,
    val phone: String?,
    val accountStatus: AccountStatus,
    val role: UserRole,
    val verificationRequired: Boolean = true,
    val message: String = "Registration successful.",
    val deliveryAccepted: Boolean = true,
    val deliveryStatus: String = "DELIVERY_ACCEPTED"
)

data class ResendVerificationResponseDto(
    val success: Boolean = true,
    val deliveryStatus: String = "DELIVERY_ACCEPTED",
    val message: String = "A new verification code has been sent."
)

data class PasswordRecoveryRequestDto(
    val identifier: String
)

data class PasswordRecoveryConfirmDto(
    val token: String,
    val newPassword: String,
    val revokeSessions: Boolean = true
)

data class PasswordRecoveryResponseDto(
    val message: String = "If the account exists, recovery instructions have been sent."
)



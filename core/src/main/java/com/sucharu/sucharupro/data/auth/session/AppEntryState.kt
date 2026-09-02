package com.sucharu.sucharupro.data.auth.session

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.auth.model.AccountStatus

/**
 * Client-Side Application Entry State Machine (INFRA-03 Step 05).
 * Represents the server-authoritative state of the application for the active session.
 */
sealed class AppEntryState {
    object Initializing : AppEntryState()
    object Public : AppEntryState()
    object Authenticating : AppEntryState()

    data class Authenticated(
        val principal: AuthenticatedPrincipal
    ) : AppEntryState()

    data class VerificationRequired(
        val userId: String,
        val email: String?,
        val phone: String?
    ) : AppEntryState()

    object SessionExpired : AppEntryState()

    data class AccountUnavailable(
        val status: AccountStatus,
        val displayMessage: String
    ) : AppEntryState()

    object RecoveryFlow : AppEntryState()

    data class Error(
        val message: String
    ) : AppEntryState()
}

/**
 * Maps backend [AccountStatus] to user-safe, sanitized UI messaging (INFRA-03 Step 05).
 * Enforces zero-knowledge leakage regarding internal security rules.
 */
fun AccountStatus.toSanitizedDisplayMessage(): String {
    return when (this) {
        AccountStatus.PENDING -> "Your account verification is required."
        AccountStatus.ACTIVE -> "Account active."
        AccountStatus.LOCKED -> "Your account is temporarily unavailable. Please try again later."
        AccountStatus.SUSPENDED -> "Your account is currently unavailable."
        AccountStatus.DEACTIVATED -> "This account is currently inactive."
        AccountStatus.SECURITY_REVIEW -> "Your account is under security review."
        AccountStatus.INACTIVE -> "This account is inactive."
        AccountStatus.DELETED -> "Unable to continue with this account."
    }
}

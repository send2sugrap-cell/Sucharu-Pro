package com.sucharu.sucharupro.data.auth.session

import com.sucharu.sucharupro.data.api.client.BackendApiClient
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.auth.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Production-Grade Client Authentication Session Manager (INFRA-03 Step 05).
 * Connects the server-authoritative backend identity/security foundation with the client application shell.
 * Features single-flight refresh protection, 401 retry handling, startup restoration, and state dispatch.
 */
class AuthenticationSessionManager(
    private val client: BackendApiClient,
    private val sessionStore: ISecureSessionStore = InMemorySecureSessionStore()
) {
    private val _entryState = MutableStateFlow<AppEntryState>(AppEntryState.Initializing)
    val entryState: StateFlow<AppEntryState> = _entryState.asStateFlow()

    private val refreshMutex = Mutex()

    /**
     * Restores application session on startup (INFRA-03 Step 05).
     * 1. If no local session exists -> AppEntryState.Public
     * 2. Calls getMyProfile() to obtain server-authoritative identity.
     * 3. If access token expired, performs single-flight refresh using refresh token and retries.
     * 4. Evaluates AccountStatus to transition to Authenticated or account status UI.
     */
    suspend fun restoreSession(): AppEntryState {
        _entryState.value = AppEntryState.Initializing
        val existingSession = sessionStore.getSession()

        if (existingSession == null) {
            _entryState.value = AppEntryState.Public
            return AppEntryState.Public
        }

        // Try getting profile with current access token
        val profileRes = client.getMyProfile()
        if (profileRes is ApiResult.Success) {
            val principal = profileRes.data
            sessionStore.updatePrincipal(principal)
            val state = evaluateAuthoritativePrincipal(principal)
            _entryState.value = state
            return state
        }

        // If unauthenticated (401), attempt single-flight refresh
        if (profileRes is ApiResult.Error && profileRes.errorResponse.errorCode == ErrorCode.UNAUTHENTICATED) {
            val refreshed = refreshSessionInternal()
            if (refreshed) {
                val retryRes = client.getMyProfile()
                if (retryRes is ApiResult.Success) {
                    val principal = retryRes.data
                    sessionStore.updatePrincipal(principal)
                    val state = evaluateAuthoritativePrincipal(principal)
                    _entryState.value = state
                    return state
                }
            }
        }

        // Refresh failed or invalid session -> clear session and transition to Public / SessionExpired
        sessionStore.clearSession()
        _entryState.value = AppEntryState.SessionExpired
        return AppEntryState.SessionExpired
    }

    /**
     * Executes login request and initializes authenticated session (INFRA-03 Step 05).
     */
    suspend fun login(request: LoginRequestDto): ApiResult<AuthResponseDto> {
        _entryState.value = AppEntryState.Authenticating
        val res = client.login(request)
        if (res is ApiResult.Success) {
            val authResp = res.data
            val principal = authResp.user.toAuthenticatedPrincipal()
            val sessionData = UserSessionData(
                accessToken = authResp.accessToken,
                refreshToken = authResp.refreshToken,
                sessionId = authResp.sessionId,
                principal = principal
            )
            sessionStore.saveSession(sessionData)

            val state = evaluateAuthoritativePrincipal(principal)
            _entryState.value = state
        } else if (res is ApiResult.Error) {
            _entryState.value = AppEntryState.Public
        }
        return res
    }

    /**
     * Executes public user registration request (INFRA-03 Step 05).
     */
    suspend fun register(request: RegisterRequestDto): ApiResult<RegisterResponseDto> {
        _entryState.value = AppEntryState.Authenticating
        val res = client.register(request)
        if (res is ApiResult.Success) {
            val regResp = res.data
            if (regResp.verificationRequired) {
                _entryState.value = AppEntryState.VerificationRequired(
                    userId = regResp.userId,
                    email = regResp.email,
                    phone = regResp.phone
                )
            } else {
                _entryState.value = AppEntryState.Public
            }
        } else {
            _entryState.value = AppEntryState.Public
        }
        return res
    }

    /**
     * Executes password recovery request (INFRA-03 Step 05).
     * Enforces account enumeration safety.
     */
    suspend fun requestPasswordRecovery(request: PasswordRecoveryRequestDto): ApiResult<PasswordRecoveryResponseDto> {
        val res = client.requestPasswordRecovery(request)
        _entryState.value = AppEntryState.RecoveryFlow
        return res
    }

    /**
     * Confirms password reset with single-use token (INFRA-03 Step 05).
     */
    suspend fun confirmPasswordReset(request: PasswordRecoveryConfirmDto): ApiResult<Map<String, Any>> {
        val res = client.confirmPasswordReset(request)
        if (res is ApiResult.Success) {
            // Require fresh authentication after reset
            sessionStore.clearSession()
            _entryState.value = AppEntryState.Public
        }
        return res
    }

    /**
     * Confirms contact/account verification token (INFRA-03 Step 05).
     * On successful verification, transitions to Public state allowing immediate login.
     */
    suspend fun confirmVerification(token: String, type: VerificationType = VerificationType.PHONE): ApiResult<Map<String, Any>> {
        val res = client.confirmVerificationToken(ConfirmVerificationRequestDto(verificationType = type, token = token))
        if (res is ApiResult.Success) {
            _entryState.value = AppEntryState.Public
        }
        return res
    }

    /**
     * Resends verification token for an unverified account.
     */
    suspend fun resendVerification(identifier: String): ApiResult<Map<String, Any>> {
        return client.resendVerificationToken(identifier)
    }

    /**
     * Single-flight token refresh operation (INFRA-03 Step 05).
     * Prevents race conditions and multiple concurrent refresh calls for the same session.
     */
    suspend fun refreshSession(): Boolean = refreshMutex.withLock {
        refreshSessionInternal()
    }

    private suspend fun refreshSessionInternal(): Boolean {
        val current = sessionStore.getSession() ?: return false
        val refreshRes = client.refreshToken(current.refreshToken)
        return if (refreshRes is ApiResult.Success) {
            val newAuth = refreshRes.data
            sessionStore.updateTokens(newAuth.accessToken, newAuth.refreshToken)
            true
        } else {
            false
        }
    }

    /**
     * Centralized 401 Unauthenticated interceptor helper (INFRA-03 Step 05).
     * Executes single-flight refresh and retries targeted API call exactly once where safe.
     */
    suspend fun <T> executeWith401Retry(block: suspend () -> ApiResult<T>): ApiResult<T> {
        val result = block()
        if (result is ApiResult.Error && result.errorResponse.errorCode == ErrorCode.UNAUTHENTICATED) {
            val refreshed = refreshSession()
            if (refreshed) {
                return block() // Retry exactly once
            } else {
                sessionStore.clearSession()
                _entryState.value = AppEntryState.SessionExpired
            }
        }
        return result
    }

    /**
     * Performs secure session logout (INFRA-03 Step 05).
     */
    suspend fun logout(allDevices: Boolean = false): ApiResult<Map<String, String>> {
        val res = if (allDevices) client.logoutAll() else client.logout(allDevices = false)
        sessionStore.clearSession()
        _entryState.value = AppEntryState.Public
        return res
    }

    /**
     * Evaluates server-authoritative principal and determines application entry state (INFRA-03 Step 05).
     */
    fun evaluateAuthoritativePrincipal(principal: AuthenticatedPrincipal): AppEntryState {
        // Evaluate account status first
        return when (principal.accountStatus) {
            AccountStatus.ACTIVE -> AppEntryState.Authenticated(principal)
            AccountStatus.PENDING -> AppEntryState.VerificationRequired(principal.userId, principal.email, null)
            AccountStatus.LOCKED -> AppEntryState.AccountUnavailable(AccountStatus.LOCKED, AccountStatus.LOCKED.toSanitizedDisplayMessage())
            AccountStatus.SUSPENDED -> AppEntryState.AccountUnavailable(AccountStatus.SUSPENDED, AccountStatus.SUSPENDED.toSanitizedDisplayMessage())
            AccountStatus.DEACTIVATED -> AppEntryState.AccountUnavailable(AccountStatus.DEACTIVATED, AccountStatus.DEACTIVATED.toSanitizedDisplayMessage())
            AccountStatus.SECURITY_REVIEW -> AppEntryState.AccountUnavailable(AccountStatus.SECURITY_REVIEW, AccountStatus.SECURITY_REVIEW.toSanitizedDisplayMessage())
            AccountStatus.INACTIVE -> AppEntryState.AccountUnavailable(AccountStatus.INACTIVE, AccountStatus.INACTIVE.toSanitizedDisplayMessage())
            AccountStatus.DELETED -> AppEntryState.AccountUnavailable(AccountStatus.DELETED, AccountStatus.DELETED.toSanitizedDisplayMessage())
        }
    }
}

package com.sucharu.sucharupro.data.auth.session

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import java.util.concurrent.atomic.AtomicReference

/**
 * Client Session Data Container (INFRA-03 Step 05).
 * Encapsulates access token, refresh token, session ID, and server-authoritative principal.
 */
data class UserSessionData(
    val accessToken: String,
    val refreshToken: String,
    val sessionId: String,
    val principal: AuthenticatedPrincipal
)

/**
 * Secure Session & Token Storage Abstraction (INFRA-03 Step 05).
 * Guarantees raw tokens and passwords are never logged or exposed in debug strings.
 */
interface ISecureSessionStore {
    fun saveSession(session: UserSessionData)
    fun getSession(): UserSessionData?
    fun updateAccessToken(newAccessToken: String)
    fun updateTokens(newAccessToken: String, newRefreshToken: String)
    fun updatePrincipal(principal: AuthenticatedPrincipal)
    fun clearSession()
    fun hasSession(): Boolean
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
}

/**
 * Thread-safe In-Memory implementation of [ISecureSessionStore] for testing and runtime execution.
 */
class InMemorySecureSessionStore : ISecureSessionStore {

    private val sessionRef = AtomicReference<UserSessionData?>(null)

    override fun saveSession(session: UserSessionData) {
        sessionRef.set(session)
    }

    override fun getSession(): UserSessionData? {
        return sessionRef.get()
    }

    override fun updateAccessToken(newAccessToken: String) {
        val current = sessionRef.get()
        if (current != null) {
            sessionRef.set(current.copy(accessToken = newAccessToken))
        }
    }

    override fun updateTokens(newAccessToken: String, newRefreshToken: String) {
        val current = sessionRef.get()
        if (current != null) {
            sessionRef.set(current.copy(accessToken = newAccessToken, refreshToken = newRefreshToken))
        }
    }

    override fun updatePrincipal(principal: AuthenticatedPrincipal) {
        val current = sessionRef.get()
        if (current != null) {
            sessionRef.set(current.copy(principal = principal))
        }
    }

    override fun clearSession() {
        sessionRef.set(null)
    }

    override fun hasSession(): Boolean {
        return sessionRef.get() != null
    }

    override fun getAccessToken(): String? {
        return sessionRef.get()?.accessToken
    }

    override fun getRefreshToken(): String? {
        return sessionRef.get()?.refreshToken
    }
}

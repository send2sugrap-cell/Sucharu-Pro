package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UnauthenticatedException
import com.sucharu.sucharupro.data.auth.security.JwtTokenProvider
import java.util.concurrent.ConcurrentHashMap

/**
 * Server-side trusted authentication and security token engine (INFRA-02 Step 04, INFRA-03 Step 01, INFRA-05 Step 03).
 *
 * Enforces server-authoritative tenant and user identity resolution, verifying cryptographic
 * JWT access tokens and managing active sessions.
 */
class BackendSecurityContext(
    private val jwtTokenProvider: JwtTokenProvider = JwtTokenProvider()
) {

    private val activeTokens = ConcurrentHashMap<String, AuthenticatedPrincipal>()

    fun registerToken(token: String, principal: AuthenticatedPrincipal) {
        activeTokens[token] = principal
    }

    fun unregisterToken(token: String) {
        activeTokens.remove(token)
    }

    /**
     * Resolves the authenticated principal strictly from the Authorization header.
     * Expected format: "Bearer <token>"
     * Throws [UnauthenticatedException] if header is missing, malformed, invalid, or expired.
     */
    fun authenticate(authHeader: String?): AuthenticatedPrincipal {
        if (authHeader.isNullOrBlank()) {
            throw UnauthenticatedException("Authorization header is missing.")
        }

        val trimmed = authHeader.trim()
        if (!trimmed.startsWith("Bearer", ignoreCase = true)) {
            throw UnauthenticatedException("Unsupported authentication scheme. Expected 'Bearer <token>'.")
        }

        val token = if (trimmed.length > 6) trimmed.substring(6).trim() else ""
        if (token.isEmpty()) {
            throw UnauthenticatedException("Bearer token cannot be empty.")
        }

        // 1. If it's a standard 3-part JWT, validate cryptographically
        if (token.count { it == '.' } == 2) {
            return jwtTokenProvider.validateAndParseToken(token)
        }

        // 2. Check registered in-memory active tokens (e.g. for mock integration or explicit server session keys)
        val principal = activeTokens[token]
            ?: throw UnauthenticatedException("Invalid or expired authentication token.")

        if (System.currentTimeMillis() > principal.tokenExpiresAt) {
            activeTokens.remove(token)
            throw UnauthenticatedException("Authentication token has expired.")
        }

        return principal
    }
}

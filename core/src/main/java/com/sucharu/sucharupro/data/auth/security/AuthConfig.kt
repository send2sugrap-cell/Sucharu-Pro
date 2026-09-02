package com.sucharu.sucharupro.data.auth.security

/**
 * Production-grade security configuration for authentication, JWT and sessions (INFRA-03 Step 01).
 */
data class AuthConfig(
    val accessTokenTtlSeconds: Long = 900L, // 15 minutes
    val refreshTokenTtlSeconds: Long = 604800L, // 7 days
    val jwtIssuer: String = "sucharu-pro-auth",
    val jwtAudience: String = "sucharu-pro-api",
    val jwtKeyId: String = "kid-2026-v1",
    val jwtSigningSecret: String = System.getenv("JWT_SIGNING_SECRET") ?: "development_only_jwt_signing_secret_do_not_use_in_prod",
    val maxLoginAttempts: Int = 5,
    val accountLockDurationSeconds: Long = 900L, // 15 minutes
    val pbkdf2Iterations: Int = 65536
) {
    fun toSafeString(): String {
        return "AuthConfig(accessTokenTtl=${accessTokenTtlSeconds}s, refreshTokenTtl=${refreshTokenTtlSeconds}s, issuer='$jwtIssuer', audience='$jwtAudience', keyId='$jwtKeyId', signingSecret=[REDACTED], maxLoginAttempts=$maxLoginAttempts, lockDuration=${accountLockDurationSeconds}s)"
    }

    fun validateForProduction(): List<String> {
        val errors = mutableListOf<String>()
        if (jwtSigningSecret.isBlank() || jwtSigningSecret.length < 32 || jwtSigningSecret.contains("development_only")) {
            errors.add("JWT signing secret must be at least 32 characters long and not use development fallback.")
        }
        if (accessTokenTtlSeconds <= 0) {
            errors.add("Access token TTL must be positive.")
        }
        if (refreshTokenTtlSeconds <= accessTokenTtlSeconds) {
            errors.add("Refresh token TTL must be strictly greater than access token TTL.")
        }
        return errors
    }
}

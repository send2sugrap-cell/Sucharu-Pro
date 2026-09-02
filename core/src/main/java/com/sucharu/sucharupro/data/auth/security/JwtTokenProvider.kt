package com.sucharu.sucharupro.data.auth.security

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UnauthenticatedException
import com.sucharu.sucharupro.data.api.model.UserPermission
import com.sucharu.sucharupro.data.api.model.UserRole
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Production-grade JWT Access Token Issuer and Verifier (INFRA-03 Step 01, INFRA-05 Step 03).
 *
 * Implements standard RFC 7519 HMAC-SHA256 signing, strict algorithm enforcement (HS256 only),
 * expiration checks, issuer/audience validation, anti-downgrade defense, and safe claims extraction.
 */
class JwtTokenProvider(
    private val config: AuthConfig = AuthConfig()
) {

    private val base64UrlEncoder = Base64.getUrlEncoder().withoutPadding()
    private val base64UrlDecoder = Base64.getUrlDecoder()

    /**
     * Generates a signed JWT access token for the given authenticated principal and session.
     */
    fun generateAccessToken(
        principal: AuthenticatedPrincipal,
        sessionId: String = "sess_${UUID.randomUUID().toString().take(8)}",
        ttlSeconds: Long = config.accessTokenTtlSeconds
    ): String {
        val nowSeconds = System.currentTimeMillis() / 1000L
        val expSeconds = nowSeconds + ttlSeconds
        val jti = UUID.randomUUID().toString()

        val headerJson = """{"alg":"HS256","typ":"JWT","kid":"${config.jwtKeyId}"}"""
        val headerB64 = base64UrlEncoder.encodeToString(headerJson.toByteArray(Charsets.UTF_8))

        val permsStr = principal.permissions.joinToString(",") { it.name }
        val payloadJson = """
            {
                "sub":"${principal.userId}",
                "sid":"$sessionId",
                "pid":"${principal.projectId}",
                "usr":"${escapeJson(principal.username)}",
                "role":"${principal.role.name}",
                "perms":"$permsStr",
                "iss":"${config.jwtIssuer}",
                "aud":"${config.jwtAudience}",
                "iat":$nowSeconds,
                "exp":$expSeconds,
                "jti":"$jti"
            }
        """.trimIndent().replace("\n", "").replace(" ", "")

        val payloadB64 = base64UrlEncoder.encodeToString(payloadJson.toByteArray(Charsets.UTF_8))
        val signatureB64 = sign("$headerB64.$payloadB64", config.jwtSigningSecret)

        return "$headerB64.$payloadB64.$signatureB64"
    }

    /**
     * Validates signature, expiration, issuer, audience, and extracts the trusted [AuthenticatedPrincipal].
     */
    fun validateAndParseToken(token: String): AuthenticatedPrincipal {
        val parts = token.trim().split(".")
        if (parts.size != 3) {
            throw UnauthenticatedException("Malformed JWT token structure.")
        }

        val headerB64 = parts[0]
        val payloadB64 = parts[1]
        val signatureB64 = parts[2]

        // 1. Verify Header & Algorithm (Strict allow-list: HS256 only)
        val headerJson = try {
            String(base64UrlDecoder.decode(headerB64), Charsets.UTF_8)
        } catch (_: Exception) {
            throw UnauthenticatedException("Invalid JWT header encoding.")
        }

        val headerClaims = parseSimpleJson(headerJson)
        val alg = headerClaims["alg"] ?: throw UnauthenticatedException("Missing JWT algorithm in header.")
        if (alg != "HS256") {
            throw UnauthenticatedException("Unsupported or insecure JWT algorithm: '$alg'. Only 'HS256' is permitted.")
        }

        // 2. Verify Cryptographic Signature (Timing-safe comparison)
        val expectedSigB64 = sign("$headerB64.$payloadB64", config.jwtSigningSecret)
        if (!MessageDigest.isEqual(expectedSigB64.toByteArray(Charsets.UTF_8), signatureB64.toByteArray(Charsets.UTF_8))) {
            throw UnauthenticatedException("Invalid JWT signature.")
        }

        // 3. Verify Payload Claims
        val payloadJson = try {
            String(base64UrlDecoder.decode(payloadB64), Charsets.UTF_8)
        } catch (_: Exception) {
            throw UnauthenticatedException("Invalid JWT payload encoding.")
        }

        val claims = parseSimpleJson(payloadJson)

        val iss = claims["iss"] ?: throw UnauthenticatedException("Missing issuer in JWT.")
        if (iss != config.jwtIssuer) {
            throw UnauthenticatedException("Invalid JWT issuer: expected '${config.jwtIssuer}', got '$iss'.")
        }

        val aud = claims["aud"] ?: throw UnauthenticatedException("Missing audience in JWT.")
        if (aud != config.jwtAudience) {
            throw UnauthenticatedException("Invalid JWT audience: expected '${config.jwtAudience}', got '$aud'.")
        }

        val exp = claims["exp"]?.toLongOrNull() ?: throw UnauthenticatedException("Missing expiration in JWT.")
        val nowSeconds = System.currentTimeMillis() / 1000L
        if (nowSeconds >= exp) {
            throw UnauthenticatedException("JWT access token has expired.")
        }

        val userId = claims["sub"] ?: throw UnauthenticatedException("Missing subject ('sub') in JWT.")
        if (userId.isBlank()) throw UnauthenticatedException("Subject ('sub') in JWT cannot be blank.")

        val projectId = claims["pid"] ?: throw UnauthenticatedException("Missing tenant project ID ('pid') in JWT.")
        if (projectId.isBlank()) throw UnauthenticatedException("Tenant project ID ('pid') in JWT cannot be blank.")

        val username = claims["usr"] ?: userId
        val roleStr = claims["role"] ?: "CUSTOMER"
        val role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.CUSTOMER }

        val permsStr = claims["perms"] ?: ""
        val permissions = permsStr.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull {
                try { UserPermission.valueOf(it.trim()) } catch (_: Exception) { null }
            }.toSet()

        return AuthenticatedPrincipal(
            userId = userId,
            projectId = projectId,
            username = username,
            role = role,
            permissions = permissions,
            tokenExpiresAt = exp * 1000L
        )
    }

    private fun sign(data: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKey)
        val hmacBytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return base64UrlEncoder.encodeToString(hmacBytes)
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private fun parseSimpleJson(json: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val regex = Regex(""""([^"]+)":(?:"([^"]*)"|([^,}]+))""")
        val matches = regex.findAll(json)
        for (m in matches) {
            val key = m.groupValues[1]
            val strVal = m.groupValues[2]
            val rawVal = m.groupValues[3]
            val value = if (strVal.isNotEmpty() || m.value.contains(""":""""")) strVal else rawVal.trim()
            result[key] = value
        }
        return result
    }
}

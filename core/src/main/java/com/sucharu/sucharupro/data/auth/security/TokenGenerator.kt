package com.sucharu.sucharupro.data.auth.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Cryptographically secure token and session identifier generator (INFRA-03 Step 01).
 */
object TokenGenerator {

    private val secureRandom = SecureRandom()

    /**
     * Generates a URL-safe high-entropy random token.
     */
    fun generateSecureToken(byteLength: Int = 32): String {
        val bytes = ByteArray(byteLength)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * Generates a cryptographically secure numeric OTP of the specified digit length (default 6).
     * Standard format for SMS phone verification.
     */
    fun generateNumericOtp(digits: Int = 6): String {
        require(digits in 4..10) { "OTP digit length must be between 4 and 10." }
        val min = Math.pow(10.0, (digits - 1).toDouble()).toInt()
        val max = Math.pow(10.0, digits.toDouble()).toInt() - 1
        val code = secureRandom.nextInt(max - min + 1) + min
        return code.toString()
    }

    /**
     * Generates a unique session identifier.
     */
    fun generateSessionId(): String {
        return "sess_${generateSecureToken(18)}"
    }

    /**
     * Computes the SHA-256 fingerprint hash of a raw token for safe database persistence.
     * Guarantees that raw refresh tokens are NEVER stored in PostgreSQL.
     */
    fun hashToken(rawToken: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(rawToken.toByteArray(Charsets.UTF_8))
        return bytesToHex(hashBytes)
    }

    /**
     * Constant-time comparison of two token hashes to prevent timing attacks.
     */
    fun secureCompare(hashA: String, hashB: String): Boolean {
        val bytesA = hashA.toByteArray(Charsets.UTF_8)
        val bytesB = hashB.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(bytesA, bytesB)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = Character.forDigit(v ushr 4, 16)
            hexChars[i * 2 + 1] = Character.forDigit(v and 0x0F, 16)
        }
        return String(hexChars)
    }
}

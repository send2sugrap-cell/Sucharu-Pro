package com.sucharu.sucharupro.data.auth.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Result of password hashing.
 */
data class HashedPassword(
    val hashHex: String,
    val saltHex: String,
    val algorithm: String = "PBKDF2_SHA256",
    val iterations: Int = 65536
)

/**
 * Production-grade, adaptive password hashing engine using PBKDF2WithHmacSHA256 (INFRA-03 Step 01).
 *
 * Enforces strong salted hashing (65,536 iterations), constant-time verification,
 * and zero plaintext storage/exposure.
 */
object PasswordHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val DEFAULT_ITERATIONS = 65536
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTE_LENGTH = 16

    private val secureRandom = SecureRandom()

    /**
     * Hashes a plaintext password with a cryptographically secure random salt.
     */
    fun hashPassword(plaintext: String, customSaltHex: String? = null, iterations: Int = DEFAULT_ITERATIONS): HashedPassword {
        require(plaintext.isNotBlank()) { "Password cannot be blank." }

        val saltBytes = if (customSaltHex != null) {
            hexToBytes(customSaltHex)
        } else {
            ByteArray(SALT_BYTE_LENGTH).also { secureRandom.nextBytes(it) }
        }

        val spec = PBEKeySpec(plaintext.toCharArray(), saltBytes, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hashBytes = factory.generateSecret(spec).encoded

        return HashedPassword(
            hashHex = bytesToHex(hashBytes),
            saltHex = bytesToHex(saltBytes),
            algorithm = "PBKDF2_SHA256",
            iterations = iterations
        )
    }

    /**
     * Verifies a plaintext password against expected salt and hash using constant-time comparison.
     */
    fun verifyPassword(plaintext: String, saltHex: String, expectedHashHex: String, algorithm: String = "PBKDF2_SHA256", iterations: Int = DEFAULT_ITERATIONS): Boolean {
        if (plaintext.isBlank() || saltHex.isBlank() || expectedHashHex.isBlank()) return false
        return try {
            val computed = hashPassword(plaintext, saltHex, iterations)
            val expectedBytes = hexToBytes(expectedHashHex)
            val computedBytes = hexToBytes(computed.hashHex)
            MessageDigest.isEqual(expectedBytes, computedBytes)
        } catch (_: Exception) {
            false
        }
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

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}

package com.sucharu.sucharupro.data.observability.correlation

import java.security.SecureRandom
import java.util.UUID

/**
 * Validates, sanitizes, and generates bounded collision-resistant correlation IDs (INFRA-05 Step 06).
 */
object CorrelationIdGenerator {

    private const val MAX_ID_LENGTH = 64
    private val SAFE_ID_REGEX = Regex("^[a-zA-Z0-9_-]{1,64}$")
    private val secureRandom = SecureRandom()
    private const val HEX_CHARS = "0123456789abcdef"

    /**
     * Generates a collision-resistant correlation identifier with prefix.
     */
    fun generate(prefix: String = "req"): String {
        val bytes = ByteArray(8)
        secureRandom.nextBytes(bytes)
        val hex = buildString(16) {
            for (b in bytes) {
                val v = b.toInt() and 0xFF
                append(HEX_CHARS[v ushr 4])
                append(HEX_CHARS[v and 0x0F])
            }
        }
        return "$prefix-$hex"
    }

    /**
     * Normalizes an inbound header or generates a fallback if invalid, null, or unsafe.
     */
    fun normalizeOrGenerate(inboundId: String?, prefix: String = "req"): String {
        if (inboundId.isNullOrBlank()) {
            return generate(prefix)
        }
        val trimmed = inboundId.trim()
        if (trimmed.length > MAX_ID_LENGTH || !SAFE_ID_REGEX.matches(trimmed)) {
            return generate(prefix)
        }
        return trimmed
    }
}

package com.sucharu.sucharupro.data.integration.security

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

/**
 * Interface for verifying external webhook payload cryptographic signatures (INFRA-05 Step 05).
 */
interface WebhookSignatureVerifier {
    /**
     * Verifies that the raw payload and headers match the provider signature.
     * @return true if valid and authentic; false if tampered, forged, or expired.
     */
    fun verify(
        rawPayload: String,
        headers: Map<String, String>,
        secret: String
    ): Boolean
}

/**
 * HMAC-SHA256 Webhook Signature Verifier with timing-safe comparison and replay protection.
 *
 * Supports:
 * - Standard Hex HMAC-SHA256 (e.g., `X-Hub-Signature-256`, `X-Webhook-Signature`)
 * - Timestamp-aware signatures (e.g., `t=1756000000,v1=hex...`)
 * - Constant-time byte comparison using [MessageDigest.isEqual]
 */
class HmacSha256SignatureVerifier(
    private val timestampHeaderName: String = "X-Webhook-Timestamp",
    private val signatureHeaderNames: List<String> = listOf(
        "X-Webhook-Signature",
        "X-Hub-Signature-256",
        "X-Signature-256",
        "Webhook-Signature"
    ),
    private val maxTimestampDriftSeconds: Long = 300L // 5 minute replay protection window
) : WebhookSignatureVerifier {

    override fun verify(
        rawPayload: String,
        headers: Map<String, String>,
        secret: String
    ): Boolean {
        if (secret.isBlank()) return false

        // Normalize header lookup (case-insensitive)
        val signature = findHeader(headers, signatureHeaderNames) ?: return false
        val timestamp = findHeader(headers, listOf(timestampHeaderName))

        // 1. Replay Protection via Timestamp validation (if timestamp header is present)
        if (!timestamp.isNullOrBlank()) {
            val tsLong = timestamp.toLongOrNull() ?: return false
            val currentSeconds = System.currentTimeMillis() / 1000L
            val drift = abs(currentSeconds - tsLong)
            if (drift > maxTimestampDriftSeconds) {
                return false // Timestamp outside acceptable tolerance window
            }
        }

        // 2. Compute expected HMAC
        val payloadToSign = if (!timestamp.isNullOrBlank()) {
            "$timestamp.$rawPayload"
        } else {
            rawPayload
        }

        val expectedHex = computeHmacSha256Hex(payloadToSign, secret)
        val cleanSignature = normalizeSignature(signature)

        // 3. Constant-time comparison
        return timingSafeEquals(expectedHex, cleanSignature)
    }

    private fun findHeader(headers: Map<String, String>, candidateNames: List<String>): String? {
        for (candidate in candidateNames) {
            val entry = headers.entries.find { it.key.equals(candidate, ignoreCase = true) }
            if (entry != null && entry.value.isNotBlank()) {
                return entry.value
            }
        }
        return null
    }

    private fun normalizeSignature(signature: String): String {
        var clean = signature.trim()
        if (clean.startsWith("sha256=", ignoreCase = true)) {
            clean = clean.substring(7).trim()
        } else if (clean.contains("v1=")) {
            // e.g. t=1756000,v1=abcdef...
            val parts = clean.split(",")
            val v1Part = parts.find { it.trim().startsWith("v1=") }
            if (v1Part != null) {
                clean = v1Part.substring(v1Part.indexOf("v1=") + 3).trim()
            }
        }
        return clean.lowercase()
    }

    private fun computeHmacSha256Hex(data: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKey)
        val hmacBytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hmacBytes.joinToString("") { "%02x".format(it) }
    }

    private fun timingSafeEquals(a: String, b: String): Boolean {
        return MessageDigest.isEqual(
            a.toByteArray(Charsets.UTF_8),
            b.toByteArray(Charsets.UTF_8)
        )
    }
}

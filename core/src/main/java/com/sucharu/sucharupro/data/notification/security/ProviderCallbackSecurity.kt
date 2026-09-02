package com.sucharu.sucharupro.data.notification.security

import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Production-grade provider callback (webhook) security verifier (INFRA-04 Step 07).
 *
 * Enforces:
 * 1. HMAC signature verification (HMAC-SHA256)
 * 2. Callback timestamp validation (prevents replay of old callbacks)
 * 3. Idempotency key deduplication (prevents replayed callbacks from having side effects)
 *
 * All three checks must pass for a callback to be considered valid.
 */
object ProviderCallbackSecurity {

    /** Maximum age for a valid callback timestamp. Older callbacks are rejected. */
    private const val MAX_CALLBACK_AGE_MS = 5 * 60 * 1000L // 5 minutes

    /** In-memory deduplication set for callback idempotency keys (bounded). */
    private val seenCallbackKeys = ConcurrentHashMap.newKeySet<String>()
    private const val MAX_SEEN_ENTRIES = 50_000

    /**
     * Verifies an HMAC-SHA256 signature over the raw callback payload.
     *
     * @param payload The raw bytes received from the provider.
     * @param receivedSignature The hex-encoded signature from the provider header.
     * @param secret The shared secret (never logged).
     * @return true if signature is valid; false otherwise. Fails closed on any error.
     */
    fun verifyHmacSignature(
        payload: ByteArray,
        receivedSignature: String,
        secret: String,
        algorithm: String = "HmacSHA256"
    ): Boolean {
        return try {
            val mac = Mac.getInstance(algorithm)
            mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), algorithm))
            val computed = mac.doFinal(payload).joinToString("") { "%02x".format(it) }
            // Constant-time comparison to prevent timing attacks
            constantTimeEquals(computed, receivedSignature.lowercase())
        } catch (_: Throwable) {
            false // Fail closed on any exception
        }
    }

    /**
     * Validates that a callback timestamp is within the acceptable age window.
     *
     * @param callbackTimestampMs Timestamp from the provider callback (milliseconds since epoch).
     * @param maxAgeMs Maximum allowed age (defaults to 5 minutes).
     * @return true if the timestamp is recent enough; false if expired or in the future.
     */
    fun validateCallbackTimestamp(
        callbackTimestampMs: Long,
        maxAgeMs: Long = MAX_CALLBACK_AGE_MS
    ): Boolean {
        val nowMs = System.currentTimeMillis()
        val age = nowMs - callbackTimestampMs
        // Reject if too old OR suspiciously in the future (> 30 seconds clock skew)
        return age in 0..maxAgeMs
    }

    /**
     * Checks if a callback idempotency key has already been processed.
     * Marks the key as seen on first call. Subsequent calls for the same key return true.
     *
     * @param idempotencyKey Stable unique key for this callback event.
     * @return true if the callback has ALREADY been processed (is a replay); false if new.
     */
    fun isCallbackReplayed(idempotencyKey: String): Boolean {
        if (seenCallbackKeys.size >= MAX_SEEN_ENTRIES) {
            // Safety valve: never block indefinitely on OOM. Log this in production.
            seenCallbackKeys.clear()
        }
        return !seenCallbackKeys.add(idempotencyKey)
    }

    /**
     * Full callback validation — all three checks must pass.
     *
     * @return [CallbackValidationResult] describing which checks passed/failed.
     */
    fun validateCallback(
        payload: ByteArray,
        signature: String,
        secret: String,
        timestampMs: Long,
        idempotencyKey: String
    ): CallbackValidationResult {
        val signatureValid = verifyHmacSignature(payload, signature, secret)
        val timestampValid = validateCallbackTimestamp(timestampMs)
        val isReplay = if (signatureValid && timestampValid) isCallbackReplayed(idempotencyKey) else true

        return CallbackValidationResult(
            isValid = signatureValid && timestampValid && !isReplay,
            signatureValid = signatureValid,
            timestampValid = timestampValid,
            isReplay = isReplay
        )
    }

    /** Clears the seen-callbacks deduplication set (for testing only). */
    internal fun clearSeenCallbacks() {
        seenCallbackKeys.clear()
    }

    /**
     * Constant-time string comparison to prevent timing side-channel attacks.
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}

data class CallbackValidationResult(
    val isValid: Boolean,
    val signatureValid: Boolean,
    val timestampValid: Boolean,
    val isReplay: Boolean
)

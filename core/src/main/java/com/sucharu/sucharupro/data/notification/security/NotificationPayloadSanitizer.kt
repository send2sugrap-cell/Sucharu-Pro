package com.sucharu.sucharupro.data.notification.security

import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.event.boundary.NotificationIntent
import com.sucharu.sucharupro.domain.notification.security.NotificationDataClassification

/**
 * Production-grade notification payload sanitizer (INFRA-04 Step 07).
 *
 * Deterministically removes credentials, secrets, injection vectors, and oversized content
 * from notification payloads before provider dispatch or audit logging.
 */
object NotificationPayloadSanitizer {

    // Fields that must never appear in notification payloads or logs
    private val SENSITIVE_FIELD_PATTERN = Regex(
        "(password|secret|token|api[_-]?key|apikey|credential|auth[_-]?header|" +
        "access[_-]?token|refresh[_-]?token|session[_-]?id|session[_-]?token|" +
        "bearer|private[_-]?key|signing[_-]?key)",
        RegexOption.IGNORE_CASE
    )

    // Header injection / CRLF injection sequences for title fields
    private val TITLE_HEADER_INJECTION_PATTERN = Regex("[\r\n]|%0[aAdD]|\\\\r|\\\\n")

    // CRLF injection for body fields (encoded header injection sequences)
    private val BODY_ENCODED_INJECTION_PATTERN = Regex("%0[aAdD]|\\\\r|\\\\n")

    // Script injection patterns
    private val SCRIPT_INJECTION_PATTERN = Regex(
        "<script[^>]*>.*?</script>|javascript:|on\\w+\\s*=",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    // Control characters except tab and standard newline
    private val CONTROL_CHAR_PATTERN = Regex("[\u0000-\u0008\u000B\u000C\u000E-\u001F\u007F]")

    // Stack trace indicator patterns
    private val STACK_TRACE_PATTERN = Regex("at (com|org|java|kotlin|android)\\.[\\w.]+\\(")

    // Per-channel max sizes
    private const val SMS_MAX_BODY_CHARS = 160
    private const val PUSH_MAX_TITLE_CHARS = 64
    private const val PUSH_MAX_BODY_CHARS = 256
    private const val EMAIL_MAX_TITLE_CHARS = 998  // RFC 2822 subject limit
    private const val DEFAULT_MAX_BODY_CHARS = 4096

    /**
     * Sanitizes a [NotificationIntent] for provider dispatch.
     * Returns a new payload with clean title, body, and metadata.
     */
    fun sanitize(
        intent: NotificationIntent,
        channel: NotificationChannel,
        classification: NotificationDataClassification = NotificationDataClassification.PUBLIC,
        metadata: Map<String, String> = emptyMap()
    ): SanitizedNotificationPayload {
        val sanitizedTitle = sanitizeText(intent.title, isTitleField = true)
        val sanitizedBody = sanitizeText(intent.body, isTitleField = false)

        val truncatedTitle = truncateTitle(sanitizedTitle, channel)
        val truncatedBody = truncateBody(sanitizedBody, channel)

        val sanitizedMetadata = sanitizeMetadata(metadata)

        return SanitizedNotificationPayload(
            title = truncatedTitle,
            body = truncatedBody,
            metadata = sanitizedMetadata,
            channel = channel,
            wasRedacted = (truncatedTitle != intent.title || truncatedBody != intent.body)
        )
    }

    /**
     * Sanitizes free-form text fields, removing injection vectors and stack traces.
     */
    fun sanitizeText(text: String, isTitleField: Boolean = false): String {
        var result = text

        // Strip stack traces first while line structure is preserved
        if (!isTitleField) {
            result = stripStackTrace(result)
        }

        // Header injection sanitization
        result = if (isTitleField) {
            TITLE_HEADER_INJECTION_PATTERN.replace(result, " ")
        } else {
            BODY_ENCODED_INJECTION_PATTERN.replace(result, " ")
        }

        // Remove control characters
        result = CONTROL_CHAR_PATTERN.replace(result, "")

        // Remove script injection
        result = SCRIPT_INJECTION_PATTERN.replace(result, "[REMOVED]")

        return result.trim()
    }

    /**
     * Sanitizes a metadata map, removing all credential-like keys and their values.
     */
    fun sanitizeMetadata(metadata: Map<String, String>): Map<String, String> {
        return metadata.filterKeys { key ->
            !SENSITIVE_FIELD_PATTERN.containsMatchIn(key)
        }.mapValues { (_, value) ->
            if (looksLikeCredential(value)) "[REDACTED]" else value
        }
    }

    /**
     * Checks if a text field contains credential-like patterns (JWT, API keys, etc.).
     */
    fun containsCredentialLeak(text: String): Boolean {
        // JWT: 3 base64url segments separated by dots (min 8 chars per segment, min 25 total)
        val jwtPattern = Regex("[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}")
        val apiKeyPattern = Regex("[A-Fa-f0-9]{32,}|[A-Za-z0-9_\\-]{40,}")
        return (jwtPattern.containsMatchIn(text) && text.length >= 25) || apiKeyPattern.containsMatchIn(text)
    }

    /**
     * Checks if a text field contains HTML/script injection or title header injection.
     */
    fun containsInjection(text: String): Boolean {
        return SCRIPT_INJECTION_PATTERN.containsMatchIn(text) ||
               TITLE_HEADER_INJECTION_PATTERN.containsMatchIn(text) ||
               CONTROL_CHAR_PATTERN.containsMatchIn(text)
    }

    private fun looksLikeCredential(value: String): Boolean {
        if (value.length < 20) return false
        val jwtPattern = Regex("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")
        val longHex = Regex("^[A-Fa-f0-9]{32,}$")
        return jwtPattern.matches(value) || longHex.matches(value)
    }

    private fun stripStackTrace(text: String): String {
        if (!STACK_TRACE_PATTERN.containsMatchIn(text) && !text.contains("Caused by:")) return text
        return text.lines().filterNot { line ->
            STACK_TRACE_PATTERN.containsMatchIn(line) ||
            line.trim().startsWith("Caused by:") ||
            line.trim().startsWith("... ")
        }.joinToString("\n").trim()
    }

    private fun truncateTitle(title: String, channel: NotificationChannel): String {
        val maxLen = when (channel) {
            NotificationChannel.PUSH -> PUSH_MAX_TITLE_CHARS
            NotificationChannel.EMAIL -> EMAIL_MAX_TITLE_CHARS
            else -> 256
        }
        return if (title.length > maxLen) title.take(maxLen - 3) + "..." else title
    }

    private fun truncateBody(body: String, channel: NotificationChannel): String {
        val maxLen = when (channel) {
            NotificationChannel.SMS -> SMS_MAX_BODY_CHARS
            NotificationChannel.PUSH -> PUSH_MAX_BODY_CHARS
            else -> DEFAULT_MAX_BODY_CHARS
        }
        return if (body.length > maxLen) body.take(maxLen - 3) + "..." else body
    }
}

/**
 * Result of payload sanitization for a specific channel.
 */
data class SanitizedNotificationPayload(
    val title: String,
    val body: String,
    val metadata: Map<String, String>,
    val channel: NotificationChannel,
    val wasRedacted: Boolean
)

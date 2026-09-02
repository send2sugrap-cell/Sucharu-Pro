package com.sucharu.sucharupro.data.observability.logging

/**
 * Log and diagnostic sanitizer preventing credential, token, and secret leakage (INFRA-05 Step 06).
 */
object LogSanitizer {

    private val SENSITIVE_KEY_PATTERN = Regex(
        "(?i)(authorization|bearer|password|pwd|secret|apikey|api_key|token|accesstoken|refreshtoken|clientsecret|webhooksecret|signingsecret|credential|private_key|privatekey)"
    )

    private val JWT_PATTERN = Regex("ey[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.?[A-Za-z0-9-_.+/=]*")
    private val BEARER_PATTERN = Regex("(?i)Bearer\\s+[A-Za-z0-9-_=.]+")
    private val KEY_VALUE_PATTERN = Regex("(?i)(\"?)(authorization|password|token|secret|apiKey|api_key|clientSecret|signingSecret)(\"?)\\s*[:=]\\s*\"?([^\"\\s,}]+)\"?")

    /**
     * Masks sensitive tokens or credentials in arbitrary text or error messages.
     */
    fun sanitize(message: String?): String {
        if (message.isNullOrBlank()) return ""

        var sanitized = message
        // 1. Mask Bearer headers
        sanitized = BEARER_PATTERN.replace(sanitized) { "Bearer [MASKED]" }

        // 2. Mask JWT tokens
        sanitized = JWT_PATTERN.replace(sanitized) { "jwt_****[MASKED]" }

        // 3. Mask Key-Value pairs like password="abc" or "apiKey": "123"
        sanitized = KEY_VALUE_PATTERN.replace(sanitized) { mr ->
            val p1 = mr.groups[1]?.value ?: ""
            val key = mr.groups[2]?.value ?: ""
            val p3 = mr.groups[3]?.value ?: ""
            "$p1$key$p3: \"[MASKED]\""
        }

        return sanitized
    }

    /**
     * Masks a known secret string into standard masked format.
     */
    fun maskSecret(secret: String?): String {
        if (secret.isNullOrBlank()) return "empty"
        if (secret.length <= 8) return "sec_****"
        val suffix = secret.takeLast(4)
        return "sec_****$suffix"
    }

    /**
     * Filters a map of parameters or headers, masking any sensitive keys.
     */
    fun sanitizeMap(data: Map<String, Any?>): Map<String, Any?> {
        return data.mapValues { (key, value) ->
            if (SENSITIVE_KEY_PATTERN.containsMatchIn(key)) {
                "[MASKED]"
            } else if (value is String) {
                sanitize(value)
            } else {
                value
            }
        }
    }
}

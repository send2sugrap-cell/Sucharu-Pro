package com.sucharu.sucharupro.data.observability.logging

import com.sucharu.sucharupro.data.notification.security.NotificationPayloadSanitizer
import com.sucharu.sucharupro.domain.observability.TraceContext
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Structured log entry with distributed tracing and zero-credential leakage.
 */
data class StructuredLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val projectId: String,
    val subsystem: String,
    val operation: String,
    val level: String,
    val message: String,
    val traceContext: TraceContext? = null,
    val durationMs: Long? = null,
    val failureClass: String? = null,
    val sanitizedMetadata: Map<String, String> = emptyMap()
)

/**
 * Production-grade Structured Observability Logger with Auto-Redaction (INFRA-04 Step 09).
 */
class StructuredObservabilityLogger {

    private val inMemoryLogs = CopyOnWriteArrayList<StructuredLogEntry>()

    companion object {
        private val JWT_PATTERN = Regex("[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}")
        private val API_KEY_PATTERN = Regex("((api[_-]?key|secret|token|password)\\s*=\\s*['\"]?)([A-Za-z0-9_\\-]{16,})(['\"]?)", RegexOption.IGNORE_CASE)

        fun redactSensitiveData(text: String): String {
            var sanitized = JWT_PATTERN.replace(text, "[REDACTED_JWT]")
            sanitized = API_KEY_PATTERN.replace(sanitized, "$1[REDACTED]$4")
            return NotificationPayloadSanitizer.sanitizeText(sanitized)
        }
    }

    fun log(
        projectId: String,
        subsystem: String,
        operation: String,
        level: String = "INFO",
        message: String,
        traceContext: TraceContext? = null,
        durationMs: Long? = null,
        failureClass: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): StructuredLogEntry {
        // Redact message and metadata
        val sanitizedMsg = redactSensitiveData(message)
        val sanitizedMeta = NotificationPayloadSanitizer.sanitizeMetadata(metadata)

        val entry = StructuredLogEntry(
            projectId = projectId,
            subsystem = subsystem,
            operation = operation,
            level = level,
            message = sanitizedMsg,
            traceContext = traceContext,
            durationMs = durationMs,
            failureClass = failureClass,
            sanitizedMetadata = sanitizedMeta
        )

        inMemoryLogs.add(entry)
        return entry
    }

    fun getLogsForProject(projectId: String, limit: Int = 100): List<StructuredLogEntry> {
        return inMemoryLogs.filter { it.projectId == projectId }.takeLast(limit)
    }

    fun getAllLogs(limit: Int = 100): List<StructuredLogEntry> = inMemoryLogs.takeLast(limit)

    fun clear() = inMemoryLogs.clear()
}

package com.sucharu.sucharupro.data.observability.logging

import com.sucharu.sucharupro.data.observability.model.ObservabilityLogLevel
import java.io.PrintStream
import java.time.Instant

/**
 * High-performance, JSON structured logger with zero-secret leakage guarantee (INFRA-05 Step 06).
 */
class ProductionStructuredLogger(
    private val serviceName: String = "sucharu-server",
    private val outputStream: PrintStream = System.out
) {

    fun info(
        component: String,
        event: String,
        correlationId: String? = null,
        durationMs: Long? = null,
        details: Map<String, Any?> = emptyMap()
    ) {
        log(ObservabilityLogLevel.INFO, component, event, correlationId, durationMs, details, null)
    }

    fun warn(
        component: String,
        event: String,
        correlationId: String? = null,
        durationMs: Long? = null,
        details: Map<String, Any?> = emptyMap(),
        error: Throwable? = null
    ) {
        log(ObservabilityLogLevel.WARN, component, event, correlationId, durationMs, details, error)
    }

    fun error(
        component: String,
        event: String,
        correlationId: String? = null,
        durationMs: Long? = null,
        details: Map<String, Any?> = emptyMap(),
        error: Throwable? = null
    ) {
        log(ObservabilityLogLevel.ERROR, component, event, correlationId, durationMs, details, error)
    }

    fun debug(
        component: String,
        event: String,
        correlationId: String? = null,
        details: Map<String, Any?> = emptyMap()
    ) {
        log(ObservabilityLogLevel.DEBUG, component, event, correlationId, null, details, null)
    }

    private fun log(
        level: ObservabilityLogLevel,
        component: String,
        event: String,
        correlationId: String?,
        durationMs: Long?,
        details: Map<String, Any?>,
        error: Throwable?
    ) {
        try {
            val sanitizedDetails = LogSanitizer.sanitizeMap(details)
            val errorMessage = error?.let { LogSanitizer.sanitize(it.message ?: it.javaClass.simpleName) }

            val json = buildString {
                append("{")
                append("\"timestamp\":\"${Instant.now()}\",")
                append("\"service\":\"$serviceName\",")
                append("\"level\":\"${level.name}\",")
                append("\"component\":\"$component\",")
                append("\"event\":\"$event\"")
                if (correlationId != null) {
                    append(",\"correlationId\":\"$correlationId\"")
                }
                if (durationMs != null) {
                    append(",\"durationMs\":$durationMs")
                }
                if (errorMessage != null) {
                    append(",\"error\":\"${escapeJson(errorMessage)}\"")
                }
                if (sanitizedDetails.isNotEmpty()) {
                    append(",\"details\":{")
                    val entries = sanitizedDetails.entries.toList()
                    for (i in entries.indices) {
                        val (k, v) = entries[i]
                        append("\"$k\":\"${escapeJson(v?.toString() ?: "null")}\"")
                        if (i < entries.size - 1) append(",")
                    }
                    append("}")
                }
                append("}")
            }
            outputStream.println(json)
        } catch (_: Exception) {
            // Fail-safe: Logging failure must never fail business runtime
        }
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

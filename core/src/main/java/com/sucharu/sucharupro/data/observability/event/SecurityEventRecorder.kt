package com.sucharu.sucharupro.data.observability.event

import com.sucharu.sucharupro.data.observability.logging.LogSanitizer
import com.sucharu.sucharupro.data.observability.metrics.ObservabilityMetricsRegistry
import com.sucharu.sucharupro.data.observability.model.SecurityEvent
import com.sucharu.sucharupro.data.observability.model.SecurityEventType
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Thread-safe, memory-bounded security telemetry recorder (INFRA-05 Step 06).
 */
class SecurityEventRecorder(
    private val metricsRegistry: ObservabilityMetricsRegistry? = null,
    private val maxRetainedEvents: Int = 500
) {

    private val eventBuffer = ConcurrentLinkedDeque<SecurityEvent>()

    fun recordEvent(
        eventType: SecurityEventType,
        correlationId: String,
        component: String,
        reasonCode: String,
        severity: String = "WARN",
        details: Map<String, String> = emptyMap()
    ): SecurityEvent {
        val sanitizedDetails = details.mapValues { (_, v) -> LogSanitizer.sanitize(v) }
        val event = SecurityEvent(
            eventType = eventType,
            correlationId = correlationId,
            component = component,
            reasonCode = reasonCode,
            severity = severity,
            details = sanitizedDetails
        )

        eventBuffer.addLast(event)
        while (eventBuffer.size > maxRetainedEvents) {
            eventBuffer.pollFirst()
        }

        // Mirror to metrics registry
        metricsRegistry?.increment("security_events_total", 1, mapOf("event_type" to eventType.name, "reason" to reasonCode.take(24)))

        when (eventType) {
            SecurityEventType.AUTHENTICATION_FAILED,
            SecurityEventType.INVALID_TOKEN,
            SecurityEventType.EXPIRED_TOKEN,
            SecurityEventType.INVALID_SIGNATURE -> {
                metricsRegistry?.recordAuthFailure(reasonCode)
            }
            SecurityEventType.AUTHORIZATION_DENIED -> {
                metricsRegistry?.recordAuthzDenied(
                    role = sanitizedDetails["role"] ?: "UNKNOWN",
                    capability = sanitizedDetails["capability"] ?: "UNKNOWN",
                    reason = reasonCode
                )
            }
            SecurityEventType.TENANT_SPOOF_ATTEMPT -> {
                metricsRegistry?.recordTenantBoundaryViolation(reasonCode)
            }
            else -> {
                // Additional specialized tracking if applicable
            }
        }

        return event
    }

    fun getRecentEvents(limit: Int = 100): List<SecurityEvent> {
        return eventBuffer.toList().takeLast(limit.coerceAtMost(maxRetainedEvents))
    }

    fun clear() {
        eventBuffer.clear()
    }
}

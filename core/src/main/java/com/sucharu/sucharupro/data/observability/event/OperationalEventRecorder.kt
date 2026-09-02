package com.sucharu.sucharupro.data.observability.event

import com.sucharu.sucharupro.data.observability.logging.LogSanitizer
import com.sucharu.sucharupro.data.observability.metrics.ObservabilityMetricsRegistry
import com.sucharu.sucharupro.data.observability.model.OperationalEvent
import com.sucharu.sucharupro.data.observability.model.OperationalEventType
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Thread-safe, memory-bounded operational event recorder (INFRA-05 Step 06).
 */
class OperationalEventRecorder(
    private val metricsRegistry: ObservabilityMetricsRegistry? = null,
    private val maxRetainedEvents: Int = 500
) {

    private val eventBuffer = ConcurrentLinkedDeque<OperationalEvent>()

    fun recordEvent(
        eventType: OperationalEventType,
        correlationId: String,
        component: String,
        summary: String,
        details: Map<String, String> = emptyMap()
    ): OperationalEvent {
        val sanitizedDetails = details.mapValues { (_, v) -> LogSanitizer.sanitize(v) }
        val event = OperationalEvent(
            eventType = eventType,
            correlationId = correlationId,
            component = component,
            summary = LogSanitizer.sanitize(summary),
            details = sanitizedDetails
        )

        eventBuffer.addLast(event)
        while (eventBuffer.size > maxRetainedEvents) {
            eventBuffer.pollFirst()
        }

        // Mirror to metrics registry
        metricsRegistry?.increment("operational_events_total", 1, mapOf("event_type" to eventType.name))

        return event
    }

    fun getRecentEvents(limit: Int = 100): List<OperationalEvent> {
        return eventBuffer.toList().takeLast(limit.coerceAtMost(maxRetainedEvents))
    }

    fun clear() {
        eventBuffer.clear()
    }
}

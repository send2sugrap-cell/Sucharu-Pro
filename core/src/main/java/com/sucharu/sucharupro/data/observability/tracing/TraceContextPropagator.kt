package com.sucharu.sucharupro.data.observability.tracing

import com.sucharu.sucharupro.domain.observability.TraceContext
import java.util.UUID

/**
 * Provider-neutral distributed trace context propagator (INFRA-04 Step 09).
 */
object TraceContextPropagator {

    const val HEADER_TRACE_ID = "X-Trace-Id"
    const val HEADER_SPAN_ID = "X-Span-Id"
    const val HEADER_CORRELATION_ID = "X-Correlation-Id"
    const val HEADER_CAUSATION_ID = "X-Causation-Id"
    const val HEADER_REQUEST_ID = "X-Request-Id"

    fun inject(context: TraceContext, carrier: MutableMap<String, String>) {
        carrier[HEADER_TRACE_ID] = context.traceId
        carrier[HEADER_SPAN_ID] = context.spanId
        carrier[HEADER_CORRELATION_ID] = context.correlationId
        context.causationId?.let { carrier[HEADER_CAUSATION_ID] = it }
        context.requestId?.let { carrier[HEADER_REQUEST_ID] = it }
    }

    fun extract(carrier: Map<String, String>): TraceContext {
        val traceId = carrier[HEADER_TRACE_ID] ?: "trc-${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val spanId = carrier[HEADER_SPAN_ID] ?: UUID.randomUUID().toString().replace("-", "").take(16)
        val correlationId = carrier[HEADER_CORRELATION_ID] ?: UUID.randomUUID().toString()
        val causationId = carrier[HEADER_CAUSATION_ID]
        val requestId = carrier[HEADER_REQUEST_ID]

        return TraceContext(
            traceId = traceId,
            spanId = spanId,
            correlationId = correlationId,
            causationId = causationId,
            requestId = requestId
        )
    }

    fun childContext(parent: TraceContext): TraceContext {
        return TraceContext(
            traceId = parent.traceId,
            spanId = UUID.randomUUID().toString().replace("-", "").take(16),
            correlationId = parent.correlationId,
            causationId = parent.spanId,
            requestId = parent.requestId
        )
    }
}

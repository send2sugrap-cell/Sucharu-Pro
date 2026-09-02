package com.sucharu.sucharupro.data.observability.correlation

/**
 * Immutable correlation and tracing context across request lifecycle, worker jobs, and integrations.
 */
data class CorrelationContext(
    val correlationId: String,
    val requestId: String = correlationId,
    val causationId: String? = null,
    val traceId: String? = null,
    val spanId: String? = null,
    val source: String = "HTTP"
) {
    companion object {
        fun create(correlationId: String, source: String = "HTTP"): CorrelationContext {
            return CorrelationContext(
                correlationId = correlationId,
                requestId = correlationId,
                source = source
            )
        }
    }
}

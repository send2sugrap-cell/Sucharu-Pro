package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.observability.tracing.TraceContextPropagator
import com.sucharu.sucharupro.domain.observability.TraceContext
import org.junit.Assert.*
import org.junit.Test

/**
 * Distributed Trace Context carrier injection and extraction test suite (INFRA-04 Step 09).
 */
class TraceContextPropagationTest {

    @Test
    fun test01_injectAndExtract_roundtrip() {
        val context = TraceContext.createNew("corr-trace-123", "req-trace-456")
        val carrier = mutableMapOf<String, String>()

        TraceContextPropagator.inject(context, carrier)
        assertEquals(context.traceId, carrier[TraceContextPropagator.HEADER_TRACE_ID])
        assertEquals(context.correlationId, carrier[TraceContextPropagator.HEADER_CORRELATION_ID])

        val extracted = TraceContextPropagator.extract(carrier)
        assertEquals(context.traceId, extracted.traceId)
        assertEquals(context.spanId, extracted.spanId)
        assertEquals(context.correlationId, extracted.correlationId)
        assertEquals(context.requestId, extracted.requestId)
    }

    @Test
    fun test02_childContext_inheritsTraceIdAndCorrelationId() {
        val parent = TraceContext.createNew("corr-parent-1")
        val child = TraceContextPropagator.childContext(parent)

        assertEquals("Child must inherit parent traceId", parent.traceId, child.traceId)
        assertEquals("Child must inherit parent correlationId", parent.correlationId, child.correlationId)
        assertEquals("Child causationId must be parent's spanId", parent.spanId, child.causationId)
        assertNotEquals("Child must have new spanId", parent.spanId, child.spanId)
    }
}

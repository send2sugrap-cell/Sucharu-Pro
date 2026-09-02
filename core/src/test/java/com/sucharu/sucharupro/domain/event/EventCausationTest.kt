package com.sucharu.sucharupro.domain.event

import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.EventTraceContext
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import com.sucharu.sucharupro.domain.event.model.events.PaymentReceivedEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class EventCausationTest {

    @Test
    fun test01_causationIdTracksPrecedingEventId() {
        val rootTrace = EventTraceContext(correlationId = "CORR-FLOW-1", causationId = null)
        val parentEvent = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1"),
            traceContext = rootTrace
        )

        assertNull(parentEvent.causationId)

        val childTrace = rootTrace.createChildContext(parentEvent.eventId)
        val childEvent = EventEnvelope.create(
            payload = PaymentReceivedEvent("PAY-1", "INV-1", "ORD-1", "CUST-1", BigDecimal("100"), "BDT", "CASH", "TRX-1"),
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1"),
            traceContext = childTrace
        )

        assertEquals("CORR-FLOW-1", childEvent.correlationId)
        assertEquals(parentEvent.eventId, childEvent.causationId)
    }
}

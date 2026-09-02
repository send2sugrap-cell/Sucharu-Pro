package com.sucharu.sucharupro.domain.event

import com.sucharu.sucharupro.data.event.fake.FakeEventStore
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.EventTraceContext
import com.sucharu.sucharupro.domain.event.model.events.DeliveryDispatchedEvent
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import com.sucharu.sucharupro.domain.event.model.events.PaymentReceivedEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class EventCorrelationTest {

    @Test
    fun test01_correlationIdSpansAcrossEntireBusinessWorkflow() = runBlocking {
        val store = FakeEventStore()
        val tenant = TenantContext("sucharu_main")
        val workflowCorrelationId = "WORKFLOW-CORR-999"

        val trace1 = EventTraceContext(correlationId = workflowCorrelationId, requestId = "REQ-01")
        val orderCreated = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("2000"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1"),
            traceContext = trace1
        )
        store.append(orderCreated, tenant)

        val trace2 = trace1.createChildContext(orderCreated.eventId)
        val paymentReceived = EventEnvelope.create(
            payload = PaymentReceivedEvent("PAY-1", "INV-1", "ORD-1", "CUST-1", BigDecimal("2000"), "BDT", "BKASH", "TRX1"),
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1"),
            traceContext = trace2
        )
        store.append(paymentReceived, tenant)

        val trace3 = trace2.createChildContext(paymentReceived.eventId)
        val deliveryDispatched = EventEnvelope.create(
            payload = DeliveryDispatchedEvent("DC-1", "ORD-1", "Steadfast Courier", "TRK-99", 2L),
            projectId = "sucharu_main",
            actor = EventActor.human("STAFF-1"),
            traceContext = trace3
        )
        store.append(deliveryDispatched, tenant)

        // Query by correlation ID returns all 3 events in order
        val events = store.getByCorrelationId(workflowCorrelationId, tenant)
        assertEquals(3, events.size)
        assertEquals(orderCreated.eventId, events[0].eventId)
        assertEquals(paymentReceived.eventId, events[1].eventId)
        assertEquals(deliveryDispatched.eventId, events[2].eventId)
    }
}

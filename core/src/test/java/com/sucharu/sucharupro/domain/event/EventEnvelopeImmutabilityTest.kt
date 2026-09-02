package com.sucharu.sucharupro.domain.event

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.EventTraceContext
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.math.BigDecimal

class EventEnvelopeImmutabilityTest {

    @Test
    fun test01_eventEnvelope_creation_preservesAllAuthoritativeFields() {
        val payload = OrderCreatedEvent(
            orderId = "ORD-1001",
            customerId = "CUST-001",
            totalAmount = BigDecimal("5000.00"),
            itemCount = 2,
            aggregateVersion = 1L
        )

        val actor = EventActor.human("USER-STAFF-1")
        val trace = EventTraceContext(
            correlationId = "CORR-XYZ",
            causationId = "CAUS-ABC",
            requestId = "REQ-123"
        )

        val envelope = EventEnvelope.create(
            payload = payload,
            projectId = "sucharu_main",
            actor = actor,
            traceContext = trace,
            source = "order-service",
            metadata = mapOf("channel" to "WEB", "priority" to "HIGH")
        )

        assertEquals("sucharu_main", envelope.projectId)
        assertEquals("ORDER", envelope.aggregateType)
        assertEquals("ORD-1001", envelope.aggregateId)
        assertEquals(1L, envelope.aggregateVersion)
        assertEquals("USER-STAFF-1", envelope.actorId)
        assertEquals(PrincipalType.HUMAN, envelope.actorType)
        assertEquals("CORR-XYZ", envelope.correlationId)
        assertEquals("CAUS-ABC", envelope.causationId)
        assertEquals("REQ-123", envelope.requestId)
        assertEquals("order-service", envelope.source)
        assertEquals("WEB", envelope.metadata["channel"])
        assertEquals("OrderCreated:v1", envelope.versionedEventType)
    }

    @Test
    fun test02_envelopeCopy_producesDistinctInstanceWithoutModifyingOriginal() {
        val payload = OrderCreatedEvent(
            orderId = "ORD-1001",
            customerId = "CUST-001",
            totalAmount = BigDecimal("5000.00"),
            itemCount = 2
        )
        val original = EventEnvelope.create(
            payload = payload,
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1")
        )

        val modified = original.copy(projectId = "sucharu_second")

        assertNotEquals(original.projectId, modified.projectId)
        assertEquals("sucharu_main", original.projectId)
        assertEquals("sucharu_second", modified.projectId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun test03_envelopeValidation_rejectsBlankProjectId() {
        val payload = OrderCreatedEvent(
            orderId = "ORD-1",
            customerId = "CUST-1",
            totalAmount = BigDecimal("100"),
            itemCount = 1
        )
        EventEnvelope.create(
            payload = payload,
            projectId = "  ",
            actor = EventActor.human("USER-1")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun test04_envelopeValidation_rejectsNegativeAggregateVersion() {
        val payload = OrderCreatedEvent(
            orderId = "ORD-1",
            customerId = "CUST-1",
            totalAmount = BigDecimal("100"),
            itemCount = 1
        )
        EventEnvelope.create(
            payload = payload,
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1"),
            aggregateVersion = -1L
        )
    }
}

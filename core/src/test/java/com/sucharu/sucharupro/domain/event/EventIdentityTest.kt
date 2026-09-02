package com.sucharu.sucharupro.domain.event

import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.UUID

class EventIdentityTest {

    @Test
    fun test01_eventIdentity_autoGeneratesValidUuidV4() {
        val payload = OrderCreatedEvent(
            orderId = "ORD-1001",
            customerId = "CUST-001",
            totalAmount = BigDecimal("1000"),
            itemCount = 1
        )
        val envelope = EventEnvelope.create(
            payload = payload,
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1")
        )

        assertNotNull(envelope.eventId)
        assertTrue(envelope.eventId.isNotBlank())
        // Validate UUID structure
        val parsedUuid = UUID.fromString(envelope.eventId)
        assertNotNull(parsedUuid)
    }

    @Test
    fun test02_distinctEvents_alwaysReceiveUniqueEventIds() {
        val payload = OrderCreatedEvent(
            orderId = "ORD-1001",
            customerId = "CUST-001",
            totalAmount = BigDecimal("1000"),
            itemCount = 1
        )

        val idSet = mutableSetOf<String>()
        val count = 1000
        for (i in 1..count) {
            val env = EventEnvelope.create(
                payload = payload,
                projectId = "sucharu_main",
                actor = EventActor.human("USER-1")
            )
            idSet.add(env.eventId)
        }

        assertEquals(count, idSet.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun test03_envelope_rejectsBlankEventId() {
        val payload = OrderCreatedEvent(
            orderId = "ORD-1",
            customerId = "CUST-1",
            totalAmount = BigDecimal("100"),
            itemCount = 1
        )
        EventEnvelope(
            eventId = "  ",
            eventType = payload.eventType,
            projectId = "sucharu_main",
            aggregateType = payload.aggregateType,
            aggregateId = payload.aggregateId,
            actorId = "USER-1",
            payload = payload
        )
    }
}

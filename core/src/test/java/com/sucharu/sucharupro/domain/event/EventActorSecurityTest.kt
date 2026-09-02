package com.sucharu.sucharupro.domain.event

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.math.BigDecimal

class EventActorSecurityTest {

    @Test
    fun test01_actorDistinguishesHumanAiAgentAndSystem() {
        val humanActor = EventActor.human("USER-123")
        val agentActor = EventActor.aiAgent("AGENT-OPS-1")
        val systemActor = EventActor.system("CRON-SCHEDULER")

        assertEquals(PrincipalType.HUMAN, humanActor.actorType)
        assertEquals("USER-123", humanActor.actorId)

        assertEquals(PrincipalType.AI_AGENT, agentActor.actorType)
        assertEquals("AGENT-OPS-1", agentActor.actorId)

        assertEquals(PrincipalType.SYSTEM, systemActor.actorType)
        assertEquals("CRON-SCHEDULER", systemActor.actorId)
    }

    @Test
    fun test02_envelopePreservesActorProperties() {
        val payload = OrderCreatedEvent(
            orderId = "ORD-1",
            customerId = "CUST-1",
            totalAmount = BigDecimal("100"),
            itemCount = 1
        )
        val actor = EventActor.aiAgent("AGENT-ORDER-CREATOR")
        val envelope = EventEnvelope.create(
            payload = payload,
            projectId = "sucharu_main",
            actor = actor
        )

        assertEquals("AGENT-ORDER-CREATOR", envelope.actorId)
        assertEquals(PrincipalType.AI_AGENT, envelope.actorType)
        assertEquals(PrincipalType.AI_AGENT, envelope.principalType)
    }

    @Test(expected = IllegalArgumentException::class)
    fun test03_actor_rejectsBlankActorId() {
        EventActor(actorId = "   ")
    }
}

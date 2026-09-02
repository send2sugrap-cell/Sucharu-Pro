package com.sucharu.sucharupro.domain.event

import com.sucharu.sucharupro.domain.event.boundary.N8nIntegrationBoundary
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.AuthenticationFailedEvent
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.math.BigDecimal

class N8nIntegrationBoundaryTest {

    @Test
    fun test01_n8nBoundary_exportsSanitizedPayloadForBusinessEvents() {
        val payload = OrderCreatedEvent(
            orderId = "ORD-1001",
            customerId = "CUST-001",
            totalAmount = BigDecimal("3500.00"),
            itemCount = 4
        )

        val envelope = EventEnvelope.create(
            payload = payload,
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1"),
            metadata = mapOf("clientChannel" to "MOBILE_APP", "internalSecretToken" to "SHOULD_BE_STRIPPED")
        )

        val webhook = N8nIntegrationBoundary.toSanitizedWebhookPayload(envelope)
        assertEquals(envelope.eventId, webhook.eventId)
        assertEquals("OrderCreated", webhook.eventType)
        assertEquals("v1", webhook.eventVersion)
        assertEquals("sucharu_main", webhook.projectId)
        assertEquals("ORDER", webhook.aggregateType)
        assertEquals("ORD-1001", webhook.aggregateId)

        // Verifies secret token was stripped
        assertEquals("MOBILE_APP", webhook.payloadSummary["clientChannel"])
        assertEquals(null, webhook.payloadSummary["internalSecretToken"])
    }

    @Test(expected = IllegalArgumentException::class)
    fun test02_n8nBoundary_strictlyBlocksSecurityEvents() {
        val secPayload = AuthenticationFailedEvent(
            attemptedIdentifierMasked = "bad_user",
            failureReason = "INVALID_CREDENTIALS"
        )
        val secEnvelope = EventEnvelope.create(
            payload = secPayload,
            projectId = "sucharu_main",
            actor = EventActor.system()
        )

        N8nIntegrationBoundary.toSanitizedWebhookPayload(secEnvelope)
    }
}

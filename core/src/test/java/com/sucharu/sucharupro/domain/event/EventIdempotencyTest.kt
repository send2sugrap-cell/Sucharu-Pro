package com.sucharu.sucharupro.domain.event

import com.sucharu.sucharupro.data.event.fake.FakeDomainEventConsumer
import com.sucharu.sucharupro.data.event.fake.FakeIdempotencyStore
import com.sucharu.sucharupro.domain.event.dispatcher.DomainEventDispatcher
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class EventIdempotencyTest {

    @Test
    fun test01_duplicateDelivery_isDetectedAndHandledWithoutDuplicateSideEffects() = runBlocking {
        val idempotencyStore = FakeIdempotencyStore()
        val dispatcher = DomainEventDispatcher(idempotencyStore)

        val consumer = FakeDomainEventConsumer<OrderCreatedEvent>(
            consumerId = "EmailNotificationConsumer",
            supportedEventType = DomainEventType.ORDER_CREATED
        )
        dispatcher.registerConsumer(consumer)

        val envelope = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("500"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1")
        )

        // First dispatch: success
        val summary1 = dispatcher.dispatch(envelope)
        assertEquals(1, summary1.successCount)
        assertEquals(0, summary1.skippedCount)
        assertEquals(1, consumer.consumedEnvelopes.size)

        // Verify idempotency store recorded the event
        val isProcessed = idempotencyStore.isProcessed(envelope.eventId, consumer.consumerId, envelope.projectId)
        assertTrue(isProcessed)

        // Second dispatch with same event: skipped due to idempotency
        val summary2 = dispatcher.dispatch(envelope)
        assertEquals(0, summary2.successCount)
        assertEquals(1, summary2.skippedCount)
        // Consumer should not have received the duplicate envelope
        assertEquals(1, consumer.consumedEnvelopes.size)
    }
}

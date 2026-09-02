package com.sucharu.sucharupro.domain.event

import com.sucharu.sucharupro.data.event.fake.FakeDomainEventConsumer
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.dispatcher.DomainEventDispatcher
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class EventConsumerTest {

    @Test
    fun test01_consumerReceivesTypedEventAndReturnsSuccess() = runBlocking {
        val dispatcher = DomainEventDispatcher()
        val consumer = FakeDomainEventConsumer<OrderCreatedEvent>(
            consumerId = "InvoiceGeneratorConsumer",
            supportedEventType = DomainEventType.ORDER_CREATED
        )
        dispatcher.registerConsumer(consumer)

        val env = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-100", "CUST-1", BigDecimal("1500"), 2),
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1")
        )

        val summary = dispatcher.dispatch(env)
        assertTrue(summary.isFullySuccessful)
        assertEquals(1, consumer.consumedEnvelopes.size)
        assertEquals("ORD-100", consumer.consumedEnvelopes[0].payload.orderId)
    }

    @Test
    fun test02_consumerReturnsStructuredFailureWhenConfigured() = runBlocking {
        val dispatcher = DomainEventDispatcher()
        val failingConsumer = FakeDomainEventConsumer<OrderCreatedEvent>(
            consumerId = "FailingConsumer",
            supportedEventType = DomainEventType.ORDER_CREATED,
            configuredResult = EventConsumerResult.Failure(
                reason = "Database connection pool exhausted",
                classification = EventFailureClassification.TRANSIENT
            )
        )
        dispatcher.registerConsumer(failingConsumer)

        val env = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-100", "CUST-1", BigDecimal("1500"), 2),
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1")
        )

        val summary = dispatcher.dispatch(env)
        assertFalse(summary.isFullySuccessful)
        assertEquals(1, summary.failureCount)

        val result = summary.consumerResults["FailingConsumer"] as EventConsumerResult.Failure
        assertEquals(EventFailureClassification.TRANSIENT, result.classification)
        assertTrue(result.isRetryable)
    }
}

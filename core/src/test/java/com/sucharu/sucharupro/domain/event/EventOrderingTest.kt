package com.sucharu.sucharupro.domain.event

import com.sucharu.sucharupro.data.event.fake.FakeDomainEventConsumer
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.dispatcher.DomainEventDispatcher
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import com.sucharu.sucharupro.domain.event.model.events.OrderUpdatedEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class EventOrderingTest {

    @Test
    fun test01_monotonicAggregateVersion_progressesSuccessfully() = runBlocking {
        val dispatcher = DomainEventDispatcher()
        val consumer = FakeDomainEventConsumer<OrderUpdatedEvent>(
            consumerId = "OrderUpdateConsumer",
            supportedEventType = DomainEventType.ORDER_UPDATED
        )
        dispatcher.registerConsumer(consumer)

        val envV1 = EventEnvelope.create(
            payload = OrderUpdatedEvent("ORD-1", "CUST-1", BigDecimal("200"), "Update 1", 1L),
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1"),
            aggregateVersion = 1L
        )
        val envV2 = EventEnvelope.create(
            payload = OrderUpdatedEvent("ORD-1", "CUST-1", BigDecimal("300"), "Update 2", 2L),
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1"),
            aggregateVersion = 2L
        )

        val res1 = dispatcher.dispatch(envV1)
        assertTrue(res1.isFullySuccessful)

        val res2 = dispatcher.dispatch(envV2)
        assertTrue(res2.isFullySuccessful)

        assertEquals(2, consumer.consumedEnvelopes.size)
    }

    @Test
    fun test02_staleAggregateVersion_isDetectedAndRejected() = runBlocking {
        val dispatcher = DomainEventDispatcher()
        val consumer = FakeDomainEventConsumer<OrderUpdatedEvent>(
            consumerId = "OrderUpdateConsumer",
            supportedEventType = DomainEventType.ORDER_UPDATED
        )
        dispatcher.registerConsumer(consumer)

        val envV2 = EventEnvelope.create(
            payload = OrderUpdatedEvent("ORD-1", "CUST-1", BigDecimal("300"), "Update 2", 2L),
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1"),
            aggregateVersion = 2L
        )
        val envV1Stale = EventEnvelope.create(
            payload = OrderUpdatedEvent("ORD-1", "CUST-1", BigDecimal("200"), "Update 1", 1L),
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1"),
            aggregateVersion = 1L
        )

        // Dispatch version 2 first
        val res1 = dispatcher.dispatch(envV2)
        assertTrue(res1.isFullySuccessful)

        // Then dispatch stale version 1 for same aggregate
        val res2 = dispatcher.dispatch(envV1Stale)
        assertFalse(res2.isFullySuccessful)
        assertEquals(1, res2.failureCount)

        val failure = res2.consumerResults["STREAM_ORDERING_VALIDATOR"] as EventConsumerResult.Failure
        assertEquals(EventFailureClassification.STALE_VERSION, failure.classification)
        assertFalse(failure.isRetryable)
    }
}

package com.sucharu.sucharupro.domain.event

import com.sucharu.sucharupro.data.event.fake.FakeDomainEventConsumer
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

class EventVersioningTest {

    @Test
    fun test01_eventVersion_explicitlyExposesSchemaVersion() {
        val payload = OrderCreatedEvent(
            orderId = "ORD-1001",
            customerId = "CUST-001",
            totalAmount = BigDecimal("500"),
            itemCount = 1
        )
        val envelope = EventEnvelope.create(
            payload = payload,
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1")
        )

        assertEquals("v1", envelope.eventVersion)
        assertEquals("OrderCreated:v1", envelope.versionedEventType)
    }

    @Test
    fun test02_dispatcherRoutesMatchingVersionToConsumer() = runBlocking {
        val dispatcher = DomainEventDispatcher()
        val v1Consumer = FakeDomainEventConsumer<OrderCreatedEvent>(
            consumerId = "OrderV1Consumer",
            supportedEventType = DomainEventType.ORDER_CREATED,
            supportedVersion = "v1"
        )
        dispatcher.registerConsumer(v1Consumer)

        val payload = OrderCreatedEvent(
            orderId = "ORD-1",
            customerId = "CUST-1",
            totalAmount = BigDecimal("100"),
            itemCount = 1
        )
        val envV1 = EventEnvelope.create(
            payload = payload,
            projectId = "sucharu_main",
            actor = EventActor.human("U1")
        )

        val summary = dispatcher.dispatch(envV1)
        assertEquals(1, summary.totalConsumersMatched)
        assertEquals(1, summary.successCount)
        assertEquals(1, v1Consumer.consumedEnvelopes.size)
    }

    @Test
    fun test03_dispatcherIgnoresConsumerWithMismatchedVersion() = runBlocking {
        val dispatcher = DomainEventDispatcher()
        val v2Consumer = FakeDomainEventConsumer<OrderCreatedEvent>(
            consumerId = "OrderV2Consumer",
            supportedEventType = DomainEventType.ORDER_CREATED,
            supportedVersion = "v2"
        )
        dispatcher.registerConsumer(v2Consumer)

        val payload = OrderCreatedEvent(
            orderId = "ORD-1",
            customerId = "CUST-1",
            totalAmount = BigDecimal("100"),
            itemCount = 1
        )
        val envV1 = EventEnvelope.create(
            payload = payload,
            projectId = "sucharu_main",
            actor = EventActor.human("U1")
        )

        val summary = dispatcher.dispatch(envV1)
        assertEquals(0, summary.totalConsumersMatched)
        assertEquals(0, v2Consumer.consumedEnvelopes.size)
    }
}

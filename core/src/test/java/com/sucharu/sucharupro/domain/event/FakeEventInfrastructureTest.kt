package com.sucharu.sucharupro.domain.event

import com.sucharu.sucharupro.data.event.fake.FakeDomainEventPublisher
import com.sucharu.sucharupro.data.event.fake.FakeIdempotencyStore
import com.sucharu.sucharupro.data.event.fake.FakeTransactionalOutboxStore
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.dispatcher.DomainEventDispatcher
import com.sucharu.sucharupro.domain.event.idempotency.EventProcessingRecord
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import com.sucharu.sucharupro.domain.event.store.OutboxEventRecord
import com.sucharu.sucharupro.domain.event.store.OutboxStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class FakeEventInfrastructureTest {

    @Test
    fun test01_fakePublisher_recordsPublishedEnvelopes() = runBlocking {
        val dispatcher = DomainEventDispatcher()
        val publisher = FakeDomainEventPublisher(dispatcher)

        val env = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("500"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("U1")
        )

        val result = publisher.publish(env)
        assertTrue(result.isSuccess)
        assertEquals(1, publisher.publishedEvents.size)
        assertEquals(env.eventId, publisher.publishedEvents[0].eventId)
    }

    @Test
    fun test02_fakeTransactionalOutbox_lifecycle() = runBlocking {
        val outbox = FakeTransactionalOutboxStore()
        val tenant = TenantContext("sucharu_main")

        val record = OutboxEventRecord(
            outboxId = "OUT-1",
            eventId = "EVT-1",
            eventType = DomainEventType.ORDER_CREATED,
            projectId = "sucharu_main",
            aggregateType = "ORDER",
            aggregateId = "ORD-1",
            aggregateVersion = 1L,
            payloadJson = "{\"orderId\":\"ORD-1\"}"
        )

        outbox.appendOutboxRecord(record, tenant)

        val pending = outbox.getPendingRecords(10, tenant)
        assertEquals(1, pending.size)
        assertEquals(OutboxStatus.PENDING, pending[0].status)

        outbox.markPublished("OUT-1", tenant)
        val pendingAfter = outbox.getPendingRecords(10, tenant)
        assertEquals(0, pendingAfter.size)
    }

    @Test
    fun test03_fakeIdempotencyStore_tracksProcessing() = runBlocking {
        val store = FakeIdempotencyStore()

        val record = EventProcessingRecord(
            eventId = "EVT-100",
            consumerId = "TestConsumer",
            projectId = "sucharu_main"
        )
        store.recordProcessing(record)

        assertTrue(store.isProcessed("EVT-100", "TestConsumer", "sucharu_main"))
        val fetched = store.getRecord("EVT-100", "TestConsumer", "sucharu_main")
        assertNotNull(fetched)
    }
}

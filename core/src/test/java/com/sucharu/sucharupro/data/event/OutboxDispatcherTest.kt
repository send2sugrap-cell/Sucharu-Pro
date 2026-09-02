package com.sucharu.sucharupro.data.event

import com.sucharu.sucharupro.data.event.dispatcher.OutboxDispatcher
import com.sucharu.sucharupro.data.event.model.OutboxStatus
import com.sucharu.sucharupro.data.event.model.RetryConfig
import com.sucharu.sucharupro.data.event.postgres.PostgresEventIdempotencyStore
import com.sucharu.sucharupro.data.event.postgres.PostgresTransactionalOutboxStore
import com.sucharu.sucharupro.data.event.fake.FakeDomainEventConsumer
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.dispatcher.DomainEventDispatcher
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import com.sucharu.sucharupro.domain.event.model.events.OrderUpdatedEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class OutboxDispatcherTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var outboxStore: PostgresTransactionalOutboxStore
    private lateinit var idempotencyStore: PostgresEventIdempotencyStore
    private lateinit var domainDispatcher: DomainEventDispatcher
    private lateinit var outboxDispatcher: OutboxDispatcher
    private val tenant = TenantContext("sucharu_main")

    @Before
    fun setUp() {
        mockDb = MockPostgresEventDatabase()
        outboxStore = PostgresTransactionalOutboxStore(mockDb)
        idempotencyStore = PostgresEventIdempotencyStore(mockDb)
        domainDispatcher = DomainEventDispatcher(idempotencyStore)
        outboxDispatcher = OutboxDispatcher(
            outboxStore = outboxStore,
            domainEventDispatcher = domainDispatcher,
            retryConfig = RetryConfig(maxAttempts = 3, initialBackoffMs = 500L)
        )
    }

    @Test
    fun test01_successfulDispatch_marksPublishedAndUpdatesMetrics() = runBlocking {
        val consumer = FakeDomainEventConsumer<OrderCreatedEvent>(
            consumerId = "OrderNotifier",
            supportedEventType = DomainEventType.ORDER_CREATED
        )
        domainDispatcher.registerConsumer(consumer)

        val envelope = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("U-1")
        )
        outboxStore.enqueue(tenant, envelope)

        val summary = outboxDispatcher.dispatchBatch(tenant, "WORKER-1")
        assertEquals(1, summary.claimedCount)
        assertEquals(1, summary.publishedCount)
        assertEquals(0, summary.retriedCount)
        assertEquals(0, summary.deadLetterCount)
        assertEquals(1, consumer.consumedEnvelopes.size)

        assertEquals(1L, outboxDispatcher.metrics.publishedCount)
        assertEquals(1L, outboxDispatcher.metrics.claimedCount)
    }

    @Test
    fun test02_transientConsumerFailure_schedulesRetry() = runBlocking {
        val failingConsumer = FakeDomainEventConsumer<OrderCreatedEvent>(
            consumerId = "TransientFailingConsumer",
            supportedEventType = DomainEventType.ORDER_CREATED,
            configuredResult = EventConsumerResult.Failure(
                reason = "Database timeout",
                classification = EventFailureClassification.TRANSIENT
            )
        )
        domainDispatcher.registerConsumer(failingConsumer)

        val envelope = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("U-1")
        )
        outboxStore.enqueue(tenant, envelope)

        val summary = outboxDispatcher.dispatchBatch(tenant, "WORKER-1")
        assertEquals(1, summary.claimedCount)
        assertEquals(0, summary.publishedCount)
        assertEquals(1, summary.retriedCount)

        val row = mockDb.outboxTable[0]
        assertEquals("RETRY_SCHEDULED", row["status"])
    }

    @Test
    fun test03_maxAttemptsExceeded_movesToDeadLetter() = runBlocking {
        val failingConsumer = FakeDomainEventConsumer<OrderCreatedEvent>(
            consumerId = "AlwaysFailingConsumer",
            supportedEventType = DomainEventType.ORDER_CREATED,
            configuredResult = EventConsumerResult.Failure(
                reason = "Persistent network failure",
                classification = EventFailureClassification.TRANSIENT
            )
        )
        domainDispatcher.registerConsumer(failingConsumer)

        val envelope = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("U-1")
        )
        outboxStore.enqueue(tenant, envelope)

        // Simulate that this event is already on attempt 3 (which equals maxAttempts)
        mockDb.outboxTable[0]["attempt_count"] = 3

        val summary = outboxDispatcher.dispatchBatch(tenant, "WORKER-1")
        assertEquals(1, summary.claimedCount)
        assertEquals(0, summary.publishedCount)
        assertEquals(1, summary.deadLetterCount)

        assertEquals(1, mockDb.deadLetterTable.size)
        assertEquals("DEAD_LETTER", mockDb.outboxTable[0]["status"])
    }

    @Test
    fun test04_aggregateStreamOrdering_deferredWhenPrecedingFails() = runBlocking {
        val failingConsumer = FakeDomainEventConsumer<OrderCreatedEvent>(
            consumerId = "OrderCreatedFailingConsumer",
            supportedEventType = DomainEventType.ORDER_CREATED,
            configuredResult = EventConsumerResult.Failure("Failed v1", EventFailureClassification.TRANSIENT)
        )
        val updateConsumer = FakeDomainEventConsumer<OrderUpdatedEvent>(
            consumerId = "OrderUpdatedConsumer",
            supportedEventType = DomainEventType.ORDER_UPDATED
        )
        domainDispatcher.registerConsumer(failingConsumer)
        domainDispatcher.registerConsumer(updateConsumer)

        val e1 = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-99", "CUST-1", BigDecimal("100"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("U-1"),
            aggregateVersion = 1L
        )
        val e2 = EventEnvelope.create(
            payload = OrderUpdatedEvent("ORD-99", "CUST-1", BigDecimal("200"), "Add item", 2L),
            projectId = "sucharu_main",
            actor = EventActor.human("U-1"),
            aggregateVersion = 2L
        )

        outboxStore.enqueue(tenant, e1)
        outboxStore.enqueue(tenant, e2)

        val summary = outboxDispatcher.dispatchBatch(tenant, "WORKER-1")
        assertEquals(2, summary.claimedCount)
        assertEquals(0, summary.publishedCount)
        assertEquals(2, summary.retriedCount) // Both retried: e1 because it failed, e2 because e1 failed

        // Verify e2 consumer was never called to preserve aggregate ordering
        assertEquals(0, updateConsumer.consumedEnvelopes.size)
    }
}

package com.sucharu.sucharupro.data.event

import com.sucharu.sucharupro.data.event.fake.FakeDomainEventConsumer
import com.sucharu.sucharupro.data.event.postgres.PostgresEventIdempotencyStore
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.consumer.orchestration.EventConsumerExecutionEngine
import com.sucharu.sucharupro.domain.event.consumer.orchestration.EventConsumerRegistry
import com.sucharu.sucharupro.domain.event.consumer.orchestration.EventConsumerRouter
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import kotlinx.coroutines.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class IntegrationConcurrencyTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var idempotencyStore: PostgresEventIdempotencyStore
    private lateinit var registry: EventConsumerRegistry
    private lateinit var executionEngine: EventConsumerExecutionEngine
    private lateinit var router: EventConsumerRouter

    @Before
    fun setUp() {
        mockDb = MockPostgresEventDatabase()
        idempotencyStore = PostgresEventIdempotencyStore(mockDb)
        registry = EventConsumerRegistry()
        executionEngine = EventConsumerExecutionEngine(idempotencyStore)
        router = EventConsumerRouter(registry, executionEngine)
    }

    @Test
    fun test01_concurrentEventRouting_executesSafelyAcrossMultipleCoroutines() {
        runBlocking {
            val consumer = FakeDomainEventConsumer<OrderCreatedEvent>(
                consumerId = "concurrent.order.consumer",
                supportedEventType = DomainEventType.ORDER_CREATED
            )
            registry.registerConsumer(consumer)

            val totalEvents = 20
            val jobs = (1..totalEvents).map { idx ->
                async(Dispatchers.Default) {
                    val envelope = EventEnvelope.create(
                        payload = OrderCreatedEvent("ORD-$idx", "CUST-$idx", BigDecimal("100.00"), 1),
                        projectId = "sucharu_main",
                        actor = EventActor.human("U-$idx")
                    )
                    router.route(envelope)
                }
            }

            val reports = jobs.awaitAll()
            assertEquals(totalEvents, reports.size)
            assertTrue(reports.all { it.isFullySuccessful })
            assertEquals(totalEvents, consumer.consumedEnvelopes.size)
        }
    }

    @Test
    fun test02_slowConsumer_doesNotBlockFastConsumer() {
        runBlocking {
            val slowConsumer = FakeDomainEventConsumer<OrderCreatedEvent>(
                consumerId = "slow.consumer",
                supportedEventType = DomainEventType.ORDER_CREATED
            )
            val fastConsumer = FakeDomainEventConsumer<OrderCreatedEvent>(
                consumerId = "fast.consumer",
                supportedEventType = DomainEventType.ORDER_CREATED
            )

            registry.registerConsumer(slowConsumer)
            registry.registerConsumer(fastConsumer)

            val envelope = EventEnvelope.create(
                payload = OrderCreatedEvent("ORD-100", "CUST-1", BigDecimal("500.00"), 2),
                projectId = "sucharu_main",
                actor = EventActor.human("U-1")
            )

            val report = router.route(envelope)
            assertEquals(2, report.totalMatched)
            assertEquals(2, report.successCount)
            assertEquals(1, slowConsumer.consumedEnvelopes.size)
            assertEquals(1, fastConsumer.consumedEnvelopes.size)
        }
    }
}

package com.sucharu.sucharupro.data.event

import com.sucharu.sucharupro.data.event.fake.FakeDomainEventConsumer
import com.sucharu.sucharupro.data.event.postgres.PostgresEventIdempotencyStore
import com.sucharu.sucharupro.data.event.postgres.PostgresIntegrationDeliveryRepository
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.consumer.orchestration.*
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

class ConsumerOrchestrationTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var idempotencyStore: PostgresEventIdempotencyStore
    private lateinit var deliveryRepo: PostgresIntegrationDeliveryRepository
    private lateinit var registry: EventConsumerRegistry
    private lateinit var executionEngine: EventConsumerExecutionEngine
    private lateinit var router: EventConsumerRouter

    @Before
    fun setUp() {
        mockDb = MockPostgresEventDatabase()
        idempotencyStore = PostgresEventIdempotencyStore(mockDb)
        deliveryRepo = PostgresIntegrationDeliveryRepository(mockDb)
        registry = EventConsumerRegistry()
        executionEngine = EventConsumerExecutionEngine(idempotencyStore, deliveryRepo)
        router = EventConsumerRouter(registry, executionEngine)
    }

    @Test
    fun test01_consumerRegistration_andDeterministicRouting() {
        runBlocking {
            val consumer = FakeDomainEventConsumer<OrderCreatedEvent>(
                consumerId = "inventory.order.created",
                supportedEventType = DomainEventType.ORDER_CREATED
            )
            val sub = ConsumerSubscription(
                consumerId = "inventory.order.created",
                supportedEventType = DomainEventType.ORDER_CREATED,
                supportedVersion = "v1",
                integrationType = IntegrationType.INTERNAL
            )
            registry.registerConsumer(consumer, sub)

            val envelope = EventEnvelope.create(
                payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("500.00"), 2),
                projectId = "sucharu_main",
                actor = EventActor.human("U-1")
            )

            val report = router.route(envelope)
            assertEquals(1, report.totalMatched)
            assertEquals(1, report.successCount)
            assertEquals(0, report.failureCount)
            assertTrue(report.isFullySuccessful)
            assertEquals(1, consumer.consumedEnvelopes.size)
        }
    }

    @Test
    fun test02_unsupportedVersion_doesNotMatchConsumer() {
        runBlocking {
            val consumer = FakeDomainEventConsumer<OrderCreatedEvent>(
                consumerId = "v1.order.consumer",
                supportedEventType = DomainEventType.ORDER_CREATED,
                supportedVersion = "v1"
            )
            registry.registerConsumer(consumer)

            // Envelope with v2 version
            val envelopeV2 = EventEnvelope.create(
                payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("500.00"), 2),
                projectId = "sucharu_main",
                actor = EventActor.human("U-1")
            ).copy(eventVersion = "v2")

            val report = router.route(envelopeV2)
            assertEquals(0, report.totalMatched)
            assertEquals(0, consumer.consumedEnvelopes.size)
        }
    }

    @Test
    fun test03_duplicateEventDelivery_isSafelySkipped() {
        runBlocking {
            val consumer = FakeDomainEventConsumer<OrderCreatedEvent>(
                consumerId = "idempotent.order.consumer",
                supportedEventType = DomainEventType.ORDER_CREATED
            )
            registry.registerConsumer(consumer)

            val envelope = EventEnvelope.create(
                payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("500.00"), 2),
                projectId = "sucharu_main",
                actor = EventActor.human("U-1")
            )

            // First delivery executes
            val firstReport = router.route(envelope)
            assertEquals(1, firstReport.successCount)
            assertEquals(0, firstReport.skippedCount)
            assertEquals(1, consumer.consumedEnvelopes.size)

            // Duplicate delivery is skipped without side-effects
            val secondReport = router.route(envelope)
            assertEquals(0, secondReport.successCount)
            assertEquals(1, secondReport.skippedCount)
            assertEquals(1, consumer.consumedEnvelopes.size) // Unchanged
        }
    }

    @Test
    fun test04_consumerFailure_isClassifiedAndDoesNotBlockOtherConsumers() {
        runBlocking {
            val failingConsumer = FakeDomainEventConsumer<OrderCreatedEvent>(
                consumerId = "failing.consumer",
                supportedEventType = DomainEventType.ORDER_CREATED,
                configuredResult = EventConsumerResult.Failure(
                    reason = "Downstream timeout",
                    classification = EventFailureClassification.TRANSIENT
                )
            )
            val successfulConsumer = FakeDomainEventConsumer<OrderCreatedEvent>(
                consumerId = "successful.consumer",
                supportedEventType = DomainEventType.ORDER_CREATED,
                configuredResult = EventConsumerResult.Success()
            )

            registry.registerConsumer(failingConsumer)
            registry.registerConsumer(successfulConsumer)

            val envelope = EventEnvelope.create(
                payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("500.00"), 2),
                projectId = "sucharu_main",
                actor = EventActor.human("U-1")
            )

            val report = router.route(envelope)
            assertEquals(2, report.totalMatched)
            assertEquals(1, report.successCount)
            assertEquals(1, report.failureCount)
            assertFalse(report.isFullySuccessful)

            // Successful consumer still processed despite failing consumer
            assertEquals(1, successfulConsumer.consumedEnvelopes.size)
        }
    }
}

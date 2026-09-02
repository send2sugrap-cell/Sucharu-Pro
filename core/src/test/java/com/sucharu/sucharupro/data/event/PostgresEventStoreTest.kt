package com.sucharu.sucharupro.data.event

import com.sucharu.sucharupro.data.event.postgres.PostgresEventStore
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.EventTraceContext
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import com.sucharu.sucharupro.domain.event.model.events.OrderUpdatedEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class PostgresEventStoreTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var eventStore: PostgresEventStore
    private val tenantA = TenantContext("tenant_alpha")
    private val tenantB = TenantContext("tenant_beta")

    @Before
    fun setUp() {
        mockDb = MockPostgresEventDatabase()
        eventStore = PostgresEventStore(mockDb)
    }

    @Test
    fun test01_appendAndRetrieveLifecycle() {
        runBlocking {
            val payload = OrderCreatedEvent("ORD-101", "CUST-1", BigDecimal("500.00"), 2, "BDT", 1L)
            val envelope = EventEnvelope.create(
                payload = payload,
                projectId = "tenant_alpha",
                actor = EventActor.human("U-100"),
                traceContext = EventTraceContext(correlationId = "CORR-001", causationId = "PARENT-1")
            )

            eventStore.append(envelope, tenantA)

            val fetched = eventStore.getById(envelope.eventId, tenantA)
            assertNotNull(fetched)
            assertEquals(envelope.eventId, fetched?.eventId)
            assertEquals(DomainEventType.ORDER_CREATED, fetched?.eventType)
            assertEquals("tenant_alpha", fetched?.projectId)
            assertEquals("ORD-101", fetched?.aggregateId)
            assertEquals("CORR-001", fetched?.correlationId)
            assertEquals("PARENT-1", fetched?.causationId)

            val typedPayload = fetched?.payload as OrderCreatedEvent
            assertEquals("ORD-101", typedPayload.orderId)
            assertEquals(BigDecimal("500.00"), typedPayload.totalAmount)
        }
    }

    @Test
    fun test02_tenantIsolation_tenantACannotAccessTenantBEvents() {
        runBlocking {
            val payload = OrderCreatedEvent("ORD-101", "CUST-1", BigDecimal("500.00"), 2, "BDT", 1L)
            val envelopeA = EventEnvelope.create(
                payload = payload,
                projectId = "tenant_alpha",
                actor = EventActor.human("U-100")
            )

            eventStore.append(envelopeA, tenantA)

            // Tenant A sees the event
            val fetchedByA = eventStore.getById(envelopeA.eventId, tenantA)
            assertNotNull(fetchedByA)

            // Tenant B cannot see the event
            val fetchedByB = eventStore.getById(envelopeA.eventId, tenantB)
            assertNull(fetchedByB)
        }
    }

    @Test
    fun test03_appendCrossTenant_throwsException() {
        runBlocking {
            val payload = OrderCreatedEvent("ORD-101", "CUST-1", BigDecimal("500.00"), 2, "BDT", 1L)
            val envelopeA = EventEnvelope.create(
                payload = payload,
                projectId = "tenant_alpha",
                actor = EventActor.human("U-100")
            )

            // Attempting to append Tenant A's envelope into Tenant B context must fail
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    eventStore.append(envelopeA, tenantB)
                }
            }
        }
    }

    @Test
    fun test04_aggregateStreamRetrieval_orderedByVersion() {
        runBlocking {
            val e1 = EventEnvelope.create(
                payload = OrderCreatedEvent("ORD-101", "CUST-1", BigDecimal("500.00"), 2, "BDT", 1L),
                projectId = "tenant_alpha",
                actor = EventActor.human("U-100"),
                aggregateVersion = 1L
            )
            val e2 = EventEnvelope.create(
                payload = OrderUpdatedEvent("ORD-101", "CUST-1", BigDecimal("700.00"), "Add item", 2L),
                projectId = "tenant_alpha",
                actor = EventActor.human("U-100"),
                aggregateVersion = 2L
            )

            eventStore.appendAll(listOf(e1, e2), tenantA)

            val stream = eventStore.getByAggregate("ORDER", "ORD-101", tenantA)
            assertEquals(2, stream.size)
            assertEquals(1L, stream[0].aggregateVersion)
            assertEquals(2L, stream[1].aggregateVersion)
        }
    }

    @Test
    fun test05_duplicateEventId_isRejected() {
        runBlocking {
            val payload = OrderCreatedEvent("ORD-101", "CUST-1", BigDecimal("500.00"), 2, "BDT", 1L)
            val e1 = EventEnvelope.create(
                payload = payload,
                projectId = "tenant_alpha",
                actor = EventActor.human("U-100")
            )

            eventStore.append(e1, tenantA)

            // Inserting identical eventId triggers unique constraint failure
            assertThrows(java.sql.SQLException::class.java) {
                runBlocking {
                    eventStore.append(e1, tenantA)
                }
            }
        }
    }
}

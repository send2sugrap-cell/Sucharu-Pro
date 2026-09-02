package com.sucharu.sucharupro.domain.event

import com.sucharu.sucharupro.data.event.fake.FakeEventStore
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import com.sucharu.sucharupro.domain.event.model.events.OrderUpdatedEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class EventStoreTest {

    @Test
    fun test01_eventStore_appendAndRetrieveLifecycle() = runBlocking {
        val store = FakeEventStore()
        val tenant = TenantContext("sucharu_main")

        val env1 = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("500"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("U1"),
            aggregateVersion = 1L
        )
        val env2 = EventEnvelope.create(
            payload = OrderUpdatedEvent("ORD-1", "CUST-1", BigDecimal("600"), "Added items", 2L),
            projectId = "sucharu_main",
            actor = EventActor.human("U1"),
            aggregateVersion = 2L
        )

        store.append(env1, tenant)
        store.append(env2, tenant)

        val fetched = store.getById(env1.eventId, tenant)
        assertNotNull(fetched)
        assertEquals(env1.eventId, fetched?.eventId)

        val stream = store.getByAggregate("ORDER", "ORD-1", tenant)
        assertEquals(2, stream.size)
        assertEquals(1L, stream[0].aggregateVersion)
        assertEquals(2L, stream[1].aggregateVersion)
    }

    @Test(expected = IllegalArgumentException::class)
    fun test02_eventStore_rejectsDuplicateEventId() = runBlocking {
        val store = FakeEventStore()
        val tenant = TenantContext("sucharu_main")

        val env = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("500"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("U1")
        )

        store.append(env, tenant)
        // Re-appending exact same event envelope fails with duplicate exception
        store.append(env, tenant)
    }
}

package com.sucharu.sucharupro.domain.event

import com.sucharu.sucharupro.data.event.fake.FakeEventStore
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class EventTenantIsolationTest {

    @Test
    fun test01_tenantIsolation_storeAppendsAndQueriesStrictlyByTenant() = runBlocking {
        val store = FakeEventStore()
        val tenantA = TenantContext("tenant_a")
        val tenantB = TenantContext("tenant_b")

        val envA = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-100", "CUST-1", BigDecimal("500"), itemCount = 1),
            projectId = "tenant_a",
            actor = EventActor.human("USER-A")
        )
        val envB = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-200", "CUST-2", BigDecimal("800"), itemCount = 2),
            projectId = "tenant_b",
            actor = EventActor.human("USER-B")
        )

        store.append(envA, tenantA)
        store.append(envB, tenantB)

        // Tenant A can see envA but not envB
        assertNotNull(store.getById(envA.eventId, tenantA))
        assertNull(store.getById(envB.eventId, tenantA))

        // Tenant B can see envB but not envA
        assertNotNull(store.getById(envB.eventId, tenantB))
        assertNull(store.getById(envA.eventId, tenantB))

        // Aggregate list is isolated
        val aggA = store.getByAggregate("ORDER", "ORD-100", tenantA)
        assertEquals(1, aggA.size)
        val aggAcross = store.getByAggregate("ORDER", "ORD-100", tenantB)
        assertTrue(aggAcross.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun test02_tenantIsolation_rejectsCrossTenantAppend() = runBlocking {
        val store = FakeEventStore()
        val tenantA = TenantContext("tenant_a")

        val crossEnv = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100"), itemCount = 1),
            projectId = "tenant_b", // Spoofed / mismatched project
            actor = EventActor.human("USER-1")
        )

        store.append(crossEnv, tenantA)
    }
}

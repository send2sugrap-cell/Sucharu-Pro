package com.sucharu.sucharupro.data.event

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.authorization.RoleCapabilityMatrix
import com.sucharu.sucharupro.data.event.postgres.PostgresDeadLetterRepository
import com.sucharu.sucharupro.data.event.postgres.PostgresEventStore
import com.sucharu.sucharupro.data.event.postgres.PostgresTransactionalOutboxStore
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.boundary.AiAgentEventBoundary
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.AccountLockedEvent
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class EventStoreSecurityTest {

    private lateinit var mockDb: MockPostgresEventDatabase
    private lateinit var eventStore: PostgresEventStore
    private lateinit var outboxStore: PostgresTransactionalOutboxStore
    private lateinit var deadLetterRepo: PostgresDeadLetterRepository

    private val tenantA = TenantContext("tenant_A")
    private val tenantB = TenantContext("tenant_B")

    @Before
    fun setUp() {
        mockDb = MockPostgresEventDatabase()
        eventStore = PostgresEventStore(mockDb)
        outboxStore = PostgresTransactionalOutboxStore(mockDb)
        deadLetterRepo = PostgresDeadLetterRepository(mockDb)
    }

    @Test
    fun test01_tenantIsolation_strictPartitioningAcrossAllTables() = runBlocking {
        val envA = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-A", "CUST-1", BigDecimal("100"), 1),
            projectId = "tenant_A",
            actor = EventActor.human("USER-A")
        )

        eventStore.append(envA, tenantA)
        outboxStore.enqueue(tenantA, envA)

        // Tenant A can query records
        assertNotNull(eventStore.getById(envA.eventId, tenantA))
        assertEquals(1, outboxStore.getPending(tenantA, 10).size)

        // Tenant B gets null and empty list
        assertNull(eventStore.getById(envA.eventId, tenantB))
        assertEquals(0, outboxStore.getPending(tenantB, 10).size)
    }

    @Test
    fun test02_aiAgent_cannotAccessRestrictedDeadLettersOrSecurityEvents() {
        val agent = AuthenticatedPrincipal(
            userId = "AI-BOT-01",
            projectId = "tenant_A",
            username = "assistant",
            role = UserRole.AI_AGENT,
            principalType = PrincipalType.AI_AGENT
        )

        val secEnvelope = EventEnvelope.create(
            payload = AccountLockedEvent("USER-99", "Brute force attack"),
            projectId = "tenant_A",
            actor = EventActor.system()
        )

        val accessDecision = AiAgentEventBoundary.evaluateAccess(agent, secEnvelope)
        assertFalse(accessDecision.isAllowed)
    }

    @Test
    fun test03_crossTenantEnqueue_isRejected() {
        val envA = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-A", "CUST-1", BigDecimal("100"), 1),
            projectId = "tenant_A",
            actor = EventActor.human("USER-A")
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                outboxStore.enqueue(tenantB, envA)
            }
        }
    }
}

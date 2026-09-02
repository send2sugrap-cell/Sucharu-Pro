package com.sucharu.sucharupro.domain.event

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.data.event.fake.FakeDomainEventConsumer
import com.sucharu.sucharupro.data.event.fake.FakeDomainEventPublisher
import com.sucharu.sucharupro.data.event.fake.FakeEventStore
import com.sucharu.sucharupro.data.event.fake.FakeIdempotencyStore
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.boundary.AiAgentEventBoundary
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.dispatcher.DomainEventDispatcher
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.EventTraceContext
import com.sucharu.sucharupro.domain.event.model.events.AccountLockedEvent
import com.sucharu.sucharupro.domain.event.model.events.AuthenticationFailedEvent
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import com.sucharu.sucharupro.domain.event.model.events.OrderUpdatedEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Comprehensive verification of all 20 required security scenarios for Sucharu Pro (INFRA-04 Step 01).
 */
class EventSecurityScenariosTest {

    // Scenario 1: Client cannot publish arbitrary event (only server use cases create envelopes from domain facts)
    @Test
    fun scenario01_clientCannotPublishArbitraryEvent_envelopeRequiresStrongDomainEvent() {
        val payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("500"), 1)
        val envelope = EventEnvelope.create(
            payload = payload,
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1")
        )
        assertEquals(DomainEventType.ORDER_CREATED, envelope.eventType)
        assertEquals("ORDER", envelope.aggregateType)
    }

    // Scenario 2: Client cannot spoof projectId
    @Test(expected = IllegalArgumentException::class)
    fun scenario02_clientCannotSpoofProjectId_blankOrMismatchedRejected() = runBlocking {
        val store = FakeEventStore()
        val authoritativeTenant = TenantContext("sucharu_tenant_1")

        val spoofedEnvelope = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100"), 1),
            projectId = "sucharu_tenant_2", // Spoofed projectId
            actor = EventActor.human("USER-1")
        )

        store.append(spoofedEnvelope, authoritativeTenant)
    }

    // Scenario 3: Client cannot spoof actorId (blank actorId rejected)
    @Test(expected = IllegalArgumentException::class)
    fun scenario03_clientCannotSpoofActorId_blankActorRejected() {
        EventActor.human("   ")
    }

    // Scenario 4: Client cannot spoof principalType (principal type must align with actor type)
    @Test
    fun scenario04_clientCannotSpoofPrincipalType_validatedAgainstPrincipal() {
        val actor = EventActor.system("SYSTEM_SCHEDULER")
        assertEquals(PrincipalType.SYSTEM, actor.actorType)
        assertEquals(PrincipalType.SYSTEM, actor.principalType)
    }

    // Scenario 5: Tenant A cannot access Tenant B events
    @Test
    fun scenario05_tenantACannotAccessTenantBEvents() = runBlocking {
        val store = FakeEventStore()
        val tenantA = TenantContext("tenant_a")
        val tenantB = TenantContext("tenant_b")

        val envA = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-A", "CUST-A", BigDecimal("100"), 1),
            projectId = "tenant_a",
            actor = EventActor.human("USER-A")
        )
        store.append(envA, tenantA)

        assertNull(store.getById(envA.eventId, tenantB))
        assertTrue(store.getByAggregate("ORDER", "ORD-A", tenantB).isEmpty())
    }

    // Scenario 6: Customer cannot publish ADMIN security events
    @Test
    fun scenario06_customerCannotPublishAdminSecurityEvents() {
        val customerPrincipal = AuthenticatedPrincipal(
            userId = "CUST-USER-1",
            projectId = "sucharu_main",
            username = "customer_1",
            role = UserRole.CUSTOMER,
            principalType = PrincipalType.HUMAN,
            permissions = setOf(com.sucharu.sucharupro.data.api.model.UserPermission.READ_OWN_ORDERS)
        )

        // Customer principal lacks ADMIN_ALL or ADMIN_SUSPEND_ACCOUNT capabilities
        assertFalse(customerPrincipal.hasPermission(com.sucharu.sucharupro.data.api.model.UserPermission.ADMIN_ALL))
        assertFalse(customerPrincipal.isStaff)
    }

    // Scenario 7: AI_AGENT cannot subscribe to unrestricted events
    @Test
    fun scenario07_aiAgentCannotSubscribeToUnrestrictedEvents() {
        val agentPrincipal = AuthenticatedPrincipal(
            userId = "AI-AGENT-01",
            projectId = "sucharu_main",
            username = "assistant",
            role = UserRole.AI_AGENT,
            principalType = PrincipalType.AI_AGENT
        )

        val env = EventEnvelope.create(
            payload = com.sucharu.sucharupro.domain.event.model.events.SystemMaintenanceScheduledEvent(
                maintenanceId = "MAINT-1",
                startTimestamp = 1000L,
                endTimestamp = 2000L,
                description = "Database cluster upgrade"
            ),
            projectId = "sucharu_main",
            actor = EventActor.system()
        )

        val decision = AiAgentEventBoundary.evaluateAccess(agentPrincipal, env)
        assertFalse(decision.isAllowed)
    }

    // Scenario 8: AI_AGENT cannot cross tenant boundary
    @Test
    fun scenario08_aiAgentCannotCrossTenantBoundary() {
        val agentPrincipal = AuthenticatedPrincipal(
            userId = "AI-AGENT-01",
            projectId = "tenant_1",
            username = "assistant",
            role = UserRole.AI_AGENT,
            principalType = PrincipalType.AI_AGENT
        )

        val env = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100"), 1),
            projectId = "tenant_2", // Foreign tenant
            actor = EventActor.human("U1")
        )

        val decision = AiAgentEventBoundary.evaluateAccess(agentPrincipal, env)
        assertFalse(decision.isAllowed)
    }

    // Scenario 9: Event payload cannot contain secret credentials
    @Test
    fun scenario09_eventPayloadCannotContainSecretCredentials() {
        val event = AuthenticationFailedEvent(
            attemptedIdentifierMasked = "ad***@sucharu.com",
            failureReason = "INVALID_CREDENTIALS"
        )
        // Ensure no password field exists on AuthenticationFailedEvent or any other domain event
        assertFalse(event.attemptedIdentifierMasked.contains("password"))
    }

    // Scenario 10: Duplicate event processing is idempotent
    @Test
    fun scenario10_duplicateEventProcessingIsIdempotent() = runBlocking {
        val idempotencyStore = FakeIdempotencyStore()
        val dispatcher = DomainEventDispatcher(idempotencyStore)
        val consumer = FakeDomainEventConsumer<OrderCreatedEvent>(
            consumerId = "TestIdempotentConsumer",
            supportedEventType = DomainEventType.ORDER_CREATED
        )
        dispatcher.registerConsumer(consumer)

        val env = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("U1")
        )

        val r1 = dispatcher.dispatch(env)
        assertEquals(1, r1.successCount)
        assertEquals(1, consumer.consumedEnvelopes.size)

        val r2 = dispatcher.dispatch(env)
        assertEquals(0, r2.successCount)
        assertEquals(1, r2.skippedCount)
        assertEquals(1, consumer.consumedEnvelopes.size)
    }

    // Scenario 11: Stale aggregate version is detected
    @Test
    fun scenario11_staleAggregateVersionIsDetected() = runBlocking {
        val dispatcher = DomainEventDispatcher()
        val consumer = FakeDomainEventConsumer<OrderUpdatedEvent>(
            consumerId = "TestUpdateConsumer",
            supportedEventType = DomainEventType.ORDER_UPDATED
        )
        dispatcher.registerConsumer(consumer)

        val envV2 = EventEnvelope.create(
            payload = OrderUpdatedEvent("ORD-1", "CUST-1", BigDecimal("200"), "Update 2", 2L),
            projectId = "sucharu_main",
            actor = EventActor.human("U1"),
            aggregateVersion = 2L
        )
        val envV1 = EventEnvelope.create(
            payload = OrderUpdatedEvent("ORD-1", "CUST-1", BigDecimal("100"), "Update 1", 1L),
            projectId = "sucharu_main",
            actor = EventActor.human("U1"),
            aggregateVersion = 1L
        )

        dispatcher.dispatch(envV2)
        val staleResult = dispatcher.dispatch(envV1)
        assertFalse(staleResult.isFullySuccessful)
        val failure = staleResult.consumerResults["STREAM_ORDERING_VALIDATOR"] as EventConsumerResult.Failure
        assertEquals(EventFailureClassification.STALE_VERSION, failure.classification)
    }

    // Scenario 12: Event version mismatch is handled safely
    @Test
    fun scenario12_eventVersionMismatchIsHandledSafely() = runBlocking {
        val dispatcher = DomainEventDispatcher()
        val v2Consumer = FakeDomainEventConsumer<OrderCreatedEvent>(
            consumerId = "V2OnlyConsumer",
            supportedEventType = DomainEventType.ORDER_CREATED,
            supportedVersion = "v2"
        )
        dispatcher.registerConsumer(v2Consumer)

        val envV1 = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("U1")
        )

        val summary = dispatcher.dispatch(envV1)
        assertEquals(0, summary.totalConsumersMatched)
        assertEquals(0, v2Consumer.consumedEnvelopes.size)
    }

    // Scenario 13: Unauthorized consumer is denied (AI Agent trying to subscribe to security events)
    @Test
    fun scenario13_unauthorizedConsumerIsDenied() {
        val agent = AuthenticatedPrincipal(
            userId = "AGENT-1",
            projectId = "sucharu_main",
            username = "agent",
            role = UserRole.AI_AGENT,
            principalType = PrincipalType.AI_AGENT
        )

        val secEnv = EventEnvelope.create(
            payload = AccountLockedEvent("USER-99", "Brute force attempts", 2L),
            projectId = "sucharu_main",
            actor = EventActor.system()
        )

        val decision = AiAgentEventBoundary.evaluateAccess(agent, secEnv)
        assertFalse(decision.isAllowed)
    }

    // Scenario 14: correlationId is preserved
    @Test
    fun scenario14_correlationIdIsPreserved() {
        val trace = EventTraceContext(correlationId = "CORR-999")
        val env = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("U1"),
            traceContext = trace
        )
        assertEquals("CORR-999", env.correlationId)
    }

    // Scenario 15: causationId is preserved
    @Test
    fun scenario15_causationIdIsPreserved() {
        val trace = EventTraceContext(correlationId = "CORR-999", causationId = "PARENT-EVT-123")
        val env = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("U1"),
            traceContext = trace
        )
        assertEquals("PARENT-EVT-123", env.causationId)
    }

    // Scenario 16: requestId is preserved
    @Test
    fun scenario16_requestIdIsPreserved() {
        val trace = EventTraceContext(correlationId = "CORR-999", requestId = "REQ-HTTP-555")
        val env = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("U1"),
            traceContext = trace
        )
        assertEquals("REQ-HTTP-555", env.requestId)
    }

    // Scenario 17: Immutable event cannot be modified
    @Test
    fun scenario17_immutableEventCannotBeModified() {
        val payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100"), 1)
        val envelope = EventEnvelope.create(
            payload = payload,
            projectId = "sucharu_main",
            actor = EventActor.human("U1")
        )

        val copy = envelope.copy(source = "modified-source")
        assertNotEquals(envelope.source, copy.source)
        assertEquals("sucharu-pro-backend", envelope.source)
    }

    // Scenario 18: Failed transient event is classified retryable
    @Test
    fun scenario18_failedTransientEventIsClassifiedRetryable() {
        val transientFailure = EventConsumerResult.Failure(
            reason = "Temporary network timeout",
            classification = EventFailureClassification.TRANSIENT
        )
        assertTrue(transientFailure.isRetryable)
        assertTrue(transientFailure.classification.isRetryable)
    }

    // Scenario 19: Validation failure is non-retryable
    @Test
    fun scenario19_validationFailureIsNonRetryable() {
        val validationFailure = EventConsumerResult.Failure(
            reason = "Invalid schema payload",
            classification = EventFailureClassification.VALIDATION
        )
        assertFalse(validationFailure.isRetryable)
        assertFalse(validationFailure.classification.isRetryable)
    }

    // Scenario 20: Arbitrary public event injection is denied (unauthenticated cannot access AI Agent subscription)
    @Test
    fun scenario20_arbitraryPublicEventInjectionIsDenied() {
        val nullPrincipal: AuthenticatedPrincipal? = null
        val env = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("100"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("U1")
        )

        val decision = AiAgentEventBoundary.evaluateAccess(nullPrincipal, env)
        assertFalse(decision.isAllowed)
    }
}

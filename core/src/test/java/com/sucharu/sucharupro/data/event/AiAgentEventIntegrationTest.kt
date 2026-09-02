package com.sucharu.sucharupro.data.event

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.data.event.integration.aiagent.AiAgentEventConsumer
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.AuthenticationFailedEvent
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class AiAgentEventIntegrationTest {

    private val authorizedAgent = AuthenticatedPrincipal(
        userId = "AGENT-007",
        username = "sucharu_scheduler_agent",
        role = UserRole.AI_AGENT,
        projectId = "sucharu_main",
        principalType = PrincipalType.AI_AGENT
    )

    private val crossTenantAgent = AuthenticatedPrincipal(
        userId = "AGENT-007",
        username = "sucharu_scheduler_agent",
        role = UserRole.AI_AGENT,
        projectId = "other_tenant",
        principalType = PrincipalType.AI_AGENT
    )

    private val humanPrincipal = AuthenticatedPrincipal(
        userId = "U-1",
        username = "john_doe",
        role = UserRole.CUSTOMER,
        projectId = "sucharu_main",
        principalType = PrincipalType.HUMAN
    )

    @Test
    fun test01_authorizedAgent_consumesOrderContextSuccessfully() {
        runBlocking {
            val envelope = EventEnvelope.create(
                payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("500.00"), 2),
                projectId = "sucharu_main",
                actor = EventActor.human("U-1"),
                metadata = mapOf("token_internal" to "SECRET", "customerNote" to "Urgent order")
            )

            val consumer = AiAgentEventConsumer<OrderCreatedEvent>(
                supportedEventType = DomainEventType.ORDER_CREATED,
                targetAgentPrincipal = authorizedAgent
            )

            val result = consumer.consume(envelope)
            assertTrue(result.isSuccess)
            assertEquals(1, consumer.receivedFrames.size)

            val frame = consumer.receivedFrames[0]
            assertEquals("sucharu_main", frame.projectId)
            assertEquals("ORD-1", frame.aggregateId)
            assertEquals(AuthorizationCapability.AI_READ_ORDER_CONTEXT, frame.grantedCapability)

            // Verify data minimization stripped "token_internal"
            assertFalse(frame.contextSummary.containsKey("token_internal"))
            assertEquals("Urgent order", frame.contextSummary["customerNote"])
        }
    }

    @Test
    fun test02_crossTenantAgent_isDeniedWithSecurityClassification() {
        runBlocking {
            val envelope = EventEnvelope.create(
                payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("500.00"), 2),
                projectId = "sucharu_main",
                actor = EventActor.human("U-1")
            )

            val consumer = AiAgentEventConsumer<OrderCreatedEvent>(
                supportedEventType = DomainEventType.ORDER_CREATED,
                targetAgentPrincipal = crossTenantAgent
            )

            val result = consumer.consume(envelope)
            assertTrue(result.isFailure)
            val failure = result as EventConsumerResult.Failure
            assertEquals(EventFailureClassification.SECURITY, failure.classification)
            assertEquals(0, consumer.receivedFrames.size)
        }
    }

    @Test
    fun test03_nonAgentPrincipal_isDenied() {
        runBlocking {
            val envelope = EventEnvelope.create(
                payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("500.00"), 2),
                projectId = "sucharu_main",
                actor = EventActor.human("U-1")
            )

            val consumer = AiAgentEventConsumer<OrderCreatedEvent>(
                supportedEventType = DomainEventType.ORDER_CREATED,
                targetAgentPrincipal = humanPrincipal
            )

            val result = consumer.consume(envelope)
            assertTrue(result.isFailure)
            val failure = result as EventConsumerResult.Failure
            assertEquals(EventFailureClassification.SECURITY, failure.classification)
        }
    }

    @Test
    fun test04_securityEvents_areBlockedForAiAgents() {
        runBlocking {
            val securityEnvelope = EventEnvelope.create(
                payload = AuthenticationFailedEvent("***", "BAD_PASSWORD", "1.1.1.1"),
                projectId = "sucharu_main",
                actor = EventActor.human("U-1")
            )

            val consumer = AiAgentEventConsumer<AuthenticationFailedEvent>(
                supportedEventType = DomainEventType.AUTH_FAILED,
                targetAgentPrincipal = authorizedAgent
            )

            val result = consumer.consume(securityEnvelope)
            assertTrue(result.isFailure)
            val failure = result as EventConsumerResult.Failure
            assertEquals(EventFailureClassification.SECURITY, failure.classification)
        }
    }
}

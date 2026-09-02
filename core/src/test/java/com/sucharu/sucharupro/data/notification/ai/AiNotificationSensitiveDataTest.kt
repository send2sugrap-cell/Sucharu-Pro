package com.sucharu.sucharupro.data.notification.ai

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserPermission
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.AuthenticationFailedEvent
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * Sensitive data and blocked event categories test suite (INFRA-04 Step 08).
 */
class AiNotificationSensitiveDataTest {

    private val adminAi = AuthenticatedPrincipal(
        userId = "ai-sens",
        projectId = "p-001",
        username = "ai_sens",
        role = UserRole.AI_AGENT,
        permissions = setOf(UserPermission.ADMIN_ALL),
        principalType = PrincipalType.AI_AGENT
    )

    private val boundary = AiAgentNotificationSecurityBoundary()

    @Test
    fun test01_authFailedEvent_isBlockedFromAi() {
        assertTrue(
            "AUTH_FAILED must be blocked from AI Agent consumption",
            boundary.isEventBlockedForAi(DomainEventType.AUTH_FAILED)
        )
    }

    @Test
    fun test02_passwordChangedEvent_isBlockedFromAi() {
        assertTrue(
            "PASSWORD_CHANGED must be blocked from AI Agent consumption",
            boundary.isEventBlockedForAi(DomainEventType.PASSWORD_CHANGED)
        )
    }

    @Test
    fun test03_sessionCreatedEvent_isBlockedFromAi() {
        assertTrue(
            "SESSION_CREATED must be blocked from AI Agent consumption",
            boundary.isEventBlockedForAi(DomainEventType.SESSION_CREATED)
        )
    }

    @Test
    fun test04_orderCreatedEvent_isPermittedForAi() {
        assertFalse(
            "ORDER_CREATED is a safe domain event and must not be blocked",
            boundary.isEventBlockedForAi(DomainEventType.ORDER_CREATED)
        )
    }

    @Test
    fun test05_eventConsumer_rejectsBlockedEvents() = runBlocking {
        val consumer = AiAgentNotificationEventConsumer<AuthenticationFailedEvent>(
            supportedEventType = DomainEventType.AUTH_FAILED,
            targetAgentPrincipal = adminAi,
            securityBoundary = boundary
        )

        val envelope = EventEnvelope(
            eventId = "evt-auth-fail",
            eventType = DomainEventType.AUTH_FAILED,
            eventVersion = "v1",
            projectId = "p-001",
            aggregateType = "SECURITY",
            aggregateId = "usr_123",
            actorType = PrincipalType.HUMAN,
            actorId = "system",
            correlationId = "corr-sens-1",
            payload = AuthenticationFailedEvent(
                attemptedIdentifierMasked = "u***@example.com",
                failureReason = "INVALID_CREDENTIALS"
            )
        )

        val result = consumer.consume(envelope)
        assertTrue("Consumer must fail when consuming sensitive event", result is com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult.Failure)
        assertEquals(0, consumer.receivedViews.size)
    }

    @Test
    fun test06_paymentReceivedEvent_isBlockedFromAi() {
        assertTrue(
            "PAYMENT_RECEIVED must be blocked from AI Agent consumption",
            boundary.isEventBlockedForAi(DomainEventType.PAYMENT_RECEIVED)
        )
    }
}

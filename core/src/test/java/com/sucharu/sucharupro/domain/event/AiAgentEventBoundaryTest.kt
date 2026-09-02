package com.sucharu.sucharupro.domain.event

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.domain.event.boundary.AiAgentEventBoundary
import com.sucharu.sucharupro.domain.event.model.EventActor
import com.sucharu.sucharupro.domain.event.model.EventEnvelope
import com.sucharu.sucharupro.domain.event.model.events.AuthenticationFailedEvent
import com.sucharu.sucharupro.domain.event.model.events.OrderCreatedEvent
import com.sucharu.sucharupro.domain.event.model.events.PaymentReceivedEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class AiAgentEventBoundaryTest {

    @Test
    fun test01_aiAgentWithMatchingCapabilityAndTenant_isAllowed() {
        val agentPrincipal = AuthenticatedPrincipal(
            userId = "AGENT-01",
            projectId = "sucharu_main",
            username = "order_assistant",
            role = UserRole.AI_AGENT,
            principalType = PrincipalType.AI_AGENT
        )

        val envelope = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("500"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1")
        )

        val decision = AiAgentEventBoundary.evaluateAccess(agentPrincipal, envelope)
        assertTrue(decision.isAllowed)
    }

    @Test
    fun test02_nonAiAgentRole_isDenied() {
        val humanPrincipal = AuthenticatedPrincipal(
            userId = "USER-01",
            projectId = "sucharu_main",
            username = "john_doe",
            role = UserRole.CUSTOMER,
            principalType = PrincipalType.HUMAN
        )

        val envelope = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("500"), 1),
            projectId = "sucharu_main",
            actor = EventActor.human("USER-1")
        )

        val decision = AiAgentEventBoundary.evaluateAccess(humanPrincipal, envelope)
        assertFalse(decision.isAllowed)
    }

    @Test
    fun test03_aiAgentCrossTenantAttempt_isDenied() {
        val agentPrincipal = AuthenticatedPrincipal(
            userId = "AGENT-01",
            projectId = "tenant_a",
            username = "agent_a",
            role = UserRole.AI_AGENT,
            principalType = PrincipalType.AI_AGENT
        )

        val envelope = EventEnvelope.create(
            payload = OrderCreatedEvent("ORD-1", "CUST-1", BigDecimal("500"), 1),
            projectId = "tenant_b", // Cross-tenant
            actor = EventActor.human("USER-1")
        )

        val decision = AiAgentEventBoundary.evaluateAccess(agentPrincipal, envelope)
        assertFalse(decision.isAllowed)
    }

    @Test
    fun test04_aiAgentSubscriptionToSecurityOrDirectPaymentEvents_isStrictlyBlocked() {
        val agentPrincipal = AuthenticatedPrincipal(
            userId = "AGENT-01",
            projectId = "sucharu_main",
            username = "agent_all",
            role = UserRole.AI_AGENT,
            principalType = PrincipalType.AI_AGENT
        )

        val secEnvelope = EventEnvelope.create(
            payload = AuthenticationFailedEvent("unknown_user", "BAD_PASSWORD"),
            projectId = "sucharu_main",
            actor = EventActor.system()
        )
        val payEnvelope = EventEnvelope.create(
            payload = PaymentReceivedEvent("PAY-1", "INV-1", "ORD-1", "CUST-1", BigDecimal("100"), "BDT", "BKASH", "TRX-1"),
            projectId = "sucharu_main",
            actor = EventActor.human("U1")
        )

        assertFalse(AiAgentEventBoundary.evaluateAccess(agentPrincipal, secEnvelope).isAllowed)
        assertFalse(AiAgentEventBoundary.evaluateAccess(agentPrincipal, payEnvelope).isAllowed)
    }
}

package com.sucharu.sucharupro.data.observability

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.observability.service.ObservabilityAuthDecision
import com.sucharu.sucharupro.data.observability.service.TenantObservabilityAuthorizationService
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Authorization and RBAC test suite for operational observability (INFRA-04 Step 09).
 */
class ObservabilityAuthorizationTest {

    private lateinit var authService: TenantObservabilityAuthorizationService

    @Before
    fun setUp() {
        authService = TenantObservabilityAuthorizationService()
    }

    @Test
    fun test01_unauthenticatedPrincipal_isDenied() {
        val decision = authService.authorizeGlobalAccess(null)
        assertTrue("Unauthenticated access must be denied", decision is ObservabilityAuthDecision.Denied)
        val denied = decision as ObservabilityAuthDecision.Denied
        assertEquals("UNAUTHENTICATED", denied.code)
    }

    @Test
    fun test02_customerPrincipal_isDenied() {
        val customer = AuthenticatedPrincipal(
            userId = "cust-01",
            projectId = "p-001",
            username = "cust01",
            role = UserRole.CUSTOMER,
            principalType = PrincipalType.HUMAN
        )
        val decision = authService.authorizeGlobalAccess(customer)
        assertTrue("Customer principal cannot access global observability", decision is ObservabilityAuthDecision.Denied)
        val denied = decision as ObservabilityAuthDecision.Denied
        assertEquals("MISSING_CAPABILITY", denied.code)
    }

    @Test
    fun test03_aiAgentPrincipal_isDeniedFromGeneralTelemetry() {
        val aiAgent = AuthenticatedPrincipal(
            userId = "agent-01",
            projectId = "p-001",
            username = "ai_agent",
            role = UserRole.AI_AGENT,
            principalType = PrincipalType.AI_AGENT
        )
        val decision = authService.authorizeGlobalAccess(aiAgent)
        assertTrue("AI Agent principal is blocked from full operational telemetry", decision is ObservabilityAuthDecision.Denied)
        val denied = decision as ObservabilityAuthDecision.Denied
        assertEquals("AI_AGENT_BLOCKED", denied.code)
    }

    @Test
    fun test04_managerCanAccessGlobalObservability() {
        val manager = AuthenticatedPrincipal(
            userId = "mgr-01",
            projectId = "p-001",
            username = "mgr01",
            role = UserRole.MANAGER,
            principalType = PrincipalType.HUMAN
        )
        val decision = authService.authorizeGlobalAccess(manager)
        assertTrue("Manager role can access global observability view", decision is ObservabilityAuthDecision.Allowed)
    }
}

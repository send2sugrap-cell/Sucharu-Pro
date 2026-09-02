package com.sucharu.sucharupro.data.workflow

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.workflow.integration.aiagent.AiAgentWorkflowAuthResult
import com.sucharu.sucharupro.data.workflow.integration.aiagent.AiAgentWorkflowSecurityBoundary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAgentWorkflowSecurityBoundaryTest {

    @Test
    fun testPermittedAiAgentWorkflowIsAuthorized() {
        val tenant = TenantContext("tenant_alpha")
        val aiPrincipal = AuthenticatedPrincipal(
            userId = "agent-1",
            username = "sucharu_ai",
            role = UserRole.AI_AGENT,
            projectId = "tenant_alpha",
            principalType = PrincipalType.AI_AGENT
        )

        val result = AiAgentWorkflowSecurityBoundary.evaluateWorkflowInitiation(
            principal = aiPrincipal,
            definitionId = "order.analyze_print_specs",
            context = emptyMap(),
            tenantContext = tenant
        )

        assertEquals(AiAgentWorkflowAuthResult.Authorized, result)
    }

    @Test
    fun testAiAgentCrossTenantAttemptIsDenied() {
        val tenantTarget = TenantContext("tenant_beta")
        val aiPrincipal = AuthenticatedPrincipal(
            userId = "agent-1",
            username = "sucharu_ai",
            role = UserRole.AI_AGENT,
            projectId = "tenant_alpha",
            principalType = PrincipalType.AI_AGENT
        )

        val result = AiAgentWorkflowSecurityBoundary.evaluateWorkflowInitiation(
            principal = aiPrincipal,
            definitionId = "order.analyze_print_specs",
            context = emptyMap(),
            tenantContext = tenantTarget
        )

        assertTrue(result is AiAgentWorkflowAuthResult.Denied)
        val denied = result as AiAgentWorkflowAuthResult.Denied
        assertTrue(denied.isSecurityViolation)
        assertTrue(denied.reason.contains("Cross-tenant access prohibited"))
    }

    @Test
    fun testHighImpactWorkflowRequiresHumanConfirmationMetadata() {
        val tenant = TenantContext("tenant_alpha")
        val aiPrincipal = AuthenticatedPrincipal(
            userId = "agent-1",
            username = "sucharu_ai",
            role = UserRole.AI_AGENT,
            projectId = "tenant_alpha",
            principalType = PrincipalType.AI_AGENT
        )

        // Without human approval metadata
        val resultNoApproval = AiAgentWorkflowSecurityBoundary.evaluateWorkflowInitiation(
            principal = aiPrincipal,
            definitionId = "finance.bulk_refund_payout",
            context = emptyMap(),
            tenantContext = tenant
        )
        assertTrue(resultNoApproval is AiAgentWorkflowAuthResult.RequiresHumanApproval)

        // With human approval metadata
        val resultWithApproval = AiAgentWorkflowSecurityBoundary.evaluateWorkflowInitiation(
            principal = aiPrincipal,
            definitionId = "finance.bulk_refund_payout",
            context = mapOf(
                "requiresConfirmation" to "true",
                "confirmationId" to "CONF-FIN-888",
                "approvedByHumanId" to "manager-frank"
            ),
            tenantContext = tenant
        )
        assertEquals(AiAgentWorkflowAuthResult.Authorized, resultWithApproval)
    }

    @Test
    fun testUnlistedWorkflowIsDenied() {
        val tenant = TenantContext("tenant_alpha")
        val aiPrincipal = AuthenticatedPrincipal(
            userId = "agent-1",
            username = "sucharu_ai",
            role = UserRole.AI_AGENT,
            projectId = "tenant_alpha",
            principalType = PrincipalType.AI_AGENT
        )

        val result = AiAgentWorkflowSecurityBoundary.evaluateWorkflowInitiation(
            principal = aiPrincipal,
            definitionId = "arbitrary.unlisted_workflow",
            context = emptyMap(),
            tenantContext = tenant
        )

        assertTrue(result is AiAgentWorkflowAuthResult.Denied)
    }
}

package com.sucharu.sucharupro.data.workflow.integration.aiagent

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext

/**
 * Result of evaluating AI Agent workflow submission.
 */
sealed class AiAgentWorkflowAuthResult {
    object Authorized : AiAgentWorkflowAuthResult()
    data class RequiresHumanApproval(val reason: String) : AiAgentWorkflowAuthResult()
    data class Denied(val reason: String, val isSecurityViolation: Boolean = true) : AiAgentWorkflowAuthResult()
}

/**
 * Security boundary governing AI Agent workflow execution (INFRA-04 Step 05).
 */
object AiAgentWorkflowSecurityBoundary {

    private val permittedWorkflowDefinitions = setOf(
        "order.analyze_print_specs",
        "inventory.forecast_reorder",
        "production.schedule_optimization",
        "customer.draft_quote_workflow",
        "report.periodic_analytics"
    )

    private val highImpactWorkflows = setOf(
        "finance.bulk_refund_payout",
        "system.database_purge",
        "inventory.write_off_damage",
        "order.cancel_and_refund"
    )

    /**
     * Evaluates whether an AI Agent machine principal is authorized to initiate a workflow.
     */
    fun evaluateWorkflowInitiation(
        principal: AuthenticatedPrincipal,
        definitionId: String,
        context: Map<String, String>,
        tenantContext: TenantContext
    ): AiAgentWorkflowAuthResult {
        // 1. Validate Principal Type
        if (principal.principalType != PrincipalType.AI_AGENT && principal.role != UserRole.AI_AGENT) {
            return AiAgentWorkflowAuthResult.Denied(
                "Principal is not an AI_AGENT machine principal."
            )
        }

        // 2. Strict Tenant Isolation
        if (principal.projectId != tenantContext.projectId) {
            return AiAgentWorkflowAuthResult.Denied(
                "Cross-tenant access prohibited: principal tenant '${principal.projectId}' != target '${tenantContext.projectId}'"
            )
        }

        // 3. High Impact Check -> Requires Human Confirmation
        if (highImpactWorkflows.contains(definitionId)) {
            val hasHumanApproval = context["requiresConfirmation"] == "true" &&
                    !context["confirmationId"].isNullOrBlank() &&
                    !context["approvedByHumanId"].isNullOrBlank()

            if (!hasHumanApproval) {
                return AiAgentWorkflowAuthResult.RequiresHumanApproval(
                    "Workflow '$definitionId' is classified as HIGH-IMPACT and requires verified human confirmation."
                )
            }
            return AiAgentWorkflowAuthResult.Authorized
        }

        // 4. Capability Whitelist Check
        if (!permittedWorkflowDefinitions.contains(definitionId)) {
            return AiAgentWorkflowAuthResult.Denied(
                "AI Agent is not authorized to trigger workflow '$definitionId'. Permitted workflows: $permittedWorkflowDefinitions"
            )
        }

        return AiAgentWorkflowAuthResult.Authorized
    }
}

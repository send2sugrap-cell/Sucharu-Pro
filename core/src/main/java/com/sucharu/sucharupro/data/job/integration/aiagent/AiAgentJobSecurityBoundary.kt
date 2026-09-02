package com.sucharu.sucharupro.data.job.integration.aiagent

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext

/**
 * Result of evaluating AI Agent job creation authorization.
 */
sealed class AiAgentJobAuthResult {
    data object Authorized : AiAgentJobAuthResult()
    data class Denied(val reason: String) : AiAgentJobAuthResult()
    data class RequiresConfirmation(val reason: String, val confirmationId: String) : AiAgentJobAuthResult()
}

/**
 * Security boundary for AI Agent asynchronous job submission (INFRA-04 Step 04).
 */
object AiAgentJobSecurityBoundary {

    // High impact job types that strictly mandate prior human approval
    private val HIGH_IMPACT_JOB_TYPES = setOf(
        "finance.execute_bulk_payout",
        "inventory.delete_warehouse_stock",
        "system.purge_tenant_data",
        "admin.override_pricing"
    )

    // Allowed asynchronous job types that AI Agent may trigger
    private val PERMITTED_AI_JOB_TYPES = setOf(
        "order.analyze_print_specifications",
        "customer.generate_quote_summary",
        "production.optimize_cutting_layout",
        "report.generate_sales_forecast",
        "notification.send_customer_status"
    ) + HIGH_IMPACT_JOB_TYPES

    /**
     * Evaluates whether the AI Agent machine principal is authorized to enqueue a background job.
     */
    fun evaluateJobSubmission(
        principal: AuthenticatedPrincipal,
        jobType: String,
        metadata: Map<String, String>,
        tenantContext: TenantContext
    ): AiAgentJobAuthResult {
        // 1. Validate machine principal
        if (principal.role != UserRole.AI_AGENT || principal.principalType != PrincipalType.AI_AGENT) {
            return AiAgentJobAuthResult.Denied("Principal '${principal.userId}' is not an authorized AI_AGENT machine principal")
        }

        // 2. Validate tenant isolation
        if (principal.projectId != tenantContext.projectId) {
            return AiAgentJobAuthResult.Denied("Cross-tenant AI job submission denied: Principal tenant '${principal.projectId}' != target '${tenantContext.projectId}'")
        }

        // 3. Validate permitted job type
        if (!PERMITTED_AI_JOB_TYPES.contains(jobType)) {
            return AiAgentJobAuthResult.Denied("AI Agent is not permitted to trigger jobType '$jobType'")
        }

        // 4. Check high-impact human confirmation requirement
        if (HIGH_IMPACT_JOB_TYPES.contains(jobType)) {
            val requiresConf = metadata["requiresConfirmation"]?.toBoolean() ?: true
            val approvedBy = metadata["approvedByHumanId"]
            val confirmationId = metadata["confirmationId"]

            if (requiresConf && (approvedBy.isNullOrBlank() || confirmationId.isNullOrBlank())) {
                return AiAgentJobAuthResult.RequiresConfirmation(
                    reason = "High-impact job '$jobType' requires explicit human confirmation",
                    confirmationId = confirmationId ?: "CONF-${System.currentTimeMillis()}"
                )
            }
        }

        return AiAgentJobAuthResult.Authorized
    }
}

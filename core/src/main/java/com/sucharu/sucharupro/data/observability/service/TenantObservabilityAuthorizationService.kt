package com.sucharu.sucharupro.data.observability.service

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.data.auth.authorization.RoleCapabilityMatrix

/**
 * Result of evaluating operational observability authorization.
 */
sealed class ObservabilityAuthDecision {
    data object Allowed : ObservabilityAuthDecision()
    data class Denied(val code: String, val message: String) : ObservabilityAuthDecision()
}

/**
 * Server-authoritative Tenant-Aware Observability Authorization Service (INFRA-04 Step 09).
 */
class TenantObservabilityAuthorizationService {

    fun authorizeTenantAccess(
        principal: AuthenticatedPrincipal?,
        targetProjectId: String,
        requiredCapability: AuthorizationCapability = AuthorizationCapability.OBSERVABILITY_TENANT_VIEW
    ): ObservabilityAuthDecision {
        if (principal == null) {
            return ObservabilityAuthDecision.Denied("UNAUTHENTICATED", "Authentication required to access observability.")
        }

        // AI Agents are denied from general operational metrics by default
        if (principal.isAiAgent) {
            return ObservabilityAuthDecision.Denied("AI_AGENT_BLOCKED", "AI Agents cannot access full operational telemetry.")
        }

        // Multi-tenant boundary check
        if (principal.role != UserRole.ADMIN && principal.projectId != targetProjectId) {
            return ObservabilityAuthDecision.Denied("TENANT_MISMATCH", "Access denied: Principal '${principal.projectId}' cannot view tenant '$targetProjectId'.")
        }

        // Capability check
        val hasCap = RoleCapabilityMatrix.hasCapability(principal.role, requiredCapability)
        if (!hasCap) {
            return ObservabilityAuthDecision.Denied("MISSING_CAPABILITY", "Missing required capability '${requiredCapability.name}' for role '${principal.role.name}'.")
        }

        return ObservabilityAuthDecision.Allowed
    }

    fun authorizeGlobalAccess(
        principal: AuthenticatedPrincipal?,
        requiredCapability: AuthorizationCapability = AuthorizationCapability.OBSERVABILITY_VIEW
    ): ObservabilityAuthDecision {
        if (principal == null) {
            return ObservabilityAuthDecision.Denied("UNAUTHENTICATED", "Authentication required to access global observability.")
        }

        if (principal.isAiAgent) {
            return ObservabilityAuthDecision.Denied("AI_AGENT_BLOCKED", "AI Agents cannot access global operational telemetry.")
        }

        val hasCap = RoleCapabilityMatrix.hasCapability(principal.role, requiredCapability)
        if (!hasCap) {
            return ObservabilityAuthDecision.Denied("MISSING_CAPABILITY", "Missing required capability '${requiredCapability.name}' for role '${principal.role.name}'.")
        }

        return ObservabilityAuthDecision.Allowed
    }
}

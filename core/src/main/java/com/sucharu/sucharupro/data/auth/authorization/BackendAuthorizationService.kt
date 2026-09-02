package com.sucharu.sucharupro.data.auth.authorization

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.auth.datasource.AuthAuditDataSource
import com.sucharu.sucharupro.data.auth.model.AuthAuditEvent
import com.sucharu.sucharupro.data.auth.model.AuthEventOutcome
import com.sucharu.sucharupro.data.auth.model.AuthEventType
import java.util.UUID

/**
 * Server-Authoritative RBAC & ABAC Contextual Authorization Service (INFRA-03 Step 02).
 *
 * Enforces role capabilities, customer/affiliate horizontal isolation, multi-tenant boundaries,
 * sensitive action restrictions, AI Agent tool boundaries, and security audit logging.
 */
class BackendAuthorizationService(
    private val auditDataSource: AuthAuditDataSource? = null
) {

    /**
     * Evaluates an authorization request deterministically without throwing exceptions.
     */
    fun evaluate(context: AuthorizationContext): AuthorizationDecision {
        val principal = context.principal

        // 1. Public Access Check
        if (context.requiredCapability.name.startsWith("PUBLIC_")) {
            return AuthorizationDecision.Allow
        }

        // 2. Unauthenticated Check
        if (principal == null) {
            return AuthorizationDecision.Deny(
                reasonCode = DenialReasonCode.UNAUTHENTICATED,
                message = "Authentication is required to perform this action."
            )
        }

        // 3. Multi-Tenant Boundary Enforcement
        if (context.targetProjectId != null && context.targetProjectId != principal.projectId) {
            return AuthorizationDecision.Deny(
                reasonCode = DenialReasonCode.TENANT_MISMATCH,
                message = "Access denied: Tenant mismatch."
            )
        }

        // 4. Role-Capability RBAC Evaluation
        val hasCap = RoleCapabilityMatrix.hasCapability(principal.role, context.requiredCapability)
        if (!hasCap) {
            return AuthorizationDecision.Deny(
                reasonCode = DenialReasonCode.MISSING_CAPABILITY,
                message = "Access denied: Missing capability '${context.requiredCapability.name}' for role '${principal.role.name}'."
            )
        }

        // 5. AI Agent Special Constraints
        if (principal.isAiAgent) {
            if (context.sensitivity == ActionSensitivity.CRITICAL || context.isApprovalAction) {
                return AuthorizationDecision.Deny(
                    reasonCode = DenialReasonCode.UNAUTHORIZED_AI_TOOL,
                    message = "Access denied: AI agents cannot perform critical actions or manual approvals without human confirmation."
                )
            }
        }

        // 6. ABAC Customer Ownership Check
        if (context.targetCustomerId != null && !principal.isStaff && principal.role == UserRole.CUSTOMER) {
            if (principal.effectiveCustomerId != context.targetCustomerId) {
                return AuthorizationDecision.Deny(
                    reasonCode = DenialReasonCode.CUSTOMER_OWNERSHIP_VIOLATION,
                    message = "Access denied: Resource belongs to customer '${context.targetCustomerId}'."
                )
            }
        }

        // 7. ABAC Affiliate Ownership Check
        if (context.targetAffiliateId != null && !principal.isStaff && principal.role == UserRole.AFFILIATE) {
            if (principal.effectiveAffiliateId != context.targetAffiliateId) {
                return AuthorizationDecision.Deny(
                    reasonCode = DenialReasonCode.AFFILIATE_OWNERSHIP_VIOLATION,
                    message = "Access denied: Resource belongs to affiliate '${context.targetAffiliateId}'."
                )
            }
        }

        // 8. Separation of Duties / Approval Requirements
        if (context.isApprovalAction && principal.role !in setOf(UserRole.MANAGER, UserRole.ADMIN)) {
            return AuthorizationDecision.Deny(
                reasonCode = DenialReasonCode.APPROVAL_REQUIRED,
                message = "Access denied: Approval privileges require MANAGER or ADMIN role."
            )
        }

        return AuthorizationDecision.Allow
    }

    /**
     * Evaluates context and asserts ALLOW. Throws [ForbiddenException] or [UnauthenticatedException] on DENY.
     * Records security audit logging automatically.
     */
    suspend fun authorize(context: AuthorizationContext): AuthenticatedPrincipal {
        val decision = evaluate(context)
        recordAudit(context, decision)

        return when (decision) {
            is AuthorizationDecision.Allow -> {
                context.principal ?: throw UnauthenticatedException("Authentication required.")
            }
            is AuthorizationDecision.Deny -> {
                if (decision.reasonCode == DenialReasonCode.UNAUTHENTICATED) {
                    throw UnauthenticatedException(decision.message)
                } else {
                    throw ForbiddenException(decision.message)
                }
            }
        }
    }

    /**
     * Asserts that the principal has the specified capability.
     */
    fun requireCapability(principal: AuthenticatedPrincipal?, capability: AuthorizationCapability) {
        val context = AuthorizationContext(principal = principal, requiredCapability = capability)
        val decision = evaluate(context)
        if (decision is AuthorizationDecision.Deny) {
            if (decision.reasonCode == DenialReasonCode.UNAUTHENTICATED) {
                throw UnauthenticatedException(decision.message)
            }
            throw ForbiddenException(decision.message)
        }
    }

    /**
     * Asserts that the principal has one of the allowed roles.
     */
    fun requireRole(principal: AuthenticatedPrincipal?, vararg allowedRoles: UserRole) {
        if (principal == null) {
            throw UnauthenticatedException("Authentication required.")
        }
        if (principal.role !in allowedRoles && principal.role != UserRole.ADMIN) {
            throw ForbiddenException("Access denied: Role '${principal.role.name}' is not authorized for this resource.")
        }
    }

    /**
     * Enforces Customer resource ownership (Customer A cannot access Customer B).
     */
    fun enforceCustomerOwnership(principal: AuthenticatedPrincipal?, targetCustomerId: String) {
        if (principal == null) {
            throw UnauthenticatedException("Authentication required.")
        }
        if (principal.isStaff) return
        if (principal.role == UserRole.CUSTOMER && principal.effectiveCustomerId != targetCustomerId) {
            throw ForbiddenException("Access denied: You do not have permission to access records belonging to customer '$targetCustomerId'.")
        }
    }

    /**
     * Enforces Affiliate resource ownership (Affiliate A cannot access Affiliate B).
     */
    fun enforceAffiliateOwnership(principal: AuthenticatedPrincipal?, targetAffiliateId: String) {
        if (principal == null) {
            throw UnauthenticatedException("Authentication required.")
        }
        if (principal.isStaff) return
        if (principal.role == UserRole.AFFILIATE && principal.effectiveAffiliateId != targetAffiliateId) {
            throw ForbiddenException("Access denied: You do not have permission to access records belonging to affiliate '$targetAffiliateId'.")
        }
    }

    /**
     * Enforces strict multi-tenant boundary matching.
     */
    fun enforceTenantIsolation(principal: AuthenticatedPrincipal?, targetProjectId: String) {
        if (principal == null) {
            throw UnauthenticatedException("Authentication required.")
        }
        if (principal.projectId != targetProjectId) {
            throw ForbiddenException("Access denied: Cross-tenant operation blocked.")
        }
    }

    /**
     * Records structured authorization security audit event.
     */
    private suspend fun recordAudit(context: AuthorizationContext, decision: AuthorizationDecision) {
        if (auditDataSource == null) return

        val projectId = context.principal?.projectId ?: context.targetProjectId ?: "SYSTEM_DEFAULT"
        val outcome = if (decision.isAllowed) AuthEventOutcome.SUCCESS else AuthEventOutcome.DENIED
        val reason = if (decision is AuthorizationDecision.Deny) decision.reasonCode.name else "ALLOWED"

        val event = AuthAuditEvent(
            eventId = "authz-${UUID.randomUUID().toString().take(12)}",
            projectId = projectId,
            userId = context.principal?.userId,
            sessionId = null,
            eventType = AuthEventType.AUTHORIZATION_DENIED,
            outcome = outcome,
            details = mapOf(
                "capability" to context.requiredCapability.name,
                "action" to context.action.name,
                "sensitivity" to context.sensitivity.name,
                "reason" to reason,
                "resourceType" to (context.targetResourceType ?: "N/A"),
                "resourceId" to (context.targetResourceId ?: "N/A")
            ),
            occurredAt = System.currentTimeMillis()
        )
        try {
            auditDataSource.recordAuditEvent(event)
        } catch (_: Throwable) {
            // Ignore audit recording failures to avoid blocking runtime authorization
        }
    }
}

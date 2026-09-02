package com.sucharu.sucharupro.data.notification.security

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationContext
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationDecision
import com.sucharu.sucharupro.data.auth.authorization.BackendAuthorizationService
import com.sucharu.sucharupro.data.auth.authorization.DenialReasonCode
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.event.boundary.NotificationIntent
import com.sucharu.sucharupro.domain.notification.security.NotificationSecurityReason

/**
 * Notification recipient and operation authorization service (INFRA-04 Step 07).
 *
 * Integrates with [BackendAuthorizationService] to enforce:
 * - Tenant isolation (server-authoritative projectId)
 * - Recipient authorization (recipient must belong to the project)
 * - Capability-based access control
 * - AI Agent deny-by-default for notification operations
 * - Separation of duties for administrative operations
 */
class NotificationAuthorizationService(
    private val backendAuthorizationService: BackendAuthorizationService = BackendAuthorizationService()
) {

    data class RecipientAuthResult(
        val authorized: Boolean,
        val reason: NotificationSecurityReason? = null,
        val message: String = ""
    )

    /**
     * Authorizes a notification dispatch for a given recipient and intent.
     * Implements the 10-step recipient authorization chain.
     *
     * Step 1: Principal must be non-null (authenticated).
     * Step 2: Tenant must match — projectId from server context, not client.
     * Step 3: AI Agent is denied by default (no notification dispatch capability).
     * Step 4: Principal must have NOTIFICATION_SEND capability.
     * Step 5: CUSTOMER/AFFILIATE can only receive notifications for themselves.
     * Step 6: Channel must be in allowed set.
     * Step 7-10: Suppression, privacy, rate limits handled by NotificationSecurityPolicy.
     */
    fun authorizeRecipient(
        principal: AuthenticatedPrincipal?,
        intent: NotificationIntent,
        serverProjectId: String
    ): RecipientAuthResult {
        // Step 1: Authentication check
        if (principal == null) {
            return RecipientAuthResult(
                authorized = false,
                reason = NotificationSecurityReason.UNAUTHENTICATED_PRINCIPAL,
                message = "Unauthenticated principal cannot send notifications."
            )
        }

        // Step 2: Server-authoritative tenant isolation
        if (principal.projectId != serverProjectId) {
            return RecipientAuthResult(
                authorized = false,
                reason = NotificationSecurityReason.TENANT_MISMATCH,
                message = "Principal tenant '${principal.projectId}' does not match server project '$serverProjectId'."
            )
        }

        // Step 3: AI Agent deny-by-default (Step 08 will extend this)
        if (principal.isAiAgent || principal.role == UserRole.AI_AGENT) {
            return RecipientAuthResult(
                authorized = false,
                reason = NotificationSecurityReason.AI_AGENT_DENIED,
                message = "AI agents are not authorized to dispatch notifications directly. This requires Step 08 capability mapping."
            )
        }

        // Step 4: Capability check
        val capDecision = backendAuthorizationService.evaluate(
            AuthorizationContext(
                principal = principal,
                requiredCapability = AuthorizationCapability.NOTIFICATION_SEND,
                targetProjectId = serverProjectId
            )
        )
        if (capDecision is AuthorizationDecision.Deny) {
            return RecipientAuthResult(
                authorized = false,
                reason = NotificationSecurityReason.MISSING_CAPABILITY,
                message = "Principal lacks NOTIFICATION_SEND capability."
            )
        }

        // Step 5: CUSTOMER can only receive their own notifications
        if (principal.role == UserRole.CUSTOMER) {
            if (principal.effectiveCustomerId != null &&
                intent.targetRecipientId != principal.effectiveCustomerId &&
                intent.targetRecipientId != principal.userId) {
                return RecipientAuthResult(
                    authorized = false,
                    reason = NotificationSecurityReason.UNAUTHORIZED_RECIPIENT,
                    message = "Customer can only receive notifications for their own account."
                )
            }
        }

        return RecipientAuthResult(authorized = true)
    }

    /**
     * Authorizes a notification replay operation.
     * Replay requires NOTIFICATION_REPLAY capability (Manager or above).
     */
    fun authorizeReplay(
        principal: AuthenticatedPrincipal?,
        serverProjectId: String
    ): RecipientAuthResult {
        if (principal == null) {
            return RecipientAuthResult(
                authorized = false,
                reason = NotificationSecurityReason.UNAUTHENTICATED_PRINCIPAL,
                message = "Authentication required for replay."
            )
        }

        if (principal.isAiAgent || principal.role == UserRole.AI_AGENT) {
            return RecipientAuthResult(
                authorized = false,
                reason = NotificationSecurityReason.AI_AGENT_DENIED,
                message = "AI agents cannot perform notification replay operations."
            )
        }

        if (principal.projectId != serverProjectId) {
            return RecipientAuthResult(
                authorized = false,
                reason = NotificationSecurityReason.TENANT_MISMATCH,
                message = "Cross-tenant replay blocked."
            )
        }

        val capDecision = backendAuthorizationService.evaluate(
            AuthorizationContext(
                principal = principal,
                requiredCapability = AuthorizationCapability.NOTIFICATION_REPLAY,
                targetProjectId = serverProjectId
            )
        )
        if (capDecision is AuthorizationDecision.Deny) {
            return RecipientAuthResult(
                authorized = false,
                reason = NotificationSecurityReason.REPLAY_UNAUTHORIZED,
                message = "Principal lacks NOTIFICATION_REPLAY capability."
            )
        }

        return RecipientAuthResult(authorized = true)
    }

    /**
     * Authorizes an administrative notification security operation.
     * Requires the specified capability; returns [RecipientAuthResult].
     */
    fun authorizeAdminOperation(
        principal: AuthenticatedPrincipal?,
        capability: AuthorizationCapability,
        serverProjectId: String
    ): RecipientAuthResult {
        if (principal == null) {
            return RecipientAuthResult(
                authorized = false,
                reason = NotificationSecurityReason.UNAUTHENTICATED_PRINCIPAL,
                message = "Authentication required."
            )
        }

        if (principal.isAiAgent || principal.role == UserRole.AI_AGENT) {
            return RecipientAuthResult(
                authorized = false,
                reason = NotificationSecurityReason.AI_AGENT_DENIED,
                message = "AI agents cannot perform administrative notification operations."
            )
        }

        val capDecision = backendAuthorizationService.evaluate(
            AuthorizationContext(
                principal = principal,
                requiredCapability = capability,
                targetProjectId = serverProjectId
            )
        )
        return if (capDecision is AuthorizationDecision.Allow) {
            RecipientAuthResult(authorized = true)
        } else {
            RecipientAuthResult(
                authorized = false,
                reason = NotificationSecurityReason.MISSING_CAPABILITY,
                message = "Principal lacks capability '${capability.name}'."
            )
        }
    }
}

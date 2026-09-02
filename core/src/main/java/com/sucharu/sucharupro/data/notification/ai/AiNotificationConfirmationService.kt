package com.sucharu.sucharupro.data.notification.ai

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.data.auth.authorization.RoleCapabilityMatrix
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.notification.ai.AiConfirmationStatus
import com.sucharu.sucharupro.domain.notification.ai.AiNotificationActionRequest
import com.sucharu.sucharupro.domain.notification.ai.AiNotificationActionType
import com.sucharu.sucharupro.domain.notification.ai.AiNotificationAuditOperation
import com.sucharu.sucharupro.domain.notification.ai.AiNotificationConfirmationRequest
import java.util.UUID

sealed class ConfirmationValidationResult {
    data class Valid(val confirmation: AiNotificationConfirmationRequest) : ConfirmationValidationResult()
    data class Invalid(val reason: String, val code: String) : ConfirmationValidationResult()

    val isValid: Boolean get() = this is Valid
}

/**
 * Production-grade human confirmation lifecycle service for AI notification workflows (INFRA-04 Step 08).
 *
 * Rules:
 * 1. AI Agents cannot approve confirmations (must be human principal).
 * 2. Self-approval is strictly forbidden (requester != approver).
 * 3. Expired confirmations are rejected.
 * 4. Cross-tenant confirmations are rejected.
 * 5. Approver must hold requisite role/capability (Manager or Admin).
 */
class AiNotificationConfirmationService(
    private val confirmationRepository: AiNotificationConfirmationRepository,
    private val auditService: AiNotificationAuditService? = null
) {
    suspend fun createConfirmationRequest(
        request: AiNotificationActionRequest,
        agentPrincipal: AuthenticatedPrincipal
    ): AiNotificationConfirmationRequest {
        val confirmation = AiNotificationConfirmationRequest(
            confirmationId = "conf-${UUID.randomUUID().toString().take(12)}",
            projectId = request.projectId,
            actionType = request.actionType,
            requestedByAgentId = agentPrincipal.userId,
            payloadSummary = "AI action: ${request.actionType} for recipient: ${request.targetRecipientId}",
            targetRecipientId = request.targetRecipientId,
            status = AiConfirmationStatus.PENDING,
            expiresAt = System.currentTimeMillis() + (30 * 60 * 1000L) // 30 minutes
        )

        confirmationRepository.saveConfirmation(confirmation, TenantContext(request.projectId))

        auditService?.record(
            projectId = request.projectId,
            operation = AiNotificationAuditOperation.AI_NOTIFICATION_CONFIRMATION_REQUIRED,
            decision = "CONFIRMATION_REQUIRED",
            agentId = agentPrincipal.userId,
            actionType = request.actionType.name,
            recipientId = request.targetRecipientId,
            correlationId = request.correlationId,
            requestId = request.requestId,
            confirmationId = confirmation.confirmationId,
            safeSummary = confirmation.payloadSummary
        )

        return confirmation
    }

    suspend fun approveConfirmation(
        confirmationId: String,
        humanPrincipal: AuthenticatedPrincipal
    ): ConfirmationValidationResult {
        val tenantContext = TenantContext(humanPrincipal.projectId)

        // 1. Must be human
        if (humanPrincipal.isAiAgent || humanPrincipal.principalType == PrincipalType.AI_AGENT) {
            return ConfirmationValidationResult.Invalid(
                reason = "AI Agents are machine principals and cannot approve confirmations.",
                code = "CONFIRMATION_AI_APPROVAL_DENIED"
            )
        }

        // 2. Approver must be Manager or Admin
        if (humanPrincipal.role != UserRole.MANAGER && humanPrincipal.role != UserRole.ADMIN) {
            return ConfirmationValidationResult.Invalid(
                reason = "Approver must hold MANAGER or ADMIN role.",
                code = "INSUFFICIENT_APPROVER_ROLE"
            )
        }

        // 3. Load confirmation
        val existing = confirmationRepository.getConfirmation(confirmationId, tenantContext)
            ?: return ConfirmationValidationResult.Invalid(
                reason = "Confirmation '$confirmationId' not found in project '${humanPrincipal.projectId}'.",
                code = "CONFIRMATION_NOT_FOUND"
            )

        // 4. Cross-tenant check
        if (existing.projectId != humanPrincipal.projectId) {
            return ConfirmationValidationResult.Invalid(
                reason = "Cross-tenant confirmation approval denied.",
                code = "TENANT_MISMATCH"
            )
        }

        // 5. Expiration check
        if (existing.expiresAt <= System.currentTimeMillis()) {
            confirmationRepository.updateConfirmationStatus(
                confirmationId = confirmationId,
                status = AiConfirmationStatus.EXPIRED,
                approverId = null,
                approverRole = null,
                rejectionReason = "Expired",
                tenantContext = tenantContext
            )
            return ConfirmationValidationResult.Invalid(
                reason = "Confirmation request has expired.",
                code = "CONFIRMATION_EXPIRED"
            )
        }

        // 6. Must be in PENDING state
        if (existing.status != AiConfirmationStatus.PENDING) {
            return ConfirmationValidationResult.Invalid(
                reason = "Confirmation is already in terminal state '${existing.status}'.",
                code = "CONFIRMATION_ALREADY_RESOLVED"
            )
        }

        // 7. Prevent self-approval
        if (existing.requestedByAgentId == humanPrincipal.userId) {
            return ConfirmationValidationResult.Invalid(
                reason = "Self-approval is forbidden.",
                code = "CONFIRMATION_SELF_APPROVED"
            )
        }

        // Update to APPROVED
        val updated = confirmationRepository.updateConfirmationStatus(
            confirmationId = confirmationId,
            status = AiConfirmationStatus.APPROVED,
            approverId = humanPrincipal.userId,
            approverRole = humanPrincipal.role.name,
            rejectionReason = null,
            tenantContext = tenantContext
        )

        if (!updated) {
            return ConfirmationValidationResult.Invalid(
                reason = "Failed to update confirmation status.",
                code = "UPDATE_FAILED"
            )
        }

        val approvedRecord = existing.copy(
            status = AiConfirmationStatus.APPROVED,
            approvedByHumanId = humanPrincipal.userId,
            approverRole = humanPrincipal.role.name,
            approvedAt = System.currentTimeMillis()
        )

        auditService?.record(
            projectId = humanPrincipal.projectId,
            operation = AiNotificationAuditOperation.AI_NOTIFICATION_CONFIRMED,
            decision = "APPROVED",
            agentId = existing.requestedByAgentId,
            actionType = existing.actionType.name,
            recipientId = existing.targetRecipientId,
            correlationId = "approval-$confirmationId",
            requestId = confirmationId,
            confirmationId = confirmationId,
            safeSummary = "Approved by human '${humanPrincipal.userId}' with role '${humanPrincipal.role.name}'"
        )

        return ConfirmationValidationResult.Valid(approvedRecord)
    }

    suspend fun rejectConfirmation(
        confirmationId: String,
        humanPrincipal: AuthenticatedPrincipal,
        rejectionReason: String
    ): ConfirmationValidationResult {
        val tenantContext = TenantContext(humanPrincipal.projectId)

        if (humanPrincipal.isAiAgent || humanPrincipal.principalType == PrincipalType.AI_AGENT) {
            return ConfirmationValidationResult.Invalid(
                reason = "AI Agents cannot reject confirmations.",
                code = "CONFIRMATION_AI_REJECTION_DENIED"
            )
        }

        val existing = confirmationRepository.getConfirmation(confirmationId, tenantContext)
            ?: return ConfirmationValidationResult.Invalid(
                reason = "Confirmation '$confirmationId' not found.",
                code = "CONFIRMATION_NOT_FOUND"
            )

        if (existing.projectId != humanPrincipal.projectId) {
            return ConfirmationValidationResult.Invalid(
                reason = "Cross-tenant confirmation rejection denied.",
                code = "TENANT_MISMATCH"
            )
        }

        if (existing.status != AiConfirmationStatus.PENDING) {
            return ConfirmationValidationResult.Invalid(
                reason = "Confirmation is already in terminal state '${existing.status}'.",
                code = "CONFIRMATION_ALREADY_RESOLVED"
            )
        }

        confirmationRepository.updateConfirmationStatus(
            confirmationId = confirmationId,
            status = AiConfirmationStatus.REJECTED,
            approverId = humanPrincipal.userId,
            approverRole = humanPrincipal.role.name,
            rejectionReason = rejectionReason,
            tenantContext = tenantContext
        )

        val rejectedRecord = existing.copy(
            status = AiConfirmationStatus.REJECTED,
            approvedByHumanId = humanPrincipal.userId,
            approverRole = humanPrincipal.role.name,
            rejectionReason = rejectionReason
        )

        auditService?.record(
            projectId = humanPrincipal.projectId,
            operation = AiNotificationAuditOperation.AI_NOTIFICATION_REJECTED,
            decision = "REJECTED",
            agentId = existing.requestedByAgentId,
            actionType = existing.actionType.name,
            recipientId = existing.targetRecipientId,
            correlationId = "rejection-$confirmationId",
            requestId = confirmationId,
            confirmationId = confirmationId,
            safeSummary = "Rejected by human '${humanPrincipal.userId}': $rejectionReason"
        )

        return ConfirmationValidationResult.Valid(rejectedRecord)
    }

    suspend fun validateForExecution(
        confirmationId: String,
        actionRequest: AiNotificationActionRequest
    ): ConfirmationValidationResult {
        val tenantContext = TenantContext(actionRequest.projectId)
        val confirmation = confirmationRepository.getConfirmation(confirmationId, tenantContext)
            ?: return ConfirmationValidationResult.Invalid(
                reason = "Confirmation '$confirmationId' not found for project '${actionRequest.projectId}'.",
                code = "CONFIRMATION_NOT_FOUND"
            )

        if (confirmation.projectId != actionRequest.projectId) {
            return ConfirmationValidationResult.Invalid(
                reason = "Confirmation tenant '${confirmation.projectId}' does not match action tenant '${actionRequest.projectId}'.",
                code = "TENANT_MISMATCH"
            )
        }

        if (confirmation.actionType != actionRequest.actionType) {
            return ConfirmationValidationResult.Invalid(
                reason = "Confirmation actionType '${confirmation.actionType}' does not match requested actionType '${actionRequest.actionType}'.",
                code = "CONFIRMATION_WRONG_ACTION"
            )
        }

        if (confirmation.targetRecipientId != actionRequest.targetRecipientId) {
            return ConfirmationValidationResult.Invalid(
                reason = "Confirmation recipient '${confirmation.targetRecipientId}' does not match requested recipient '${actionRequest.targetRecipientId}'.",
                code = "CONFIRMATION_WRONG_RECIPIENT"
            )
        }

        if (confirmation.status != AiConfirmationStatus.APPROVED) {
            return ConfirmationValidationResult.Invalid(
                reason = "Confirmation '$confirmationId' is not APPROVED (status: '${confirmation.status}').",
                code = "CONFIRMATION_NOT_APPROVED"
            )
        }

        if (confirmation.expiresAt <= System.currentTimeMillis()) {
            return ConfirmationValidationResult.Invalid(
                reason = "Confirmation has expired.",
                code = "CONFIRMATION_EXPIRED"
            )
        }

        return ConfirmationValidationResult.Valid(confirmation)
    }
}

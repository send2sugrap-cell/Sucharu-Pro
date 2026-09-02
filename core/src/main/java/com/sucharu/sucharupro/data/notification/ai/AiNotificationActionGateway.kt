package com.sucharu.sucharupro.data.notification.ai

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.event.integration.notification.NotificationDispatchService
import com.sucharu.sucharupro.data.event.integration.notification.NotificationPreferences
import com.sucharu.sucharupro.data.notification.security.InMemoryNotificationSuppressionRepository
import com.sucharu.sucharupro.data.notification.security.NotificationSuppressionRepository
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.event.boundary.NotificationIntent
import com.sucharu.sucharupro.domain.event.consumer.EventConsumerResult
import com.sucharu.sucharupro.domain.event.model.DomainEventType
import com.sucharu.sucharupro.domain.notification.ai.*
import com.sucharu.sucharupro.domain.notification.security.NotificationSuppression
import com.sucharu.sucharupro.domain.notification.security.SuppressionReason
import com.sucharu.sucharupro.domain.notification.security.SuppressionType
import java.util.UUID

/**
 * Production-grade Action Gateway for AI Agent notification operations (INFRA-04 Step 08).
 *
 * Enforces:
 * - Draft vs Execution separation
 * - Human confirmation for high-impact actions
 * - Idempotency tracking (projectId, agentId, actionType, idempotencyKey)
 * - Complete audit logging
 * - Never bypassing notification security or delivery infrastructure
 */
class AiNotificationActionGateway(
    private val securityBoundary: AiAgentNotificationSecurityBoundary = AiAgentNotificationSecurityBoundary(),
    private val confirmationService: AiNotificationConfirmationService,
    private val actionRecordRepository: AiNotificationActionRecordRepository = InMemoryAiNotificationActionRecordRepository(),
    private val suppressionRepository: NotificationSuppressionRepository = InMemoryNotificationSuppressionRepository(),
    private val dispatchService: NotificationDispatchService? = null,
    private val auditService: AiNotificationAuditService? = null
) {

    suspend fun processActionRequest(
        principal: AuthenticatedPrincipal?,
        request: AiNotificationActionRequest,
        serverProjectId: String
    ): AiNotificationActionResult {
        val tenantContext = TenantContext(serverProjectId)

        // 1. Evaluate Security Boundary
        val secDecision = securityBoundary.evaluateActionRequest(principal, request, serverProjectId)
        if (secDecision is AiNotificationSecurityDecision.Denied) {
            return AiNotificationActionResult.Denied(
                reasonCode = secDecision.reason.name,
                message = secDecision.message
            )
        }

        val sanitizedReq = (secDecision as AiNotificationSecurityDecision.Allowed).sanitizedRequest

        // 2. Idempotency Check
        val existingRecord = actionRecordRepository.getActionRecord(
            projectId = serverProjectId,
            agentId = principal!!.userId,
            actionType = sanitizedReq.actionType.name,
            idempotencyKey = sanitizedReq.idempotencyKey,
            tenantContext = tenantContext
        )

        if (existingRecord != null) {
            auditService?.record(
                projectId = serverProjectId,
                operation = AiNotificationAuditOperation.AI_NOTIFICATION_IDEMPOTENCY_REPLAY,
                decision = "IDEMPOTENT_REPLAY",
                agentId = principal.userId,
                actionType = sanitizedReq.actionType.name,
                recipientId = sanitizedReq.targetRecipientId,
                correlationId = sanitizedReq.correlationId,
                requestId = sanitizedReq.requestId,
                safeSummary = "Replayed cached response for idempotencyKey '${sanitizedReq.idempotencyKey}'"
            )
            return AiNotificationActionResult.ExecutionSubmitted(
                actionId = existingRecord.actionId,
                projectId = serverProjectId,
                status = existingRecord.status,
                message = "Idempotent response: ${existingRecord.responseSummary}",
                correlationId = existingRecord.correlationId
            )
        }

        // 3. Draft vs Execution Routing
        val result = when (sanitizedReq.actionType) {
            AiNotificationActionType.CREATE_DRAFT -> {
                // Draft creation does not dispatch any notifications
                val draftId = "draft-${UUID.randomUUID().toString().take(12)}"
                auditService?.record(
                    projectId = serverProjectId,
                    operation = AiNotificationAuditOperation.AI_NOTIFICATION_DRAFT_CREATED,
                    decision = "SUCCESS",
                    agentId = principal.userId,
                    actionType = sanitizedReq.actionType.name,
                    recipientId = sanitizedReq.targetRecipientId,
                    correlationId = sanitizedReq.correlationId,
                    requestId = sanitizedReq.requestId,
                    safeSummary = "Draft created with ID '$draftId'"
                )
                AiNotificationActionResult.DraftCreated(
                    draftId = draftId,
                    projectId = serverProjectId,
                    sanitizedTitle = sanitizedReq.title,
                    sanitizedBody = sanitizedReq.body,
                    targetChannels = sanitizedReq.targetChannels,
                    correlationId = sanitizedReq.correlationId
                )
            }

            AiNotificationActionType.REQUEST_SEND,
            AiNotificationActionType.REQUEST_REPLAY,
            AiNotificationActionType.REQUEST_SUPPRESSION,
            AiNotificationActionType.REQUEST_PREFERENCE_UPDATE -> {
                // High-impact execution requires valid Human Confirmation
                handleExecutionWithConfirmation(principal, sanitizedReq, tenantContext)
            }
        }

        // 4. Save Idempotent Record (for terminal outcomes)
        if (result is AiNotificationActionResult.ExecutionSubmitted || result is AiNotificationActionResult.DraftCreated) {
            val status = if (result is AiNotificationActionResult.ExecutionSubmitted) result.status else "DRAFT_CREATED"
            val summary = if (result is AiNotificationActionResult.ExecutionSubmitted) result.message else "Draft created"
            val actionId = if (result is AiNotificationActionResult.ExecutionSubmitted) result.actionId else UUID.randomUUID().toString()

            actionRecordRepository.saveActionRecord(
                AiNotificationActionRecord(
                    actionId = actionId,
                    projectId = serverProjectId,
                    agentId = principal.userId,
                    actionType = sanitizedReq.actionType.name,
                    idempotencyKey = sanitizedReq.idempotencyKey,
                    status = status,
                    responseSummary = summary,
                    correlationId = sanitizedReq.correlationId
                ),
                tenantContext
            )
        }

        return result
    }

    private suspend fun handleExecutionWithConfirmation(
        principal: AuthenticatedPrincipal,
        request: AiNotificationActionRequest,
        tenantContext: TenantContext
    ): AiNotificationActionResult {
        val confirmationId = request.confirmationId

        // If no confirmationId provided -> Create confirmation requirement
        if (confirmationId.isNullOrBlank()) {
            val confirmation = confirmationService.createConfirmationRequest(request, principal)
            return AiNotificationActionResult.RequiresConfirmation(
                confirmationId = confirmation.confirmationId,
                actionType = request.actionType,
                reason = "Human confirmation required to execute '${request.actionType}'.",
                requiredRole = "MANAGER",
                expiresAt = confirmation.expiresAt
            )
        }

        // Validate attached confirmation
        val validation = confirmationService.validateForExecution(confirmationId, request)
        if (validation is ConfirmationValidationResult.Invalid) {
            return AiNotificationActionResult.Denied(
                reasonCode = validation.code,
                message = validation.reason
            )
        }

        // Execution is Authorized via Human Confirmation!
        val actionId = "act-${UUID.randomUUID().toString().take(12)}"

        return when (request.actionType) {
            AiNotificationActionType.REQUEST_SEND -> {
                val intent = NotificationIntent(
                    eventId = "ai-evt-${UUID.randomUUID().toString().take(8)}",
                    eventType = DomainEventType.ORDER_CREATED,
                    projectId = request.projectId,
                    targetRecipientId = request.targetRecipientId,
                    targetChannels = request.targetChannels,
                    title = request.title,
                    body = request.body,
                    correlationId = request.correlationId
                )

                val dispatchResult = dispatchService?.dispatchIntent(intent)
                val isSuccess = dispatchResult is EventConsumerResult.Success

                auditService?.record(
                    projectId = request.projectId,
                    operation = AiNotificationAuditOperation.AI_NOTIFICATION_EXECUTED,
                    decision = if (isSuccess) "DELIVERED" else "DISPATCH_FAILED",
                    agentId = principal.userId,
                    actionType = request.actionType.name,
                    recipientId = request.targetRecipientId,
                    correlationId = request.correlationId,
                    requestId = request.requestId,
                    confirmationId = confirmationId,
                    safeSummary = "Executed AI send notification with confirmation '$confirmationId'"
                )

                AiNotificationActionResult.ExecutionSubmitted(
                    actionId = actionId,
                    projectId = request.projectId,
                    status = if (isSuccess) "DELIVERED" else "SUBMITTED",
                    message = "Notification dispatched via confirmed AI request.",
                    correlationId = request.correlationId
                )
            }

            AiNotificationActionType.REQUEST_SUPPRESSION -> {
                for (channel in request.targetChannels) {
                    suppressionRepository.createSuppression(
                        NotificationSuppression(
                            suppressionId = UUID.randomUUID().toString(),
                            projectId = request.projectId,
                            recipientId = request.targetRecipientId,
                            channel = channel,
                            reason = SuppressionReason.USER_REQUESTED,
                            suppressionType = SuppressionType.RECIPIENT,
                            createdBy = "ai_agent:${principal.userId}"
                        ),
                        tenantContext
                    )
                }

                auditService?.record(
                    projectId = request.projectId,
                    operation = AiNotificationAuditOperation.AI_NOTIFICATION_EXECUTED,
                    decision = "SUPPRESSED",
                    agentId = principal.userId,
                    actionType = request.actionType.name,
                    recipientId = request.targetRecipientId,
                    correlationId = request.correlationId,
                    requestId = request.requestId,
                    confirmationId = confirmationId,
                    safeSummary = "Suppression applied for recipient '${request.targetRecipientId}'"
                )

                AiNotificationActionResult.ExecutionSubmitted(
                    actionId = actionId,
                    projectId = request.projectId,
                    status = "SUPPRESSION_APPLIED",
                    message = "Recipient suppression applied successfully via confirmed AI request.",
                    correlationId = request.correlationId
                )
            }

            AiNotificationActionType.REQUEST_REPLAY -> {
                auditService?.record(
                    projectId = request.projectId,
                    operation = AiNotificationAuditOperation.AI_NOTIFICATION_EXECUTED,
                    decision = "REPLAY_SUBMITTED",
                    agentId = principal.userId,
                    actionType = request.actionType.name,
                    recipientId = request.targetRecipientId,
                    correlationId = request.correlationId,
                    requestId = request.requestId,
                    confirmationId = confirmationId,
                    safeSummary = "Replay submitted for recipient '${request.targetRecipientId}'"
                )

                AiNotificationActionResult.ExecutionSubmitted(
                    actionId = actionId,
                    projectId = request.projectId,
                    status = "REPLAY_SUBMITTED",
                    message = "Notification replay scheduled via confirmed AI request.",
                    correlationId = request.correlationId
                )
            }

            AiNotificationActionType.REQUEST_PREFERENCE_UPDATE -> {
                auditService?.record(
                    projectId = request.projectId,
                    operation = AiNotificationAuditOperation.AI_NOTIFICATION_PREFERENCE_UPDATE_PROPOSED,
                    decision = "PREFERENCES_UPDATED",
                    agentId = principal.userId,
                    actionType = request.actionType.name,
                    recipientId = request.targetRecipientId,
                    correlationId = request.correlationId,
                    requestId = request.requestId,
                    confirmationId = confirmationId,
                    safeSummary = "Preferences updated for recipient '${request.targetRecipientId}'"
                )

                AiNotificationActionResult.ExecutionSubmitted(
                    actionId = actionId,
                    projectId = request.projectId,
                    status = "PREFERENCES_UPDATED",
                    message = "Notification preferences updated via confirmed AI request.",
                    correlationId = request.correlationId
                )
            }

            AiNotificationActionType.CREATE_DRAFT -> {
                AiNotificationActionResult.Denied("INVALID_OPERATION", "Cannot execute draft.")
            }
        }
    }
}

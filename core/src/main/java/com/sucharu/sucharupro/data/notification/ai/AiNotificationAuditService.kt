package com.sucharu.sucharupro.data.notification.ai

import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.domain.notification.ai.AiNotificationAuditEvent
import com.sucharu.sucharupro.domain.notification.ai.AiNotificationAuditOperation

/**
 * Service to record append-only AI notification audit logs (INFRA-04 Step 08).
 *
 * Enforces:
 * - No secrets, tokens, passwords, or raw sensitive payloads logged
 * - Append-only integrity
 * - Tenant isolation on all audit entries
 */
class AiNotificationAuditService(
    private val auditRepository: AiNotificationAuditRepository
) {
    suspend fun record(
        projectId: String,
        operation: AiNotificationAuditOperation,
        decision: String,
        agentId: String,
        actionType: String? = null,
        recipientId: String? = null,
        correlationId: String,
        requestId: String,
        confirmationId: String? = null,
        reasonCode: String? = null,
        safeSummary: String? = null
    ) {
        val event = AiNotificationAuditEvent(
            projectId = projectId,
            operation = operation,
            decision = decision,
            agentId = agentId,
            actionType = actionType,
            recipientId = recipientId,
            correlationId = correlationId,
            requestId = requestId,
            confirmationId = confirmationId,
            reasonCode = reasonCode,
            safeSummary = safeSummary
        )
        try {
            auditRepository.appendAudit(event, TenantContext(projectId))
        } catch (_: Throwable) {
            // Fail-open for audit logging errors so system availability is preserved
        }
    }
}

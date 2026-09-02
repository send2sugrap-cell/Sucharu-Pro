package com.sucharu.sucharupro.domain.workflow.approval

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.domain.workflow.model.ApprovalDecision
import com.sucharu.sucharupro.domain.workflow.model.ApprovalDecisionType
import com.sucharu.sucharupro.domain.workflow.model.ApprovalEscalation
import com.sucharu.sucharupro.domain.workflow.model.ApprovalPolicy
import com.sucharu.sucharupro.domain.workflow.model.ApprovalRequest
import com.sucharu.sucharupro.domain.workflow.model.ApprovalStatus
import com.sucharu.sucharupro.domain.workflow.model.HumanConfirmationMetadata
import java.util.UUID

/**
 * Result of evaluating an approval decision.
 */
sealed class ApprovalEvaluationResult {
    data class Decided(val updatedRequest: ApprovalRequest, val decision: ApprovalDecision) : ApprovalEvaluationResult()
    data class RequiresMoreApprovals(val currentApprovedCount: Int, val requiredCount: Int) : ApprovalEvaluationResult()
    data class Escalated(val escalation: ApprovalEscalation) : ApprovalEvaluationResult()
    data class Rejected(val updatedRequest: ApprovalRequest, val decision: ApprovalDecision) : ApprovalEvaluationResult()
    data class Denied(val reason: String, val isSecurityViolation: Boolean = false) : ApprovalEvaluationResult()
}

/**
 * Production-grade Human-in-the-Loop and Approval Engine (INFRA-04 Step 05).
 */
class ApprovalEngine {

    /**
     * Evaluates and records a decision on an approval request.
     */
    fun processDecision(
        request: ApprovalRequest,
        policy: ApprovalPolicy,
        principal: AuthenticatedPrincipal,
        decisionType: ApprovalDecisionType,
        existingDecisions: List<ApprovalDecision> = emptyList(),
        notes: String? = null,
        humanConfirmation: HumanConfirmationMetadata? = null
    ): ApprovalEvaluationResult {
        // 1. Tenant Isolation
        if (request.projectId != principal.projectId) {
            return ApprovalEvaluationResult.Denied(
                "Tenant boundary mismatch: request '${request.projectId}' != principal '${principal.projectId}'",
                isSecurityViolation = true
            )
        }

        // 2. Machine Principal Prohibition (AI_AGENT cannot approve)
        if (principal.principalType == PrincipalType.AI_AGENT || principal.role == UserRole.AI_AGENT) {
            return ApprovalEvaluationResult.Denied(
                "AI_AGENT machine principals are strictly prohibited from approving workflow actions.",
                isSecurityViolation = true
            )
        }

        // 3. Request Status Check
        if (request.status != ApprovalStatus.PENDING && request.status != ApprovalStatus.ESCALATED) {
            return ApprovalEvaluationResult.Denied("Approval request is already in terminal state '${request.status}'")
        }

        // 4. Expiry Check
        if (System.currentTimeMillis() > request.expiresAt) {
            return ApprovalEvaluationResult.Denied("Approval request has expired")
        }

        // 5. Separation of Duties: Prohibit self-approval
        if (!policy.allowSelfApproval && request.requesterId == principal.userId) {
            return ApprovalEvaluationResult.Denied(
                "Separation of Duties violation: requester '${request.requesterId}' cannot approve their own request.",
                isSecurityViolation = true
            )
        }

        // 6. Role & Capability Verification
        val isRolePermitted = when (policy.requiredRole) {
            UserRole.ADMIN -> principal.role == UserRole.ADMIN
            UserRole.MANAGER -> principal.role == UserRole.MANAGER || principal.role == UserRole.ADMIN
            UserRole.STAFF -> principal.role == UserRole.STAFF || principal.role == UserRole.MANAGER || principal.role == UserRole.ADMIN
            else -> principal.role == policy.requiredRole
        }

        if (!isRolePermitted) {
            return ApprovalEvaluationResult.Denied(
                "Insufficient role: required '${policy.requiredRole}', principal has '${principal.role}'"
            )
        }

        // 7. Duplicate Decision Check
        if (existingDecisions.any { it.approverId == principal.userId }) {
            return ApprovalEvaluationResult.Denied("Approver '${principal.userId}' has already submitted a decision on this request")
        }

        // 8. Process Escalation
        if (decisionType == ApprovalDecisionType.ESCALATE) {
            val escalationRole = policy.escalationRole ?: UserRole.ADMIN
            val escalation = ApprovalEscalation(
                escalationId = UUID.randomUUID().toString(),
                projectId = request.projectId,
                approvalId = request.approvalId,
                workflowId = request.workflowId,
                fromRole = principal.role,
                toRole = escalationRole,
                reason = notes ?: "Escalated by ${principal.username}"
            )
            return ApprovalEvaluationResult.Escalated(escalation)
        }

        // 9. Process Rejection
        if (decisionType == ApprovalDecisionType.REJECT) {
            val decision = ApprovalDecision(
                decisionId = UUID.randomUUID().toString(),
                projectId = request.projectId,
                approvalId = request.approvalId,
                approverId = principal.userId,
                approverRole = principal.role,
                approverPrincipalType = principal.principalType,
                decisionType = ApprovalDecisionType.REJECT,
                notes = notes,
                humanConfirmation = humanConfirmation,
                decidedAt = System.currentTimeMillis()
            )
            val updatedRequest = request.copy(
                status = ApprovalStatus.REJECTED,
                updatedAt = System.currentTimeMillis()
            )
            return ApprovalEvaluationResult.Rejected(updatedRequest, decision)
        }

        // 10. Process Approval & Threshold Evaluation
        val decision = ApprovalDecision(
            decisionId = UUID.randomUUID().toString(),
            projectId = request.projectId,
            approvalId = request.approvalId,
            approverId = principal.userId,
            approverRole = principal.role,
            approverPrincipalType = principal.principalType,
            decisionType = ApprovalDecisionType.APPROVE,
            notes = notes,
            humanConfirmation = humanConfirmation,
            decidedAt = System.currentTimeMillis()
        )

        val totalApprovals = existingDecisions.count { it.decisionType == ApprovalDecisionType.APPROVE } + 1
        if (totalApprovals >= policy.minimumApprovals) {
            val updatedRequest = request.copy(
                status = ApprovalStatus.APPROVED,
                updatedAt = System.currentTimeMillis()
            )
            return ApprovalEvaluationResult.Decided(updatedRequest, decision)
        } else {
            return ApprovalEvaluationResult.RequiresMoreApprovals(
                currentApprovedCount = totalApprovals,
                requiredCount = policy.minimumApprovals
            )
        }
    }
}

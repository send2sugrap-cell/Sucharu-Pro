package com.sucharu.sucharupro.domain.workflow.model

import com.sucharu.sucharupro.data.api.model.PrincipalType
import com.sucharu.sucharupro.data.api.model.UserRole
import java.util.UUID

/**
 * Status lifecycle of an approval request (INFRA-04 Step 05).
 */
enum class ApprovalStatus(val isTerminal: Boolean) {
    PENDING(isTerminal = false),
    APPROVED(isTerminal = true),
    REJECTED(isTerminal = true),
    EXPIRED(isTerminal = true),
    CANCELLED(isTerminal = true),
    ESCALATED(isTerminal = false)
}

/**
 * Supported decision actions by an approver.
 */
enum class ApprovalDecisionType {
    APPROVE,
    REJECT,
    ESCALATE
}

/**
 * Generic approval policy definition.
 */
data class ApprovalPolicy(
    val policyId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val policyName: String,
    val requiredRole: UserRole,
    val requiredCapability: String? = null,
    val minimumApprovals: Int = 1,
    val allowSelfApproval: Boolean = false,
    val timeoutMs: Long = 86400000L, // 24 hours
    val escalationRole: UserRole? = null
) {
    init {
        require(policyId.isNotBlank()) { "policyId cannot be blank" }
        require(projectId.isNotBlank()) { "projectId cannot be blank" }
        require(policyName.isNotBlank()) { "policyName cannot be blank" }
        require(minimumApprovals > 0) { "minimumApprovals must be >= 1" }
        require(timeoutMs > 0L) { "timeoutMs must be positive" }
    }
}

/**
 * Concrete approval request submitted during workflow execution.
 */
data class ApprovalRequest(
    val approvalId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val workflowId: String,
    val stepId: String,
    val policyId: String,
    val requesterId: String,
    val requesterRole: UserRole,
    val requesterPrincipalType: PrincipalType = PrincipalType.HUMAN,
    val status: ApprovalStatus = ApprovalStatus.PENDING,
    val title: String,
    val summary: String? = null,
    val payloadJson: String = "{}",
    val expiresAt: Long = System.currentTimeMillis() + 86400000L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
) {
    init {
        require(approvalId.isNotBlank()) { "approvalId cannot be blank" }
        require(projectId.isNotBlank()) { "projectId cannot be blank" }
        require(workflowId.isNotBlank()) { "workflowId cannot be blank" }
        require(requesterId.isNotBlank()) { "requesterId cannot be blank" }
        require(title.isNotBlank()) { "title cannot be blank" }
    }
}

/**
 * Immutable vote or final decision on an approval request.
 */
data class ApprovalDecision(
    val decisionId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val approvalId: String,
    val approverId: String,
    val approverRole: UserRole,
    val approverPrincipalType: PrincipalType = PrincipalType.HUMAN,
    val decisionType: ApprovalDecisionType,
    val notes: String? = null,
    val humanConfirmation: HumanConfirmationMetadata? = null,
    val decidedAt: Long = System.currentTimeMillis()
)

/**
 * Cryptographic-grade metadata verifying explicit human confirmation.
 */
data class HumanConfirmationMetadata(
    val confirmationId: String,
    val confirmedByUserId: String,
    val confirmedByUserRole: UserRole,
    val confirmationTimestamp: Long = System.currentTimeMillis(),
    val actionSummary: String
)

/**
 * Escalation record when an approval times out or is escalated to a higher role.
 */
data class ApprovalEscalation(
    val escalationId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val approvalId: String,
    val workflowId: String,
    val fromRole: UserRole,
    val toRole: UserRole,
    val reason: String,
    val escalatedAt: Long = System.currentTimeMillis()
)

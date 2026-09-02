package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.ApprovalDecisionType
import com.sucharu.sucharupro.domain.model.design.ApprovalStatus
import com.sucharu.sucharupro.domain.model.design.DesignApproval
import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignProof
import com.sucharu.sucharupro.domain.model.design.DesignProofVersion
import com.sucharu.sucharupro.domain.model.design.ProofStatus
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Authoritative validator for Approval Workflow, decision rules, and final locking (Module 05 Step 04).
 */
object DesignApprovalValidator {

    /** Roles authorized to submit approval requests. */
    val AUTHORIZED_REQUESTER_ROLES = setOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.DESIGNER)

    /** Roles authorized to review, approve, request revisions, reject, and lock approvals. */
    val AUTHORIZED_APPROVER_ROLES = setOf(UserRole.ADMIN, UserRole.MANAGER)

    /**
     * Validates whether a caller with [callerRole] can submit an approval request.
     */
    fun validateApprovalRequestPermission(callerRole: UserRole?): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_REQUESTER_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to submit approval requests."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates whether a caller with [callerRole] can make an approval decision.
     */
    fun validateApprovalDecisionPermission(
        callerRole: UserRole?,
        requestedByUserId: String? = null,
        currentUserId: String? = null
    ): DomainResult<Unit> {
        if (callerRole != null && callerRole !in AUTHORIZED_APPROVER_ROLES) {
            return DomainResult.Error(
                message = "User with role '${callerRole.defaultLabel}' is not authorized to review or approve proofs."
            )
        }

        // Prevent self-approval if designer requested it unless explicitly Admin
        if (callerRole == UserRole.DESIGNER) {
            return DomainResult.Error(
                message = "Designers are not authorized to approve proof requests."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates approval request creation eligibility against [proof], [targetVersion], and [existingApprovals].
     */
    fun validateApprovalCreation(
        proof: DesignProof,
        targetVersionNumber: Int,
        existingApprovals: List<DesignApproval>,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbacResult = validateApprovalRequestPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        if (proof.isArchived) {
            return DomainResult.Error(message = "Cannot create approval request for archived proof '${proof.title}'.")
        }

        val targetVersion = proof.versions.find { it.versionNumber == targetVersionNumber }
            ?: return DomainResult.Error(message = "Target proof version V$targetVersionNumber not found in proof '${proof.title}'.")

        if (targetVersion.isArchived) {
            return DomainResult.Error(message = "Cannot request approval on archived version ${targetVersion.versionTag}.")
        }

        // Prevent duplicate active approval requests for the same proof
        val activeApproval = existingApprovals.find {
            it.proofId == proof.proofId && (it.status == ApprovalStatus.PENDING_REVIEW || it.status == ApprovalStatus.UNDER_REVIEW)
        }
        if (activeApproval != null) {
            return DomainResult.Error(
                message = "An active approval request '${activeApproval.approvalId}' is already pending for Proof '${proof.title}'."
            )
        }

        // Prevent approval request if already final locked
        val lockedApproval = existingApprovals.find {
            it.proofId == proof.proofId && it.isFinalLocked
        }
        if (lockedApproval != null) {
            return DomainResult.Error(
                message = "Proof '${proof.title}' has already been Final Locked in approval '${lockedApproval.approvalId}'."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates approval lifecycle status transitions.
     */
    fun validateStatusTransition(
        approval: DesignApproval,
        targetStatus: ApprovalStatus
    ): DomainResult<Unit> {
        val currentStatus = approval.status

        if (currentStatus == targetStatus) {
            return DomainResult.Error(
                message = "Approval '${approval.approvalId}' is already in ${currentStatus.defaultLabel} state."
            )
        }

        if (approval.isFinalLocked) {
            return DomainResult.Error(
                message = "Approval '${approval.approvalId}' is Final Locked and cannot undergo status changes."
            )
        }

        if (!currentStatus.canTransitionTo(targetStatus)) {
            return DomainResult.Error(
                message = "Cannot transition Approval from ${currentStatus.defaultLabel} to ${targetStatus.defaultLabel}."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates an approval review decision.
     */
    fun validateApprovalDecision(
        approval: DesignApproval,
        decisionType: ApprovalDecisionType,
        comments: String?,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbacResult = validateApprovalDecisionPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        if (approval.isFinalLocked) {
            return DomainResult.Error(
                message = "Cannot record decision: Approval '${approval.approvalId}' is already Final Locked."
            )
        }

        if (approval.status == ApprovalStatus.REJECTED) {
            return DomainResult.Error(
                message = "Cannot record decision: Approval '${approval.approvalId}' is already Rejected."
            )
        }

        if (approval.status != ApprovalStatus.PENDING_REVIEW && approval.status != ApprovalStatus.UNDER_REVIEW && approval.status != ApprovalStatus.RESUBMITTED) {
            return DomainResult.Error(
                message = "Cannot record decision: Approval '${approval.approvalId}' is in '${approval.status.defaultLabel}' state."
            )
        }

        if ((decisionType == ApprovalDecisionType.REVISION_REQUIRED || decisionType == ApprovalDecisionType.REJECTED) && comments.isNullOrBlank()) {
            return DomainResult.Error(
                message = "Comments/Reasons are mandatory when requesting revision or rejecting approval."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates final lock operation.
     */
    fun validateFinalLock(
        approval: DesignApproval,
        callerRole: UserRole? = null
    ): DomainResult<Unit> {
        val rbacResult = validateApprovalDecisionPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        if (approval.isFinalLocked) {
            return DomainResult.Error(
                message = "Approval '${approval.approvalId}' is already Final Locked."
            )
        }

        if (approval.status != ApprovalStatus.APPROVED) {
            return DomainResult.Error(
                message = "Cannot apply Final Lock: Approval must be in 'Approved' state (Current: ${approval.status.defaultLabel})."
            )
        }

        return DomainResult.Success(Unit)
    }
}

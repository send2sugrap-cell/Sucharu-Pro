package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignApproval
import com.sucharu.sucharupro.domain.model.design.DesignApprovalDecision
import com.sucharu.sucharupro.domain.model.design.RevisionReason
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface contract for Approval Workflow and Final Locking (Module 05 Step 04).
 */
interface DesignApprovalRepository {

    /** Reactive stream observing all approvals across all projects. */
    fun observeApprovals(): Flow<List<DesignApproval>>

    /** Reactive stream observing a single approval by [approvalId]. */
    fun getApprovalById(approvalId: String): Flow<DesignApproval?>

    /** Direct one-shot lookup of an approval by [approvalId]. */
    suspend fun findApprovalById(approvalId: String): DomainResult<DesignApproval>

    /** Reactive stream of approvals belonging to a [proofId]. */
    fun getApprovalsForProof(proofId: String): Flow<List<DesignApproval>>

    /** Reactive stream of approvals associated with a [projectId]. */
    fun getApprovalsForProject(projectId: String): Flow<List<DesignApproval>>

    /**
     * Creates an approval request targeting a specific [targetVersionNumber] of a proof.
     */
    suspend fun createApprovalRequest(
        proofId: String,
        targetVersionNumber: Int,
        comments: String? = null,
        requestedBy: String,
        requestedByName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignApproval>

    /**
     * Marks an approval request as UNDER_REVIEW by an authorized reviewer.
     */
    suspend fun startReview(
        approvalId: String,
        reviewerId: String,
        reviewerName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignApproval>

    /**
     * Approves the targeted proof version.
     */
    suspend fun approve(
        approvalId: String,
        comments: String? = null,
        reviewerId: String,
        reviewerName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignApproval>

    /**
     * Rejects current version and requires revisions. Integrates directly with Step 03 revision request workflow.
     */
    suspend fun requestRevision(
        approvalId: String,
        reason: RevisionReason,
        comments: String,
        reviewerId: String,
        reviewerName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignApprovalDecision>

    /**
     * Permanently rejects the approval request.
     */
    suspend fun reject(
        approvalId: String,
        comments: String,
        reviewerId: String,
        reviewerName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignApprovalDecision>

    /**
     * Applies Final Lock to an approved approval aggregate, rendering the approved version immutable.
     */
    suspend fun lockFinalApproval(
        approvalId: String,
        lockedBy: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignApproval>

    /** Reactive stream of historical review decisions for an approval request. */
    fun getApprovalHistory(approvalId: String): Flow<List<DesignApprovalDecision>>
}

package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.DesignApprovalDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.ApprovalDecisionType
import com.sucharu.sucharupro.domain.model.design.ApprovalStatus
import com.sucharu.sucharupro.domain.model.design.DesignApproval
import com.sucharu.sucharupro.domain.model.design.DesignApprovalDecision
import com.sucharu.sucharupro.domain.model.design.RevisionReason
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DesignApprovalRepository
import com.sucharu.sucharupro.domain.repository.DesignProofRepository
import com.sucharu.sucharupro.domain.validation.DesignApprovalValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Authoritative implementation of [DesignApprovalRepository] enforcing approval state machines,
 * Step 03 revision workflow integration, and final lock immutability (Module 05 Step 04).
 */
class DesignApprovalRepositoryImpl(
    private val dataSource: DesignApprovalDataSource,
    private val proofRepository: DesignProofRepository
) : DesignApprovalRepository {

    private val repositoryMutex = Mutex()

    override fun observeApprovals(): Flow<List<DesignApproval>> = dataSource.observeApprovals()

    override fun getApprovalById(approvalId: String): Flow<DesignApproval?> {
        return dataSource.observeApprovals().map { approvals ->
            approvals.find { it.approvalId == approvalId }
        }
    }

    override suspend fun findApprovalById(approvalId: String): DomainResult<DesignApproval> {
        return dataSource.fetchApprovalById(approvalId)
    }

    override fun getApprovalsForProof(proofId: String): Flow<List<DesignApproval>> {
        return dataSource.observeApprovals().map { approvals ->
            approvals.filter { it.proofId == proofId }
        }
    }

    override fun getApprovalsForProject(projectId: String): Flow<List<DesignApproval>> {
        return dataSource.observeApprovals().map { approvals ->
            approvals.filter { it.projectId == projectId }
        }
    }

    override suspend fun createApprovalRequest(
        proofId: String,
        targetVersionNumber: Int,
        comments: String?,
        requestedBy: String,
        requestedByName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignApproval> = repositoryMutex.withLock {
        val proof = when (val res = proofRepository.findProofById(proofId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return DomainResult.Error(message = "Cannot request approval: Proof '$proofId' not found.")
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val allApprovals = dataSource.observeApprovals().first()
        val creationValidation = DesignApprovalValidator.validateApprovalCreation(
            proof = proof,
            targetVersionNumber = targetVersionNumber,
            existingApprovals = allApprovals,
            callerRole = callerRole
        )
        if (creationValidation is DomainResult.Error) {
            return creationValidation
        }

        val targetVersion = proof.versions.find { it.versionNumber == targetVersionNumber }
            ?: return DomainResult.Error(message = "Target proof version V$targetVersionNumber not found.")

        val approvalId = "appr-" + UUID.randomUUID().toString()
        val approval = DesignApproval(
            approvalId = approvalId,
            projectId = proof.projectId,
            artworkId = proof.artworkId,
            proofId = proof.proofId,
            proofVersionId = targetVersion.versionId,
            artworkVersionId = targetVersion.artworkVersionId,
            targetProofVersionNumber = targetVersionNumber,
            status = ApprovalStatus.PENDING_REVIEW,
            requestedBy = requestedBy,
            requestedByName = requestedByName,
            requestedAt = timestamp,
            comments = comments,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        return dataSource.insertApproval(approval)
    }

    override suspend fun startReview(
        approvalId: String,
        reviewerId: String,
        reviewerName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignApproval> = repositoryMutex.withLock {
        val currentApproval = when (val res = dataSource.fetchApprovalById(approvalId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val rbacResult = DesignApprovalValidator.validateApprovalDecisionPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        val transitionValidation = DesignApprovalValidator.validateStatusTransition(currentApproval, ApprovalStatus.UNDER_REVIEW)
        if (transitionValidation is DomainResult.Error) {
            return transitionValidation
        }

        val updatedApproval = currentApproval.copy(
            status = ApprovalStatus.UNDER_REVIEW,
            reviewerId = reviewerId,
            reviewerName = reviewerName,
            reviewedAt = timestamp,
            updatedAt = timestamp
        )

        return dataSource.updateApproval(updatedApproval)
    }

    override suspend fun approve(
        approvalId: String,
        comments: String?,
        reviewerId: String,
        reviewerName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignApproval> = repositoryMutex.withLock {
        val currentApproval = when (val res = dataSource.fetchApprovalById(approvalId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val validation = DesignApprovalValidator.validateApprovalDecision(
            approval = currentApproval,
            decisionType = ApprovalDecisionType.APPROVED,
            comments = comments,
            callerRole = callerRole
        )
        if (validation is DomainResult.Error) {
            return validation
        }

        val decisionId = "dec-" + UUID.randomUUID().toString()
        val decision = DesignApprovalDecision(
            decisionId = decisionId,
            approvalId = approvalId,
            proofVersionId = currentApproval.proofVersionId,
            targetVersionNumber = currentApproval.targetProofVersionNumber,
            artworkVersionId = currentApproval.artworkVersionId,
            decisionType = ApprovalDecisionType.APPROVED,
            comments = comments ?: "Proof approved for production.",
            decidedBy = reviewerId,
            decidedByName = reviewerName,
            decidedAt = timestamp
        )

        val updatedApproval = currentApproval.copy(
            status = ApprovalStatus.APPROVED,
            reviewerId = reviewerId,
            reviewerName = reviewerName,
            reviewedAt = timestamp,
            comments = comments,
            decisions = currentApproval.decisions + decision,
            updatedAt = timestamp
        )

        dataSource.updateApproval(updatedApproval)
        dataSource.insertDecision(decision)

        return DomainResult.Success(updatedApproval)
    }

    override suspend fun requestRevision(
        approvalId: String,
        reason: RevisionReason,
        comments: String,
        reviewerId: String,
        reviewerName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignApprovalDecision> = repositoryMutex.withLock {
        val currentApproval = when (val res = dataSource.fetchApprovalById(approvalId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val validation = DesignApprovalValidator.validateApprovalDecision(
            approval = currentApproval,
            decisionType = ApprovalDecisionType.REVISION_REQUIRED,
            comments = comments,
            callerRole = callerRole
        )
        if (validation is DomainResult.Error) {
            return validation
        }

        // Direct integration with Step 03 revision request workflow
        val revisionResult = proofRepository.requestRevision(
            proofId = currentApproval.proofId,
            targetVersionNumber = currentApproval.targetProofVersionNumber,
            reason = reason,
            notes = comments,
            requesterId = reviewerId,
            requesterName = reviewerName,
            timestamp = timestamp,
            callerRole = callerRole
        )
        val revisionRequestId = when (revisionResult) {
            is DomainResult.Success -> revisionResult.data.requestId
            is DomainResult.Error -> return DomainResult.Error(message = "Failed to trigger Step 03 revision: ${revisionResult.message}")
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val decisionId = "dec-" + UUID.randomUUID().toString()
        val decision = DesignApprovalDecision(
            decisionId = decisionId,
            approvalId = approvalId,
            proofVersionId = currentApproval.proofVersionId,
            targetVersionNumber = currentApproval.targetProofVersionNumber,
            artworkVersionId = currentApproval.artworkVersionId,
            decisionType = ApprovalDecisionType.REVISION_REQUIRED,
            comments = comments,
            decidedBy = reviewerId,
            decidedByName = reviewerName,
            decidedAt = timestamp,
            revisionRequestId = revisionRequestId
        )

        val updatedApproval = currentApproval.copy(
            status = ApprovalStatus.REVISION_REQUIRED,
            reviewerId = reviewerId,
            reviewerName = reviewerName,
            reviewedAt = timestamp,
            comments = comments,
            decisions = currentApproval.decisions + decision,
            updatedAt = timestamp
        )

        dataSource.updateApproval(updatedApproval)
        dataSource.insertDecision(decision)

        return DomainResult.Success(decision)
    }

    override suspend fun reject(
        approvalId: String,
        comments: String,
        reviewerId: String,
        reviewerName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignApprovalDecision> = repositoryMutex.withLock {
        val currentApproval = when (val res = dataSource.fetchApprovalById(approvalId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val validation = DesignApprovalValidator.validateApprovalDecision(
            approval = currentApproval,
            decisionType = ApprovalDecisionType.REJECTED,
            comments = comments,
            callerRole = callerRole
        )
        if (validation is DomainResult.Error) {
            return validation
        }

        val decisionId = "dec-" + UUID.randomUUID().toString()
        val decision = DesignApprovalDecision(
            decisionId = decisionId,
            approvalId = approvalId,
            proofVersionId = currentApproval.proofVersionId,
            targetVersionNumber = currentApproval.targetProofVersionNumber,
            artworkVersionId = currentApproval.artworkVersionId,
            decisionType = ApprovalDecisionType.REJECTED,
            comments = comments,
            decidedBy = reviewerId,
            decidedByName = reviewerName,
            decidedAt = timestamp
        )

        val updatedApproval = currentApproval.copy(
            status = ApprovalStatus.REJECTED,
            reviewerId = reviewerId,
            reviewerName = reviewerName,
            reviewedAt = timestamp,
            comments = comments,
            decisions = currentApproval.decisions + decision,
            updatedAt = timestamp
        )

        dataSource.updateApproval(updatedApproval)
        dataSource.insertDecision(decision)

        return DomainResult.Success(decision)
    }

    override suspend fun lockFinalApproval(
        approvalId: String,
        lockedBy: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignApproval> = repositoryMutex.withLock {
        val currentApproval = when (val res = dataSource.fetchApprovalById(approvalId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val validation = DesignApprovalValidator.validateFinalLock(currentApproval, callerRole)
        if (validation is DomainResult.Error) {
            return validation
        }

        val updatedApproval = currentApproval.copy(
            status = ApprovalStatus.FINAL_LOCKED,
            isFinalLocked = true,
            finalApprovedProofVersionId = currentApproval.proofVersionId,
            finalApprovedArtworkVersionId = currentApproval.artworkVersionId,
            lockedAt = timestamp,
            lockedBy = lockedBy,
            updatedAt = timestamp
        )

        return dataSource.updateApproval(updatedApproval)
    }

    override fun getApprovalHistory(approvalId: String): Flow<List<DesignApprovalDecision>> {
        return dataSource.observeDecisions().map { decisions ->
            decisions.filter { it.approvalId == approvalId }.sortedByDescending { it.decidedAt }
        }
    }
}

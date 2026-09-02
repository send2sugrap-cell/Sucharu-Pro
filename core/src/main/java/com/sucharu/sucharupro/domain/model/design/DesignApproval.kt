package com.sucharu.sucharupro.domain.model.design

/**
 * Aggregate root entity managing the Approval Workflow for a [DesignProof] and [DesignProofVersion] (Module 05 Step 04).
 */
data class DesignApproval(
    val approvalId: String,
    val projectId: String,
    val artworkId: String,
    val proofId: String,
    val proofVersionId: String,
    val artworkVersionId: String,
    val targetProofVersionNumber: Int,
    val status: ApprovalStatus = ApprovalStatus.DRAFT,
    val requestedBy: String,
    val requestedByName: String? = null,
    val requestedAt: String,
    val reviewerId: String? = null,
    val reviewerName: String? = null,
    val reviewedAt: String? = null,
    val comments: String? = null,
    val decisions: List<DesignApprovalDecision> = emptyList(),
    val isFinalLocked: Boolean = false,
    val finalApprovedProofVersionId: String? = null,
    val finalApprovedArtworkVersionId: String? = null,
    val lockedAt: String? = null,
    val lockedBy: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    init {
        require(approvalId.isNotBlank()) { "Approval ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(artworkId.isNotBlank()) { "Artwork ID cannot be blank." }
        require(proofId.isNotBlank()) { "Proof ID cannot be blank." }
        require(proofVersionId.isNotBlank()) { "Proof Version ID cannot be blank." }
        require(artworkVersionId.isNotBlank()) { "Artwork Version ID cannot be blank." }
        require(targetProofVersionNumber >= 1) { "Target Proof Version Number must be at least 1." }
        require(requestedBy.isNotBlank()) { "Requested By cannot be blank." }
        require(requestedAt.isNotBlank()) { "Requested timestamp cannot be blank." }
        require(createdAt.isNotBlank()) { "Creation timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "Update timestamp cannot be blank." }
    }

    val latestDecision: DesignApprovalDecision? get() = decisions.lastOrNull()
    val isApproved: Boolean get() = status == ApprovalStatus.APPROVED || status == ApprovalStatus.FINAL_LOCKED
    val isTerminal: Boolean get() = status.isTerminal
    val decisionCount: Int get() = decisions.size
}

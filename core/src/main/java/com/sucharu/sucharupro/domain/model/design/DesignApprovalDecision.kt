package com.sucharu.sucharupro.domain.model.design

/**
 * Immutable historical record of an approval review decision (Module 05 Step 04).
 */
data class DesignApprovalDecision(
    val decisionId: String,
    val approvalId: String,
    val proofVersionId: String,
    val targetVersionNumber: Int,
    val artworkVersionId: String,
    val decisionType: ApprovalDecisionType,
    val comments: String?,
    val decidedBy: String,
    val decidedByName: String? = null,
    val decidedAt: String,
    val revisionRequestId: String? = null
) {
    init {
        require(decisionId.isNotBlank()) { "Decision ID cannot be blank." }
        require(approvalId.isNotBlank()) { "Approval ID cannot be blank." }
        require(proofVersionId.isNotBlank()) { "Target Proof Version ID cannot be blank." }
        require(targetVersionNumber >= 1) { "Target Version Number must be at least 1." }
        require(artworkVersionId.isNotBlank()) { "Artwork Version ID cannot be blank." }
        require(decidedBy.isNotBlank()) { "Decided By cannot be blank." }
        require(decidedAt.isNotBlank()) { "Decided timestamp cannot be blank." }
        if (decisionType == ApprovalDecisionType.REVISION_REQUIRED || decisionType == ApprovalDecisionType.REJECTED) {
            require(!comments.isNullOrBlank()) { "Comments/Reasons are mandatory for Revision Required or Rejected decisions." }
        }
    }

    val isApproved: Boolean get() = decisionType == ApprovalDecisionType.APPROVED
    val isRevisionRequired: Boolean get() = decisionType == ApprovalDecisionType.REVISION_REQUIRED
    val isRejected: Boolean get() = decisionType == ApprovalDecisionType.REJECTED
}

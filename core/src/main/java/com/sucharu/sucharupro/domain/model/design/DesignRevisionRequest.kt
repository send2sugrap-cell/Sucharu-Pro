package com.sucharu.sucharupro.domain.model.design

/**
 * Immutable historical record of a revision request targeting a specific [DesignProofVersion].
 *
 * NOTE: Resolving a revision request indicates that revised proof assets have been generated
 * and resubmitted; it does NOT imply approval.
 */
data class DesignRevisionRequest(
    val requestId: String,
    val proofId: String,
    val proofVersionId: String,
    val targetVersionNumber: Int,
    val reason: RevisionReason,
    val notes: String,
    val status: RevisionRequestStatus = RevisionRequestStatus.OPEN,
    val requestedBy: String,
    val requestedByName: String? = null,
    val requestedAt: String,
    val resolvedBy: String? = null,
    val resolvedAt: String? = null,
    val resultingProofVersionId: String? = null,
    val resultingVersionNumber: Int? = null
) {
    init {
        require(requestId.isNotBlank()) { "Revision Request ID cannot be blank." }
        require(proofId.isNotBlank()) { "Proof ID cannot be blank." }
        require(proofVersionId.isNotBlank()) { "Target Proof Version ID cannot be blank." }
        require(targetVersionNumber >= 1) { "Target Version Number must be at least 1." }
        require(notes.isNotBlank()) { "Revision notes/instructions cannot be blank." }
        require(requestedBy.isNotBlank()) { "Requested By cannot be blank." }
        require(requestedAt.isNotBlank()) { "Requested timestamp cannot be blank." }
    }

    val isOpen: Boolean get() = status == RevisionRequestStatus.OPEN
    val isInProgress: Boolean get() = status == RevisionRequestStatus.IN_PROGRESS
    val isResolved: Boolean get() = status == RevisionRequestStatus.RESOLVED
}

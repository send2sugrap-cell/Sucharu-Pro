package com.sucharu.sucharupro.domain.model.design

import com.sucharu.sucharupro.domain.model.common.FileReference

/**
 * Immutable historical version of a [DesignProof].
 *
 * References the originating [artworkVersionId] and a [FileReference].
 * Historical versions must NEVER be overwritten.
 */
data class DesignProofVersion(
    val versionId: String,
    val proofId: String,
    val versionNumber: Int,
    val versionTag: String = "V$versionNumber",
    val artworkVersionId: String,
    val fileReference: FileReference,
    val revisionRequestId: String? = null,
    val status: ProofStatus = ProofStatus.READY_FOR_REVIEW,
    val notes: String? = null,
    val createdAt: String,
    val createdBy: String? = null,
    val archivedAt: String? = null,
    val archivedBy: String? = null
) {
    init {
        require(versionId.isNotBlank()) { "Proof Version ID cannot be blank." }
        require(proofId.isNotBlank()) { "Proof ID cannot be blank." }
        require(versionNumber >= 1) { "Version number must be at least 1." }
        require(versionTag.isNotBlank()) { "Version Tag cannot be blank." }
        require(artworkVersionId.isNotBlank()) { "Originating Artwork Version ID cannot be blank." }
        require(createdAt.isNotBlank()) { "Creation timestamp cannot be blank." }
    }

    val isArchived: Boolean get() = status == ProofStatus.ARCHIVED
}

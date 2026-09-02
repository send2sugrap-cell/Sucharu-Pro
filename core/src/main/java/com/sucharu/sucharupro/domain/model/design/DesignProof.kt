package com.sucharu.sucharupro.domain.model.design

/**
 * Aggregate root entity representing a Proof for a [DesignArtwork] (Module 05 Step 03).
 *
 * Tracks proof versions and chronological revision request cycles.
 */
data class DesignProof(
    val proofId: String,
    val artworkId: String,
    val projectId: String,
    val productionJobId: String,
    val title: String,
    val status: ProofStatus = ProofStatus.DRAFT,
    val currentVersionNumber: Int = 0,
    val versions: List<DesignProofVersion> = emptyList(),
    val revisions: List<DesignRevisionRequest> = emptyList(),
    val createdAt: String,
    val createdBy: String? = null,
    val updatedAt: String,
    val updatedBy: String? = null,
    val archivedAt: String? = null,
    val archivedBy: String? = null
) {
    init {
        require(proofId.isNotBlank()) { "Proof ID cannot be blank." }
        require(artworkId.isNotBlank()) { "Artwork ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(productionJobId.isNotBlank()) { "Production Job ID cannot be blank." }
        require(title.isNotBlank()) { "Proof title cannot be blank." }
        require(createdAt.isNotBlank()) { "Creation timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "Update timestamp cannot be blank." }
    }

    /** The active latest version of the proof. */
    val currentVersion: DesignProofVersion?
        get() = versions.find { it.versionNumber == currentVersionNumber }
            ?: versions.maxByOrNull { it.versionNumber }

    /** Active pending revision request, if any. */
    val activeRevisionRequest: DesignRevisionRequest?
        get() = revisions.find { it.isOpen || it.isInProgress }

    val versionCount: Int get() = versions.size
    val revisionCount: Int get() = revisions.size
    val isArchived: Boolean get() = status == ProofStatus.ARCHIVED
}

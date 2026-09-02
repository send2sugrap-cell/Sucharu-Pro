package com.sucharu.sucharupro.domain.model.design

import com.sucharu.sucharupro.domain.model.common.FileReference

/**
 * Immutable historical version of a [DesignArtwork].
 *
 * Each version records an explicit file reference and printing metadata snapshot.
 * Versions must NEVER be destructively overwritten.
 */
data class DesignArtworkVersion(
    val versionId: String,
    val artworkId: String,
    val versionNumber: Int,
    val versionTag: String = "V$versionNumber",
    val fileReference: FileReference,
    val metadata: ArtworkMetadata = ArtworkMetadata(),
    val status: ArtworkStatus = ArtworkStatus.ACTIVE,
    val notes: String? = null,
    val createdAt: String,
    val createdBy: String? = null,
    val archivedAt: String? = null,
    val archivedBy: String? = null
) {
    init {
        require(versionId.isNotBlank()) { "Version ID cannot be blank." }
        require(artworkId.isNotBlank()) { "Artwork ID cannot be blank." }
        require(versionNumber >= 1) { "Version number must be at least 1." }
        require(versionTag.isNotBlank()) { "Version Tag cannot be blank." }
        require(createdAt.isNotBlank()) { "Creation timestamp cannot be blank." }
    }

    val isActive: Boolean get() = status == ArtworkStatus.ACTIVE
    val isArchived: Boolean get() = status == ArtworkStatus.ARCHIVED
}

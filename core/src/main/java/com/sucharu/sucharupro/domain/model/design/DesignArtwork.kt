package com.sucharu.sucharupro.domain.model.design

/**
 * Aggregate root entity representing a logical Artwork belonging to a [DesignProject].
 *
 * Maintains a collection of immutable [DesignArtworkVersion] records.
 */
data class DesignArtwork(
    val artworkId: String,
    val projectId: String,
    val productionJobId: String,
    val name: String,
    val description: String? = null,
    val currentVersionNumber: Int = 0,
    val status: ArtworkStatus = ArtworkStatus.ACTIVE,
    val versions: List<DesignArtworkVersion> = emptyList(),
    val createdAt: String,
    val createdBy: String? = null,
    val updatedAt: String,
    val updatedBy: String? = null,
    val archivedAt: String? = null,
    val archivedBy: String? = null
) {
    init {
        require(artworkId.isNotBlank()) { "Artwork ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(productionJobId.isNotBlank()) { "Production Job ID cannot be blank." }
        require(name.isNotBlank()) { "Artwork name cannot be blank." }
        require(createdAt.isNotBlank()) { "Creation timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "Update timestamp cannot be blank." }
    }

    /** The active latest or designated current version of the artwork. */
    val currentVersion: DesignArtworkVersion?
        get() = versions.find { it.versionNumber == currentVersionNumber }
            ?: versions.filter { it.isActive }.maxByOrNull { it.versionNumber }
            ?: versions.maxByOrNull { it.versionNumber }

    /** Total number of versions created for this artwork. */
    val versionCount: Int get() = versions.size

    val isArchived: Boolean get() = status == ArtworkStatus.ARCHIVED
    val isActive: Boolean get() = status == ArtworkStatus.ACTIVE
}

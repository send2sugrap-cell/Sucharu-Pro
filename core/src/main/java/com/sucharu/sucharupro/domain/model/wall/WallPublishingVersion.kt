package com.sucharu.sucharupro.domain.model.wall

/**
 * Immutable Historical Wall Publishing Version Entity.
 */
data class WallPublishingVersion(
    val versionId: String,
    val wallId: String,
    val versionNumber: Int,
    val status: WallPublishStatus = WallPublishStatus.PUBLISHED,
    val configurationSnapshot: ServerDrivenWallConfig,
    val createdAt: String,
    val createdBy: String
) {
    init {
        require(versionId.isNotBlank()) { "Version ID cannot be blank." }
        require(wallId.isNotBlank()) { "Wall ID cannot be blank." }
        require(versionNumber > 0) { "Version Number must be greater than zero." }
    }
}

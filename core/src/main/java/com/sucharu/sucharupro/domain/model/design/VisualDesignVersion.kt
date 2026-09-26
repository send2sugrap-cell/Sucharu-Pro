package com.sucharu.sucharupro.domain.model.design

/**
 * Immutable historical version snapshot for Form 02 Visual Design Studio.
 */
data class VisualDesignVersion(
    val versionId: String,
    val designId: String,
    val versionNumber: Int,
    val status: DesignPublishStatus = DesignPublishStatus.PUBLISHED,
    val configurationSnapshot: VisualDesignConfiguration,
    val createdAt: String,
    val createdBy: String
) {
    init {
        require(versionId.isNotBlank()) { "Version ID cannot be blank." }
        require(designId.isNotBlank()) { "Design ID cannot be blank." }
        require(versionNumber > 0) { "Version Number must be greater than zero." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
        require(createdBy.isNotBlank()) { "createdBy actor cannot be blank." }
    }
}

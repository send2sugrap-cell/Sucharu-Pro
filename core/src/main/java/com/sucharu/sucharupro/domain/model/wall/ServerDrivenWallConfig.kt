package com.sucharu.sucharupro.domain.model.wall

import com.sucharu.sucharupro.domain.model.offer.AudienceType
import com.sucharu.sucharupro.domain.model.offer.WallType

/**
 * Form 06 — Server-Driven Wall Configuration Entity.
 */
data class ServerDrivenWallConfig(
    val wallId: String,
    val wallType: WallCategoryType = WallCategoryType.PUBLIC,
    val wallTitle: String,
    val versionNumber: Int = 1,
    val status: WallPublishStatus = WallPublishStatus.DRAFT,
    val isActivePublished: Boolean = false,

    // Audience Visibility Flags
    val publicVisibility: Boolean = true,
    val guestVisibility: Boolean = true,
    val customerVisibility: Boolean = true,
    val affiliateVisibility: Boolean = true,

    // Scheduling
    val scheduledStartAt: String? = null,
    val scheduledEndAt: String? = null,

    val sections: List<WallSectionConfig> = emptyList(),

    val createdAt: String,
    val updatedAt: String,
    val createdBy: String,
    val updatedBy: String? = null
) {
    init {
        require(wallId.isNotBlank()) { "Wall ID cannot be blank." }
        require(wallTitle.isNotBlank()) { "Wall Title cannot be blank." }
        require(createdAt.isNotBlank()) { "createdAt timestamp cannot be blank." }
        require(updatedAt.isNotBlank()) { "updatedAt timestamp cannot be blank." }
        require(createdBy.isNotBlank()) { "createdBy actor cannot be blank." }
    }

    /**
     * Evaluates whether this wall configuration is visible for [audience] at [currentTimestamp].
     */
    fun isVisibleForAudience(audience: AudienceType, currentTimestamp: String = createdAt): Boolean {
        if (!isActivePublished) return false
        val startOk = scheduledStartAt == null || currentTimestamp >= scheduledStartAt
        val endOk = scheduledEndAt == null || currentTimestamp <= scheduledEndAt
        if (!startOk || !endOk) return false

        return when (audience) {
            AudienceType.GUEST -> guestVisibility || publicVisibility
            AudienceType.CUSTOMER -> customerVisibility
            AudienceType.AFFILIATE -> affiliateVisibility
        }
    }
}

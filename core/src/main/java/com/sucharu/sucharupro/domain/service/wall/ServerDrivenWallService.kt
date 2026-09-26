package com.sucharu.sucharupro.domain.service.wall

import com.sucharu.sucharupro.data.api.model.wall.ResolvedServerDrivenWallResponseDto
import com.sucharu.sucharupro.data.api.model.wall.WallSectionConfigDto
import com.sucharu.sucharupro.data.api.model.wall.WallSectionItemDto
import com.sucharu.sucharupro.domain.model.offer.AudienceType
import com.sucharu.sucharupro.domain.model.wall.ServerDrivenWallConfig
import com.sucharu.sucharupro.domain.model.wall.WallCategoryType
import com.sucharu.sucharupro.domain.model.wall.WallPublishStatus
import com.sucharu.sucharupro.domain.model.wall.WallPublishingVersion
import com.sucharu.sucharupro.domain.repository.ServerDrivenWallRepository
import java.util.UUID

/**
 * Domain Service for Form 06 — Server-Driven Wall Resolution & Section Publishing.
 */
class ServerDrivenWallService(
    private val repository: ServerDrivenWallRepository
) {

    suspend fun saveWallConfig(config: ServerDrivenWallConfig): ServerDrivenWallConfig {
        require(config.wallTitle.isNotBlank()) { "Wall title is required." }
        return repository.saveWallConfig(config)
    }

    suspend fun listAllWallConfigs(): List<ServerDrivenWallConfig> {
        return repository.getAllWallConfigs()
    }

    suspend fun publishWallVersion(wallId: String, actorId: String): WallPublishingVersion {
        val published = repository.publishWallVersion(wallId, actorId)
        val version = WallPublishingVersion(
            versionId = "WVER-${published.wallId}-${published.versionNumber}",
            wallId = published.wallId,
            versionNumber = published.versionNumber,
            status = WallPublishStatus.PUBLISHED,
            configurationSnapshot = published,
            createdAt = published.updatedAt,
            createdBy = actorId
        )
        repository.saveWallVersion(version)
        return version
    }

    /**
     * Resolves Server-Driven Wall Response for [wallType] and [audience].
     */
    suspend fun resolveServerDrivenWall(
        wallType: WallCategoryType,
        audience: AudienceType
    ): ResolvedServerDrivenWallResponseDto {
        val wallConfig = repository.getPublishedWallConfigByType(wallType)
            ?: throw IllegalArgumentException("No active published wall configuration for wall type: $wallType")

        val timestamp = "2026-09-26T14:45:00Z"
        val isAudienceVisible = wallConfig.isVisibleForAudience(audience, timestamp)

        val resolvedSections = if (isAudienceVisible) {
            wallConfig.sections
                .filter { it.isVisible }
                .sortedBy { it.displayOrder }
                .map { sec ->
                    WallSectionConfigDto(
                        sectionId = sec.sectionId,
                        wallId = sec.wallId,
                        sectionType = sec.sectionType.name,
                        sectionName = sec.sectionName,
                        sectionTitle = sec.sectionTitle,
                        sectionSubtitle = sec.sectionSubtitle,
                        layoutType = sec.layoutType.name,
                        visualDesignId = sec.visualDesignId,
                        displayOrder = sec.displayOrder,
                        priorityLevel = sec.priorityLevel,
                        isVisible = sec.isVisible,
                        isFeatured = sec.isFeatured,
                        isPinned = sec.isPinned,
                        items = sec.items.map { item ->
                            WallSectionItemDto(
                                itemId = item.itemId,
                                sectionId = item.sectionId,
                                contentFoundationId = item.contentFoundationId,
                                productId = item.productId,
                                offerId = item.offerId,
                                visualDesignId = item.visualDesignId,
                                displayOrder = item.displayOrder,
                                isPinned = item.isPinned
                            )
                        }
                    )
                }
        } else {
            emptyList()
        }

        return ResolvedServerDrivenWallResponseDto(
            wallId = wallConfig.wallId,
            wallType = wallConfig.wallType.name,
            wallTitle = wallConfig.wallTitle,
            audienceType = audience.name,
            sections = resolvedSections,
            resolvedAt = timestamp
        )
    }
}

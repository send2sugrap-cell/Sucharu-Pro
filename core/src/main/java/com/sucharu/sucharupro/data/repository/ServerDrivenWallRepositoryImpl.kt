package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.domain.model.wall.SectionLayoutType
import com.sucharu.sucharupro.domain.model.wall.SectionType
import com.sucharu.sucharupro.domain.model.wall.ServerDrivenWallConfig
import com.sucharu.sucharupro.domain.model.wall.WallCategoryType
import com.sucharu.sucharupro.domain.model.wall.WallPublishStatus
import com.sucharu.sucharupro.domain.model.wall.WallPublishingVersion
import com.sucharu.sucharupro.domain.model.wall.WallSectionConfig
import com.sucharu.sucharupro.domain.model.wall.WallSectionItem
import com.sucharu.sucharupro.domain.repository.ServerDrivenWallRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe Repository Implementation for Form 06 Server-Driven Wall & Section Publishing.
 */
class ServerDrivenWallRepositoryImpl : ServerDrivenWallRepository {
    private val configsStore = ConcurrentHashMap<String, ServerDrivenWallConfig>()
    private val versionsStore = ConcurrentHashMap<String, WallPublishingVersion>()

    init {
        // Default Sample Wall Configurations
        val p1 = ServerDrivenWallConfig(
            wallId = "WALL-PUBLIC-01",
            wallType = WallCategoryType.PUBLIC,
            wallTitle = "Public & Guest Promotional Wall",
            versionNumber = 1,
            status = WallPublishStatus.PUBLISHED,
            isActivePublished = true,
            publicVisibility = true,
            guestVisibility = true,
            customerVisibility = true,
            affiliateVisibility = true,
            sections = listOf(
                WallSectionConfig(
                    sectionId = "SEC-HERO-01",
                    wallId = "WALL-PUBLIC-01",
                    sectionType = SectionType.HERO_BANNER,
                    sectionName = "Eid Special Hero Banner",
                    sectionTitle = "🎉 বিশেষ ঈদুল ফিতর প্রিন্টিং অফার",
                    layoutType = SectionLayoutType.CAROUSEL,
                    displayOrder = 1,
                    items = listOf(
                        WallSectionItem("ITEM-01", "SEC-HERO-01", offerId = "OFFER-2026-EID", displayOrder = 1, createdAt = "2026-09-26T14:00:00Z")
                    ),
                    createdAt = "2026-09-26T14:00:00Z"
                ),
                WallSectionConfig(
                    sectionId = "SEC-GALLERY-01",
                    wallId = "WALL-PUBLIC-01",
                    sectionType = SectionType.PRODUCT_GALLERY,
                    sectionName = "Popular Commercial Printing Products",
                    sectionTitle = "⭐ জনপ্রিয় পণ্যসমূহ",
                    layoutType = SectionLayoutType.GRID,
                    displayOrder = 2,
                    items = listOf(
                        WallSectionItem("ITEM-02", "SEC-GALLERY-01", productId = "PROD-101", displayOrder = 1, createdAt = "2026-09-26T14:00:00Z")
                    ),
                    createdAt = "2026-09-26T14:00:00Z"
                )
            ),
            createdAt = "2026-09-26T14:00:00Z",
            updatedAt = "2026-09-26T14:00:00Z",
            createdBy = "SYSTEM"
        )
        configsStore[p1.wallId] = p1
    }

    override suspend fun saveWallConfig(config: ServerDrivenWallConfig): ServerDrivenWallConfig {
        configsStore[config.wallId] = config
        return config
    }

    override suspend fun getWallConfigById(wallId: String): ServerDrivenWallConfig? {
        return configsStore[wallId]
    }

    override suspend fun getPublishedWallConfigByType(wallType: WallCategoryType): ServerDrivenWallConfig? {
        return configsStore.values.firstOrNull { it.wallType == wallType && it.isActivePublished }
            ?: configsStore.values.firstOrNull { it.wallType == wallType }
    }

    override suspend fun getAllWallConfigs(): List<ServerDrivenWallConfig> {
        return configsStore.values.toList()
    }

    override suspend fun publishWallVersion(wallId: String, actorId: String): ServerDrivenWallConfig {
        val existing = configsStore[wallId] ?: throw IllegalArgumentException("Wall config not found: $wallId")

        // Unpublish other configs for same wall type
        configsStore.values.filter { it.wallType == existing.wallType }.forEach {
            configsStore[it.wallId] = it.copy(isActivePublished = false)
        }

        val published = existing.copy(
            status = WallPublishStatus.PUBLISHED,
            isActivePublished = true,
            versionNumber = existing.versionNumber + 1,
            updatedBy = actorId
        )
        configsStore[wallId] = published
        return published
    }

    override suspend fun saveWallVersion(version: WallPublishingVersion): WallPublishingVersion {
        versionsStore[version.versionId] = version
        return version
    }

    override suspend fun getWallVersions(wallId: String): List<WallPublishingVersion> {
        return versionsStore.values.filter { it.wallId == wallId }.sortedByDescending { it.versionNumber }
    }

    override suspend fun deleteWallConfig(wallId: String): Boolean {
        return configsStore.remove(wallId) != null
    }
}

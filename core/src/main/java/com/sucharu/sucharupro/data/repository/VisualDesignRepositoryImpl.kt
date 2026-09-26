package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.domain.model.design.DesignPublishStatus
import com.sucharu.sucharupro.domain.model.design.DesignTargetType
import com.sucharu.sucharupro.domain.model.design.VisualDesignConfiguration
import com.sucharu.sucharupro.domain.repository.VisualDesignRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe Repository Implementation for Form 02 Visual Design Studio persistence.
 */
class VisualDesignRepositoryImpl : VisualDesignRepository {
    private val store = ConcurrentHashMap<String, VisualDesignConfiguration>()

    init {
        // Default System Design Configurations
        val d1 = VisualDesignConfiguration(
            designId = "DSGN-STD-001",
            designName = "Default Mobile Gallery Card Theme",
            targetType = DesignTargetType.PRODUCT_GALLERY_CARD,
            versionNumber = 1,
            status = DesignPublishStatus.PUBLISHED,
            isActivePublished = true,
            cardWidthDp = 320,
            cardHeightDp = 420,
            backgroundColorHex = "#1E293B",
            borderColorHex = "#334155",
            borderRadiusDp = 12,
            fontFamily = "SOLAIMANLIPI",
            fontSizeSp = 14,
            textColorHex = "#FFFFFF",
            ctaButtonText = "অর্ডার করুন",
            ctaButtonColorHex = "#EA580C",
            createdAt = "2026-09-26T11:00:00Z",
            updatedAt = "2026-09-26T11:00:00Z",
            createdBy = "SYSTEM"
        )
        store[d1.designId] = d1
    }

    override suspend fun createDesign(config: VisualDesignConfiguration): VisualDesignConfiguration {
        store[config.designId] = config
        return config
    }

    override suspend fun updateDesign(config: VisualDesignConfiguration): VisualDesignConfiguration {
        store[config.designId] = config
        return config
    }

    override suspend fun getDesignById(designId: String): VisualDesignConfiguration? {
        return store[designId]
    }

    override suspend fun getAllDesigns(): List<VisualDesignConfiguration> {
        return store.values.toList()
    }

    override suspend fun getDesignsByTargetType(targetType: DesignTargetType): List<VisualDesignConfiguration> {
        return store.values.filter { it.targetType == targetType }
    }

    override suspend fun getPublishedDesignByTarget(targetType: DesignTargetType, targetId: String?): VisualDesignConfiguration? {
        return store.values.firstOrNull { it.targetType == targetType && it.isActivePublished }
            ?: store.values.firstOrNull { it.targetType == targetType }
    }

    override suspend fun duplicateDesign(sourceDesignId: String, newDesignName: String, actorId: String): VisualDesignConfiguration {
        val existing = store[sourceDesignId] ?: throw IllegalArgumentException("Source design not found: $sourceDesignId")
        val newId = "DSGN-" + UUID.randomUUID().toString().take(8).uppercase()
        val duplicated = existing.copy(
            designId = newId,
            designName = newDesignName,
            status = DesignPublishStatus.DRAFT,
            isActivePublished = false,
            versionNumber = 1,
            createdBy = actorId
        )
        store[newId] = duplicated
        return duplicated
    }

    override suspend fun publishDesign(designId: String, actorId: String): VisualDesignConfiguration {
        val existing = store[designId] ?: throw IllegalArgumentException("Design not found: $designId")

        // Unpublish any other active design for the same target
        store.values.filter { it.targetType == existing.targetType }.forEach {
            store[it.designId] = it.copy(isActivePublished = false)
        }

        val published = existing.copy(
            status = DesignPublishStatus.PUBLISHED,
            isActivePublished = true,
            versionNumber = existing.versionNumber + 1,
            updatedBy = actorId
        )
        store[designId] = published
        return published
    }

    override suspend fun revertDesignVersion(designId: String, versionNumber: Int, actorId: String): VisualDesignConfiguration {
        val existing = store[designId] ?: throw IllegalArgumentException("Design not found: $designId")
        val reverted = existing.copy(
            versionNumber = versionNumber,
            status = DesignPublishStatus.DRAFT,
            updatedBy = actorId
        )
        store[designId] = reverted
        return reverted
    }

    override suspend fun deleteDesign(designId: String): Boolean {
        return store.remove(designId) != null
    }
}

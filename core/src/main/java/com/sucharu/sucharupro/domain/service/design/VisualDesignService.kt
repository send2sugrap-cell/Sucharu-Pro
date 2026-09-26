package com.sucharu.sucharupro.domain.service.design

import com.sucharu.sucharupro.domain.model.design.DesignTargetType
import com.sucharu.sucharupro.domain.model.design.VisualDesignConfiguration
import com.sucharu.sucharupro.domain.repository.VisualDesignRepository

/**
 * Domain Service for Form 02 — Visual Design Studio lifecycle, versions & publishing governance.
 */
class VisualDesignService(
    private val repository: VisualDesignRepository
) {
    suspend fun createDesignStudioConfig(config: VisualDesignConfiguration): VisualDesignConfiguration {
        require(config.designName.isNotBlank()) { "Design name is required." }
        return repository.createDesign(config)
    }

    suspend fun saveDraftDesign(config: VisualDesignConfiguration): VisualDesignConfiguration {
        return repository.updateDesign(config)
    }

    suspend fun publishDesignStudioVersion(designId: String, actorId: String): VisualDesignConfiguration {
        val published = repository.publishDesign(designId, actorId)
        val versionSnapshot = com.sucharu.sucharupro.domain.model.design.VisualDesignVersion(
            versionId = "VER-${published.designId}-${published.versionNumber}",
            designId = published.designId,
            versionNumber = published.versionNumber,
            status = com.sucharu.sucharupro.domain.model.design.DesignPublishStatus.PUBLISHED,
            configurationSnapshot = published,
            createdAt = published.updatedAt,
            createdBy = actorId
        )
        repository.saveDesignVersion(versionSnapshot)
        return published
    }

    suspend fun duplicateDesignStudioConfig(designId: String, newName: String, actorId: String): VisualDesignConfiguration {
        return repository.duplicateDesign(designId, newName, actorId)
    }

    suspend fun revertDesignVersion(designId: String, versionNumber: Int, actorId: String): VisualDesignConfiguration {
        val versionSnapshot = repository.getDesignVersion(designId, versionNumber)
        val baseConfig = versionSnapshot?.configurationSnapshot ?: (repository.getDesignById(designId) ?: throw IllegalArgumentException("Design not found: $designId"))

        val currentVersionNumber = repository.getDesignById(designId)?.versionNumber ?: 1
        val newDraft = baseConfig.copy(
            versionNumber = currentVersionNumber + 1,
            status = com.sucharu.sucharupro.domain.model.design.DesignPublishStatus.DRAFT,
            isActivePublished = false,
            updatedBy = actorId
        )
        return repository.updateDesign(newDraft)
    }

    suspend fun getPublishedDesignForTarget(targetType: DesignTargetType, targetId: String? = null): VisualDesignConfiguration? {
        return repository.getPublishedDesignByTarget(targetType, targetId)
    }

    suspend fun listAllDesigns(): List<VisualDesignConfiguration> {
        return repository.getAllDesigns()
    }
}

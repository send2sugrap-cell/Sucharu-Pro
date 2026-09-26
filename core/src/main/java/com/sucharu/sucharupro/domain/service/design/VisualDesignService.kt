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
        return repository.publishDesign(designId, actorId)
    }

    suspend fun duplicateDesignStudioConfig(designId: String, newName: String, actorId: String): VisualDesignConfiguration {
        return repository.duplicateDesign(designId, newName, actorId)
    }

    suspend fun revertDesignVersion(designId: String, versionNumber: Int, actorId: String): VisualDesignConfiguration {
        return repository.revertDesignVersion(designId, versionNumber, actorId)
    }

    suspend fun getPublishedDesignForTarget(targetType: DesignTargetType, targetId: String? = null): VisualDesignConfiguration? {
        return repository.getPublishedDesignByTarget(targetType, targetId)
    }

    suspend fun listAllDesigns(): List<VisualDesignConfiguration> {
        return repository.getAllDesigns()
    }
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.design.DesignPublishStatus
import com.sucharu.sucharupro.domain.model.design.DesignTargetType
import com.sucharu.sucharupro.domain.model.design.VisualDesignConfiguration

/**
 * Domain Repository Interface for Form 02 — Visual Design Studio persistence.
 */
interface VisualDesignRepository {
    suspend fun createDesign(config: VisualDesignConfiguration): VisualDesignConfiguration
    suspend fun updateDesign(config: VisualDesignConfiguration): VisualDesignConfiguration
    suspend fun getDesignById(designId: String): VisualDesignConfiguration?
    suspend fun getAllDesigns(): List<VisualDesignConfiguration>
    suspend fun getDesignsByTargetType(targetType: DesignTargetType): List<VisualDesignConfiguration>
    suspend fun getPublishedDesignByTarget(targetType: DesignTargetType, targetId: String?): VisualDesignConfiguration?
    suspend fun duplicateDesign(sourceDesignId: String, newDesignName: String, actorId: String): VisualDesignConfiguration
    suspend fun publishDesign(designId: String, actorId: String): VisualDesignConfiguration
    suspend fun revertDesignVersion(designId: String, versionNumber: Int, actorId: String): VisualDesignConfiguration
    suspend fun deleteDesign(designId: String): Boolean
    suspend fun saveDesignVersion(version: com.sucharu.sucharupro.domain.model.design.VisualDesignVersion): com.sucharu.sucharupro.domain.model.design.VisualDesignVersion
    suspend fun getDesignVersions(designId: String): List<com.sucharu.sucharupro.domain.model.design.VisualDesignVersion>
    suspend fun getDesignVersion(designId: String, versionNumber: Int): com.sucharu.sucharupro.domain.model.design.VisualDesignVersion?
}

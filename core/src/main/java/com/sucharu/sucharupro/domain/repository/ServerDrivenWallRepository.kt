package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.wall.ServerDrivenWallConfig
import com.sucharu.sucharupro.domain.model.wall.WallCategoryType
import com.sucharu.sucharupro.domain.model.wall.WallPublishingVersion

/**
 * Domain Repository Contract for Form 06 — Wall / Section / Publishing Control.
 */
interface ServerDrivenWallRepository {
    suspend fun saveWallConfig(config: ServerDrivenWallConfig): ServerDrivenWallConfig
    suspend fun getWallConfigById(wallId: String): ServerDrivenWallConfig?
    suspend fun getPublishedWallConfigByType(wallType: WallCategoryType): ServerDrivenWallConfig?
    suspend fun getAllWallConfigs(): List<ServerDrivenWallConfig>
    suspend fun publishWallVersion(wallId: String, actorId: String): ServerDrivenWallConfig
    suspend fun saveWallVersion(version: WallPublishingVersion): WallPublishingVersion
    suspend fun getWallVersions(wallId: String): List<WallPublishingVersion>
    suspend fun deleteWallConfig(wallId: String): Boolean
}

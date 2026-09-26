package com.sucharu.sucharupro.ui.admin.wall

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.api.model.wall.ResolvedServerDrivenWallResponseDto
import com.sucharu.sucharupro.data.repository.ServerDrivenWallRepositoryImpl
import com.sucharu.sucharupro.domain.model.offer.AudienceType
import com.sucharu.sucharupro.domain.model.wall.ServerDrivenWallConfig
import com.sucharu.sucharupro.domain.model.wall.WallCategoryType
import com.sucharu.sucharupro.domain.model.wall.WallPublishStatus
import com.sucharu.sucharupro.domain.repository.ServerDrivenWallRepository
import com.sucharu.sucharupro.domain.service.wall.ServerDrivenWallService
import kotlinx.coroutines.launch

/**
 * ViewModel for Form 06 — Admin Wall / Section / Publishing Control.
 */
class AdminWallPublishingViewModel(
    repository: ServerDrivenWallRepository = ServerDrivenWallRepositoryImpl()
) : ViewModel() {

    private val service = ServerDrivenWallService(repository)

    var wallConfigsList = mutableStateListOf<ServerDrivenWallConfig>()
        private set

    var activeWallConfig by mutableStateOf<ServerDrivenWallConfig>(
        ServerDrivenWallConfig(
            wallId = "WALL-PUBLIC-01",
            wallType = WallCategoryType.PUBLIC,
            wallTitle = "Public & Guest Promotional Wall",
            versionNumber = 1,
            status = WallPublishStatus.PUBLISHED,
            isActivePublished = true,
            createdAt = "2026-09-26T14:45:00Z",
            updatedAt = "2026-09-26T14:45:00Z",
            createdBy = "ADMIN"
        )
    )
        private set

    var resolvedWallResponse by mutableStateOf<ResolvedServerDrivenWallResponseDto?>(null)
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadWallConfigs()
    }

    fun loadWallConfigs() {
        viewModelScope.launch {
            val list = service.listAllWallConfigs()
            wallConfigsList.clear()
            wallConfigsList.addAll(list)
            list.firstOrNull()?.let { activeWallConfig = it }
        }
    }

    fun updateActiveWall(updated: ServerDrivenWallConfig) {
        activeWallConfig = updated
    }

    fun saveDraft() {
        viewModelScope.launch {
            val timestamp = "2026-09-26T14:45:00Z"
            val saved = service.saveWallConfig(
                activeWallConfig.copy(
                    status = WallPublishStatus.DRAFT,
                    updatedAt = timestamp,
                    updatedBy = "ADMIN"
                )
            )
            activeWallConfig = saved
            loadWallConfigs()
            statusMessage = "✓ Server-Driven Wall draft saved!"
        }
    }

    fun publishWall() {
        viewModelScope.launch {
            val version = service.publishWallVersion(activeWallConfig.wallId, "ADMIN")
            loadWallConfigs()
            statusMessage = "🎉 Server-Driven Wall Published! Version v${version.versionNumber} is now live!"
        }
    }

    fun testResolveWall(audience: AudienceType) {
        viewModelScope.launch {
            val resolved = service.resolveServerDrivenWall(activeWallConfig.wallType, audience)
            resolvedWallResponse = resolved
        }
    }
}

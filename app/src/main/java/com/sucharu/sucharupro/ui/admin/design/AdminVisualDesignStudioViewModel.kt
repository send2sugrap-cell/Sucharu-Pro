package com.sucharu.sucharupro.ui.admin.design

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.repository.VisualDesignRepositoryImpl
import com.sucharu.sucharupro.domain.model.design.DesignPublishStatus
import com.sucharu.sucharupro.domain.model.design.DesignTargetType
import com.sucharu.sucharupro.domain.model.design.VisualDesignConfiguration
import com.sucharu.sucharupro.domain.repository.VisualDesignRepository
import com.sucharu.sucharupro.domain.service.design.VisualDesignService
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for Form 02 — Admin Visual Design Studio Workspace.
 */
class AdminVisualDesignStudioViewModel(
    repository: VisualDesignRepository = VisualDesignRepositoryImpl()
) : ViewModel() {

    private val service = VisualDesignService(repository)

    var designList = mutableStateListOf<VisualDesignConfiguration>()
        private set

    var activeDesignState by mutableStateOf<VisualDesignConfiguration>(
        VisualDesignConfiguration(
            designId = "DSGN-STD-001",
            designName = "Primary Product Gallery Card Style",
            createdAt = "2026-09-26T11:00:00Z",
            updatedAt = "2026-09-26T11:00:00Z",
            createdBy = "ADMIN"
        )
    )
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadDesigns()
    }

    fun loadDesigns() {
        viewModelScope.launch {
            val list = service.listAllDesigns()
            designList.clear()
            designList.addAll(list)
            list.firstOrNull()?.let { activeDesignState = it }
        }
    }

    fun updateActiveDesign(updated: VisualDesignConfiguration) {
        activeDesignState = updated
    }

    fun saveDraft() {
        viewModelScope.launch {
            val timestamp = "2026-09-26T11:30:00Z"
            val saved = service.saveDraftDesign(
                activeDesignState.copy(
                    status = DesignPublishStatus.DRAFT,
                    updatedAt = timestamp,
                    updatedBy = "ADMIN"
                )
            )
            activeDesignState = saved
            loadDesigns()
            statusMessage = "✓ Design draft saved successfully!"
        }
    }

    fun publishActiveDesign() {
        viewModelScope.launch {
            val published = service.publishDesignStudioVersion(activeDesignState.designId, "ADMIN")
            activeDesignState = published
            loadDesigns()
            statusMessage = "🎉 Design configuration published live!"
        }
    }

    fun duplicateCurrentDesign() {
        viewModelScope.launch {
            val dup = service.duplicateDesignStudioConfig(activeDesignState.designId, "${activeDesignState.designName} (Copy)", "ADMIN")
            activeDesignState = dup
            loadDesigns()
            statusMessage = "✓ Design duplicated!"
        }
    }
}

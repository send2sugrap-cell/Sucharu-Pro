package com.sucharu.sucharupro.ui.features.inventory.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryLocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating storage location details, parent resolution, and children list (Module 07 Step 02).
 */
class InventoryLocationDetailsViewModel(
    private val repository: InventoryLocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryLocationDetailsUiState())
    val uiState: StateFlow<InventoryLocationDetailsUiState> = _uiState.asStateFlow()

    fun loadLocation(locationId: String, projectId: String, role: UserRole = UserRole.MANAGER) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val locRes = repository.getLocationById(locationId, role)
            if (locRes is DomainResult.Success) {
                val location = locRes.data
                val parentId = location.parentLocationId
                val parent = if (parentId != null) {
                    val pRes = repository.getLocationById(parentId, role)
                    if (pRes is DomainResult.Success) pRes.data else null
                } else null

                repository.observeChildLocations(locationId, projectId).collect { children ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            location = location,
                            parentLocation = parent,
                            childLocations = children
                        )
                    }
                }
            } else if (locRes is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = locRes.message) }
            }
        }
    }

    fun activateLocation(actorId: String, timestamp: String, role: UserRole = UserRole.MANAGER) {
        val currentLoc = _uiState.value.location ?: return
        viewModelScope.launch {
            val res = repository.activateLocation(currentLoc.id, actorId, timestamp, role)
            if (res is DomainResult.Success) {
                loadLocation(currentLoc.id, currentLoc.projectId, role)
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun deactivateLocation(actorId: String, timestamp: String, role: UserRole = UserRole.MANAGER) {
        val currentLoc = _uiState.value.location ?: return
        viewModelScope.launch {
            val res = repository.deactivateLocation(currentLoc.id, actorId, timestamp, role)
            if (res is DomainResult.Success) {
                loadLocation(currentLoc.id, currentLoc.projectId, role)
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun archiveLocation(actorId: String, timestamp: String, role: UserRole = UserRole.MANAGER) {
        val currentLoc = _uiState.value.location ?: return
        viewModelScope.launch {
            val res = repository.archiveLocation(currentLoc.id, actorId, timestamp, role)
            if (res is DomainResult.Success) {
                loadLocation(currentLoc.id, currentLoc.projectId, role)
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }
}

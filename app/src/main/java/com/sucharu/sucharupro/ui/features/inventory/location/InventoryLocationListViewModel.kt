package com.sucharu.sucharupro.ui.features.inventory.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocation
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryLocationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryLocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating storage locations list and tree filtering (Module 07 Step 02).
 */
class InventoryLocationListViewModel(
    private val repository: InventoryLocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryLocationListUiState(isLoading = true))
    val uiState: StateFlow<InventoryLocationListUiState> = _uiState.asStateFlow()

    fun loadLocations(projectId: String, warehouseId: String? = null) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, warehouseId = warehouseId, errorMessage = null) }
        viewModelScope.launch {
            val flow = if (warehouseId != null) {
                repository.observeLocationsByWarehouse(warehouseId, projectId)
            } else {
                repository.observeLocations(projectId)
            }
            flow.collect { list ->
                _uiState.update { current ->
                    val filtered = applyFilters(
                        locations = list,
                        query = current.searchQuery,
                        typeFilter = current.selectedTypeFilter,
                        statusFilter = current.selectedStatusFilter
                    )
                    current.copy(
                        isLoading = false,
                        locations = list,
                        filteredLocations = filtered
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(
                locations = current.locations,
                query = query,
                typeFilter = current.selectedTypeFilter,
                statusFilter = current.selectedStatusFilter
            )
            current.copy(searchQuery = query, filteredLocations = filtered)
        }
    }

    fun onTypeFilterChanged(type: InventoryLocationType?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                locations = current.locations,
                query = current.searchQuery,
                typeFilter = type,
                statusFilter = current.selectedStatusFilter
            )
            current.copy(selectedTypeFilter = type, filteredLocations = filtered)
        }
    }

    fun onStatusFilterChanged(status: InventoryLocationStatus?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                locations = current.locations,
                query = current.searchQuery,
                typeFilter = current.selectedTypeFilter,
                statusFilter = status
            )
            current.copy(selectedStatusFilter = status, filteredLocations = filtered)
        }
    }

    fun activateLocation(locationId: String, actorId: String, timestamp: String, role: UserRole = UserRole.MANAGER) {
        viewModelScope.launch {
            val res = repository.activateLocation(locationId, actorId, timestamp, role)
            if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun deactivateLocation(locationId: String, actorId: String, timestamp: String, role: UserRole = UserRole.MANAGER) {
        viewModelScope.launch {
            val res = repository.deactivateLocation(locationId, actorId, timestamp, role)
            if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun archiveLocation(locationId: String, actorId: String, timestamp: String, role: UserRole = UserRole.MANAGER) {
        viewModelScope.launch {
            val res = repository.archiveLocation(locationId, actorId, timestamp, role)
            if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    private fun applyFilters(
        locations: List<InventoryLocation>,
        query: String,
        typeFilter: InventoryLocationType?,
        statusFilter: InventoryLocationStatus?
    ): List<InventoryLocation> {
        val q = query.trim().lowercase()
        return locations.filter { l ->
            val matchesQuery = q.isBlank() || l.name.lowercase().contains(q) || l.code.lowercase().contains(q)
            val matchesType = typeFilter == null || l.type == typeFilter
            val matchesStatus = statusFilter == null || l.status == statusFilter
            matchesQuery && matchesType && matchesStatus
        }
    }
}

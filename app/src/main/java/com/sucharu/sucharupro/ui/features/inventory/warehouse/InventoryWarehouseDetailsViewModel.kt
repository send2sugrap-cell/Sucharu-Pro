package com.sucharu.sucharupro.ui.features.inventory.warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryLocationRepository
import com.sucharu.sucharupro.domain.repository.InventoryWarehouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating warehouse details and child locations view (Module 07 Step 02).
 */
class InventoryWarehouseDetailsViewModel(
    private val warehouseRepository: InventoryWarehouseRepository,
    private val locationRepository: InventoryLocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryWarehouseDetailsUiState())
    val uiState: StateFlow<InventoryWarehouseDetailsUiState> = _uiState.asStateFlow()

    fun loadWarehouse(warehouseId: String, projectId: String, role: UserRole = UserRole.MANAGER) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val whResult = warehouseRepository.getWarehouseById(warehouseId, role)
            if (whResult is DomainResult.Success) {
                val warehouse = whResult.data
                locationRepository.observeLocationsByWarehouse(warehouseId, projectId).collect { locs ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            warehouse = warehouse,
                            locations = locs
                        )
                    }
                }
            } else if (whResult is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = whResult.message) }
            }
        }
    }

    fun activateWarehouse(actorId: String, timestamp: String, role: UserRole = UserRole.MANAGER) {
        val currentWh = _uiState.value.warehouse ?: return
        viewModelScope.launch {
            val res = warehouseRepository.activateWarehouse(currentWh.id, actorId, timestamp, role)
            if (res is DomainResult.Success) {
                loadWarehouse(currentWh.id, currentWh.projectId, role)
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun deactivateWarehouse(actorId: String, timestamp: String, role: UserRole = UserRole.MANAGER) {
        val currentWh = _uiState.value.warehouse ?: return
        viewModelScope.launch {
            val res = warehouseRepository.deactivateWarehouse(currentWh.id, actorId, timestamp, role)
            if (res is DomainResult.Success) {
                loadWarehouse(currentWh.id, currentWh.projectId, role)
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun archiveWarehouse(actorId: String, timestamp: String, role: UserRole = UserRole.MANAGER) {
        val currentWh = _uiState.value.warehouse ?: return
        viewModelScope.launch {
            val res = warehouseRepository.archiveWarehouse(currentWh.id, actorId, timestamp, role)
            if (res is DomainResult.Success) {
                loadWarehouse(currentWh.id, currentWh.projectId, role)
            } else if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }
}

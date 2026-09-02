package com.sucharu.sucharupro.ui.features.inventory.warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouse
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseStatus
import com.sucharu.sucharupro.domain.model.inventory.InventoryWarehouseType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryWarehouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating physical warehouse list operations and reactive filtering (Module 07 Step 02).
 */
class InventoryWarehouseListViewModel(
    private val repository: InventoryWarehouseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryWarehouseListUiState(isLoading = true))
    val uiState: StateFlow<InventoryWarehouseListUiState> = _uiState.asStateFlow()

    fun loadWarehouses(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        viewModelScope.launch {
            repository.observeWarehouses(projectId).collect { list ->
                _uiState.update { current ->
                    val filtered = applyFilters(
                        warehouses = list,
                        query = current.searchQuery,
                        typeFilter = current.selectedTypeFilter,
                        statusFilter = current.selectedStatusFilter
                    )
                    current.copy(
                        isLoading = false,
                        warehouses = list,
                        filteredWarehouses = filtered
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(
                warehouses = current.warehouses,
                query = query,
                typeFilter = current.selectedTypeFilter,
                statusFilter = current.selectedStatusFilter
            )
            current.copy(searchQuery = query, filteredWarehouses = filtered)
        }
    }

    fun onTypeFilterChanged(type: InventoryWarehouseType?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                warehouses = current.warehouses,
                query = current.searchQuery,
                typeFilter = type,
                statusFilter = current.selectedStatusFilter
            )
            current.copy(selectedTypeFilter = type, filteredWarehouses = filtered)
        }
    }

    fun onStatusFilterChanged(status: InventoryWarehouseStatus?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                warehouses = current.warehouses,
                query = current.searchQuery,
                typeFilter = current.selectedTypeFilter,
                statusFilter = status
            )
            current.copy(selectedStatusFilter = status, filteredWarehouses = filtered)
        }
    }

    fun activateWarehouse(warehouseId: String, actorId: String, timestamp: String, role: UserRole = UserRole.MANAGER) {
        viewModelScope.launch {
            val res = repository.activateWarehouse(warehouseId, actorId, timestamp, role)
            if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun deactivateWarehouse(warehouseId: String, actorId: String, timestamp: String, role: UserRole = UserRole.MANAGER) {
        viewModelScope.launch {
            val res = repository.deactivateWarehouse(warehouseId, actorId, timestamp, role)
            if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    fun archiveWarehouse(warehouseId: String, actorId: String, timestamp: String, role: UserRole = UserRole.MANAGER) {
        viewModelScope.launch {
            val res = repository.archiveWarehouse(warehouseId, actorId, timestamp, role)
            if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            }
        }
    }

    private fun applyFilters(
        warehouses: List<InventoryWarehouse>,
        query: String,
        typeFilter: InventoryWarehouseType?,
        statusFilter: InventoryWarehouseStatus?
    ): List<InventoryWarehouse> {
        val q = query.trim().lowercase()
        return warehouses.filter { w ->
            val matchesQuery = q.isBlank() || w.name.lowercase().contains(q) || w.code.lowercase().contains(q)
            val matchesType = typeFilter == null || w.type == typeFilter
            val matchesStatus = statusFilter == null || w.status == statusFilter
            matchesQuery && matchesType && matchesStatus
        }
    }
}

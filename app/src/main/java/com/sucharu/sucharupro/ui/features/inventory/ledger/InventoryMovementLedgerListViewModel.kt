package com.sucharu.sucharupro.ui.features.inventory.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerType
import com.sucharu.sucharupro.domain.repository.InventoryMovementLedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating the movement ledger list screen (Module 07 Step 09).
 */
class InventoryMovementLedgerListViewModel(
    private val repository: InventoryMovementLedgerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryMovementLedgerListUiState(isLoading = true))
    val uiState: StateFlow<InventoryMovementLedgerListUiState> = _uiState.asStateFlow()

    fun loadLedger(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        viewModelScope.launch {
            repository.observeEntries(projectId).collect { list ->
                _uiState.update { current ->
                    val filtered = applyFilters(
                        entries = list,
                        query = current.searchQuery,
                        typeFilter = current.selectedTypeFilter,
                        productId = current.selectedProductId,
                        locationId = current.selectedLocationId
                    )
                    current.copy(
                        isLoading = false,
                        entries = list,
                        filteredEntries = filtered
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(
                entries = current.entries,
                query = query,
                typeFilter = current.selectedTypeFilter,
                productId = current.selectedProductId,
                locationId = current.selectedLocationId
            )
            current.copy(searchQuery = query, filteredEntries = filtered)
        }
    }

    fun onTypeFilterChanged(type: InventoryMovementLedgerType?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                entries = current.entries,
                query = current.searchQuery,
                typeFilter = type,
                productId = current.selectedProductId,
                locationId = current.selectedLocationId
            )
            current.copy(selectedTypeFilter = type, filteredEntries = filtered)
        }
    }

    private fun applyFilters(
        entries: List<InventoryMovementLedgerEntry>,
        query: String,
        typeFilter: InventoryMovementLedgerType?,
        productId: String?,
        locationId: String?
    ): List<InventoryMovementLedgerEntry> {
        val q = query.trim().lowercase()
        return entries.filter { e ->
            val matchesQuery = q.isBlank() ||
                e.referenceId.lowercase().contains(q) ||
                e.productId.lowercase().contains(q)
            val matchesType = typeFilter == null || e.movementType == typeFilter
            val matchesProduct = productId == null || e.productId == productId
            val matchesLocation = locationId == null || e.locationId == locationId
            matchesQuery && matchesType && matchesProduct && matchesLocation
        }
    }

    fun synchronizeLedger() {
        val projectId = _uiState.value.projectId
        if (projectId.isBlank()) return
        
        viewModelScope.launch {
            repository.synchronizeLedger(projectId)
        }
    }
}

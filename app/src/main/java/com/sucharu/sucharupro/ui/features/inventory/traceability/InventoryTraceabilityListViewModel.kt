package com.sucharu.sucharupro.ui.features.inventory.traceability

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryBatch
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryLot
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryTraceabilityStatus
import com.sucharu.sucharupro.domain.repository.InventoryTraceabilityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating the batch and lot traceability list screen (Module 07 Step 07).
 */
class InventoryTraceabilityListViewModel(
    private val repository: InventoryTraceabilityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryTraceabilityListUiState(isLoading = true))
    val uiState: StateFlow<InventoryTraceabilityListUiState> = _uiState.asStateFlow()

    fun loadTraceabilityData(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        
        viewModelScope.launch {
            repository.observeBatches(projectId).collect { list ->
                _uiState.update { current ->
                    val filtered = applyBatchFilters(list, current.searchQuery, current.selectedStatusFilter)
                    current.copy(batches = list, filteredBatches = filtered, isLoading = false)
                }
            }
        }

        viewModelScope.launch {
            repository.observeLots(projectId).collect { list ->
                _uiState.update { current ->
                    val filtered = applyLotFilters(list, current.searchQuery, current.selectedStatusFilter)
                    current.copy(lots = list, filteredLots = filtered)
                }
            }
        }
    }

    fun setViewMode(mode: TraceabilityViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filteredBatches = applyBatchFilters(current.batches, query, current.selectedStatusFilter)
            val filteredLots = applyLotFilters(current.lots, query, current.selectedStatusFilter)
            current.copy(searchQuery = query, filteredBatches = filteredBatches, filteredLots = filteredLots)
        }
    }

    fun onStatusFilterChanged(status: InventoryTraceabilityStatus?) {
        _uiState.update { current ->
            val filteredBatches = applyBatchFilters(current.batches, current.searchQuery, status)
            val filteredLots = applyLotFilters(current.lots, current.searchQuery, status)
            current.copy(selectedStatusFilter = status, filteredBatches = filteredBatches, filteredLots = filteredLots)
        }
    }

    private fun applyBatchFilters(
        batches: List<InventoryBatch>,
        query: String,
        statusFilter: InventoryTraceabilityStatus?
    ): List<InventoryBatch> {
        val q = query.trim().lowercase()
        return batches.filter { b ->
            val matchesQuery = q.isBlank() || b.batchNo.lowercase().contains(q) || b.productId.lowercase().contains(q)
            val matchesStatus = statusFilter == null || b.status == statusFilter
            matchesQuery && matchesStatus
        }
    }

    private fun applyLotFilters(
        lots: List<InventoryLot>,
        query: String,
        statusFilter: InventoryTraceabilityStatus?
    ): List<InventoryLot> {
        val q = query.trim().lowercase()
        return lots.filter { l ->
            val matchesQuery = q.isBlank() || l.lotNo.lowercase().contains(q) || l.productId.lowercase().contains(q)
            val matchesStatus = statusFilter == null || l.status == statusFilter
            matchesQuery && matchesStatus
        }
    }
}

package com.sucharu.sucharupro.ui.features.inventory.traceability

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryTraceabilityRecord
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryTraceabilityStatus
import com.sucharu.sucharupro.domain.repository.InventoryTraceabilityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating the traceability details screen (Module 07 Step 07).
 */
class InventoryTraceabilityDetailsViewModel(
    private val repository: InventoryTraceabilityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryTraceabilityDetailsUiState(isLoading = true))
    val uiState: StateFlow<InventoryTraceabilityDetailsUiState> = _uiState.asStateFlow()

    fun loadBatchDetails(batchId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val batch = repository.getBatchDetails(batchId)
            if (batch != null) {
                val history = repository.getTraceHistory(batchId, "BATCH")
                val totalQty = calculateTotalQuantity(history)
                _uiState.update { it.copy(isLoading = false, batch = batch, lot = null, traceHistory = history, totalQuantity = totalQty) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Batch not found.") }
            }
        }
    }

    fun loadLotDetails(lotId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val lot = repository.getLotDetails(lotId)
            if (lot != null) {
                val history = repository.getTraceHistory(lotId, "LOT")
                val totalQty = calculateTotalQuantity(history)
                _uiState.update { it.copy(isLoading = false, lot = lot, batch = null, traceHistory = history, totalQuantity = totalQty) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Lot not found.") }
            }
        }
    }

    fun updateStatus(status: InventoryTraceabilityStatus, actorId: String, actorName: String?) {
        val currentState = _uiState.value
        viewModelScope.launch {
            if (currentState.batch != null) {
                repository.updateBatchStatus(currentState.batch.batchId, status, actorId, actorName)
                loadBatchDetails(currentState.batch.batchId)
            } else if (currentState.lot != null) {
                repository.updateLotStatus(currentState.lot.lotId, status, actorId, actorName)
                loadLotDetails(currentState.lot.lotId)
            }
        }
    }

    private fun calculateTotalQuantity(history: List<Any>): Double {
        return history.filterIsInstance<InventoryTraceabilityRecord>().sumOf { record ->
            // This is a simplification. Real logic might depend on movement types.
            // But usually traceability records are associated with movements that add or remove from that specific batch/lot.
            record.quantity 
        }
    }
}

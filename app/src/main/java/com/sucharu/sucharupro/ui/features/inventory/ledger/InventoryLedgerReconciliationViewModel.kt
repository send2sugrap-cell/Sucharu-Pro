package com.sucharu.sucharupro.ui.features.inventory.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryLedgerReconciliationResult
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementDirection
import com.sucharu.sucharupro.domain.repository.InventoryMovementLedgerRepository
import com.sucharu.sucharupro.domain.service.inventory.InventoryLedgerReconciliationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating the ledger reconciliation view (Module 07 Step 09).
 */
class InventoryLedgerReconciliationViewModel(
    private val repository: InventoryMovementLedgerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryLedgerReconciliationUiState(isLoading = true))
    val uiState: StateFlow<InventoryLedgerReconciliationUiState> = _uiState.asStateFlow()

    fun runReconciliation(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        viewModelScope.launch {
            repository.observeEntries(projectId).collect { entries ->
                val grouped = entries.groupBy { it.productId to it.locationId }
                val results = grouped.map { (key, groupEntries) ->
                    val (productId, locationId) = key
                    val ledgerQty = groupEntries.sumOf { it.quantity }
                    
                    // In a real scenario, sourceCalculatedQuantity would come from an independent check
                    // For this implementation, we simulate it by calling a repository method if available
                    // or just using the ledger sum as source (which would always match unless simulated mismatch)
                    val sourceQty = repository.getBalance(projectId, productId, locationId)
                    
                    InventoryLedgerReconciliationService.reconcile(
                        projectId = projectId,
                        productId = productId,
                        locationId = locationId,
                        ledgerQuantity = ledgerQty,
                        sourceCalculatedQuantity = sourceQty
                    )
                }
                
                _uiState.update { current ->
                    val filtered = applyFilters(results, current.searchQuery)
                    current.copy(
                        isLoading = false,
                        results = results,
                        filteredResults = filtered
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(current.results, query)
            current.copy(searchQuery = query, filteredResults = filtered)
        }
    }

    private fun applyFilters(
        results: List<InventoryLedgerReconciliationResult>,
        query: String
    ): List<InventoryLedgerReconciliationResult> {
        val q = query.trim().lowercase()
        return if (q.isBlank()) results else {
            results.filter { 
                it.productId.lowercase().contains(q) || it.locationId.lowercase().contains(q)
            }
        }
    }
}

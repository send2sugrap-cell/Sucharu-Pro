package com.sucharu.sucharupro.ui.features.inventory.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementDirection
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry
import com.sucharu.sucharupro.domain.repository.InventoryMovementLedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating the inventory balance view (Module 07 Step 09).
 */
class InventoryInventoryBalanceViewModel(
    private val repository: InventoryMovementLedgerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryInventoryBalanceUiState(isLoading = true))
    val uiState: StateFlow<InventoryInventoryBalanceUiState> = _uiState.asStateFlow()

    fun loadBalances(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        viewModelScope.launch {
            repository.observeEntries(projectId).collect { entries ->
                val groupedBalances = calculateGroupedBalances(entries)
                _uiState.update { current ->
                    val filtered = applyFilters(groupedBalances, current.searchQuery)
                    current.copy(
                        isLoading = false,
                        balances = groupedBalances,
                        filteredBalances = filtered
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(current.balances, query)
            current.copy(searchQuery = query, filteredBalances = filtered)
        }
    }

    private fun calculateGroupedBalances(entries: List<InventoryMovementLedgerEntry>): List<ProductLocationBalance> {
        val groups = entries.groupBy { it.productId to it.locationId }
        return groups.map { (key, groupEntries) ->
            val (productId, locationId) = key
            val totalIn = groupEntries.filter { it.direction == InventoryMovementDirection.IN }.sumOf { it.quantity }
            val totalOut = groupEntries.filter { it.direction == InventoryMovementDirection.OUT }.sumOf { kotlin.math.abs(it.quantity) }
            ProductLocationBalance(
                productId = productId,
                locationId = locationId,
                openingBalance = 0.0, // Assuming 0 for now as ledger is the source of truth
                totalIn = totalIn,
                totalOut = totalOut,
                closingBalance = totalIn - totalOut
            )
        }
    }

    private fun applyFilters(
        balances: List<ProductLocationBalance>,
        query: String
    ): List<ProductLocationBalance> {
        val q = query.trim().lowercase()
        return if (q.isBlank()) balances else {
            balances.filter { 
                it.productId.lowercase().contains(q) || it.locationId.lowercase().contains(q)
            }
        }
    }
}

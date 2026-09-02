package com.sucharu.sucharupro.ui.features.inventory.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryValuationMethod
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryValuationSnapshot
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryMovementLedgerRepository
import com.sucharu.sucharupro.domain.service.inventory.InventoryValuationCalculator
import com.sucharu.sucharupro.domain.service.inventory.ValuationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel orchestrating the valuation summary (Module 07 Step 09).
 */
class InventoryValuationSummaryViewModel(
    private val repository: InventoryMovementLedgerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryValuationSummaryUiState(isLoading = true))
    val uiState: StateFlow<InventoryValuationSummaryUiState> = _uiState.asStateFlow()

    fun loadValuation(projectId: String, role: UserRole?) {
        val hasAccess = role?.hasFinancialAccess == true
        _uiState.update { 
            it.copy(
                isLoading = true, 
                projectId = projectId, 
                hasFinancialAccess = hasAccess,
                errorMessage = if (!hasAccess) "Access Denied: Financial permissions required." else null
            ) 
        }
        
        if (!hasAccess) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        viewModelScope.launch {
            repository.observeEntries(projectId).collect { entries ->
                calculateSnapshots(entries, _uiState.value.selectedMethod)
            }
        }
    }

    fun onMethodChanged(method: InventoryValuationMethod) {
        _uiState.update { it.copy(selectedMethod = method, isLoading = true) }
        viewModelScope.launch {
            val entriesResult = repository.getEntries(_uiState.value.projectId)
            if (entriesResult is com.sucharu.sucharupro.domain.model.common.DomainResult.Success) {
                calculateSnapshots(entriesResult.data, method)
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load entries") }
            }
        }
    }

    private fun calculateSnapshots(
        entries: List<InventoryMovementLedgerEntry>,
        method: InventoryValuationMethod
    ) {
        val groupedEntries = entries.groupBy { it.productId }
        val snapshots = groupedEntries.mapNotNull { (productId, productEntries) ->
            val result = InventoryValuationCalculator.calculateValuation(productEntries, method)
            if (result is ValuationResult.Success) {
                InventoryValuationSnapshot(
                    snapshotId = UUID.randomUUID().toString(),
                    projectId = _uiState.value.projectId,
                    productId = productId,
                    quantity = productEntries.sumOf { it.quantity },
                    unitCost = result.unitCost,
                    totalValue = result.totalValue,
                    valuationMethod = method,
                    calculatedAt = "2026-08-17T17:00:00Z" // Placeholder for current time
                )
            } else null
        }

        _uiState.update { current ->
            current.copy(
                isLoading = false,
                snapshots = snapshots,
                totalInventoryValue = snapshots.sumOf { it.totalValue }
            )
        }
    }
}

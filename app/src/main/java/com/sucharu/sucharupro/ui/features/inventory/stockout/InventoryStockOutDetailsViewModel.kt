package com.sucharu.sucharupro.ui.features.inventory.stockout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryStockOutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating the stock-out details screen (Module 07 Step 04).
 *
 * Provides reactive observation of a single stock-out's header, lines, records,
 * and audit history. Exposes actions gated by role and current status.
 */
class InventoryStockOutDetailsViewModel(
    private val repository: InventoryStockOutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryStockOutDetailsUiState(isLoading = true))
    val uiState: StateFlow<InventoryStockOutDetailsUiState> = _uiState.asStateFlow()

    fun loadStockOut(stockOutId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.observeStockOut(stockOutId).collect { stockOut ->
                _uiState.update { current ->
                    current.copy(isLoading = false, stockOut = stockOut)
                }
                if (stockOut != null) {
                    loadRelatedData(stockOut.projectId, stockOut.stockOutId)
                }
            }
        }
    }

    private fun loadRelatedData(projectId: String, stockOutId: String) {
        viewModelScope.launch {
            repository.observeStockOutLines(stockOutId).collect { lines ->
                _uiState.update { current -> current.copy(lines = lines) }
            }
        }
        viewModelScope.launch {
            repository.observeStockOutRecords(projectId).collect { records ->
                val filtered = records.filter { it.stockOutId == stockOutId }
                _uiState.update { current -> current.copy(stockOutRecords = filtered) }
            }
        }
        viewModelScope.launch {
            val auditResult = repository.getAuditHistory(stockOutId)
            if (auditResult is DomainResult.Success) {
                _uiState.update { current -> current.copy(auditEvents = auditResult.data) }
            }
        }
    }

    fun submitStockOut(
        stockOutId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.submitStockOut(stockOutId, actorId, timestamp, role)
            handleResult(res, "Stock-out submitted.")
        }
    }

    fun approveStockOut(
        stockOutId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.approveStockOut(stockOutId, actorId, timestamp, role)
            handleResult(res, "Stock-out approved.")
        }
    }

    fun completeStockOut(
        stockOutId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.completeStockOut(stockOutId, actorId, timestamp, role)
            handleResult(res, "Stock-out completed.")
        }
    }

    fun cancelStockOut(
        stockOutId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.cancelStockOut(stockOutId, actorId, timestamp, role)
            handleResult(res, "Stock-out cancelled.")
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, operationMessage = null) }
    }

    private fun handleResult(result: DomainResult<*>, successMsg: String) {
        if (result is DomainResult.Error) {
            _uiState.update { it.copy(errorMessage = result.message) }
        } else {
            _uiState.update { it.copy(operationMessage = successMsg, errorMessage = null) }
        }
    }
}

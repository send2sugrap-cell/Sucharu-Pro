package com.sucharu.sucharupro.ui.features.inventory.adjustment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryStockAdjustmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating the stock adjustment details screen (Module 07 Step 06).
 *
 * Provides reactive observation of a single adjustment's header, lines, records,
 * and audit history. Exposes actions gated by role and current status.
 */
class InventoryStockAdjustmentDetailsViewModel(
    private val repository: InventoryStockAdjustmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryStockAdjustmentDetailsUiState(isLoading = true))
    val uiState: StateFlow<InventoryStockAdjustmentDetailsUiState> = _uiState.asStateFlow()

    fun loadAdjustment(adjustmentId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.observeStockAdjustment(adjustmentId).collect { adjustment ->
                _uiState.update { current ->
                    current.copy(isLoading = false, adjustment = adjustment)
                }
                if (adjustment != null) {
                    loadRelatedData(adjustment.projectId, adjustment.adjustmentId)
                }
            }
        }
    }

    private fun loadRelatedData(projectId: String, adjustmentId: String) {
        viewModelScope.launch {
            repository.observeStockAdjustmentLines(adjustmentId).collect { lines ->
                _uiState.update { current -> current.copy(lines = lines) }
            }
        }
        viewModelScope.launch {
            repository.observeStockAdjustmentRecords(projectId).collect { records ->
                val filtered = records.filter { it.adjustmentId == adjustmentId }
                _uiState.update { current -> current.copy(adjustmentRecords = filtered) }
            }
        }
        viewModelScope.launch {
            val auditResult = repository.getAuditHistory(adjustmentId)
            if (auditResult is DomainResult.Success) {
                _uiState.update { current -> current.copy(auditEvents = auditResult.data) }
            }
        }
    }

    fun submitAdjustment(
        adjustmentId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.submitStockAdjustment(adjustmentId, actorId, timestamp, role)
            handleResult(res, "Stock adjustment submitted.")
        }
    }

    fun approveAdjustment(
        adjustmentId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.approveStockAdjustment(adjustmentId, actorId, timestamp, role)
            handleResult(res, "Stock adjustment approved.")
        }
    }

    fun completeAdjustment(
        adjustmentId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.completeStockAdjustment(adjustmentId, actorId, timestamp, role)
            handleResult(res, "Stock adjustment completed.")
        }
    }

    fun cancelAdjustment(
        adjustmentId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.cancelStockAdjustment(adjustmentId, actorId, timestamp, role)
            handleResult(res, "Stock adjustment cancelled.")
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

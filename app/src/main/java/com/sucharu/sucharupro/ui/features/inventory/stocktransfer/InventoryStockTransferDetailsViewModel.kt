package com.sucharu.sucharupro.ui.features.inventory.stocktransfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryStockTransferRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating the stock transfer details screen (Module 07 Step 05).
 *
 * Provides reactive observation of a single transfer's header, lines, records,
 * and audit history. Exposes actions gated by role and current status.
 */
class InventoryStockTransferDetailsViewModel(
    private val repository: InventoryStockTransferRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryStockTransferDetailsUiState(isLoading = true))
    val uiState: StateFlow<InventoryStockTransferDetailsUiState> = _uiState.asStateFlow()

    fun loadTransfer(transferId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.observeStockTransfer(transferId).collect { transfer ->
                _uiState.update { current ->
                    current.copy(isLoading = false, transfer = transfer)
                }
                if (transfer != null) {
                    loadRelatedData(transfer.projectId, transfer.transferId)
                }
            }
        }
    }

    private fun loadRelatedData(projectId: String, transferId: String) {
        viewModelScope.launch {
            repository.observeStockTransferLines(transferId).collect { lines ->
                _uiState.update { current -> current.copy(lines = lines) }
            }
        }
        viewModelScope.launch {
            repository.observeStockTransferRecords(projectId).collect { records ->
                val filtered = records.filter { it.transferId == transferId }
                _uiState.update { current -> current.copy(transferRecords = filtered) }
            }
        }
        viewModelScope.launch {
            val auditResult = repository.getAuditHistory(transferId)
            if (auditResult is DomainResult.Success) {
                _uiState.update { current -> current.copy(auditEvents = auditResult.data) }
            }
        }
    }

    fun submitTransfer(
        transferId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.submitStockTransfer(transferId, actorId, timestamp, role)
            handleResult(res, "Stock transfer submitted.")
        }
    }

    fun approveTransfer(
        transferId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.approveStockTransfer(transferId, actorId, timestamp, role)
            handleResult(res, "Stock transfer approved.")
        }
    }

    fun completeTransfer(
        transferId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.completeStockTransfer(transferId, actorId, timestamp, role)
            handleResult(res, "Stock transfer completed.")
        }
    }

    fun cancelTransfer(
        transferId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.cancelStockTransfer(transferId, actorId, timestamp, role)
            handleResult(res, "Stock transfer cancelled.")
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

package com.sucharu.sucharupro.ui.features.inventory.receiving

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryReceivingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating the receiving details screen (Module 07 Step 03).
 *
 * Provides reactive observation of a single receiving's header, lines, stock-in records,
 * and audit history. Exposes actions gated by role and current status.
 */
class InventoryReceivingDetailsViewModel(
    private val repository: InventoryReceivingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryReceivingDetailsUiState(isLoading = true))
    val uiState: StateFlow<InventoryReceivingDetailsUiState> = _uiState.asStateFlow()

    fun loadReceiving(receivingId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.observeReceiving(receivingId).collect { receiving ->
                _uiState.update { current ->
                    current.copy(isLoading = false, receiving = receiving)
                }
            }
        }
        viewModelScope.launch {
            repository.observeReceivingLines(receivingId).collect { lines ->
                _uiState.update { current -> current.copy(lines = lines) }
            }
        }
        viewModelScope.launch {
            repository.observeStockInRecordsByReceiving(receivingId).collect { records ->
                _uiState.update { current -> current.copy(stockInRecords = records) }
            }
        }
        viewModelScope.launch {
            val auditResult = repository.getAuditHistory(receivingId)
            if (auditResult is DomainResult.Success) {
                _uiState.update { current -> current.copy(auditEvents = auditResult.data) }
            }
        }
    }

    fun submitReceiving(
        receivingId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.submitReceiving(receivingId, actorId, timestamp, role)
            handleResult(res)
        }
    }

    fun startReceiving(
        receivingId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.WAREHOUSE
    ) {
        viewModelScope.launch {
            val res = repository.startReceiving(receivingId, actorId, timestamp, role)
            handleResult(res)
        }
    }

    fun completeReceiving(
        receivingId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.completeReceiving(receivingId, actorId, timestamp, role)
            handleResult(res)
        }
    }

    fun cancelReceiving(
        receivingId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.cancelReceiving(receivingId, actorId, timestamp, role)
            handleResult(res)
        }
    }

    fun acceptLine(
        receivingLineId: String,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.acceptLine(receivingLineId, actorId, timestamp, role)
            if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            } else {
                _uiState.update { it.copy(operationMessage = "Line accepted successfully.", errorMessage = null) }
            }
        }
    }

    fun rejectLine(
        receivingLineId: String,
        rejectionReason: String?,
        actorId: String,
        timestamp: String,
        role: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            val res = repository.rejectLine(receivingLineId, rejectionReason, actorId, timestamp, role)
            if (res is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = res.message) }
            } else {
                _uiState.update { it.copy(operationMessage = "Line rejected.", errorMessage = null) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, operationMessage = null) }
    }

    private fun handleResult(result: DomainResult<*>) {
        if (result is DomainResult.Error) {
            _uiState.update { it.copy(errorMessage = result.message) }
        } else {
            _uiState.update { it.copy(errorMessage = null) }
        }
    }
}

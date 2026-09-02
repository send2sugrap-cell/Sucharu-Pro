package com.sucharu.sucharupro.ui.features.inventory.reorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryReorderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating the reorder alert details screen (Module 07 Step 08).
 *
 * Manages alert lifecycle actions and policy context.
 */
class InventoryReorderDetailsViewModel(
    private val repository: InventoryReorderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryReorderDetailsUiState(isLoading = true))
    val uiState: StateFlow<InventoryReorderDetailsUiState> = _uiState.asStateFlow()

    fun loadAlertDetails(alertId: String, userRole: UserRole) {
        _uiState.update { it.copy(isLoading = true, currentUserRole = userRole, errorMessage = null) }
        viewModelScope.launch {
            val alertResult = repository.getAlert(alertId, userRole)
            if (alertResult is DomainResult.Success) {
                val alert = alertResult.data
                _uiState.update { it.copy(alert = alert) }
                
                // Load policy info
                val policyResult = repository.getPolicy(alert.policyId, userRole)
                if (policyResult is DomainResult.Success) {
                    _uiState.update { it.copy(isLoading = false, policy = policyResult.data) }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } else if (alertResult is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = alertResult.message) }
            }
        }
    }

    fun acknowledgeAlert(alertId: String, userId: String) {
        val role = _uiState.value.currentUserRole
        viewModelScope.launch {
            val res = repository.acknowledgeAlert(alertId, userId, role)
            handleResult(res, "Alert acknowledged successfully.")
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(alert = res.data) }
            }
        }
    }

    fun resolveAlert(alertId: String, userId: String) {
        val role = _uiState.value.currentUserRole
        viewModelScope.launch {
            val res = repository.resolveAlert(alertId, userId, role)
            handleResult(res, "Alert marked as resolved.")
            if (res is DomainResult.Success) {
                _uiState.update { it.copy(alert = res.data) }
            }
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

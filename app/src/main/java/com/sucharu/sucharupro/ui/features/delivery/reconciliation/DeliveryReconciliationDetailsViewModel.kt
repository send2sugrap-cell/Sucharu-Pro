package com.sucharu.sucharupro.ui.features.delivery.reconciliation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryReconciliationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeliveryReconciliationDetailsViewModel(
    private val repository: DeliveryReconciliationRepository,
    private val reconciliationId: String,
    private val currentRole: UserRole = UserRole.MANAGER,
    private val currentActorId: String = "system-user"
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryReconciliationDetailsUiState(isLoading = true))
    val uiState: StateFlow<DeliveryReconciliationDetailsUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
    }

    private fun loadDetails() {
        viewModelScope.launch {
            combine(
                repository.observeReconciliation(reconciliationId),
                repository.observeItems(reconciliationId),
                repository.observeDiscrepancies(reconciliationId),
                repository.observeActivityEvents(reconciliationId)
            ) { rec, items, discrepancies, events ->
                DeliveryReconciliationDetailsUiState(
                    reconciliation = rec,
                    items = items,
                    discrepancies = discrepancies,
                    events = events,
                    isLoading = false,
                    isActionInProgress = false,
                    errorMessage = null
                )
            }.catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun onRefreshCalculation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }
            val result = repository.refreshCalculation(reconciliationId, currentActorId, currentRole)
            if (result is DomainResult.Error) {
                _uiState.update { it.copy(isActionInProgress = false, errorMessage = result.message) }
            } else {
                _uiState.update { it.copy(isActionInProgress = false, actionSuccessMessage = "Quantities refreshed successfully.") }
            }
        }
    }

    fun onStartReconciliation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }
            val result = repository.startReconciliation(reconciliationId, currentActorId, currentRole)
            if (result is DomainResult.Error) {
                _uiState.update { it.copy(isActionInProgress = false, errorMessage = result.message) }
            } else {
                _uiState.update { it.copy(isActionInProgress = false, actionSuccessMessage = "Reconciliation started.") }
            }
        }
    }

    fun onResolveDiscrepancy(discrepancyId: String, notes: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }
            val result = repository.resolveDiscrepancy(reconciliationId, discrepancyId, notes, currentActorId, currentRole)
            if (result is DomainResult.Error) {
                _uiState.update { it.copy(isActionInProgress = false, errorMessage = result.message) }
            } else {
                _uiState.update { it.copy(isActionInProgress = false, actionSuccessMessage = "Discrepancy resolved.") }
            }
        }
    }

    fun onMarkReconciled(notes: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }
            val result = repository.markReconciled(reconciliationId, currentActorId, notes, currentRole)
            if (result is DomainResult.Error) {
                _uiState.update { it.copy(isActionInProgress = false, errorMessage = result.message) }
            } else {
                _uiState.update { it.copy(isActionInProgress = false, actionSuccessMessage = "Marked as reconciled.") }
            }
        }
    }

    fun onCloseReconciliation(notes: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }
            val result = repository.closeReconciliation(reconciliationId, currentActorId, notes, currentRole)
            if (result is DomainResult.Error) {
                _uiState.update { it.copy(isActionInProgress = false, errorMessage = result.message) }
            } else {
                _uiState.update { it.copy(isActionInProgress = false, actionSuccessMessage = "Reconciliation closed.") }
            }
        }
    }
}

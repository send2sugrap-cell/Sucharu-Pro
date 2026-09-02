package com.sucharu.sucharupro.ui.features.finance.receivable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.CustomerReceivableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CustomerReceivableDetailsViewModel(
    private val repository: CustomerReceivableRepository,
    private val receivableId: String,
    private val currentUserId: String,
    private val currentUserRole: UserRole,
    private val authenticatedCustomerId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerReceivableDetailsUiState(isLoading = true))
    val uiState: StateFlow<CustomerReceivableDetailsUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val recRes = repository.getReceivableById(receivableId, currentUserRole, authenticatedCustomerId)
            if (recRes is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = recRes.message) }
                return@launch
            }

            val receivable = (recRes as DomainResult.Success).data
            val eventsRes = repository.getActivityEvents(receivableId, currentUserRole)
            val events = if (eventsRes is DomainResult.Success) eventsRes.data else emptyList()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    receivable = receivable,
                    activityEvents = events,
                    errorMessage = null
                )
            }
        }
    }

    fun cancelReceivable(reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }
            val result = repository.cancelReceivable(receivableId, reason, currentUserId, currentUserRole)
            when (result) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isActionInProgress = false, actionSuccessMessage = "Receivable cancelled.") }
                    loadDetails()
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isActionInProgress = false, errorMessage = result.message) }
                }
                is DomainResult.Loading -> {
                    _uiState.update { it.copy(isActionInProgress = true) }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, actionSuccessMessage = null) }
    }
}

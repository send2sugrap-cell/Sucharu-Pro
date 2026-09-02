package com.sucharu.sucharupro.ui.features.finance.receivable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus
import com.sucharu.sucharupro.domain.model.finance.ReceivableAgingBucket
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.CustomerReceivableRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CustomerReceivableListViewModel(
    private val repository: CustomerReceivableRepository,
    private val projectId: String,
    private val currentUserRole: UserRole,
    private val authenticatedCustomerId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerReceivableListUiState(isLoading = true))
    val uiState: StateFlow<CustomerReceivableListUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val summaryRes = repository.getCustomerDueSummary(
                projectId = projectId,
                customerId = authenticatedCustomerId,
                callerRole = currentUserRole,
                authenticatedCustomerId = authenticatedCustomerId
            )
            if (summaryRes is DomainResult.Success) {
                _uiState.update { it.copy(summary = summaryRes.data) }
            }
        }

        val flow = if (authenticatedCustomerId != null) {
            repository.observeCustomerReceivables(projectId, authenticatedCustomerId, currentUserRole, authenticatedCustomerId)
        } else {
            repository.observeReceivables(projectId, currentUserRole)
        }

        flow.onEach { list ->
            _uiState.update { it.copy(isLoading = false, receivables = list, errorMessage = null) }
        }.catch { error ->
            _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Failed to load receivables.") }
        }.launchIn(viewModelScope)
    }

    fun onStatusFilterSelected(status: CustomerReceivableStatus?) {
        _uiState.update { it.copy(selectedStatusFilter = status) }
    }

    fun onAgingFilterSelected(aging: ReceivableAgingBucket?) {
        _uiState.update { it.copy(selectedAgingFilter = aging) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

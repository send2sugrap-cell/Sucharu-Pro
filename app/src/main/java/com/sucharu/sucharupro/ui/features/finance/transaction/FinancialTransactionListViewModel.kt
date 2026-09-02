package com.sucharu.sucharupro.ui.features.finance.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinancialTransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class FinancialTransactionListViewModel(
    private val repository: FinancialTransactionRepository,
    private val projectId: String,
    private val currentUserRole: UserRole
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancialTransactionListUiState(isLoading = true))
    val uiState: StateFlow<FinancialTransactionListUiState> = _uiState.asStateFlow()

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        repository.observeTransactions(projectId, currentUserRole)
            .onEach { list ->
                _uiState.update { it.copy(isLoading = false, transactions = list, errorMessage = null) }
            }
            .catch { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Failed to load transactions.") }
            }
            .launchIn(viewModelScope)
    }

    fun onStatusFilterSelected(status: FinancialTransactionStatus?) {
        _uiState.update { it.copy(selectedStatusFilter = status) }
    }

    fun onTypeFilterSelected(type: FinancialTransactionType?) {
        _uiState.update { it.copy(selectedTypeFilter = type) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

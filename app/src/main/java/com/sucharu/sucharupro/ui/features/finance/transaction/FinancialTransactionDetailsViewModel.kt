package com.sucharu.sucharupro.ui.features.finance.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinancialTransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FinancialTransactionDetailsViewModel(
    private val repository: FinancialTransactionRepository,
    private val transactionId: String,
    private val currentUserId: String,
    private val currentUserRole: UserRole
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancialTransactionDetailsUiState(isLoading = true))
    val uiState: StateFlow<FinancialTransactionDetailsUiState> = _uiState.asStateFlow()

    init {
        loadDetails()
    }

    fun loadDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val txnRes = repository.getTransactionById(transactionId, currentUserRole)
            if (txnRes is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = txnRes.message) }
                return@launch
            }

            val txn = (txnRes as DomainResult.Success).data
            val ledgerRes = repository.getLedgerEntriesByTransaction(transactionId, currentUserRole)
            val eventsRes = repository.getActivityEvents(transactionId, currentUserRole)

            val ledgerEntries = if (ledgerRes is DomainResult.Success) ledgerRes.data else emptyList()
            val events = if (eventsRes is DomainResult.Success) eventsRes.data else emptyList()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    transaction = txn,
                    ledgerEntries = ledgerEntries,
                    activityEvents = events,
                    errorMessage = null
                )
            }
        }
    }

    fun submitTransaction() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }
            val result = repository.submitTransaction(transactionId, currentUserId, currentUserRole)
            when (result) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isActionInProgress = false, actionSuccessMessage = "Transaction submitted successfully.") }
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

    fun postTransaction(accountHead: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }
            val result = repository.postTransaction(transactionId, accountHead, currentUserId, currentUserRole)
            when (result) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isActionInProgress = false, actionSuccessMessage = "Transaction posted to ledger successfully.") }
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

    fun rejectTransaction(reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }
            val result = repository.rejectTransaction(transactionId, reason, currentUserId, currentUserRole)
            when (result) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isActionInProgress = false, actionSuccessMessage = "Transaction rejected.") }
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

    fun cancelTransaction(reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }
            val result = repository.cancelTransaction(transactionId, reason, currentUserId, currentUserRole)
            when (result) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isActionInProgress = false, actionSuccessMessage = "Transaction cancelled.") }
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

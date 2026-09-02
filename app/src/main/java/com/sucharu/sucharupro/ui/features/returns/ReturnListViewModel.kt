package com.sucharu.sucharupro.ui.features.returns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.repository.ReturnRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the Return Request List Screen state and filters (Module 11 Step 02).
 */
class ReturnListViewModel(
    private val repository: ReturnRepository,
    private val coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope get() = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(ReturnListUiState(isLoading = true))
    val uiState: StateFlow<ReturnListUiState> = _uiState.asStateFlow()

    fun loadReturns(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        scope.launch {
            repository.observeReturns(projectId).collect { list ->
                _uiState.update { current ->
                    val filtered = applyFilters(
                        returns = list,
                        query = current.searchQuery,
                        status = current.selectedStatusFilter,
                        reason = current.selectedReasonFilter
                    )
                    current.copy(
                        isLoading = false,
                        returns = list,
                        filteredReturns = filtered
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = applyFilters(
                returns = current.returns,
                query = query,
                status = current.selectedStatusFilter,
                reason = current.selectedReasonFilter
            )
            current.copy(searchQuery = query, filteredReturns = filtered)
        }
    }

    fun onStatusFilterChanged(status: ReturnStatus?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                returns = current.returns,
                query = current.searchQuery,
                status = status,
                reason = current.selectedReasonFilter
            )
            current.copy(selectedStatusFilter = status, filteredReturns = filtered)
        }
    }

    fun onReasonFilterChanged(reason: ReturnReason?) {
        _uiState.update { current ->
            val filtered = applyFilters(
                returns = current.returns,
                query = current.searchQuery,
                status = current.selectedStatusFilter,
                reason = reason
            )
            current.copy(selectedReasonFilter = reason, filteredReturns = filtered)
        }
    }

    private fun applyFilters(
        returns: List<ReturnRequest>,
        query: String,
        status: ReturnStatus?,
        reason: ReturnReason?
    ): List<ReturnRequest> {
        return returns.filter { req ->
            val matchesQuery = query.isBlank() ||
                req.returnNo.contains(query, ignoreCase = true) ||
                req.customerId.contains(query, ignoreCase = true) ||
                (req.originalChallanId?.contains(query, ignoreCase = true) == true)

            val matchesStatus = status == null || req.status == status
            val matchesReason = reason == null || req.reason == reason

            matchesQuery && matchesStatus && matchesReason
        }
    }
}

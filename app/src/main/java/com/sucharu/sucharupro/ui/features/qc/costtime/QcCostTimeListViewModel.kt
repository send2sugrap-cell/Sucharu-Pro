package com.sucharu.sucharupro.ui.features.qc.costtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeReconciliation
import com.sucharu.sucharupro.domain.repository.QcCostTimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * ViewModel for QC Cost & Time Reconciliation List Screen (Module 06 Step 08).
 */
class QcCostTimeListViewModel(
    private val repository: QcCostTimeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QcCostTimeListUiState(isLoading = true))
    val uiState: StateFlow<QcCostTimeListUiState> = _uiState.asStateFlow()

    init {
        observeReconciliations()
    }

    private fun observeReconciliations() {
        repository.observeReconciliations()
            .onEach { list ->
                _uiState.update { state ->
                    val filtered = applyFilters(list, state.searchQuery, state.selectedStatusFilter)
                    state.copy(
                        isLoading = false,
                        reconciliations = list,
                        filteredReconciliations = filtered,
                        totalReconciledJobs = list.size,
                        totalCostOverrunJobs = list.count { it.hasCostOverrun },
                        totalTimeOverrunJobs = list.count { it.hasTimeOverrun }
                    )
                }
            }
            .catch { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val filtered = applyFilters(state.reconciliations, query, state.selectedStatusFilter)
            state.copy(searchQuery = query, filteredReconciliations = filtered)
        }
    }

    fun onStatusFilterSelected(status: QcCostStatus?) {
        _uiState.update { state ->
            val filtered = applyFilters(state.reconciliations, state.searchQuery, status)
            state.copy(selectedStatusFilter = status, filteredReconciliations = filtered)
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun applyFilters(
        list: List<QcCostTimeReconciliation>,
        query: String,
        statusFilter: QcCostStatus?
    ): List<QcCostTimeReconciliation> {
        return list.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.productionJobId.contains(query, ignoreCase = true) ||
                item.projectId.contains(query, ignoreCase = true) ||
                (item.reconciledByName?.contains(query, ignoreCase = true) ?: false)

            val matchesStatus = statusFilter == null || item.status == statusFilter

            matchesQuery && matchesStatus
        }
    }
}

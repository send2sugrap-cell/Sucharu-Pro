package com.sucharu.sucharupro.ui.features.qc.finalqc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.qc.FinalQcInspection
import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus
import com.sucharu.sucharupro.domain.repository.FinalQcRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Final QC list state (Module 06 Step 07).
 */
class FinalQcListViewModel(
    private val finalQcRepository: FinalQcRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinalQcListUiState(isLoading = true))
    val uiState: StateFlow<FinalQcListUiState> = _uiState.asStateFlow()

    init {
        loadFinalQcList()
    }

    private fun loadFinalQcList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            finalQcRepository.observeFinalQcList()
                .catch { ex ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = ex.message ?: "Failed to load Final QC records.") }
                }
                .collect { list ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            inspections = list,
                            filteredInspections = applyFilters(list, state.selectedFilter, state.searchQuery)
                        )
                    }
                }
        }
    }

    fun onFilterSelected(filter: FinalQcFilter) {
        _uiState.update { state ->
            state.copy(
                selectedFilter = filter,
                filteredInspections = applyFilters(state.inspections, filter, state.searchQuery)
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredInspections = applyFilters(state.inspections, state.selectedFilter, query)
            )
        }
    }

    private fun applyFilters(
        list: List<FinalQcInspection>,
        filter: FinalQcFilter,
        query: String
    ): List<FinalQcInspection> {
        val filteredByStatus = when (filter) {
            FinalQcFilter.ALL -> list
            FinalQcFilter.PENDING -> list.filter { it.status == FinalQcStatus.PENDING }
            FinalQcFilter.ASSIGNED -> list.filter { it.status == FinalQcStatus.ASSIGNED }
            FinalQcFilter.IN_INSPECTION -> list.filter { it.status == FinalQcStatus.IN_INSPECTION }
            FinalQcFilter.PASSED -> list.filter { it.status == FinalQcStatus.PASSED }
            FinalQcFilter.FAILED -> list.filter { it.status == FinalQcStatus.FAILED }
            FinalQcFilter.BLOCKED -> list.filter { it.status == FinalQcStatus.BLOCKED }
            FinalQcFilter.RELEASED -> list.filter { it.status == FinalQcStatus.RELEASED }
        }

        if (query.isBlank()) return filteredByStatus

        val q = query.trim().lowercase()
        return filteredByStatus.filter {
            it.finalQcId.lowercase().contains(q) ||
                it.productionJobId.lowercase().contains(q) ||
                it.projectId.lowercase().contains(q) ||
                (it.assignedInspectorName?.lowercase()?.contains(q) == true) ||
                (it.inspectedByName?.lowercase()?.contains(q) == true)
        }
    }
}

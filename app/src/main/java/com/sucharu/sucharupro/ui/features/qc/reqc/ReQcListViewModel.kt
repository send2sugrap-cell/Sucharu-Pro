package com.sucharu.sucharupro.ui.features.qc.reqc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.qc.ReQcCycleType
import com.sucharu.sucharupro.domain.model.qc.ReQcDecision
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus
import com.sucharu.sucharupro.domain.repository.ProductionReQcRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Re-QC List Screen (Module 06 Step 06).
 */
class ReQcListViewModel(
    private val reQcRepository: ProductionReQcRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReQcListUiState(isLoading = true))
    val uiState: StateFlow<ReQcListUiState> = _uiState.asStateFlow()

    init {
        loadReQcs()
    }

    fun loadReQcs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            reQcRepository.observeReQcList()
                .catch { ex ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = ex.message ?: "Failed to load Re-QC records.") }
                }
                .collect { list ->
                    _uiState.update { it.copy(reQcs = list, isLoading = false, errorMessage = null) }
                }
        }
    }

    fun setStatusFilter(status: ReQcStatus?) {
        _uiState.update { it.copy(selectedStatusFilter = status) }
    }

    fun setCycleTypeFilter(type: ReQcCycleType?) {
        _uiState.update { it.copy(selectedCycleTypeFilter = type) }
    }

    fun setDecisionFilter(decision: ReQcDecision?) {
        _uiState.update { it.copy(selectedDecisionFilter = decision) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                selectedStatusFilter = null,
                selectedCycleTypeFilter = null,
                selectedDecisionFilter = null,
                searchQuery = ""
            )
        }
    }
}

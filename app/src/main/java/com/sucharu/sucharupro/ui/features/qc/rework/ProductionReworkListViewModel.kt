package com.sucharu.sucharupro.ui.features.qc.rework

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.repository.ProductionReworkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for QC Rework List Screen (Module 06 Step 05).
 */
class ProductionReworkListViewModel(
    private val reworkRepository: ProductionReworkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductionReworkListUiState(isLoading = true))
    val uiState: StateFlow<ProductionReworkListUiState> = _uiState.asStateFlow()

    init {
        loadReworks()
    }

    fun loadReworks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            reworkRepository.observeReworkList()
                .catch { ex ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = ex.message ?: "Failed to load rework records.") }
                }
                .collect { list ->
                    _uiState.update { it.copy(reworks = list, isLoading = false, errorMessage = null) }
                }
        }
    }

    fun setStatusFilter(status: ReworkStatus?) {
        _uiState.update { it.copy(selectedStatusFilter = status) }
    }

    fun setTypeFilter(type: ReworkType?) {
        _uiState.update { it.copy(selectedTypeFilter = type) }
    }

    fun setReasonFilter(reason: ReworkReason?) {
        _uiState.update { it.copy(selectedReasonFilter = reason) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                selectedStatusFilter = null,
                selectedTypeFilter = null,
                selectedReasonFilter = null,
                searchQuery = ""
            )
        }
    }
}

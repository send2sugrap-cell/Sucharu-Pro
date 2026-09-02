package com.sucharu.sucharupro.ui.features.qc.defect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.repository.ProductionDefectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for QC Defect List Screen (Module 06 Step 04).
 */
class ProductionDefectListViewModel(
    private val defectRepository: ProductionDefectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductionDefectListUiState(isLoading = true))
    val uiState: StateFlow<ProductionDefectListUiState> = _uiState.asStateFlow()

    init {
        loadDefects()
    }

    fun loadDefects() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            defectRepository.observeDefectList()
                .catch { ex ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = ex.message ?: "Failed to load defects.") }
                }
                .collect { list ->
                    _uiState.update { it.copy(defects = list, isLoading = false, errorMessage = null) }
                }
        }
    }

    fun setStatusFilter(status: DefectStatus?) {
        _uiState.update { it.copy(selectedStatusFilter = status) }
    }

    fun setSeverityFilter(severity: DefectSeverity?) {
        _uiState.update { it.copy(selectedSeverityFilter = severity) }
    }

    fun setCategoryFilter(category: DefectCategory?) {
        _uiState.update { it.copy(selectedCategoryFilter = category) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                selectedStatusFilter = null,
                selectedSeverityFilter = null,
                selectedCategoryFilter = null,
                searchQuery = ""
            )
        }
    }
}

package com.sucharu.sucharupro.ui.features.production.history

import com.sucharu.sucharupro.domain.model.job.ProductionHistoryFilter
import com.sucharu.sucharupro.domain.model.job.ProductionHistorySummary

/**
 * UI State representation for Production History Screen.
 */
sealed interface ProductionHistoryUiState {
    data object Loading : ProductionHistoryUiState

    data class Success(
        val allSummaries: List<ProductionHistorySummary>,
        val filteredSummaries: List<ProductionHistorySummary>,
        val searchQuery: String = "",
        val filter: ProductionHistoryFilter = ProductionHistoryFilter()
    ) : ProductionHistoryUiState {
        val isEmpty: Boolean get() = allSummaries.isEmpty()
        val isFilteredEmpty: Boolean get() = filteredSummaries.isEmpty() && allSummaries.isNotEmpty()
    }

    data class Error(val message: String) : ProductionHistoryUiState
}

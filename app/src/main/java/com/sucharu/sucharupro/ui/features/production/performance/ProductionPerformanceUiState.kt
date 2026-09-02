package com.sucharu.sucharupro.ui.features.production.performance

import com.sucharu.sucharupro.domain.model.job.ProductionDateRangeFilter
import com.sucharu.sucharupro.domain.model.job.ProductionOperatorPerformanceItem
import com.sucharu.sucharupro.domain.model.job.ProductionPerformanceMetrics
import com.sucharu.sucharupro.domain.model.job.ProductionStagePerformanceItem

/**
 * UI state for Production Performance Analytics Screen.
 */
sealed interface ProductionPerformanceUiState {
    data object Loading : ProductionPerformanceUiState

    data class Success(
        val metrics: ProductionPerformanceMetrics,
        val operatorPerformances: List<ProductionOperatorPerformanceItem>,
        val stagePerformances: List<ProductionStagePerformanceItem>,
        val selectedDateRange: ProductionDateRangeFilter = ProductionDateRangeFilter.ALL_TIME
    ) : ProductionPerformanceUiState

    data class Error(val message: String) : ProductionPerformanceUiState
}

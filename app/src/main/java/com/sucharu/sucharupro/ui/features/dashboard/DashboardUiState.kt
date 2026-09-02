package com.sucharu.sucharupro.ui.features.dashboard

import com.sucharu.sucharupro.domain.model.dashboard.DashboardSummary

/**
 * UI State definition for the Sucharu Pro Dashboard screen.
 */
sealed interface DashboardUiState {
    /**
     * Initial loading state while fetching summary metrics and active jobs.
     */
    data object Loading : DashboardUiState

    /**
     * Successfully loaded dashboard summary data.
     */
    data class Success(
        val summary: DashboardSummary,
        val isRefreshing: Boolean = false
    ) : DashboardUiState

    /**
     * Empty state when no jobs or metrics are available for the day.
     */
    data class Empty(
        val message: String = "No orders or printing jobs recorded for today. Tap '+ New Order' to begin."
    ) : DashboardUiState

    /**
     * Error state when fetching or computing dashboard data fails.
     */
    data class Error(
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : DashboardUiState
}

package com.sucharu.sucharupro.ui.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.repository.FakeDashboardRepository
import com.sucharu.sucharupro.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * ViewModel managing presentation state and user actions for the Sucharu Pro Dashboard.
 * 
 * Interacts purely through the [DashboardRepository] abstraction and exposes a reactive [DashboardUiState] StateFlow.
 */
class DashboardViewModel(
    private val repository: DashboardRepository = FakeDashboardRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    /**
     * Observes the dashboard summary stream from the repository.
     */
    fun loadDashboardData() {
        viewModelScope.launch {
            repository.getDashboardSummary()
                .onStart {
                    _uiState.value = DashboardUiState.Loading
                }
                .catch { exception ->
                    _uiState.value = DashboardUiState.Error(
                        errorMessage = exception.localizedMessage ?: "Failed to load dashboard data. Please try again."
                    )
                }
                .collect { summary ->
                    _uiState.value = if (summary.recentOrders.isEmpty() && summary.kpis.todayOrdersCount == 0) {
                        DashboardUiState.Empty()
                    } else {
                        DashboardUiState.Success(summary = summary)
                    }
                }
        }
    }

    /**
     * Triggers a manual refresh of the dashboard data (e.g. from swipe-to-refresh or refresh button).
     */
    fun refresh() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is DashboardUiState.Success) {
                _uiState.value = currentState.copy(isRefreshing = true)
            }

            val result = repository.refreshDashboardSummary()
            if (result.isFailure) {
                _uiState.value = DashboardUiState.Error(
                    errorMessage = result.exceptionOrNull()?.localizedMessage
                        ?: "Failed to refresh dashboard. Please try again."
                )
            }
        }
    }

    /**
     * Retries loading dashboard data following a previous failure.
     */
    fun retry() {
        loadDashboardData()
    }
}

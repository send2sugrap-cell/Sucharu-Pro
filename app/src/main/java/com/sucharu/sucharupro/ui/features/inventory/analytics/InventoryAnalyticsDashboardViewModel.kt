package com.sucharu.sucharupro.ui.features.inventory.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryAnalyticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating the Inventory Analytics Dashboard (Module 07 Step 10).
 */
class InventoryAnalyticsDashboardViewModel(
    private val repository: InventoryAnalyticsRepository,
    private val currentUserRole: UserRole = UserRole.ADMIN
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        InventoryAnalyticsDashboardUiState(
            hasFinancialAccess = currentUserRole.hasFinancialAccess
        )
    )
    val uiState: StateFlow<InventoryAnalyticsDashboardUiState> = _uiState.asStateFlow()

    fun loadDashboard(projectId: String) {
        _uiState.update { it.copy(isLoading = true, projectId = projectId, errorMessage = null) }
        
        viewModelScope.launch {
            // Observe activity events
            launch {
                repository.observeActivityEvents(projectId).collect { events ->
                    _uiState.update { it.copy(activityEvents = events) }
                }
            }
            
            // Load summary and trends
            refreshAnalytics()
        }
    }

    fun onPeriodChanged(period: InventoryAnalyticsPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        refreshAnalytics()
    }

    private fun refreshAnalytics() {
        val currentState = _uiState.value
        val projectId = currentState.projectId
        val period = currentState.selectedPeriod
        
        if (projectId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val summaryRes = repository.getAnalyticsSummary(projectId, period, currentUserRole)
            val trendsRes = repository.getStockTrends(projectId, period, currentUserRole)
            
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    summary = if (summaryRes is DomainResult.Success) summaryRes.data else current.summary,
                    trends = if (trendsRes is DomainResult.Success) trendsRes.data else current.trends,
                    errorMessage = if (summaryRes is DomainResult.Error) summaryRes.message else null
                )
            }
        }
    }
}

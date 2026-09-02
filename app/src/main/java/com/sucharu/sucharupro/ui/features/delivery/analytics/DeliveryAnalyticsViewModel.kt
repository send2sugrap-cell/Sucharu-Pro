package com.sucharu.sucharupro.ui.features.delivery.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsFilter
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryAnalyticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeliveryAnalyticsViewModel(
    private val repository: DeliveryAnalyticsRepository,
    private val projectId: String,
    private val currentUserRole: UserRole = UserRole.ADMIN
) : ViewModel() {

    private val _uiState = MutableStateFlow<DeliveryAnalyticsUiState>(DeliveryAnalyticsUiState.Loading)
    val uiState: StateFlow<DeliveryAnalyticsUiState> = _uiState.asStateFlow()

    private var currentFilter = DeliveryAnalyticsFilter(projectId = projectId)

    init {
        loadAnalytics(currentFilter)
    }

    fun setPeriod(period: DeliveryAnalyticsPeriod) {
        currentFilter = currentFilter.copy(period = period)
        loadAnalytics(currentFilter)
    }

    fun refresh() {
        loadAnalytics(currentFilter)
    }

    private fun loadAnalytics(filter: DeliveryAnalyticsFilter) {
        viewModelScope.launch {
            _uiState.value = DeliveryAnalyticsUiState.Loading

            val summaryRes = repository.getSummary(filter, currentUserRole)
            if (summaryRes is DomainResult.Error) {
                _uiState.value = DeliveryAnalyticsUiState.Error(summaryRes.message)
                return@launch
            }

            val breakdownRes = repository.getBreakdown(filter, currentUserRole)
            val trendRes = repository.getTrends(projectId, filter.period, currentUserRole)

            val summary = (summaryRes as DomainResult.Success).data
            val breakdown = (breakdownRes as? DomainResult.Success)?.data
                ?: com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsBreakdown(projectId)
            val trend = (trendRes as? DomainResult.Success)?.data
                ?: com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsTrend(projectId, filter.period)

            if (summary.totalDeliveryOrders == 0 && summary.totalShipments == 0) {
                _uiState.value = DeliveryAnalyticsUiState.Empty
            } else {
                _uiState.value = DeliveryAnalyticsUiState.Success(
                    summary = summary,
                    breakdown = breakdown,
                    trend = trend,
                    filter = filter
                )
            }
        }
    }
}

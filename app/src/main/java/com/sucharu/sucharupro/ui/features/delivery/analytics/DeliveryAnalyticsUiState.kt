package com.sucharu.sucharupro.ui.features.delivery.analytics

import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsBreakdown
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsFilter
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsSummary
import com.sucharu.sucharupro.domain.model.delivery.analytics.DeliveryAnalyticsTrend

sealed interface DeliveryAnalyticsUiState {
    data object Loading : DeliveryAnalyticsUiState
    data object Empty : DeliveryAnalyticsUiState
    data class Success(
        val summary: DeliveryAnalyticsSummary,
        val breakdown: DeliveryAnalyticsBreakdown,
        val trend: DeliveryAnalyticsTrend,
        val filter: DeliveryAnalyticsFilter
    ) : DeliveryAnalyticsUiState
    data class Error(val message: String) : DeliveryAnalyticsUiState
}

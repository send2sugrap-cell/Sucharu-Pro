package com.sucharu.sucharupro.ui.features.inventory.analytics

import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryAnalyticsActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryAnalyticsSummary
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryAnalyticsTrendPoint

/**
 * UI State for the Inventory Analytics Dashboard (Module 07 Step 10).
 */
data class InventoryAnalyticsDashboardUiState(
    val isLoading: Boolean = false,
    val projectId: String = "",
    val hasFinancialAccess: Boolean = false,
    val selectedPeriod: InventoryAnalyticsPeriod = InventoryAnalyticsPeriod.CURRENT_MONTH,
    val summary: InventoryAnalyticsSummary? = null,
    val trends: List<InventoryAnalyticsTrendPoint> = emptyList(),
    val activityEvents: List<InventoryAnalyticsActivityEvent> = emptyList(),
    val errorMessage: String? = null
)

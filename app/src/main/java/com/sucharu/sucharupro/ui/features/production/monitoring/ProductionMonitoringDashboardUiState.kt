package com.sucharu.sucharupro.ui.features.production.monitoring

import com.sucharu.sucharupro.domain.model.job.ActiveProductionStageItem
import com.sucharu.sucharupro.domain.model.job.OperatorWorkloadItem
import com.sucharu.sucharupro.domain.model.job.ProductionAttentionItem
import com.sucharu.sucharupro.domain.model.job.ProductionMonitoringSnapshot

/**
 * UI State definition for the Production Monitoring Dashboard screen.
 */
sealed interface ProductionMonitoringDashboardUiState {

    /** Initial loading state. */
    data object Loading : ProductionMonitoringDashboardUiState

    /** Successfully computed and observed live production monitoring state. */
    data class Success(
        val snapshot: ProductionMonitoringSnapshot = ProductionMonitoringSnapshot(),
        val activeStages: List<ActiveProductionStageItem> = emptyList(),
        val operatorWorkloads: List<OperatorWorkloadItem> = emptyList(),
        val attentionItems: List<ProductionAttentionItem> = emptyList(),
        val filter: ProductionMonitoringFilter = ProductionMonitoringFilter.ALL,
        val searchQuery: String = "",
        val filteredActiveStages: List<ActiveProductionStageItem> = emptyList(),
        val filteredAttentionItems: List<ProductionAttentionItem> = emptyList(),
        val isActionInProgress: Boolean = false,
        val errorMessage: String? = null
    ) : ProductionMonitoringDashboardUiState

    /** Error state when live monitoring calculations fail. */
    data class Error(
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : ProductionMonitoringDashboardUiState
}

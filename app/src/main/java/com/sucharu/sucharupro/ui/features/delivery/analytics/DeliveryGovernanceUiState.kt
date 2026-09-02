package com.sucharu.sucharupro.ui.features.delivery.analytics

import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlert

sealed interface DeliveryGovernanceUiState {
    data object Loading : DeliveryGovernanceUiState
    data object Empty : DeliveryGovernanceUiState
    data class Success(
        val alerts: List<DeliveryGovernanceAlert>,
        val openCount: Int,
        val criticalCount: Int,
        val warningCount: Int,
        val actionMessage: String? = null
    ) : DeliveryGovernanceUiState
    data class Error(val message: String) : DeliveryGovernanceUiState
}

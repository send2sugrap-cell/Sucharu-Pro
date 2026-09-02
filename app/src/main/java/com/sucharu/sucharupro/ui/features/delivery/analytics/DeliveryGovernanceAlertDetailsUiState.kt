package com.sucharu.sucharupro.ui.features.delivery.analytics

import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlert

sealed interface DeliveryGovernanceAlertDetailsUiState {
    data object Loading : DeliveryGovernanceAlertDetailsUiState
    data class Success(
        val alert: DeliveryGovernanceAlert,
        val events: List<DeliveryGovernanceActivityEvent>
    ) : DeliveryGovernanceAlertDetailsUiState
    data class Error(val message: String) : DeliveryGovernanceAlertDetailsUiState
}

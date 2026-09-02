package com.sucharu.sucharupro.ui.features.delivery.reconciliation

import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliation
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationSummary

data class DeliveryReconciliationListUiState(
    val reconciliations: List<DeliveryReconciliation> = emptyList(),
    val summary: DeliveryReconciliationSummary = DeliveryReconciliationSummary(projectId = ""),
    val searchQuery: String = "",
    val selectedStatusFilter: DeliveryReconciliationStatus? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

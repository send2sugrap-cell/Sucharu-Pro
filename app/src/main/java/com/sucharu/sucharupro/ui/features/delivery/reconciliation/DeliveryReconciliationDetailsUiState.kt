package com.sucharu.sucharupro.ui.features.delivery.reconciliation

import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliation
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationDiscrepancy
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationItem

data class DeliveryReconciliationDetailsUiState(
    val reconciliation: DeliveryReconciliation? = null,
    val items: List<DeliveryReconciliationItem> = emptyList(),
    val discrepancies: List<DeliveryReconciliationDiscrepancy> = emptyList(),
    val events: List<DeliveryReconciliationActivityEvent> = emptyList(),
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val errorMessage: String? = null,
    val actionSuccessMessage: String? = null
)

package com.sucharu.sucharupro.ui.features.returns

import com.sucharu.sucharupro.domain.model.returns.ReturnActivityEvent
import com.sucharu.sucharupro.domain.model.returns.ReturnInspection
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReceivingInfo
import com.sucharu.sucharupro.domain.model.returns.ReturnReconciliationResult
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlement

/**
 * UI State for Return Request Details Screen (Module 11 Step 02, 03, 04, 05).
 */
data class ReturnDetailsUiState(
    val isLoading: Boolean = false,
    val returnRequest: ReturnRequest? = null,
    val items: List<ReturnItem> = emptyList(),
    val inspection: ReturnInspection? = null,
    val receivingInfo: ReturnReceivingInfo? = null,
    val reconciliationResult: ReturnReconciliationResult? = null,
    val settlement: ReturnSettlement? = null,
    val auditEvents: List<ReturnActivityEvent> = emptyList(),
    val errorMessage: String? = null,
    val isSubmittingAction: Boolean = false,
    val actionSuccessMessage: String? = null,
    val showReceiveDialog: Boolean = false,
    val showReconcileDialog: Boolean = false,
    val showSettleDialog: Boolean = false
)

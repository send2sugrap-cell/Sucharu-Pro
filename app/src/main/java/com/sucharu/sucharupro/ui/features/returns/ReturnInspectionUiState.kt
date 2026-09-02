package com.sucharu.sucharupro.ui.features.returns

import com.sucharu.sucharupro.domain.model.returns.InspectionChecklistItem
import com.sucharu.sucharupro.domain.model.returns.ReturnActivityEvent
import com.sucharu.sucharupro.domain.model.returns.ReturnDecision
import com.sucharu.sucharupro.domain.model.returns.ReturnInspection
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest

/**
 * UI State for Return Inspection & Decision Screen (Module 11 Step 03).
 */
data class ReturnInspectionUiState(
    val isLoading: Boolean = false,
    val returnRequest: ReturnRequest? = null,
    val items: List<ReturnItem> = emptyList(),
    val inspection: ReturnInspection? = null,
    val checklist: List<InspectionChecklistItem> = emptyList(),
    val findings: String = "",
    val decision: ReturnDecision? = null,
    val decisionReason: String = "",
    val itemAcceptedQuantities: Map<String, Int> = emptyMap(),
    val itemRejectedQuantities: Map<String, Int> = emptyMap(),
    val auditEvents: List<ReturnActivityEvent> = emptyList(),
    val isSubmittingAction: Boolean = false,
    val actionSuccessMessage: String? = null,
    val errorMessage: String? = null
)

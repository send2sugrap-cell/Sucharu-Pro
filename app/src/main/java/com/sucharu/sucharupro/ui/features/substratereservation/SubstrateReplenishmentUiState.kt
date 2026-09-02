package com.sucharu.sucharupro.ui.features.substratereservation

import com.sucharu.sucharupro.data.api.model.substratereservation.SubstrateReplenishmentResponseDto
import com.sucharu.sucharupro.data.api.model.substratereservation.SupplierReorderAlertResponseDto

/**
 * UI State for Substrate Auto-Replenishment Triggers & Supplier Reorder Alerts Command Center.
 * Module 19 Step 04.
 */
data class SubstrateReplenishmentUiState(
    val isLoading: Boolean = false,
    val isEvaluating: Boolean = false,
    val isTriggeringAlert: Boolean = false,
    val selectedTab: Int = 0, // 0: Overview, 1: Stock Risk, 2: Recommendations, 3: Supplier Alerts, 4: Audit & AI Handoff
    val currentEvaluation: SubstrateReplenishmentResponseDto? = null,
    val evaluations: List<SubstrateReplenishmentResponseDto> = emptyList(),
    val alerts: List<SupplierReorderAlertResponseDto> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showEvaluateDialog: Boolean = false,
    val showAlertConfirmDialog: Boolean = false,
    val jsonHandoffPreview: String? = null
)

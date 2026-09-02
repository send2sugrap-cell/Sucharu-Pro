package com.sucharu.sucharupro.ui.features.imposition

import com.sucharu.sucharupro.data.api.model.imposition.*

/**
 * UI State for Prepress Orchestration Master Command Center.
 * Module 18 Step 06.
 */
data class PrepressOrchestrationUiState(
    val isLoading: Boolean = false,
    val selectedTab: Int = 0, // 0: Executive Overview, 1: Pipeline Stages, 2: Reconciliation, 3: Recommendations, 4: Final Package, 5: Audit & AI Handoff
    val currentPlan: PrepressOrchestrationPlanDto? = null,
    val plansList: List<PrepressOrchestrationPlanDto> = emptyList(),
    val handoffContractJson: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

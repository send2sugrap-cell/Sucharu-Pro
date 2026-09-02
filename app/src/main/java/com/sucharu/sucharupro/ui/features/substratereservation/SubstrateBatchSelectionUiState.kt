package com.sucharu.sucharupro.ui.features.substratereservation

import com.sucharu.sucharupro.data.api.model.substratereservation.BatchLotSelectionResponseDto

/**
 * UI State for Substrate Batch/Lot Selection & Grain/Dimension Matching Command Center.
 * Module 19 Step 03.
 */
data class SubstrateBatchSelectionUiState(
    val isLoading: Boolean = false,
    val isEvaluating: Boolean = false,
    val isConfirming: Boolean = false,
    val selectedTab: Int = 0, // 0: Overview, 1: Candidates, 2: Visualizer, 3: Decision, 4: Audit & Handoff
    val currentSelection: BatchLotSelectionResponseDto? = null,
    val recentSelections: List<BatchLotSelectionResponseDto> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showEvaluateDialog: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val jsonHandoffPreview: String? = null
)

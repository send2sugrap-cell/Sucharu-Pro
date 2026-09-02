package com.sucharu.sucharupro.ui.features.production.jobcosting

import com.sucharu.sucharupro.data.api.model.jobcosting.*

data class ProductionJobCostingUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentJobId: String? = null,
    val actualCostRecord: ProductionActualJobCostResponseDto? = null,
    val varianceSummary: ProductionJobCostVarianceResponseDto? = null,
    val reconciliationResult: ProductionJobCostingReconciliationResponseDto? = null,
    val handoffContract: Module17Step09JobCostingVarianceHandoffContractDto? = null,
    val selectedTab: Int = 0,
    val isCalculateCostDialogOpen: Boolean = false,
    val isVarianceDialogOpen: Boolean = false,
    val isHandoffContractDialogOpen: Boolean = false
)

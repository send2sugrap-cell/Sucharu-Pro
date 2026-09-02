package com.sucharu.sucharupro.ui.features.production.jobclosure

import com.sucharu.sucharupro.data.api.model.jobclosure.*

data class ProductionJobClosureUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentJobId: String? = null,
    val closureRecord: ProductionJobClosureResponseDto? = null,
    val readinessAudit: JobClosureReadinessAuditDto? = null,
    val scorecard: ManufacturingPerformanceScorecardDto? = null,
    val handoffContract: Module17Step10JobClosureGovernanceHandoffContractDto? = null,
    val selectedTab: Int = 0,
    val isCloseJobDialogOpen: Boolean = false,
    val isAuditDetailsDialogOpen: Boolean = false,
    val isHandoffContractDialogOpen: Boolean = false
)

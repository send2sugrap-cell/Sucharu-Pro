package com.sucharu.sucharupro.ui.features.production.finalqc

import com.sucharu.sucharupro.data.api.model.finalqc.*

data class FinalQcPackagingUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentJobId: String? = null,
    val inspections: List<FinalQcInspectionResponseDto> = emptyList(),
    val defects: List<DefectContainmentResponseDto> = emptyList(),
    val packagingRecords: List<PackagingResponseDto> = emptyList(),
    val releaseRecords: List<FinishedGoodsReleaseResponseDto> = emptyList(),
    val varianceSummary: FinalQcPackagingVarianceResponseDto? = null,
    val reconciliationResult: FinalQcPackagingReconciliationResponseDto? = null,
    val handoffContract: Module17Step08FinalQcPackagingHandoffContractDto? = null,
    val selectedTab: Int = 0,
    val isInspectionDialogOpen: Boolean = false,
    val isDefectDialogOpen: Boolean = false,
    val isPackagingDialogOpen: Boolean = false,
    val isReleaseDialogOpen: Boolean = false,
    val isHandoffContractDialogOpen: Boolean = false,
    val activeInspectionId: String? = null
)

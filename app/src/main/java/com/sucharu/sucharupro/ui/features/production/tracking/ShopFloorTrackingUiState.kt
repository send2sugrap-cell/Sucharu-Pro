package com.sucharu.sucharupro.ui.features.production.tracking

import com.sucharu.sucharupro.data.api.model.shopfloortracking.*

data class ShopFloorTrackingUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentJobId: String? = null,
    val operatorTimeRecords: List<OperatorTimeTrackingResponseDto> = emptyList(),
    val materialConsumptions: List<ProductionMaterialConsumptionResponseDto> = emptyList(),
    val telemetryLogs: List<MachineTelemetryResponseDto> = emptyList(),
    val stageHandovers: List<StageOutputHandoverResponseDto> = emptyList(),
    val varianceSummary: ProductionExecutionVarianceResponseDto? = null,
    val reconciliationResult: ShopFloorTrackingReconciliationResponseDto? = null,
    val handoffContract: Module17Step07ShopFloorTrackingHandoffContractDto? = null,
    val selectedTab: Int = 0,
    val isPauseDialogOpen: Boolean = false,
    val isOutputDialogOpen: Boolean = false,
    val isMaterialDialogOpen: Boolean = false,
    val isHandoverDialogOpen: Boolean = false,
    val isHandoffContractDialogOpen: Boolean = false,
    val activeWorkOrderId: String? = null
)

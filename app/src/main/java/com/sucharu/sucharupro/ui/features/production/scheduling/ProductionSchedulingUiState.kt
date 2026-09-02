package com.sucharu.sucharupro.ui.features.production.scheduling

import com.sucharu.sucharupro.data.api.model.productionscheduling.*

data class ProductionSchedulingUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentSchedule: ProductionScheduleResponseDto? = null,
    val scheduleVersions: List<ProductionScheduleResponseDto> = emptyList(),
    val capacityWindows: List<ProductionCapacityWindowDto> = emptyList(),
    val dispatchQueue: List<ProductionDispatchQueueItemDto> = emptyList(),
    val conflicts: List<ProductionScheduleConflictDto> = emptyList(),
    val reconciliationResult: ProductionScheduleReconciliationResponseDto? = null,
    val handoffContract: Module17Step06ProductionSchedulingHandoffContractDto? = null,
    val selectedVersion: Int = 1,
    val selectedTab: Int = 0,
    val isSupersedeDialogOpen: Boolean = false,
    val isDispatchDialogOpen: Boolean = false,
    val isConflictDialogOpen: Boolean = false,
    val isHandoffDialogOpen: Boolean = false,
    val targetQueueItemId: String? = null
)

package com.sucharu.sucharupro.ui.features.production.operator

import com.sucharu.sucharupro.domain.model.job.ProductionOperator
import com.sucharu.sucharupro.domain.model.job.StageAssignmentStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * Sorting options for the Operator Work Queue.
 */
enum class OperatorWorkSortOrder(val defaultLabel: String) {
    PRIORITY_DESC("Priority (Urgent first)"),
    STAGE_SEQUENCE_ASC("Stage Order (1 to 13)"),
    ASSIGNED_AT_DESC("Newest Assignment"),
    ASSIGNED_AT_ASC("Oldest Assignment")
}

/**
 * UI State definition for the Operator Work Queue Screen (Module 04 Step 04).
 */
sealed interface ProductionOperatorWorkQueueUiState {

    data object Loading : ProductionOperatorWorkQueueUiState

    data class Success(
        val allWorkItems: List<OperatorWorkItem>,
        val visibleWorkItems: List<OperatorWorkItem>,
        val availableOperators: List<ProductionOperator>,
        val selectedOperatorId: String? = null,
        val searchQuery: String = "",
        val selectedAssignmentStatus: StageAssignmentStatus? = null,
        val selectedStageStatus: ProductionStageStatus? = null,
        val selectedPriority: OrderPriority? = null,
        val selectedStageType: ProductionStageType? = null,
        val sortOrder: OperatorWorkSortOrder = OperatorWorkSortOrder.PRIORITY_DESC,
        val isActionInProgress: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : ProductionOperatorWorkQueueUiState {
        val totalCount: Int get() = allWorkItems.size
        val visibleCount: Int get() = visibleWorkItems.size
        val isFiltered: Boolean
            get() = selectedOperatorId != null ||
                    searchQuery.isNotBlank() ||
                    selectedAssignmentStatus != null ||
                    selectedStageStatus != null ||
                    selectedPriority != null ||
                    selectedStageType != null
    }

    data class Empty(
        val message: String = "No stage assignments found. Assignments made to production operators will appear here."
    ) : ProductionOperatorWorkQueueUiState

    data class Error(
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : ProductionOperatorWorkQueueUiState
}

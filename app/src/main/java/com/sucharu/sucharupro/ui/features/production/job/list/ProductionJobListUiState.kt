package com.sucharu.sucharupro.ui.features.production.job.list

import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageType

/**
 * Sorting strategy options for the Production Job Queue.
 */
enum class ProductionJobSortOrder(val defaultLabel: String) {
    PRIORITY_DESC("Priority (Urgent first)"),
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    PROGRESS_ASC("Least Progressed"),
    PROGRESS_DESC("Most Progressed")
}

/**
 * UI State definition for Production Job Queue, Search, and Filtering (Module 04).
 */
sealed interface ProductionJobListUiState {

    /** Initial loading state while fetching Production Jobs. */
    data object Loading : ProductionJobListUiState

    /** Successfully loaded Production Jobs with active search and filters. */
    data class Success(
        val allJobs: List<ProductionJob>,
        val visibleJobs: List<ProductionJob>,
        val searchQuery: String = "",
        val selectedStatus: ProductionJobStatus? = null,
        val selectedPriority: OrderPriority? = null,
        val selectedStage: ProductionStageType? = null,
        val sortOrder: ProductionJobSortOrder = ProductionJobSortOrder.PRIORITY_DESC,
        val isActionInProgress: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : ProductionJobListUiState {
        val totalCount: Int get() = allJobs.size
        val visibleCount: Int get() = visibleJobs.size
        val isFiltered: Boolean
            get() = searchQuery.isNotBlank() || selectedStatus != null || selectedPriority != null || selectedStage != null
    }

    /** Empty state when no Production Job records exist at all. */
    data class Empty(
        val message: String = "No production jobs available. Jobs converted from commercial order handoffs will appear here."
    ) : ProductionJobListUiState

    /** Error state when fetching Production Jobs fails. */
    data class Error(
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : ProductionJobListUiState
}

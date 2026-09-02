package com.sucharu.sucharupro.ui.features.production.job.details

import com.sucharu.sucharupro.domain.model.job.ProductionJob

/**
 * UI State definition for Production Job Details & Stage Control screen (Module 04).
 */
sealed interface ProductionJobDetailsUiState {

    /** Initial loading state while fetching the Job Card. */
    data object Loading : ProductionJobDetailsUiState

    /** Successfully loaded Production Job Card with active stage states, execution history, output tracking, and activity timeline. */
    data class Success(
        val job: ProductionJob,
        val activities: List<com.sucharu.sucharupro.domain.model.job.ProductionActivityEvent> = emptyList(),
        val stageExecutions: List<com.sucharu.sucharupro.domain.model.job.ProductionStageExecution> = emptyList(),
        val stageOutputs: List<com.sucharu.sucharupro.domain.model.job.ProductionStageOutput> = emptyList(),
        val reconciliation: com.sucharu.sucharupro.domain.model.job.ProductionOutputReconciliation? = null,
        val completionChecklist: com.sucharu.sucharupro.domain.model.job.ProductionCompletionChecklist? = null,
        val isActionInProgress: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : ProductionJobDetailsUiState

    /** Job Card not found with the requested ID. */
    data class NotFound(
        val jobId: String
    ) : ProductionJobDetailsUiState

    /** Error state when Job Card retrieval fails. */
    data class Error(
        val errorMessage: String,
        val canRetry: Boolean = true
    ) : ProductionJobDetailsUiState
}

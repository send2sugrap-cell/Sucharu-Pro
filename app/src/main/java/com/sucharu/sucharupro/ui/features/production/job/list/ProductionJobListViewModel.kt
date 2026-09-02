package com.sucharu.sucharupro.ui.features.production.job.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.repository.ProductionJobRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * ViewModel managing reactive observation, search, compound filtering, sorting,
 * and handoff conversion for the Production Job Queue.
 */
class ProductionJobListViewModel(
    private val repository: ProductionJobRepository,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow<ProductionJobListUiState>(ProductionJobListUiState.Loading)
    val uiState: StateFlow<ProductionJobListUiState> = _uiState.asStateFlow()

    private var rawJobs: List<ProductionJob> = emptyList()
    private var currentSearchQuery: String = ""
    private var currentStatusFilter: ProductionJobStatus? = null
    private var currentPriorityFilter: OrderPriority? = null
    private var currentStageFilter: ProductionStageType? = null
    private var currentSortOrder: ProductionJobSortOrder = ProductionJobSortOrder.PRIORITY_DESC

    init {
        loadJobs()
    }

    private fun getCurrentIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    fun loadJobs() {
        scope.launch {
            repository.observeJobs()
                .onStart {
                    _uiState.value = ProductionJobListUiState.Loading
                }
                .catch { exception ->
                    _uiState.value = ProductionJobListUiState.Error(
                        errorMessage = exception.localizedMessage ?: "Failed to load production jobs."
                    )
                }
                .collect { jobs ->
                    rawJobs = jobs
                    if (jobs.isEmpty()) {
                        _uiState.value = ProductionJobListUiState.Empty()
                    } else {
                        updateFilteredState()
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        currentSearchQuery = query
        updateFilteredState()
    }

    fun onStatusFilterChange(status: ProductionJobStatus?) {
        currentStatusFilter = status
        updateFilteredState()
    }

    fun onPriorityFilterChange(priority: OrderPriority?) {
        currentPriorityFilter = priority
        updateFilteredState()
    }

    fun onStageFilterChange(stageType: ProductionStageType?) {
        currentStageFilter = stageType
        updateFilteredState()
    }

    fun onSortOrderChange(sortOrder: ProductionJobSortOrder) {
        currentSortOrder = sortOrder
        updateFilteredState()
    }

    fun clearFilters() {
        currentSearchQuery = ""
        currentStatusFilter = null
        currentPriorityFilter = null
        currentStageFilter = null
        currentSortOrder = ProductionJobSortOrder.PRIORITY_DESC
        updateFilteredState()
    }

    fun createJobFromHandoff(
        handoff: OrderJobHandoff,
        title: String? = null,
        description: String? = null,
        createdBy: String? = null
    ) {
        val currentSuccess = _uiState.value as? ProductionJobListUiState.Success
        if (currentSuccess != null) {
            _uiState.value = currentSuccess.copy(isActionInProgress = true, actionError = null)
        }

        scope.launch {
            val result = repository.createJobFromHandoff(
                handoff = handoff,
                title = title,
                description = description,
                createdBy = createdBy,
                timestamp = getCurrentIsoTimestamp()
            )
            when (result) {
                is DomainResult.Success -> {
                    val updatedSuccess = _uiState.value as? ProductionJobListUiState.Success
                    if (updatedSuccess != null) {
                        _uiState.value = updatedSuccess.copy(
                            isActionInProgress = false,
                            actionMessage = "Production Job ${result.data.jobNumber} created successfully.",
                            actionError = null
                        )
                    }
                }
                is DomainResult.Error -> {
                    val updatedSuccess = _uiState.value as? ProductionJobListUiState.Success
                    if (updatedSuccess != null) {
                        _uiState.value = updatedSuccess.copy(
                            isActionInProgress = false,
                            actionError = result.message
                        )
                    } else {
                        _uiState.value = ProductionJobListUiState.Error(
                            errorMessage = result.message
                        )
                    }
                }
                DomainResult.Loading -> Unit
            }
        }
    }

    fun dismissActionFeedback() {
        val current = _uiState.value as? ProductionJobListUiState.Success ?: return
        _uiState.value = current.copy(actionMessage = null, actionError = null)
    }

    private fun updateFilteredState() {
        if (rawJobs.isEmpty()) {
            _uiState.value = ProductionJobListUiState.Empty()
            return
        }

        val filtered = rawJobs.filter { job ->
            matchesSearch(job, currentSearchQuery) &&
                    matchesStatus(job, currentStatusFilter) &&
                    matchesPriority(job, currentPriorityFilter) &&
                    matchesStage(job, currentStageFilter)
        }

        val sorted = applySorting(filtered, currentSortOrder)

        val previousSuccess = _uiState.value as? ProductionJobListUiState.Success
        _uiState.value = ProductionJobListUiState.Success(
            allJobs = rawJobs,
            visibleJobs = sorted,
            searchQuery = currentSearchQuery,
            selectedStatus = currentStatusFilter,
            selectedPriority = currentPriorityFilter,
            selectedStage = currentStageFilter,
            sortOrder = currentSortOrder,
            isActionInProgress = previousSuccess?.isActionInProgress ?: false,
            actionMessage = previousSuccess?.actionMessage,
            actionError = previousSuccess?.actionError
        )
    }

    private fun matchesSearch(job: ProductionJob, query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return true

        return job.jobNumber.contains(trimmed, ignoreCase = true) ||
                job.jobId.contains(trimmed, ignoreCase = true) ||
                job.orderNumber.contains(trimmed, ignoreCase = true) ||
                job.customerId.contains(trimmed, ignoreCase = true) ||
                job.title.contains(trimmed, ignoreCase = true) ||
                (job.specification?.contains(trimmed, ignoreCase = true) == true) ||
                (job.description?.contains(trimmed, ignoreCase = true) == true) ||
                job.handoffId.contains(trimmed, ignoreCase = true)
    }

    private fun matchesStatus(job: ProductionJob, status: ProductionJobStatus?): Boolean {
        if (status == null) return true
        return job.status == status
    }

    private fun matchesPriority(job: ProductionJob, priority: OrderPriority?): Boolean {
        if (priority == null) return true
        return job.priority == priority
    }

    private fun matchesStage(job: ProductionJob, stageType: ProductionStageType?): Boolean {
        if (stageType == null) return true
        return job.currentStage?.stageType == stageType
    }

    private fun applySorting(jobs: List<ProductionJob>, sortOrder: ProductionJobSortOrder): List<ProductionJob> {
        return when (sortOrder) {
            ProductionJobSortOrder.PRIORITY_DESC -> jobs.sortedWith(
                compareByDescending<ProductionJob> { priorityWeight(it.priority) }
                    .thenByDescending { it.createdAt }
            )
            ProductionJobSortOrder.DATE_DESC -> jobs.sortedByDescending { it.createdAt }
            ProductionJobSortOrder.DATE_ASC -> jobs.sortedBy { it.createdAt }
            ProductionJobSortOrder.PROGRESS_ASC -> jobs.sortedBy { it.progressFraction }
            ProductionJobSortOrder.PROGRESS_DESC -> jobs.sortedByDescending { it.progressFraction }
        }
    }

    private fun priorityWeight(priority: OrderPriority): Int {
        return when (priority) {
            OrderPriority.URGENT -> 3
            OrderPriority.HIGH -> 2
            OrderPriority.NORMAL -> 1
        }
    }
}

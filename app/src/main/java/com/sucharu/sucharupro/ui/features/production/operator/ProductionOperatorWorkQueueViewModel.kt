package com.sucharu.sucharupro.ui.features.production.operator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionOperator
import com.sucharu.sucharupro.domain.model.job.ProductionStageAssignment
import com.sucharu.sucharupro.domain.model.job.StageAssignmentStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.production.ProductionStageStatus
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.repository.ProductionJobRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * ViewModel managing reactive observation, search, filtering, and sorting for the Operator Work Queue.
 */
class ProductionOperatorWorkQueueViewModel(
    private val repository: ProductionJobRepository,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow<ProductionOperatorWorkQueueUiState>(ProductionOperatorWorkQueueUiState.Loading)
    val uiState: StateFlow<ProductionOperatorWorkQueueUiState> = _uiState.asStateFlow()

    private var rawWorkItems: List<OperatorWorkItem> = emptyList()
    private var availableOperators: List<ProductionOperator> = emptyList()

    private var currentOperatorId: String? = null
    private var currentSearchQuery: String = ""
    private var currentAssignmentStatus: StageAssignmentStatus? = null
    private var currentStageStatus: ProductionStageStatus? = null
    private var currentPriority: OrderPriority? = null
    private var currentStageType: ProductionStageType? = null
    private var currentSortOrder: OperatorWorkSortOrder = OperatorWorkSortOrder.PRIORITY_DESC

    init {
        availableOperators = repository.getAvailableOperators()
        loadWorkQueue()
    }

    fun loadWorkQueue() {
        scope.launch {
            combine(
                repository.observeJobs(),
                repository.observeStageAssignments()
            ) { jobs, assignments ->
                buildWorkItems(jobs, assignments)
            }
                .onStart {
                    _uiState.value = ProductionOperatorWorkQueueUiState.Loading
                }
                .catch { exception ->
                    _uiState.value = ProductionOperatorWorkQueueUiState.Error(
                        errorMessage = exception.localizedMessage ?: "Failed to load operator work queue."
                    )
                }
                .collect { workItems ->
                    rawWorkItems = workItems
                    if (workItems.isEmpty()) {
                        _uiState.value = ProductionOperatorWorkQueueUiState.Empty()
                    } else {
                        updateFilteredState()
                    }
                }
        }
    }

    private fun buildWorkItems(
        jobs: List<ProductionJob>,
        assignments: List<ProductionStageAssignment>
    ): List<OperatorWorkItem> {
        val jobsMap = jobs.associateBy { it.jobId }

        return assignments.mapNotNull { assignment ->
            val job = jobsMap[assignment.jobId] ?: return@mapNotNull null
            val stage = job.stages.find { it.stageId == assignment.stageId } ?: return@mapNotNull null
            OperatorWorkItem(
                assignment = assignment,
                job = job,
                stage = stage
            )
        }
    }

    fun onOperatorSelect(operatorId: String?) {
        currentOperatorId = operatorId
        updateFilteredState()
    }

    fun onSearchQueryChange(query: String) {
        currentSearchQuery = query
        updateFilteredState()
    }

    fun onAssignmentStatusFilterChange(status: StageAssignmentStatus?) {
        currentAssignmentStatus = status
        updateFilteredState()
    }

    fun onStageStatusFilterChange(status: ProductionStageStatus?) {
        currentStageStatus = status
        updateFilteredState()
    }

    fun onPriorityFilterChange(priority: OrderPriority?) {
        currentPriority = priority
        updateFilteredState()
    }

    fun onStageTypeFilterChange(stageType: ProductionStageType?) {
        currentStageType = stageType
        updateFilteredState()
    }

    fun onSortOrderChange(sortOrder: OperatorWorkSortOrder) {
        currentSortOrder = sortOrder
        updateFilteredState()
    }

    fun clearFilters() {
        currentOperatorId = null
        currentSearchQuery = ""
        currentAssignmentStatus = null
        currentStageStatus = null
        currentPriority = null
        currentStageType = null
        currentSortOrder = OperatorWorkSortOrder.PRIORITY_DESC
        updateFilteredState()
    }

    private fun updateFilteredState() {
        if (rawWorkItems.isEmpty()) {
            _uiState.value = ProductionOperatorWorkQueueUiState.Empty()
            return
        }

        val filtered = rawWorkItems.filter { item ->
            matchesOperator(item, currentOperatorId) &&
                    matchesSearch(item, currentSearchQuery) &&
                    matchesAssignmentStatus(item, currentAssignmentStatus) &&
                    matchesStageStatus(item, currentStageStatus) &&
                    matchesPriority(item, currentPriority) &&
                    matchesStageType(item, currentStageType)
        }

        val sorted = applySorting(filtered, currentSortOrder)

        val prev = _uiState.value as? ProductionOperatorWorkQueueUiState.Success
        _uiState.value = ProductionOperatorWorkQueueUiState.Success(
            allWorkItems = rawWorkItems,
            visibleWorkItems = sorted,
            availableOperators = availableOperators,
            selectedOperatorId = currentOperatorId,
            searchQuery = currentSearchQuery,
            selectedAssignmentStatus = currentAssignmentStatus,
            selectedStageStatus = currentStageStatus,
            selectedPriority = currentPriority,
            selectedStageType = currentStageType,
            sortOrder = currentSortOrder,
            isActionInProgress = prev?.isActionInProgress ?: false,
            actionMessage = prev?.actionMessage,
            actionError = prev?.actionError
        )
    }

    private fun matchesOperator(item: OperatorWorkItem, operatorId: String?): Boolean {
        if (operatorId == null) return true
        return item.assignment.operatorId == operatorId
    }

    private fun matchesSearch(item: OperatorWorkItem, query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return true

        return item.job.jobNumber.contains(trimmed, ignoreCase = true) ||
                item.job.orderNumber.contains(trimmed, ignoreCase = true) ||
                item.job.title.contains(trimmed, ignoreCase = true) ||
                item.job.customerId.contains(trimmed, ignoreCase = true) ||
                item.stage.stageType.defaultLabel.contains(trimmed, ignoreCase = true) ||
                item.stage.stageType.shortCode.contains(trimmed, ignoreCase = true) ||
                item.assignment.operatorName.contains(trimmed, ignoreCase = true) ||
                item.assignment.operatorId.contains(trimmed, ignoreCase = true) ||
                (item.assignment.notes?.contains(trimmed, ignoreCase = true) == true)
    }

    private fun matchesAssignmentStatus(item: OperatorWorkItem, status: StageAssignmentStatus?): Boolean {
        if (status == null) return true
        return item.assignment.status == status
    }

    private fun matchesStageStatus(item: OperatorWorkItem, status: ProductionStageStatus?): Boolean {
        if (status == null) return true
        return item.stage.status == status
    }

    private fun matchesPriority(item: OperatorWorkItem, priority: OrderPriority?): Boolean {
        if (priority == null) return true
        return item.job.priority == priority
    }

    private fun matchesStageType(item: OperatorWorkItem, stageType: ProductionStageType?): Boolean {
        if (stageType == null) return true
        return item.stage.stageType == stageType
    }

    private fun applySorting(items: List<OperatorWorkItem>, sortOrder: OperatorWorkSortOrder): List<OperatorWorkItem> {
        return when (sortOrder) {
            OperatorWorkSortOrder.PRIORITY_DESC -> items.sortedWith(
                compareByDescending<OperatorWorkItem> { priorityWeight(it.job.priority) }
                    .thenBy { it.stage.sequence }
                    .thenByDescending { it.assignment.assignedAt }
            )
            OperatorWorkSortOrder.STAGE_SEQUENCE_ASC -> items.sortedWith(
                compareBy<OperatorWorkItem> { it.stage.sequence }
                    .thenByDescending { priorityWeight(it.job.priority) }
            )
            OperatorWorkSortOrder.ASSIGNED_AT_DESC -> items.sortedByDescending { it.assignment.assignedAt }
            OperatorWorkSortOrder.ASSIGNED_AT_ASC -> items.sortedBy { it.assignment.assignedAt }
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

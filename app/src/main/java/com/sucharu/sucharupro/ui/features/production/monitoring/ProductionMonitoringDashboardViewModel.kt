package com.sucharu.sucharupro.ui.features.production.monitoring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.job.ActiveProductionStageItem
import com.sucharu.sucharupro.domain.model.job.AttentionReasonType
import com.sucharu.sucharupro.domain.model.job.OperatorWorkloadItem
import com.sucharu.sucharupro.domain.model.job.ProductionAttentionItem
import com.sucharu.sucharupro.domain.model.job.ProductionMonitoringSnapshot
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.repository.ProductionJobRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel for the Live Production Monitoring and Supervisor Dashboard.
 */
class ProductionMonitoringDashboardViewModel(
    private val repository: ProductionJobRepository,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _filter = MutableStateFlow(ProductionMonitoringFilter.ALL)
    private val _searchQuery = MutableStateFlow("")

    private val _uiState = MutableStateFlow<ProductionMonitoringDashboardUiState>(
        ProductionMonitoringDashboardUiState.Loading
    )
    val uiState: StateFlow<ProductionMonitoringDashboardUiState> = _uiState.asStateFlow()

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        scope.launch {
            val repoDataFlow = combine(
                repository.observeProductionMonitoringSnapshot(),
                repository.observeActiveProductionStages(),
                repository.observeOperatorWorkloads(),
                repository.observeProductionAttentionItems()
            ) { snapshot, activeStages, workloads, attentionItems ->
                MonitoringData(snapshot, activeStages, workloads, attentionItems)
            }

            combine(
                repoDataFlow,
                _filter,
                _searchQuery
            ) { data, filter, query ->
                val (filteredStages, filteredAttention) = filterData(
                    activeStages = data.activeStages,
                    attentionItems = data.attentionItems,
                    filter = filter,
                    query = query.trim()
                )

                ProductionMonitoringDashboardUiState.Success(
                    snapshot = data.snapshot,
                    activeStages = data.activeStages,
                    operatorWorkloads = data.workloads,
                    attentionItems = data.attentionItems,
                    filter = filter,
                    searchQuery = query,
                    filteredActiveStages = filteredStages,
                    filteredAttentionItems = filteredAttention
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setFilter(filter: ProductionMonitoringFilter) {
        _filter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearFilters() {
        _filter.value = ProductionMonitoringFilter.ALL
        _searchQuery.value = ""
    }

    private fun filterData(
        activeStages: List<ActiveProductionStageItem>,
        attentionItems: List<ProductionAttentionItem>,
        filter: ProductionMonitoringFilter,
        query: String
    ): Pair<List<ActiveProductionStageItem>, List<ProductionAttentionItem>> {
        val matchesQueryStage: (ActiveProductionStageItem) -> Boolean = { item ->
            query.isBlank() ||
                    item.jobNumber.contains(query, ignoreCase = true) ||
                    item.jobTitle.contains(query, ignoreCase = true) ||
                    item.orderNumber.contains(query, ignoreCase = true) ||
                    item.stageType.defaultLabel.contains(query, ignoreCase = true) ||
                    (item.assignedOperatorName?.contains(query, ignoreCase = true) == true)
        }

        val matchesQueryAttention: (ProductionAttentionItem) -> Boolean = { item ->
            query.isBlank() ||
                    item.jobNumber.contains(query, ignoreCase = true) ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.description.contains(query, ignoreCase = true) ||
                    (item.stageType?.defaultLabel?.contains(query, ignoreCase = true) == true) ||
                    (item.operatorName?.contains(query, ignoreCase = true) == true)
        }

        val stageFiltered = activeStages.filter { item ->
            matchesQueryStage(item) && when (filter) {
                ProductionMonitoringFilter.ALL -> true
                ProductionMonitoringFilter.IN_PROGRESS -> true
                ProductionMonitoringFilter.READY_FOR_PRODUCTION -> false
                ProductionMonitoringFilter.ON_HOLD -> false
                ProductionMonitoringFilter.READY -> false
                ProductionMonitoringFilter.URGENT -> item.priority == OrderPriority.URGENT
                ProductionMonitoringFilter.UNASSIGNED -> item.assignedOperatorId == null
            }
        }

        val attentionFiltered = attentionItems.filter { item ->
            matchesQueryAttention(item) && when (filter) {
                ProductionMonitoringFilter.ALL -> true
                ProductionMonitoringFilter.IN_PROGRESS -> item.reasonType == AttentionReasonType.ACTIVE_STAGE || item.reasonType == AttentionReasonType.URGENT_ACTIVE_JOB
                ProductionMonitoringFilter.READY_FOR_PRODUCTION -> item.reasonType == AttentionReasonType.WAITING_TO_START || item.reasonType == AttentionReasonType.UNASSIGNED_ELIGIBLE_STAGE
                ProductionMonitoringFilter.ON_HOLD -> item.reasonType == AttentionReasonType.ON_HOLD_JOB
                ProductionMonitoringFilter.READY -> item.reasonType == AttentionReasonType.READY_FOR_DELIVERY
                ProductionMonitoringFilter.URGENT -> item.priority == OrderPriority.URGENT
                ProductionMonitoringFilter.UNASSIGNED -> item.reasonType == AttentionReasonType.UNASSIGNED_ELIGIBLE_STAGE
            }
        }

        return Pair(stageFiltered, attentionFiltered)
    }
}

private data class MonitoringData(
    val snapshot: ProductionMonitoringSnapshot,
    val activeStages: List<ActiveProductionStageItem>,
    val workloads: List<OperatorWorkloadItem>,
    val attentionItems: List<ProductionAttentionItem>
)

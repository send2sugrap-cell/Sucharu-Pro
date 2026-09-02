package com.sucharu.sucharupro.ui.features.production.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.job.CompletionFilter
import com.sucharu.sucharupro.domain.model.job.ProductionDateRangeFilter
import com.sucharu.sucharupro.domain.model.job.ProductionHistoryFilter
import com.sucharu.sucharupro.domain.model.job.ProductionHistorySortBy
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.repository.ProductionJobRepository
import com.sucharu.sucharupro.domain.validation.ProductionHistoryCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel managing the state, search, filter, and sorting for Production History.
 */
class ProductionHistoryViewModel(
    private val repository: ProductionJobRepository,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _searchQuery = MutableStateFlow("")
    private val _filter = MutableStateFlow(ProductionHistoryFilter())
    private val _uiState = MutableStateFlow<ProductionHistoryUiState>(ProductionHistoryUiState.Loading)
    val uiState: StateFlow<ProductionHistoryUiState> = _uiState.asStateFlow()

    init {
        startObservingHistory()
    }

    private fun startObservingHistory() {
        scope.launch {
            combine(
                repository.observeProductionHistory(),
                _searchQuery,
                _filter
            ) { summaries, query, filter ->
                val filtered = ProductionHistoryCalculator.filterAndSortHistory(
                    summaries = summaries,
                    filter = filter,
                    query = query
                )
                ProductionHistoryUiState.Success(
                    allSummaries = summaries,
                    filteredSummaries = filtered,
                    searchQuery = query,
                    filter = filter
                )
            }.catch { error ->
                _uiState.value = ProductionHistoryUiState.Error(error.message ?: "Failed to load production history.")
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: ProductionJobStatus?) {
        _filter.value = _filter.value.copy(status = status)
    }

    fun setPriorityFilter(priority: OrderPriority?) {
        _filter.value = _filter.value.copy(priority = priority)
    }

    fun setCompletionFilter(completion: CompletionFilter) {
        _filter.value = _filter.value.copy(completion = completion)
    }

    fun setDateRange(dateRange: ProductionDateRangeFilter) {
        _filter.value = _filter.value.copy(dateRange = dateRange)
    }

    fun setSortBy(sortBy: ProductionHistorySortBy) {
        _filter.value = _filter.value.copy(sortBy = sortBy)
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _filter.value = ProductionHistoryFilter()
    }

    fun retry() {
        _uiState.value = ProductionHistoryUiState.Loading
        startObservingHistory()
    }
}

package com.sucharu.sucharupro.ui.features.production.performance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.job.ProductionDateRangeFilter
import com.sucharu.sucharupro.domain.repository.ProductionJobRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * ViewModel managing reactive production performance analytics and date-range controls.
 */
class ProductionPerformanceViewModel(
    private val repository: ProductionJobRepository,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope get() = externalScope ?: viewModelScope

    private val _dateRange = MutableStateFlow(ProductionDateRangeFilter.ALL_TIME)
    private val _uiState = MutableStateFlow<ProductionPerformanceUiState>(ProductionPerformanceUiState.Loading)
    val uiState: StateFlow<ProductionPerformanceUiState> = _uiState.asStateFlow()

    init {
        startObservingPerformance()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun startObservingPerformance() {
        scope.launch {
            _dateRange.flatMapLatest { range ->
                combine(
                    repository.observeProductionPerformanceMetrics(range),
                    repository.observeOperatorPerformance(),
                    repository.observeStagePerformance()
                ) { metrics, operators, stages ->
                    ProductionPerformanceUiState.Success(
                        metrics = metrics,
                        operatorPerformances = operators,
                        stagePerformances = stages,
                        selectedDateRange = range
                    )
                }
            }.catch { error ->
                _uiState.value = ProductionPerformanceUiState.Error(error.message ?: "Failed to load performance analytics.")
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setDateRange(range: ProductionDateRangeFilter) {
        _dateRange.value = range
    }

    fun retry() {
        _uiState.value = ProductionPerformanceUiState.Loading
        startObservingPerformance()
    }
}

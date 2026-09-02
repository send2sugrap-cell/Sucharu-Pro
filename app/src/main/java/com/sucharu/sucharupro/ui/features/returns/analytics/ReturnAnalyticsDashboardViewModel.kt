package com.sucharu.sucharupro.ui.features.returns.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ReturnAnalyticsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Return Analytics Dashboard (Module 11 Step 06).
 */
class ReturnAnalyticsDashboardViewModel(
    private val repository: ReturnAnalyticsRepository,
    private val coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope get() = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(ReturnAnalyticsUiState())
    val uiState: StateFlow<ReturnAnalyticsUiState> = _uiState.asStateFlow()

    fun loadAnalytics(
        projectId: String,
        period: ReturnAnalyticsPeriod = _uiState.value.selectedPeriod,
        totalDispatchedCount: Int? = _uiState.value.totalDispatchedCount,
        callerRole: UserRole? = null,
        callerProjectId: String? = projectId
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, projectId = projectId, selectedPeriod = period, totalDispatchedCount = totalDispatchedCount) }

        scope.launch {
            val summaryRes = repository.getAnalyticsSummary(projectId, period, totalDispatchedCount, callerRole, callerProjectId)
            val defectRes = repository.getDefectBreakdown(projectId, period, callerRole, callerProjectId)
            val financeRes = repository.getFinancialBreakdown(projectId, period, callerRole, callerProjectId)
            val trendRes = repository.getTrends(projectId, period, callerRole, callerProjectId)

            if (summaryRes is DomainResult.Success &&
                defectRes is DomainResult.Success &&
                financeRes is DomainResult.Success &&
                trendRes is DomainResult.Success
            ) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        summary = summaryRes.data,
                        defectBreakdown = defectRes.data,
                        financialBreakdown = financeRes.data,
                        trends = trendRes.data,
                        errorMessage = null
                    )
                }
            } else {
                val errorMsg = when {
                    summaryRes is DomainResult.Error -> summaryRes.message
                    defectRes is DomainResult.Error -> defectRes.message
                    financeRes is DomainResult.Error -> financeRes.message
                    trendRes is DomainResult.Error -> trendRes.message
                    else -> "Failed to load Return Analytics."
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
            }
        }
    }

    fun onPeriodChanged(period: ReturnAnalyticsPeriod, callerRole: UserRole? = null) {
        val currentProj = _uiState.value.projectId
        if (currentProj.isNotBlank()) {
            loadAnalytics(currentProj, period = period, callerRole = callerRole)
        }
    }

    fun refresh(callerRole: UserRole? = null) {
        val currentProj = _uiState.value.projectId
        if (currentProj.isNotBlank()) {
            loadAnalytics(currentProj, period = _uiState.value.selectedPeriod, callerRole = callerRole)
        }
    }
}

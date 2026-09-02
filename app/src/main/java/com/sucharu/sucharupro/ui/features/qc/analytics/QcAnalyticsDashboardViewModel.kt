package com.sucharu.sucharupro.ui.features.qc.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsThresholdConfig
import com.sucharu.sucharupro.domain.model.qc.analytics.QcPeriodType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.QcAnalyticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel governing the QC Analytics Dashboard (Module 06 Step 09).
 */
class QcAnalyticsDashboardViewModel(
    private val repository: QcAnalyticsRepository,
    private val currentUserRole: UserRole = UserRole.ADMIN
) : ViewModel() {

    private val _uiState = MutableStateFlow(QcAnalyticsDashboardUiState())
    val uiState: StateFlow<QcAnalyticsDashboardUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    fun selectTab(tab: QcAnalyticsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setPeriodType(type: QcPeriodType, customStart: String? = null, customEnd: String? = null) {
        val newPeriod = when (type) {
            QcPeriodType.TODAY -> QcAnalyticsPeriod.today()
            QcPeriodType.THIS_WEEK -> QcAnalyticsPeriod.thisWeek()
            QcPeriodType.THIS_MONTH -> QcAnalyticsPeriod.thisMonth()
            QcPeriodType.CUSTOM -> {
                if (customStart != null && customEnd != null) {
                    QcAnalyticsPeriod.custom(customStart, customEnd)
                } else {
                    _uiState.value.period
                }
            }
        }
        _uiState.update { it.copy(selectedPeriodType = type, period = newPeriod) }
        loadAnalytics()
    }

    fun setProjectFilter(projectId: String?) {
        _uiState.update { it.copy(selectedProjectId = projectId) }
        loadAnalytics()
    }

    fun updateThresholdConfig(config: QcAnalyticsThresholdConfig) {
        _uiState.update { it.copy(thresholdConfig = config) }
        loadAnalytics()
    }

    fun refresh() {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val period = _uiState.value.period
            val projectId = _uiState.value.selectedProjectId
            val config = _uiState.value.thresholdConfig

            val summaryRes = repository.getSummary(period, projectId, currentUserRole)
            val jobsRes = repository.getJobAnalytics(period, projectId, currentUserRole)
            val defectRes = repository.getDefectAnalytics(period, projectId, currentUserRole)
            val stageRes = repository.getStageAnalytics(period, projectId, currentUserRole)
            val trendsRes = repository.getTrends(period, projectId, currentUserRole)
            val insightsRes = repository.getOperationalInsights(period, projectId, config, currentUserRole)

            if (summaryRes is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, errorMessage = summaryRes.message) }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    summary = (summaryRes as? DomainResult.Success)?.data,
                    jobAnalytics = (jobsRes as? DomainResult.Success)?.data ?: emptyList(),
                    defectAnalytics = (defectRes as? DomainResult.Success)?.data ?: emptyList(),
                    stageAnalytics = (stageRes as? DomainResult.Success)?.data ?: emptyList(),
                    trends = (trendsRes as? DomainResult.Success)?.data ?: emptyList(),
                    insights = (insightsRes as? DomainResult.Success)?.data ?: emptyList(),
                    errorMessage = null
                )
            }
        }
    }
}

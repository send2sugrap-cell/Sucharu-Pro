package com.sucharu.sucharupro.ui.features.finance.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finance.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinanceAnalyticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for Finance Analytics & Governance (Module 09 Step 10).
 */
class FinanceAnalyticsViewModel(
    private val analyticsRepository: FinanceAnalyticsRepository,
    private val projectId: String = "PRJ-DEFAULT",
    private val currentActorId: String = "ACTOR-001",
    private val currentUserRole: UserRole = UserRole.ADMIN
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FinanceAnalyticsUiState(
            filter = AnalyticsFilter(projectId = projectId)
        )
    )
    val uiState: StateFlow<FinanceAnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
        observeSnapshots()
        observeActivityEvents()
    }

    fun selectTab(tab: FinanceAnalyticsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        loadCurrentTabData()
    }

    fun selectPeriod(period: FinancialReportPeriod) {
        _uiState.update {
            val updated = it.filter.copy(reportPeriod = period)
            it.copy(filter = updated)
        }
        loadCurrentTabData()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val filter = _uiState.value.filter
            when (val res = analyticsRepository.getDashboard(projectId, filter, currentActorId, currentUserRole)) {
                is DomainResult.Success -> {
                    val d = res.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            dashboard = d,
                            summary = d.summary,
                            profitability = d.profitability,
                            cashFlow = d.cashFlow,
                            receivable = d.receivable,
                            payable = d.payable,
                            expense = d.expense,
                            healthScore = d.healthScore,
                            risks = d.topRisks,
                            anomalies = d.recentAnomalies,
                            governanceControls = d.governanceControls
                        )
                    }
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadCurrentTabData() {
        val tab = _uiState.value.selectedTab
        if (tab == FinanceAnalyticsTab.OVERVIEW) {
            loadDashboard()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val filter = _uiState.value.filter
            when (tab) {
                FinanceAnalyticsTab.HEALTH -> {
                    when (val res = analyticsRepository.calculateFinancialHealth(projectId, filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, healthScore = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinanceAnalyticsTab.PROFITABILITY -> {
                    when (val res = analyticsRepository.getProfitabilityAnalytics(projectId, filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, profitability = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinanceAnalyticsTab.CASH_FLOW -> {
                    when (val res = analyticsRepository.getCashFlowAnalytics(projectId, filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, cashFlow = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinanceAnalyticsTab.RECEIVABLES -> {
                    when (val res = analyticsRepository.getReceivableAnalytics(projectId, filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, receivable = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinanceAnalyticsTab.PAYABLES -> {
                    when (val res = analyticsRepository.getPayableAnalytics(projectId, filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, payable = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinanceAnalyticsTab.EXPENSES -> {
                    when (val res = analyticsRepository.getExpenseAnalytics(projectId, filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, expense = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinanceAnalyticsTab.COLLECTIONS -> {
                    when (val res = analyticsRepository.getCollectionPerformance(projectId, filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, collectionPerformance = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinanceAnalyticsTab.RISKS -> {
                    when (val res = analyticsRepository.detectRisks(projectId, filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, risks = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinanceAnalyticsTab.ANOMALIES -> {
                    when (val res = analyticsRepository.detectAnomalies(projectId, filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, anomalies = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinanceAnalyticsTab.GOVERNANCE -> {
                    when (val res = analyticsRepository.runGovernanceControls(projectId, filter, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, governanceControls = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinanceAnalyticsTab.COMPARISON -> {
                    when (val res = analyticsRepository.comparePeriods(
                        projectId, FinancialReportPeriod.CurrentMonth, FinancialReportPeriod.PreviousMonth, currentActorId, currentUserRole
                    )) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, periodComparison = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinanceAnalyticsTab.FORECAST -> {
                    when (val res = analyticsRepository.generateForecast(projectId, ForecastMethod.MOVING_AVERAGE, currentActorId, currentUserRole)) {
                        is DomainResult.Success -> _uiState.update { it.copy(isLoading = false, forecast = res.data) }
                        is DomainResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                        else -> _uiState.update { it.copy(isLoading = false) }
                    }
                }
                FinanceAnalyticsTab.SNAPSHOTS -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun createSnapshot() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val filter = _uiState.value.filter
            val requestId = UUID.randomUUID().toString()
            when (val res = analyticsRepository.createSnapshot(projectId, filter, requestId, currentActorId, currentUserRole)) {
                is DomainResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Analytics Snapshot (#${res.data.snapshotId.take(8)}) saved with SHA-256 integrity seal."
                        )
                    }
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun observeSnapshots() {
        viewModelScope.launch {
            analyticsRepository.observeSnapshots(projectId, currentUserRole).collect { list ->
                _uiState.update { it.copy(snapshots = list) }
            }
        }
    }

    private fun observeActivityEvents() {
        viewModelScope.launch {
            analyticsRepository.observeActivityEvents(projectId, currentUserRole).collect { list ->
                _uiState.update { it.copy(activityEvents = list) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

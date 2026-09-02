package com.sucharu.sucharupro.ui.features.qc.governance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.qc.governance.QcAlertSeverity
import com.sucharu.sucharupro.domain.model.qc.governance.QcGovernanceKpi
import com.sucharu.sucharupro.domain.model.qc.governance.QcImprovementEffectiveness
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.QcGovernanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel orchestrating quality governance dashboard reactive flows and user interactions.
 */
class QcGovernanceDashboardViewModel(
    private val repository: QcGovernanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QcGovernanceDashboardUiState())
    val uiState: StateFlow<QcGovernanceDashboardUiState> = _uiState.asStateFlow()

    fun loadGovernanceDashboard(
        projectId: String,
        period: QcAnalyticsPeriod = QcAnalyticsPeriod.thisMonth(),
        callerRole: UserRole = UserRole.ADMIN
    ) {
        _uiState.update { it.copy(isLoading = true, selectedProjectId = projectId, selectedPeriod = period, errorMessage = null) }

        viewModelScope.launch {
            // Evaluate thresholds
            val evalResult = repository.evaluateProjectKpis(period, projectId, callerRole)
            if (evalResult is DomainResult.Success) {
                val evaluations = evalResult.data
                val efficiencyScore = evaluations.find { it.kpiType == QcGovernanceKpi.QUALITY_EFFICIENCY_SCORE }?.currentValue ?: 100.0

                _uiState.update { current ->
                    current.copy(
                        kpiEvaluations = evaluations,
                        overallQualityScore = efficiencyScore
                    )
                }
            } else if (evalResult is DomainResult.Error) {
                _uiState.update { it.copy(errorMessage = evalResult.message) }
            }

            // Combine reactive streams in pairs / groups
            val alertsAndTargetsFlow = combine(
                repository.observeTargets(projectId),
                repository.observeAlerts(projectId)
            ) { targets, alerts -> Pair(targets, alerts) }

            val actionsAndReviewsFlow = combine(
                repository.observeImprovementActions(projectId),
                repository.observeReviews(projectId)
            ) { actions, reviews -> Pair(actions, reviews) }

            val snapshotsAndEventsFlow = combine(
                repository.observeSnapshots(projectId),
                repository.observeActivityEvents(projectId)
            ) { snapshots, events -> Pair(snapshots, events) }

            combine(
                alertsAndTargetsFlow,
                actionsAndReviewsFlow,
                snapshotsAndEventsFlow
            ) { (targets, alerts), (actions, reviews), (snapshots, events) ->
                val activeAlerts = alerts.filter { !it.isTerminal }
                val criticals = activeAlerts.count { it.severity == QcAlertSeverity.CRITICAL }
                val warnings = activeAlerts.count { it.severity == QcAlertSeverity.WARNING }
                val openActions = actions.filter { !it.isTerminal }

                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        activeTargets = targets.filter { it.active },
                        alerts = alerts,
                        activeAlerts = activeAlerts,
                        criticalAlertCount = criticals,
                        warningAlertCount = warnings,
                        improvementActions = actions,
                        openActionCount = openActions.size,
                        reviews = reviews,
                        snapshots = snapshots,
                        recentActivity = events.take(10)
                    )
                }
            }.collect {}
        }
    }

    fun acknowledgeAlert(
        alertId: String,
        acknowledgedBy: String,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole = UserRole.QC_INSPECTOR
    ) {
        viewModelScope.launch {
            repository.acknowledgeAlert(alertId, acknowledgedBy, notes, timestamp, callerRole)
        }
    }

    fun resolveAlert(
        alertId: String,
        resolvedBy: String,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            repository.resolveAlert(alertId, resolvedBy, notes, timestamp, callerRole)
        }
    }

    fun approveAction(
        actionId: String,
        approvedBy: String,
        timestamp: String,
        callerRole: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            repository.approveImprovementAction(actionId, approvedBy, null, timestamp, callerRole)
        }
    }

    fun verifyAction(
        actionId: String,
        verifiedBy: String,
        effectiveness: QcImprovementEffectiveness,
        notes: String,
        timestamp: String,
        callerRole: UserRole = UserRole.MANAGER
    ) {
        viewModelScope.launch {
            repository.verifyImprovementAction(actionId, verifiedBy, null, effectiveness, notes, timestamp, callerRole)
        }
    }

    fun createSnapshot(
        projectId: String,
        period: QcAnalyticsPeriod,
        generatedBy: String,
        timestamp: String,
        callerRole: UserRole = UserRole.ADMIN
    ) {
        viewModelScope.launch {
            repository.createSnapshot(period, projectId, generatedBy, timestamp, callerRole)
        }
    }
}

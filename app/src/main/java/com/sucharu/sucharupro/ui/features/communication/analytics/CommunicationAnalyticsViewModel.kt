package com.sucharu.sucharupro.ui.features.communication.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAnalyticsFilter
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.communication.analytics.CommunicationAnalyticsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class CommunicationAnalyticsViewModel(
    private val repository: CommunicationAnalyticsRepository,
    private val currentUserRole: UserRole, // Assume injected or retrieved from user session
    private val currentUserId: String, // Assume injected
    private val projectId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CommunicationAnalyticsUiState(
            projectId = projectId,
            filter = CommunicationAnalyticsFilter(
                projectId = projectId,
                fromDate = Instant.now().minus(30, ChronoUnit.DAYS),
                toDate = Instant.now()
            )
        )
    )
    val uiState: StateFlow<CommunicationAnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeSnapshots()
        observeExportRequests()
    }

    fun updateFilter(newFilter: CommunicationAnalyticsFilter) {
        _uiState.update { it.copy(filter = newFilter) }
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val filter = _uiState.value.filter

            // In production, these should be parallelized via async/await
            val kpiResult = repository.getKpiSummary(filter, currentUserRole)
            val channelsResult = repository.getChannelAnalytics(filter, currentUserRole)
            val typesResult = repository.getTypeAnalytics(filter, currentUserRole)
            val risksResult = repository.getRiskIndicators(filter, currentUserRole)
            val anomaliesResult = repository.getAnomalies(filter, currentUserRole)
            val govResult = repository.getGovernanceResult(filter, currentUserRole)
            val custEngResult = repository.getCustomerEngagement(filter, currentUserRole)
            val intEngResult = repository.getInternalCommunicationAnalytics(filter, currentUserRole)
            val vendorEngResult = repository.getVendorCommunicationAnalytics(filter, currentUserRole)
            val campaignResult = repository.getCampaignPerformance(filter, currentUserRole)
            val automationResult = repository.getAutomationAnalytics(filter, currentUserRole)
            val forecastResult = repository.getForecast(filter, currentUserRole)

            // Period Comparison
            val durationMs = filter.toDate.toEpochMilli() - filter.fromDate.toEpochMilli()
            val prevFilter = filter.copy(
                fromDate = Instant.ofEpochMilli(filter.fromDate.toEpochMilli() - durationMs),
                toDate = filter.fromDate
            )
            val compResult = repository.comparePeriods(projectId, filter, prevFilter, currentUserRole)
            
            // History
            val historyResult = repository.getActivityHistory(projectId, currentUserRole)

            // Step 10
            val healthResult = repository.getOperationalHealthProjection(projectId, currentUserRole)
            val auditResult = repository.getAuditEvents(projectId, currentUserRole)

            if (kpiResult is DomainResult.Error) {
                _uiState.update { it.copy(isLoading = false, error = kpiResult.message) }
                return@launch
            }

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    kpiSummary = (kpiResult as? DomainResult.Success)?.data,
                    channelAnalytics = (channelsResult as? DomainResult.Success)?.data ?: emptyList(),
                    typeAnalytics = (typesResult as? DomainResult.Success)?.data ?: emptyList(),
                    riskIndicators = (risksResult as? DomainResult.Success)?.data ?: emptyList(),
                    anomalies = (anomaliesResult as? DomainResult.Success)?.data ?: emptyList(),
                    governanceResult = (govResult as? DomainResult.Success)?.data,
                    customerEngagement = (custEngResult as? DomainResult.Success)?.data ?: emptyList(),
                    internalEngagement = (intEngResult as? DomainResult.Success)?.data ?: emptyList(),
                    vendorEngagement = (vendorEngResult as? DomainResult.Success)?.data ?: emptyList(),
                    campaignAnalytics = (campaignResult as? DomainResult.Success)?.data ?: emptyList(),
                    automationAnalytics = (automationResult as? DomainResult.Success)?.data ?: emptyList(),
                    forecastSummary = (forecastResult as? DomainResult.Success)?.data,
                    periodComparison = (compResult as? DomainResult.Success)?.data,
                    activityHistory = (historyResult as? DomainResult.Success)?.data ?: emptyList(),
                    operationalHealth = (healthResult as? DomainResult.Success)?.data,
                    auditEvents = (auditResult as? DomainResult.Success)?.data ?: emptyList()
                )
            }
        }
    }

    private fun observeSnapshots() {
        viewModelScope.launch {
            repository.observeSnapshots(projectId).collect { snaps ->
                _uiState.update { it.copy(snapshots = snaps) }
            }
        }
    }

    fun generateSnapshot() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSnapshotGenerationLoading = true, error = null) }
            val idempotencyKey = UUID.randomUUID().toString()
            val res = repository.createSnapshot(
                filter = _uiState.value.filter,
                actorId = currentUserId,
                actorRole = currentUserRole,
                idempotencyKey = idempotencyKey
            )
            if (res is DomainResult.Error) {
                _uiState.update { it.copy(isSnapshotGenerationLoading = false, error = res.message) }
            } else {
                _uiState.update { it.copy(isSnapshotGenerationLoading = false) }
                // Activity history refresh
                val historyResult = repository.getActivityHistory(projectId, currentUserRole)
                if (historyResult is DomainResult.Success) {
                    _uiState.update { it.copy(activityHistory = historyResult.data) }
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun dismissVerificationResult() {
        _uiState.update { it.copy(verificationResult = null) }
    }

    // =========================================================================
    // STEP 10: Governance, Export & Verification Actions
    // =========================================================================

    private fun observeExportRequests() {
        viewModelScope.launch {
            repository.observeExportRequests(projectId).collect { requests ->
                _uiState.update { it.copy(exportRequests = requests) }
            }
        }
    }

    fun verifySnapshot(snapshotId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifyingSnapshot = true, error = null, verificationResult = null) }
            val res = repository.verifySnapshot(
                snapshotId = snapshotId,
                projectId = projectId,
                actorUserId = currentUserId,
                actorRole = currentUserRole
            )
            
            _uiState.update { it.copy(isVerifyingSnapshot = false) }
            
            when (res) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(verificationResult = res.data) }
                    refreshAuditLogs()
                }
                is DomainResult.Error -> {
                    _uiState.update { it.copy(error = res.message) }
                }
                is DomainResult.Loading -> { }
            }
        }
    }

    fun requestExport(exportType: com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationExportType, snapshotId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRequestingExport = true, error = null) }
            
            val request = com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationExportRequest(
                exportId = UUID.randomUUID().toString(),
                projectId = projectId,
                requestedBy = currentUserId,
                exportType = exportType,
                snapshotReference = snapshotId,
                correlationId = UUID.randomUUID().toString()
            )
            
            val res = repository.requestExport(request, currentUserRole)
            
            _uiState.update { it.copy(isRequestingExport = false) }
            
            if (res is DomainResult.Error) {
                _uiState.update { it.copy(error = res.message) }
            } else {
                refreshAuditLogs()
            }
        }
    }

    fun acknowledgeGovernanceAction(
        targetType: String,
        targetId: String,
        actionType: com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationGovernanceActionType,
        resultingState: String,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAcknowledgingGovernance = true, error = null) }
            
            val action = com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationGovernanceAction(
                actionId = UUID.randomUUID().toString(),
                projectId = projectId,
                actorUserId = currentUserId,
                actionType = actionType,
                targetType = targetType,
                targetId = targetId,
                resultingState = resultingState,
                reason = notes
            )
            
            val res = repository.acknowledgeGovernanceAction(action, currentUserRole)
            
            _uiState.update { it.copy(isAcknowledgingGovernance = false) }
            
            if (res is DomainResult.Error) {
                _uiState.update { it.copy(error = res.message) }
            } else {
                refreshAuditLogs()
                // Refresh operational health
                loadData()
            }
        }
    }

    private suspend fun refreshAuditLogs() {
        val auditResult = repository.getAuditEvents(projectId, currentUserRole)
        if (auditResult is DomainResult.Success) {
            _uiState.update { it.copy(auditEvents = auditResult.data) }
        }
    }
}

package com.sucharu.sucharupro.domain.repository.communication.analytics

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.analytics.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

interface CommunicationAnalyticsRepository {
    
    suspend fun getKpiSummary(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<CommunicationKpiSummary>

    suspend fun getChannelAnalytics(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<List<CommunicationChannelAnalytics>>

    suspend fun getTypeAnalytics(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<List<CommunicationTypeAnalytics>>

    suspend fun getCustomerEngagement(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<List<CustomerEngagementAnalytics>>

    suspend fun getInternalCommunicationAnalytics(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<List<InternalCommunicationAnalytics>>

    suspend fun getVendorCommunicationAnalytics(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<List<VendorCommunicationAnalytics>>

    suspend fun getCampaignPerformance(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<List<CampaignPerformanceAnalytics>>

    suspend fun getAutomationAnalytics(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<List<CommunicationAutomationAnalytics>>

    suspend fun getRiskIndicators(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<List<CommunicationRiskIndicator>>

    suspend fun getAnomalies(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<List<CommunicationAnomaly>>

    suspend fun getGovernanceResult(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<CommunicationGovernanceResult>

    suspend fun comparePeriods(
        projectId: String,
        currentFilter: CommunicationAnalyticsFilter,
        previousFilter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<CommunicationPeriodComparison>

    suspend fun getForecast(
        filter: CommunicationAnalyticsFilter,
        actorRole: UserRole
    ): DomainResult<CommunicationForecastSummary>

    suspend fun createSnapshot(
        filter: CommunicationAnalyticsFilter,
        actorId: String,
        actorRole: UserRole,
        idempotencyKey: String
    ): DomainResult<CommunicationAnalyticsSnapshot>

    suspend fun getSnapshots(
        projectId: String,
        actorRole: UserRole
    ): DomainResult<List<CommunicationAnalyticsSnapshot>>
    
    fun observeSnapshots(projectId: String): Flow<List<CommunicationAnalyticsSnapshot>>

    suspend fun getActivityHistory(
        projectId: String,
        actorRole: UserRole
    ): DomainResult<List<CommunicationAnalyticsActivityEvent>>

    // Step 10: Snapshot Verification
    suspend fun verifySnapshot(
        snapshotId: String,
        projectId: String,
        actorUserId: String,
        actorRole: UserRole
    ): DomainResult<CommunicationSnapshotVerificationResult>

    // Step 10: Governance Actions
    suspend fun acknowledgeGovernanceAction(
        action: CommunicationGovernanceAction,
        actorRole: UserRole
    ): DomainResult<CommunicationGovernanceAction>

    // Step 10: Export Requests
    suspend fun requestExport(
        request: CommunicationExportRequest,
        actorRole: UserRole
    ): DomainResult<CommunicationExportPayload>

    suspend fun getExportRequests(
        projectId: String,
        actorRole: UserRole
    ): DomainResult<List<CommunicationExportRequest>>

    fun observeExportRequests(projectId: String): Flow<List<CommunicationExportRequest>>

    // Step 10: Audit Events
    suspend fun getAuditEvents(
        projectId: String,
        actorRole: UserRole
    ): DomainResult<List<CommunicationAuditEvent>>

    // Step 10: Operational Consumption Contract
    suspend fun getOperationalHealthProjection(
        projectId: String,
        actorRole: UserRole
    ): DomainResult<CommunicationOperationalHealthProjection>
}

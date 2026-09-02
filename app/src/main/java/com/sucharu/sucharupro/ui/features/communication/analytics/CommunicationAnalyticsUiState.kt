package com.sucharu.sucharupro.ui.features.communication.analytics

import com.sucharu.sucharupro.domain.model.common.DomainError
import com.sucharu.sucharupro.domain.model.communication.analytics.*
import java.time.Instant

data class CommunicationAnalyticsUiState(
    val projectId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val filter: CommunicationAnalyticsFilter = CommunicationAnalyticsFilter(
        projectId = "DEFAULT",
        fromDate = Instant.now().minus(java.time.Duration.ofDays(30)),
        toDate = Instant.now()
    ),
    val kpiSummary: CommunicationKpiSummary? = null,
    val channelAnalytics: List<CommunicationChannelAnalytics> = emptyList(),
    val typeAnalytics: List<CommunicationTypeAnalytics> = emptyList(),
    val customerEngagement: List<CustomerEngagementAnalytics> = emptyList(),
    val internalEngagement: List<InternalCommunicationAnalytics> = emptyList(),
    val vendorEngagement: List<VendorCommunicationAnalytics> = emptyList(),
    val campaignAnalytics: List<CampaignPerformanceAnalytics> = emptyList(),
    val automationAnalytics: List<CommunicationAutomationAnalytics> = emptyList(),
    val riskIndicators: List<CommunicationRiskIndicator> = emptyList(),
    val anomalies: List<CommunicationAnomaly> = emptyList(),
    val governanceResult: CommunicationGovernanceResult? = null,
    val periodComparison: CommunicationPeriodComparison? = null,
    val forecastSummary: CommunicationForecastSummary? = null,
    val isSnapshotGenerationLoading: Boolean = false,
    val snapshots: List<CommunicationAnalyticsSnapshot> = emptyList(),
    val activityHistory: List<CommunicationAnalyticsActivityEvent> = emptyList(),
    
    // Step 10: Governance, Export, Audit, Health
    val exportRequests: List<CommunicationExportRequest> = emptyList(),
    val auditEvents: List<CommunicationAuditEvent> = emptyList(),
    val operationalHealth: CommunicationOperationalHealthProjection? = null,
    
    val isVerifyingSnapshot: Boolean = false,
    val verificationResult: CommunicationSnapshotVerificationResult? = null,
    val isRequestingExport: Boolean = false,
    val isAcknowledgingGovernance: Boolean = false
)

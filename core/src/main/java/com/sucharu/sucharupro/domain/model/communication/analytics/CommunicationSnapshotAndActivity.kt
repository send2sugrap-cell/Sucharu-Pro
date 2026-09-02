package com.sucharu.sucharupro.domain.model.communication.analytics

import com.sucharu.sucharupro.domain.model.user.UserRole
import java.time.Instant

data class CommunicationAnalyticsSnapshot(
    val snapshotId: String,
    val projectId: String,
    val fromDate: Instant,
    val toDate: Instant,
    val generatedAt: Instant,
    
    val kpiSummary: CommunicationKpiSummary,
    val channelAnalytics: List<CommunicationChannelAnalytics>,
    val typeAnalytics: List<CommunicationTypeAnalytics>,
    
    val customerEngagement: List<CustomerEngagementAnalytics>,
    val internalEngagement: List<InternalCommunicationAnalytics>,
    val vendorEngagement: List<VendorCommunicationAnalytics>,
    
    val campaignAnalytics: List<CampaignPerformanceAnalytics>,
    val automationAnalytics: List<CommunicationAutomationAnalytics>,
    
    val riskIndicators: List<CommunicationRiskIndicator>,
    val anomalies: List<CommunicationAnomaly>,
    val governanceResult: CommunicationGovernanceResult,
    
    val sha256Hash: String // Immutable hash of the payload
)

data class CommunicationAnalyticsActivityEvent(
    val eventId: String,
    val projectId: String,
    val actorId: String,
    val actorRole: UserRole,
    val action: String, // e.g., "SNAPSHOT_GENERATED", "GOVERNANCE_REVIEWED"
    val timestamp: Instant,
    val targetType: String, // e.g., "SNAPSHOT", "RISK_INDICATOR"
    val targetId: String,
    val metadata: Map<String, String> = emptyMap()
)

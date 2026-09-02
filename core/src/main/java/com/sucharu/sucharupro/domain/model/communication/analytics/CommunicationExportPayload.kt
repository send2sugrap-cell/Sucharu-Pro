package com.sucharu.sucharupro.domain.model.communication.analytics

import java.time.Instant

/**
 * Structured export payload derived from analytics projections.
 * Contains only the data category requested via [CommunicationExportRequest.exportType].
 *
 * Boundary: READ/PROJECT only. Contains no direct references to mutable operational records.
 */
data class CommunicationExportPayload(
    val exportId: String,
    val projectId: String,
    val exportType: CommunicationExportType,
    val generatedAt: Instant = Instant.now(),
    val generatedBy: String,
    /** SHA-256 hash for tamper-evidence of this export payload. */
    val payloadHash: String,

    // Populated based on exportType:
    val kpiSummary: CommunicationKpiSummary? = null,
    val channelAnalytics: List<CommunicationChannelAnalytics>? = null,
    val riskIndicators: List<CommunicationRiskIndicator>? = null,
    val anomalies: List<CommunicationAnomaly>? = null,
    val governanceSummary: CommunicationGovernanceResult? = null,
    val forecastSummary: CommunicationForecastSummary? = null,
    val snapshotMetadata: List<SnapshotMetadataEntry>? = null,
    val auditSummary: List<CommunicationAuditEvent>? = null
)

/** Lightweight metadata entry for snapshot listings in exports. */
data class SnapshotMetadataEntry(
    val snapshotId: String,
    val generatedAt: Instant,
    val fromDate: Instant,
    val toDate: Instant,
    val sha256Hash: String,
    val governanceStatus: String
)

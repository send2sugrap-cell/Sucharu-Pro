package com.sucharu.sucharupro.domain.validation.communication.analytics

import com.sucharu.sucharupro.domain.model.communication.analytics.*
import java.security.MessageDigest
import java.time.Instant

/**
 * Pure, stateless export engine.
 *
 * Export Boundary:
 * - Derives export payloads exclusively from analytics projections.
 * - NEVER accesses or transforms operational source records directly.
 * - All data in the payload is a derived projection of already-computed analytics.
 */
object CommunicationExportEngine {

    /**
     * Generates a [CommunicationExportPayload] for [request] from [snapshot].
     * If [snapshot] is null, the payload sections dependent on snapshot data will be empty.
     */
    fun buildPayload(
        request: CommunicationExportRequest,
        snapshot: CommunicationAnalyticsSnapshot?,
        auditEvents: List<CommunicationAuditEvent> = emptyList()
    ): CommunicationExportPayload {
        val kpi = snapshot?.kpiSummary
        val channels = snapshot?.channelAnalytics
        val risks = snapshot?.riskIndicators
        val anomalies = snapshot?.anomalies
        val governance = snapshot?.governanceResult
        val forecast = null // snapshot does not hold forecast

        val snapshotMetadata = if (snapshot != null) {
            listOf(
                SnapshotMetadataEntry(
                    snapshotId = snapshot.snapshotId,
                    generatedAt = snapshot.generatedAt,
                    fromDate = snapshot.fromDate,
                    toDate = snapshot.toDate,
                    sha256Hash = snapshot.sha256Hash,
                    governanceStatus = snapshot.governanceResult.governanceStatus.name
                )
            )
        } else null

        val payload = CommunicationExportPayload(
            exportId = request.exportId,
            projectId = request.projectId,
            exportType = request.exportType,
            generatedAt = Instant.now(),
            generatedBy = request.requestedBy,
            payloadHash = "", // computed below
            kpiSummary = if (request.exportType in listOf(
                CommunicationExportType.KPI_SUMMARY, CommunicationExportType.FULL_REPORT
            )) kpi else null,
            channelAnalytics = if (request.exportType in listOf(
                CommunicationExportType.CHANNEL_PERFORMANCE, CommunicationExportType.FULL_REPORT
            )) channels else null,
            riskIndicators = if (request.exportType in listOf(
                CommunicationExportType.RISK_SUMMARY, CommunicationExportType.FULL_REPORT
            )) risks else null,
            anomalies = if (request.exportType in listOf(
                CommunicationExportType.ANOMALY_SUMMARY, CommunicationExportType.FULL_REPORT
            )) anomalies else null,
            governanceSummary = if (request.exportType in listOf(
                CommunicationExportType.GOVERNANCE_SUMMARY, CommunicationExportType.FULL_REPORT
            )) governance else null,
            forecastSummary = if (request.exportType in listOf(
                CommunicationExportType.FORECAST_SUMMARY, CommunicationExportType.FULL_REPORT
            )) forecast else null,
            snapshotMetadata = if (request.exportType in listOf(
                CommunicationExportType.SNAPSHOT_METADATA, CommunicationExportType.FULL_REPORT
            )) snapshotMetadata else null,
            auditSummary = if (request.exportType in listOf(
                CommunicationExportType.AUDIT_SUMMARY, CommunicationExportType.FULL_REPORT
            )) auditEvents else null
        )

        // Compute payload hash for tamper-evidence
        val rawData = "${payload.exportId}|${payload.projectId}|${payload.exportType}|${payload.generatedAt}|${payload.kpiSummary?.totalCommunications}|${payload.governanceSummary?.governanceStatus}"
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(rawData.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

        return payload.copy(payloadHash = hash)
    }

    /**
     * Derives operational health from the latest governance result, risk indicators, and anomalies.
     * Read-only projection — does not access source records.
     */
    fun deriveOperationalHealth(
        projectId: String,
        governance: CommunicationGovernanceResult?,
        risks: List<CommunicationRiskIndicator>,
        anomalies: List<CommunicationAnomaly>,
        forecast: CommunicationForecastSummary?,
        latestVerification: CommunicationSnapshotVerificationResult?
    ): CommunicationOperationalHealthProjection {
        val highRiskCount = risks.count { it.severity == RiskSeverity.HIGH || it.severity == RiskSeverity.CRITICAL }
        val criticalAnomalyCount = anomalies.count { it.severity == RiskSeverity.CRITICAL }

        val govStatus = governance?.communicationGovernanceStatus ?: CommunicationGovernanceStatus.AT_RISK

        val health = when {
            governance == null -> CommunicationHealth.UNKNOWN
            criticalAnomalyCount > 0 || governance.governanceStatus == GovernanceStatus.CRITICAL -> CommunicationHealth.CRITICAL
            highRiskCount > 2 || governance.governanceStatus == GovernanceStatus.DEGRADED -> CommunicationHealth.DEGRADED
            highRiskCount > 0 || governance.governanceStatus == GovernanceStatus.WARNING -> CommunicationHealth.DEGRADED
            else -> CommunicationHealth.GOOD
        }

        val integrityStatus = when (latestVerification?.status) {
            SnapshotVerificationStatus.VERIFIED -> SnapshotIntegrityStatus.VERIFIED
            SnapshotVerificationStatus.INTEGRITY_FAILURE -> SnapshotIntegrityStatus.INTEGRITY_FAILURE
            null -> SnapshotIntegrityStatus.NOT_VERIFIED
            else -> SnapshotIntegrityStatus.UNKNOWN
        }

        return CommunicationOperationalHealthProjection(
            projectId = projectId,
            communicationHealth = health,
            highRiskCount = highRiskCount,
            criticalAnomalyCount = criticalAnomalyCount,
            governanceStatus = govStatus,
            forecastConfidence = forecast?.confidenceIntervalPercentage ?: 0.0,
            snapshotIntegrityStatus = integrityStatus,
            latestSnapshotId = latestVerification?.snapshotId
        )
    }
}

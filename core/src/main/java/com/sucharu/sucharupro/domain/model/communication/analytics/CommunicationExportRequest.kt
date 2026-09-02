package com.sucharu.sucharupro.domain.model.communication.analytics

import java.time.Instant

enum class CommunicationExportType {
    KPI_SUMMARY,
    CHANNEL_PERFORMANCE,
    RISK_SUMMARY,
    ANOMALY_SUMMARY,
    GOVERNANCE_SUMMARY,
    FORECAST_SUMMARY,
    SNAPSHOT_METADATA,
    AUDIT_SUMMARY,
    FULL_REPORT
}

enum class CommunicationExportStatus {
    REQUESTED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Represents an analytics export request.
 * Exports are generated from analytics projections — they NEVER mutate operational records.
 *
 * Boundary: READ/PROJECT only. Idempotency enforced via [correlationId].
 */
data class CommunicationExportRequest(
    val exportId: String,
    val projectId: String,
    val requestedBy: String,
    val exportType: CommunicationExportType,
    /** Optional snapshot to export from; null means export from live analytics. */
    val snapshotReference: String? = null,
    val requestedAt: Instant = Instant.now(),
    val status: CommunicationExportStatus = CommunicationExportStatus.REQUESTED,
    val completedAt: Instant? = null,
    val failureReason: String? = null,
    /** Idempotency key — prevents duplicate export requests for the same intent. */
    val correlationId: String,
    /** SHA-256 hash of the exported payload for tamper-evidence. */
    val payloadHash: String? = null
)

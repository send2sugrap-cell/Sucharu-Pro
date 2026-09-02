package com.sucharu.sucharupro.domain.model.communication.analytics

import java.time.Instant

/**
 * Immutable read-only operational health projection.
 * Consumed by other modules that need a health summary without full analytics access.
 *
 * Boundary: READ ONLY. Future consumers must not receive mutable references to internal aggregates.
 */
data class CommunicationOperationalHealthProjection(
    val projectId: String,
    val generatedAt: Instant = Instant.now(),

    /** Overall health derived from governance status. */
    val communicationHealth: CommunicationHealth,

    /** Count of active HIGH or CRITICAL risk indicators. */
    val highRiskCount: Int,

    /** Count of CRITICAL severity anomalies in the current period. */
    val criticalAnomalyCount: Int,

    /** Latest governance evaluation status. */
    val governanceStatus: CommunicationGovernanceStatus,

    /** Confidence level of the latest forecast. */
    val forecastConfidence: Double,

    /** Integrity status of the most recent snapshot, or UNKNOWN if none exists. */
    val snapshotIntegrityStatus: SnapshotIntegrityStatus,

    /** ID of the most recent snapshot evaluated, or null if none. */
    val latestSnapshotId: String? = null
)

enum class CommunicationHealth {
    EXCELLENT, GOOD, DEGRADED, CRITICAL, UNKNOWN
}

enum class SnapshotIntegrityStatus {
    VERIFIED, INTEGRITY_FAILURE, NOT_VERIFIED, UNKNOWN
}

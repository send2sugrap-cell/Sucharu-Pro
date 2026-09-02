package com.sucharu.sucharupro.domain.model.communication.analytics

import java.time.Instant

enum class RiskSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class CommunicationRiskType {
    HIGH_FAILURE_RATE,
    LOW_DELIVERY_RATE,
    LOW_READ_RATE,
    LOW_ACKNOWLEDGEMENT_RATE,
    AUTOMATION_FAILURE_SPIKE,
    CAMPAIGN_UNDERPERFORMANCE,
    CHANNEL_DEGRADATION,
    UNREAD_BACKLOG,
    COMMUNICATION_BOTTLENECK,
    EXCESSIVE_BROADCAST_VOLUME,
    DUPLICATE_TRIGGER_SPIKE
}

data class CommunicationRiskIndicator(
    val riskType: CommunicationRiskType,
    val severity: RiskSeverity,
    val observedValue: Double,
    val threshold: Double,
    val explanation: String,
    val detectedAt: Instant = Instant.now()
)

data class CommunicationAnomaly(
    val anomalyType: String,
    val baselineValue: Double,
    val observedValue: Double,
    val deviationPercentage: Double,
    val severity: RiskSeverity,
    val explanation: String,
    val detectedAt: Instant = Instant.now()
)

/**
 * Internal engine-level governance status (used by [CommunicationGovernanceEngine]).
 */
enum class GovernanceStatus {
    HEALTHY, WARNING, DEGRADED, CRITICAL
}

/**
 * UI-facing canonical governance status for display and export.
 * Maps cleanly to [GovernanceStatus] via [CommunicationGovernanceResult.communicationGovernanceStatus].
 */
enum class CommunicationGovernanceStatus {
    COMPLIANT, AT_RISK, NON_COMPLIANT
}

data class CommunicationGovernanceResult(
    val projectId: String,
    val evaluatedAt: Instant = Instant.now(),
    val passedControls: List<String>,
    val warningControls: List<String>,
    val failedControls: List<String>,
    val riskCount: Int,
    val anomalyCount: Int,
    val governanceStatus: GovernanceStatus,
    val governanceScore: Int = when {
        // 0-100 score derived from the status
        else -> 100 - (failedControls.size * 30).coerceAtMost(60) - (warningControls.size * 10).coerceAtMost(30)
    }
) {
    /** UI-facing status derived from the internal [governanceStatus]. */
    val communicationGovernanceStatus: CommunicationGovernanceStatus
        get() = when (governanceStatus) {
            GovernanceStatus.HEALTHY -> CommunicationGovernanceStatus.COMPLIANT
            GovernanceStatus.WARNING -> CommunicationGovernanceStatus.AT_RISK
            GovernanceStatus.DEGRADED, GovernanceStatus.CRITICAL -> CommunicationGovernanceStatus.NON_COMPLIANT
        }
}


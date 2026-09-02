package com.sucharu.sucharupro.domain.model.communication.analytics

import java.time.Instant

/**
 * Represents a deliberate governance action taken by an authorized actor.
 * Governance actions are audit-oriented and MUST NOT mutate operational communication records.
 *
 * Boundary: APPEND-ONLY. Never update or delete a committed governance action.
 */
enum class CommunicationGovernanceActionType {
    ACKNOWLEDGE_ANOMALY,
    ACKNOWLEDGE_RISK,
    REVIEW_GOVERNANCE,
    VERIFY_SNAPSHOT,
    EXPORT_ANALYTICS,
    DISMISS_ALERT
}

data class CommunicationGovernanceAction(
    val actionId: String,
    val projectId: String,
    val actorUserId: String,
    val actionType: CommunicationGovernanceActionType,
    /** ID of the affected entity (snapshotId, riskType, anomalyType, etc.) */
    val targetId: String,
    val targetType: String, // e.g. "SNAPSHOT", "RISK_INDICATOR", "ANOMALY"
    val timestamp: Instant = Instant.now(),
    val previousState: String? = null,
    val resultingState: String? = null,
    val reason: String? = null,
    /** Correlates this action to an export or verification request. */
    val correlationId: String? = null
)

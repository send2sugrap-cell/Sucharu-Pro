package com.sucharu.sucharupro.domain.model.qc.governance

/**
 * Event types for immutable append-only governance activity audit trail (Module 06 Step 10).
 */
enum class QcGovernanceActivityType(
    val defaultLabel: String
) {
    GOVERNANCE_CREATED("Governance Policy Initialized"),
    KPI_TARGET_CREATED("KPI Target Created"),
    KPI_TARGET_UPDATED("KPI Target Updated"),
    THRESHOLD_CREATED("Threshold Configured"),
    THRESHOLD_BREACHED("Threshold Breached"),
    ALERT_CREATED("Quality Alert Detected"),
    ALERT_ACKNOWLEDGED("Quality Alert Acknowledged"),
    ALERT_ESCALATED("Quality Alert Escalated"),
    ALERT_RESOLVED("Quality Alert Resolved"),
    ALERT_DISMISSED("Quality Alert Dismissed"),
    REVIEW_CREATED("Quality Review Created"),
    REVIEW_STARTED("Quality Review Started"),
    REVIEW_COMPLETED("Quality Review Completed"),
    REVIEW_CANCELLED("Quality Review Cancelled"),
    IMPROVEMENT_ACTION_CREATED("Improvement Action Proposed"),
    IMPROVEMENT_ACTION_APPROVED("Improvement Action Approved"),
    IMPROVEMENT_ACTION_ASSIGNED("Improvement Action Assigned"),
    IMPROVEMENT_ACTION_STARTED("Improvement Action Started"),
    IMPROVEMENT_ACTION_COMPLETED("Improvement Action Completed"),
    IMPROVEMENT_ACTION_VERIFIED("Improvement Action Verified"),
    IMPROVEMENT_ACTION_REJECTED("Improvement Action Rejected"),
    IMPROVEMENT_ACTION_CANCELLED("Improvement Action Cancelled"),
    IMPROVEMENT_EFFECTIVENESS_RECORDED("Improvement Effectiveness Evaluated"),
    SNAPSHOT_CREATED("Governance Snapshot Created");

    companion object {
        fun fromString(value: String?): QcGovernanceActivityType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}

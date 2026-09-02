package com.sucharu.sucharupro.domain.model.finance

/**
 * Immutable audit event for finance analytics and governance operations (Module 09 Step 10).
 */
data class FinanceGovernanceActivityEvent(
    val eventId: String,
    val projectId: String,
    val eventType: FinanceGovernanceEventType,
    val actorId: String,
    val description: String,
    val referenceId: String? = null,
    val metadata: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(description.isNotBlank()) { "Description cannot be blank." }
    }
}

enum class FinanceGovernanceEventType(val defaultLabel: String) {
    ANALYTICS_VIEWED("Analytics Viewed"),
    ANALYTICS_GENERATED("Analytics Generated"),
    ANALYTICS_SNAPSHOT_CREATED("Analytics Snapshot Created"),
    GOVERNANCE_CHECK_EXECUTED("Governance Control Check Executed"),
    RISK_DETECTED("Financial Risk Detected"),
    ANOMALY_DETECTED("Financial Anomaly Detected"),
    CONTROL_EXCEPTION_DETECTED("Governance Control Exception Detected"),
    FORECAST_GENERATED("Forecast Generated"),
    PERIOD_COMPARISON_GENERATED("Period Comparison Generated"),
    ANALYTICS_EXPORTED("Analytics Exported")
}

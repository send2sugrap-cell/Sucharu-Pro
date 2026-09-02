package com.sucharu.sucharupro.domain.model.returns

/**
 * Deterministic policy and governance anomaly exception categories (Module 11 Step 06).
 */
enum class ReturnExceptionType(val defaultLabel: String, val defaultSeverity: String) {
    AGING_UNINSPECTED("Aging Uninspected Return", "HIGH"),
    AGING_UNRECEIVED("Aging Unreceived Return", "MEDIUM"),
    UNSETTLED_PROCESSED("Processed Unsettled Return", "HIGH"),
    HIGH_VALUE_RETURN("High-Value Return Alert", "CRITICAL"),
    HIGH_RETURN_RATE("High Return-Rate Alert", "CRITICAL"),
    SLA_BREACH("SLA Turnaround Breach", "HIGH"),
    OTHER_POLICY_EXCEPTION("Policy Governance Exception", "MEDIUM");

    val displayName: String
        get() = defaultLabel
}

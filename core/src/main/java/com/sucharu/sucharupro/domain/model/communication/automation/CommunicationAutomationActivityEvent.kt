package com.sucharu.sucharupro.domain.model.communication.automation

/**
 * Immutable append-only audit trail event for Automation operations (Module 10 Step 08).
 */
data class CommunicationAutomationActivityEvent(
    val eventId: String,
    val projectId: String,
    val executionId: String? = null,
    val ruleId: String? = null,
    val eventType: AutomationActivityEventType,
    val actorUserId: String,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(actorUserId.isNotBlank()) { "Actor User ID cannot be blank." }
        require(summary.isNotBlank()) { "Summary cannot be blank." }
    }
}

enum class AutomationActivityEventType(val defaultLabel: String) {
    RULE_CREATED("Automation Rule Created"),
    RULE_UPDATED("Automation Rule Updated"),
    RULE_ENABLED("Automation Rule Enabled"),
    RULE_DISABLED("Automation Rule Disabled"),
    TRIGGER_RECEIVED("Event Trigger Received"),
    RULE_MATCHED("Rule Condition Matched"),
    RULE_NOT_MATCHED("No Rules Matched"),
    COMMUNICATION_GENERATED("Communication Generated"),
    COMMUNICATION_SCHEDULED("Communication Scheduled"),
    COMMUNICATION_SUPPRESSED("Communication Suppressed"),
    DUPLICATE_BLOCKED("Duplicate Trigger Blocked"),
    PREFERENCE_BLOCKED("User Preference Blocked"),
    ESCALATION_TRIGGERED("Escalation Triggered"),
    DISPATCH_REQUESTED("Dispatch Requested"),
    EXECUTION_COMPLETED("Execution Completed"),
    EXECUTION_FAILED("Execution Failed")
}

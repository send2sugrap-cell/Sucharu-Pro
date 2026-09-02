package com.sucharu.sucharupro.domain.model.communication.automation

import com.sucharu.sucharupro.domain.model.notification.NotificationChannel

/**
 * Execution tracking record for an automation trigger cycle (Module 10 Step 08).
 */
data class CommunicationAutomationExecution(
    val executionId: String,
    val executionNo: String,
    val projectId: String,
    val triggerId: String,
    val ruleId: String?,
    val status: AutomationExecutionStatus = AutomationExecutionStatus.RECEIVED,
    val decision: CommunicationAutomationDecision,
    val recipientUserId: String? = null,
    val channel: NotificationChannel? = null,
    val notificationId: String? = null,
    val evaluatedAt: Long = System.currentTimeMillis(),
    val dispatchedAt: Long? = null,
    val completedAt: Long? = null,
    val failureReason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(executionId.isNotBlank()) { "Execution ID cannot be blank." }
        require(executionNo.isNotBlank()) { "Execution Number cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(triggerId.isNotBlank()) { "Trigger ID cannot be blank." }
    }
}

enum class AutomationExecutionStatus(val defaultLabel: String, val isTerminal: Boolean = false) {
    RECEIVED("Trigger Received", false),
    EVALUATING("Evaluating Rules", false),
    MATCHED("Rule Matched", false),
    QUEUED("Queued", false),
    SCHEDULED("Scheduled", false),
    DISPATCHING("Dispatching", false),
    DISPATCHED("Dispatched", false),
    COMPLETED("Completed", true),
    SUPPRESSED("Suppressed", true),
    FAILED("Failed", true),
    CANCELLED("Cancelled", true)
}

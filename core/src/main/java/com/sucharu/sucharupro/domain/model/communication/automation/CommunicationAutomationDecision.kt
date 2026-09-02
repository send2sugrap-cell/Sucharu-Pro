package com.sucharu.sucharupro.domain.model.communication.automation

import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority

/**
 * Deterministic decision result from Rule Engine evaluation (Module 10 Step 08).
 */
data class CommunicationAutomationDecision(
    val decisionType: AutomationDecisionType,
    val matchedRuleId: String? = null,
    val recipientUserId: String? = null,
    val selectedChannel: NotificationChannel? = null,
    val calculatedPriority: NotificationPriority = NotificationPriority.NORMAL,
    val renderedTitle: String? = null,
    val renderedMessage: String? = null,
    val scheduledAt: Long? = null,
    val suppressionReason: String? = null
)

enum class AutomationDecisionType(val defaultLabel: String) {
    SEND("Approved for Immediate Dispatch"),
    SCHEDULE("Scheduled for Delayed Dispatch"),
    SUPPRESS("Suppressed by Cooldown / Anti-Spam"),
    SKIP("Skipped"),
    ESCALATE("Escalated to Management"),
    NO_MATCH("No Rule Matched"),
    DUPLICATE_BLOCKED("Duplicate Trigger Blocked"),
    PREFERENCE_BLOCKED("Blocked by User Preference"),
    INVALID_RECIPIENT("Invalid / Ineligible Recipient")
}

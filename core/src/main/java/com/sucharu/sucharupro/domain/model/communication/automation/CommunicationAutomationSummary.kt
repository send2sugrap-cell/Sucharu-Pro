package com.sucharu.sucharupro.domain.model.communication.automation

/**
 * Project-scoped summary analytics for communication automation (Module 10 Step 08).
 */
data class CommunicationAutomationSummary(
    val projectId: String,
    val totalRules: Int = 0,
    val activeRules: Int = 0,
    val totalTriggers: Int = 0,
    val matchedRules: Int = 0,
    val notificationsGenerated: Int = 0,
    val notificationsSuppressed: Int = 0,
    val scheduledCount: Int = 0,
    val failedCount: Int = 0,
    val duplicateBlockedCount: Int = 0,
    val preferenceBlockedCount: Int = 0,
    val escalatedCount: Int = 0
)

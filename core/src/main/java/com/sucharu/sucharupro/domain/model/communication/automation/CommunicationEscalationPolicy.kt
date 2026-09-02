package com.sucharu.sucharupro.domain.model.communication.automation

import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Escalation policy for unattended or critical business issues (Module 10 Step 08).
 */
data class CommunicationEscalationPolicy(
    val enabled: Boolean = false,
    val timeoutMs: Long = 86400000L, // 24 hours
    val escalationRole: UserRole = UserRole.MANAGER,
    val escalationUserId: String? = null,
    val escalationPriority: NotificationPriority = NotificationPriority.URGENT,
    val escalationMessageTemplate: String = "Escalated: Immediate attention required."
)

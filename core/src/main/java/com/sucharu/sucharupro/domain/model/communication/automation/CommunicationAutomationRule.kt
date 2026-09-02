package com.sucharu.sucharupro.domain.model.communication.automation

import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignAudienceType
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.notification.NotificationType

/**
 * Core aggregate root representing an Automation Rule in Sucharu Pro ERP (Module 10 Step 08).
 *
 * Implements deterministic matching, type-safe condition checks, and policy controls.
 */
data class CommunicationAutomationRule(
    val ruleId: String,
    val ruleNo: String,
    val projectId: String,
    val name: String,
    val description: String = "",
    val eventType: CommunicationAutomationEventType,
    val conditions: List<CommunicationAutomationCondition> = emptyList(),
    val audienceType: CampaignAudienceType = CampaignAudienceType.ROLE,
    val notificationType: NotificationType = NotificationType.GENERAL,
    val defaultChannel: NotificationChannel = NotificationChannel.IN_APP,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val titleTemplate: String,
    val messageTemplate: String,
    val enabled: Boolean = true,
    val schedulePolicy: CommunicationSchedulePolicy = CommunicationSchedulePolicy(),
    val cooldownPolicy: CommunicationCooldownPolicy = CommunicationCooldownPolicy(),
    val escalationPolicy: CommunicationEscalationPolicy = CommunicationEscalationPolicy(),
    val createdBy: String,
    val updatedBy: String? = null,
    val approvedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(ruleId.isNotBlank()) { "Rule ID cannot be blank." }
        require(ruleNo.isNotBlank()) { "Rule Number cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(name.isNotBlank()) { "Rule name cannot be blank." }
        require(titleTemplate.isNotBlank()) { "Title template cannot be blank." }
        require(messageTemplate.isNotBlank()) { "Message template cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation timestamp." }

        // Metadata security sanitization
        val forbiddenKeys = listOf("password", "token", "secret", "cvv", "card_number", "pin", "api_key", "bearer")
        for (key in metadata.keys) {
            val lower = key.lowercase()
            require(forbiddenKeys.none { lower.contains(it) }) {
                "Sensitive key '$key' is prohibited in automation rule metadata."
            }
        }
    }
}

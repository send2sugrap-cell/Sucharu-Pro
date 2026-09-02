package com.sucharu.sucharupro.domain.model.communication.automation

/**
 * Anti-spam and notification cooldown configuration (Module 10 Step 08).
 */
data class CommunicationCooldownPolicy(
    val enabled: Boolean = false,
    val cooldownPeriodMs: Long = 3600000L, // 1 hour default
    val maxOccurrencesPerWindow: Int = 1,
    val scope: CooldownScope = CooldownScope.SAME_RECIPIENT_SAME_EVENT
)

enum class CooldownScope(val defaultLabel: String) {
    SAME_RECIPIENT_SAME_EVENT("Same Recipient & Same Event"),
    SAME_RECIPIENT_ANY_EVENT("Same Recipient Any Event"),
    PROJECT_WIDE("Project Wide")
}

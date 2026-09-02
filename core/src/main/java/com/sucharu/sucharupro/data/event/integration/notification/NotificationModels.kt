package com.sucharu.sucharupro.data.event.integration.notification

import com.sucharu.sucharupro.domain.event.boundary.NotificationChannel
import com.sucharu.sucharupro.domain.event.consumer.EventFailureClassification

/**
 * Resolved notification recipient details.
 */
data class NotificationRecipient(
    val recipientId: String,
    val projectId: String,
    val displayName: String,
    val email: String? = null,
    val phone: String? = null,
    val pushToken: String? = null,
    val preferredLanguage: String = "bn-BD",
    val isPhoneVerified: Boolean = true,
    val isEmailVerified: Boolean = true
)

/**
 * User channel preferences and delivery constraints.
 */
data class NotificationPreferences(
    val recipientId: String,
    val projectId: String,
    val enabledChannels: Set<NotificationChannel> = setOf(
        NotificationChannel.IN_APP,
        NotificationChannel.EMAIL,
        NotificationChannel.SMS,
        NotificationChannel.PUSH
    ),
    val quietHoursStartHour: Int? = null, // e.g. 22 (10 PM)
    val quietHoursEndHour: Int? = null,   // e.g. 7 (7 AM)
    val optOutMarketing: Boolean = false
) {
    /**
     * Checks if a channel is permitted given recipient preferences and current hour.
     */
    fun isChannelAllowed(channel: NotificationChannel, currentHour: Int = 12): Boolean {
        if (!enabledChannels.contains(channel)) return false

        // In-app is always allowed even during quiet hours
        if (channel == NotificationChannel.IN_APP) return true

        // Check quiet hours for SMS/Push
        if (quietHoursStartHour != null && quietHoursEndHour != null) {
            val inQuietHours = if (quietHoursStartHour <= quietHoursEndHour) {
                currentHour in quietHoursStartHour..quietHoursEndHour
            } else {
                currentHour >= quietHoursStartHour || currentHour < quietHoursEndHour
            }
            if (inQuietHours && (channel == NotificationChannel.SMS || channel == NotificationChannel.PUSH)) {
                return false
            }
        }
        return true
    }
}

/**
 * Result of executing a delivery via an external notification provider.
 */
data class NotificationDeliveryResult(
    val channel: NotificationChannel,
    val isSuccess: Boolean,
    val providerRef: String? = null,
    val errorMessage: String? = null,
    val failureClassification: EventFailureClassification? = null
)

/**
 * Interface for channel-specific notification providers (e.g. In-App, Push, Email, SMS).
 */
interface NotificationProvider {
    val channel: NotificationChannel
    suspend fun deliver(
        recipient: NotificationRecipient,
        title: String,
        body: String,
        metadata: Map<String, String>,
        idempotencyKey: String
    ): NotificationDeliveryResult
}

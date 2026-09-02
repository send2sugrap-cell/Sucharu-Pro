package com.sucharu.sucharupro.domain.repository.notification

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.notification.Notification
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationDeliveryAttempt

/**
 * Provider-neutral interface for external or in-app notification delivery (Module 10 Step 01).
 *
 * Concrete adapters (e.g. Firebase, SMTP, Twilio, SMS Gateway, WhatsApp) implement this interface.
 */
interface NotificationDeliveryProvider {
    val providerName: String

    fun supportsChannel(channel: NotificationChannel): Boolean

    suspend fun deliver(
        notification: Notification,
        attemptNumber: Int
    ): DomainResult<NotificationDeliveryAttempt>
}

package com.sucharu.sucharupro.data.notification

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.notification.Notification
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationDeliveryAttempt
import com.sucharu.sucharupro.domain.model.notification.NotificationStatus
import com.sucharu.sucharupro.domain.repository.notification.NotificationDeliveryProvider
import java.util.UUID

/**
 * In-memory delivery provider for local testing and in-app notifications (Module 10 Step 01).
 */
class InMemoryNotificationDeliveryProvider(
    override val providerName: String = "LOCAL_IN_MEMORY_PROVIDER",
    private val shouldSimulateFailure: Boolean = false
) : NotificationDeliveryProvider {

    override fun supportsChannel(channel: NotificationChannel): Boolean {
        // Supports all channels in local simulated mode
        return true
    }

    override suspend fun deliver(
        notification: Notification,
        attemptNumber: Int
    ): DomainResult<NotificationDeliveryAttempt> {
        val now = System.currentTimeMillis()
        val attemptId = UUID.randomUUID().toString()

        if (shouldSimulateFailure) {
            val failedAttempt = NotificationDeliveryAttempt(
                attemptId = attemptId,
                projectId = notification.projectId,
                notificationId = notification.notificationId,
                channel = notification.channel,
                attemptNumber = attemptNumber,
                status = NotificationStatus.FAILED,
                provider = providerName,
                requestedAt = now,
                startedAt = now,
                completedAt = now + 50L,
                failureCode = "PROVIDER_UNAVAILABLE",
                failureMessage = "Simulated delivery failure for provider $providerName",
                createdAt = now
            )
            return DomainResult.Success(failedAttempt)
        }

        val successAttempt = NotificationDeliveryAttempt(
            attemptId = attemptId,
            projectId = notification.projectId,
            notificationId = notification.notificationId,
            channel = notification.channel,
            attemptNumber = attemptNumber,
            status = NotificationStatus.DELIVERED,
            provider = providerName,
            providerMessageId = "MSG-${UUID.randomUUID().toString().take(12)}",
            requestedAt = now,
            startedAt = now,
            completedAt = now + 25L,
            createdAt = now
        )

        return DomainResult.Success(successAttempt)
    }
}

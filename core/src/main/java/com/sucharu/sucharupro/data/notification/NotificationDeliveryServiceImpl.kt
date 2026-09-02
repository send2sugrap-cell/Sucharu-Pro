package com.sucharu.sucharupro.data.notification

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.notification.Notification
import com.sucharu.sucharupro.domain.model.notification.NotificationDeliveryAttempt
import com.sucharu.sucharupro.domain.model.notification.NotificationStatus
import com.sucharu.sucharupro.domain.repository.notification.NotificationDeliveryProvider
import com.sucharu.sucharupro.domain.repository.notification.NotificationDeliveryService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production-ready delivery service orchestrating provider selection and attempt recording (Module 10 Step 01).
 */
class NotificationDeliveryServiceImpl(
    initialProviders: List<NotificationDeliveryProvider> = listOf(InMemoryNotificationDeliveryProvider())
) : NotificationDeliveryService {

    private val mutex = Mutex()
    private val providers = mutableListOf<NotificationDeliveryProvider>().apply {
        addAll(initialProviders)
    }

    override fun registerProvider(provider: NotificationDeliveryProvider) {
        providers.add(0, provider) // Prepend for higher priority resolution
    }

    override suspend fun processDelivery(
        notification: Notification,
        attemptNumber: Int
    ): DomainResult<NotificationDeliveryAttempt> = mutex.withLock {
        val provider = providers.firstOrNull { it.supportsChannel(notification.channel) }
        if (provider == null) {
            val now = System.currentTimeMillis()
            val failedAttempt = NotificationDeliveryAttempt(
                attemptId = UUID.randomUUID().toString(),
                projectId = notification.projectId,
                notificationId = notification.notificationId,
                channel = notification.channel,
                attemptNumber = attemptNumber,
                status = NotificationStatus.FAILED,
                provider = "NONE",
                requestedAt = now,
                completedAt = now,
                failureCode = "NO_SUPPORTED_PROVIDER",
                failureMessage = "No delivery provider registered for channel '${notification.channel.defaultLabel}'."
            )
            return@withLock DomainResult.Success(failedAttempt)
        }

        provider.deliver(notification, attemptNumber)
    }
}

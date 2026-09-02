package com.sucharu.sucharupro.domain.repository.notification

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.notification.Notification
import com.sucharu.sucharupro.domain.model.notification.NotificationDeliveryAttempt

/**
 * Service orchestrating provider resolution, delivery attempts, and retry execution (Module 10 Step 01).
 */
interface NotificationDeliveryService {

    fun registerProvider(provider: NotificationDeliveryProvider)

    suspend fun processDelivery(
        notification: Notification,
        attemptNumber: Int
    ): DomainResult<NotificationDeliveryAttempt>
}

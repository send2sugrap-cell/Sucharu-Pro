package com.sucharu.sucharupro.domain.validation.notification

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.notification.NotificationStatus

/**
 * State machine validator for notification lifecycle transitions (Module 10 Step 01).
 */
object NotificationLifecycleValidator {

    private val allowedTransitions = mapOf(
        NotificationStatus.DRAFT to setOf(NotificationStatus.QUEUED, NotificationStatus.CANCELLED),
        NotificationStatus.QUEUED to setOf(NotificationStatus.PROCESSING, NotificationStatus.CANCELLED),
        NotificationStatus.PROCESSING to setOf(NotificationStatus.SENT, NotificationStatus.FAILED, NotificationStatus.DELIVERED),
        NotificationStatus.SENT to setOf(NotificationStatus.DELIVERED, NotificationStatus.FAILED, NotificationStatus.READ),
        NotificationStatus.DELIVERED to setOf(NotificationStatus.READ),
        NotificationStatus.FAILED to setOf(NotificationStatus.QUEUED, NotificationStatus.CANCELLED),
        NotificationStatus.READ to emptySet(),
        NotificationStatus.CANCELLED to emptySet()
    )

    fun validateTransition(currentStatus: NotificationStatus, targetStatus: NotificationStatus): DomainResult<Unit> {
        if (currentStatus == targetStatus) {
            return DomainResult.Success(Unit)
        }
        val allowed = allowedTransitions[currentStatus] ?: emptySet()
        return if (allowed.contains(targetStatus)) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(
                message = "Invalid notification lifecycle transition from '${currentStatus.defaultLabel}' to '${targetStatus.defaultLabel}'."
            )
        }
    }
}

package com.sucharu.sucharupro.domain.validation.notification

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.notification.Notification
import com.sucharu.sucharupro.domain.model.notification.NotificationTrigger

/**
 * Domain structural, semantic, and security validator for notifications (Module 10 Step 01).
 */
object NotificationValidator {

    private val forbiddenMetadataKeywords = listOf(
        "password", "token", "secret", "cvv", "card_number", "pin", "api_key", "bearer", "private_key"
    )

    fun validateNotification(notification: Notification): DomainResult<Unit> {
        if (notification.notificationId.isBlank()) {
            return DomainResult.Error(message = "Notification ID cannot be blank.")
        }
        if (notification.notificationNo.isBlank()) {
            return DomainResult.Error(message = "Notification Number cannot be blank.")
        }
        if (notification.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (notification.recipientUserId.isBlank()) {
            return DomainResult.Error(message = "Recipient User ID cannot be blank.")
        }
        if (notification.title.isBlank()) {
            return DomainResult.Error(message = "Notification title cannot be blank.")
        }
        if (notification.message.isBlank()) {
            return DomainResult.Error(message = "Notification message cannot be blank.")
        }
        if (notification.createdBy.isBlank()) {
            return DomainResult.Error(message = "Created By identifier cannot be blank.")
        }
        if (notification.createdAt <= 0) {
            return DomainResult.Error(message = "Creation timestamp must be positive.")
        }
        if (notification.updatedAt < notification.createdAt) {
            return DomainResult.Error(message = "Updated timestamp cannot precede creation timestamp.")
        }

        // Validate metadata keys
        for ((key, value) in notification.metadata) {
            val lowerKey = key.lowercase()
            val lowerVal = value.lowercase()
            for (forbidden in forbiddenMetadataKeywords) {
                if (lowerKey.contains(forbidden) || lowerVal.contains(forbidden)) {
                    return DomainResult.Error(
                        message = "Sensitive key or data '$forbidden' is strictly prohibited in notification metadata."
                    )
                }
            }
        }

        return DomainResult.Success(Unit)
    }

    fun validateTrigger(trigger: NotificationTrigger): DomainResult<Unit> {
        if (trigger.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (trigger.recipientUserId.isBlank()) {
            return DomainResult.Error(message = "Recipient User ID cannot be blank.")
        }
        if (trigger.title.isBlank()) {
            return DomainResult.Error(message = "Title cannot be blank.")
        }
        if (trigger.message.isBlank()) {
            return DomainResult.Error(message = "Message cannot be blank.")
        }
        if (trigger.actorUserId.isBlank()) {
            return DomainResult.Error(message = "Actor User ID cannot be blank.")
        }

        for ((key, value) in trigger.metadata) {
            val lowerKey = key.lowercase()
            val lowerVal = value.lowercase()
            for (forbidden in forbiddenMetadataKeywords) {
                if (lowerKey.contains(forbidden) || lowerVal.contains(forbidden)) {
                    return DomainResult.Error(
                        message = "Sensitive data '$forbidden' is strictly prohibited in notification trigger metadata."
                    )
                }
            }
        }

        return DomainResult.Success(Unit)
    }
}

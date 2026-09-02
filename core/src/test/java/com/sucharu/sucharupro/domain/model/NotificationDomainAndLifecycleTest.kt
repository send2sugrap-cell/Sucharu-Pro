package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.notification.*
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.validation.notification.NotificationAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.notification.NotificationLifecycleValidator
import com.sucharu.sucharupro.domain.validation.notification.NotificationValidator
import org.junit.Assert.*
import org.junit.Test

class NotificationDomainAndLifecycleTest {

    private val validNotification = Notification(
        notificationId = "notif-1",
        notificationNo = "NTF-2026-00001",
        projectId = "PRJ-01",
        recipientUserId = "USER-100",
        notificationType = NotificationType.ORDER_CREATED,
        channel = NotificationChannel.IN_APP,
        priority = NotificationPriority.NORMAL,
        status = NotificationStatus.DRAFT,
        title = "Order #123 Placed",
        message = "New order #123 has been received.",
        createdBy = "ACTOR-01"
    )

    @Test
    fun `Notification validation passes for complete valid notification`() {
        val result = NotificationValidator.validateNotification(validNotification)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `Notification constructor rejects missing required fields`() {
        assertThrows(IllegalArgumentException::class.java) {
            validNotification.copy(title = "   ")
        }
    }

    @Test
    fun `Notification constructor rejects sensitive private secrets in metadata`() {
        assertThrows(IllegalArgumentException::class.java) {
            validNotification.copy(
                metadata = mapOf("user_token" to "secret_bearer_token_123")
            )
        }
    }

    @Test
    fun `NotificationTrigger validator rejects invalid fields`() {
        val invalidTrigger = NotificationTrigger(
            projectId = "PRJ-01",
            notificationType = NotificationType.GENERAL,
            recipientUserId = "USER-101",
            title = "Test",
            message = "Test message",
            actorUserId = "ACTOR-01",
            metadata = mapOf("secret_key" to "unauthorized_val")
        )
        val res = NotificationValidator.validateTrigger(invalidTrigger)
        assertTrue(res is DomainResult.Error)
    }

    @Test
    fun `NotificationLifecycleValidator allows canonical lifecycle progression`() {
        // DRAFT -> QUEUED -> PROCESSING -> DELIVERED -> READ
        assertTrue(NotificationLifecycleValidator.validateTransition(NotificationStatus.DRAFT, NotificationStatus.QUEUED) is DomainResult.Success)
        assertTrue(NotificationLifecycleValidator.validateTransition(NotificationStatus.QUEUED, NotificationStatus.PROCESSING) is DomainResult.Success)
        assertTrue(NotificationLifecycleValidator.validateTransition(NotificationStatus.PROCESSING, NotificationStatus.DELIVERED) is DomainResult.Success)
        assertTrue(NotificationLifecycleValidator.validateTransition(NotificationStatus.DELIVERED, NotificationStatus.READ) is DomainResult.Success)
    }

    @Test
    fun `NotificationLifecycleValidator blocks illegal transition from READ or CANCELLED`() {
        // Terminal states cannot transition to QUEUED or PROCESSING
        assertTrue(NotificationLifecycleValidator.validateTransition(NotificationStatus.READ, NotificationStatus.QUEUED) is DomainResult.Error)
        assertTrue(NotificationLifecycleValidator.validateTransition(NotificationStatus.CANCELLED, NotificationStatus.PROCESSING) is DomainResult.Error)
        assertTrue(NotificationLifecycleValidator.validateTransition(NotificationStatus.DRAFT, NotificationStatus.DELIVERED) is DomainResult.Error)
    }

    @Test
    fun `NotificationAuthorizationValidator enforces recipient containment for Customer and Vendor`() {
        // Customer can only view own notification
        val custAllowed = NotificationAuthorizationValidator.validateNotificationView(
            notification = validNotification.copy(recipientUserId = "CUST-01"),
            requestProjectId = "PRJ-01",
            actorId = "CUST-01",
            callerRole = UserRole.CUSTOMER
        )
        assertTrue(custAllowed is DomainResult.Success)

        val custBlocked = NotificationAuthorizationValidator.validateNotificationView(
            notification = validNotification.copy(recipientUserId = "CUST-01"),
            requestProjectId = "PRJ-01",
            actorId = "CUST-02",
            callerRole = UserRole.CUSTOMER
        )
        assertTrue(custBlocked is DomainResult.Error)
    }

    @Test
    fun `NotificationAuthorizationValidator strictly blocks cross-project access`() {
        val crossProject = NotificationAuthorizationValidator.validateNotificationView(
            notification = validNotification.copy(projectId = "PRJ-AAA"),
            requestProjectId = "PRJ-BBB",
            actorId = "ADMIN-01",
            callerRole = UserRole.ADMIN
        )
        assertTrue(crossProject is DomainResult.Error)
    }

    @Test
    fun `NotificationPreference disallows disabling mandatory in-app security alerts`() {
        assertThrows(IllegalArgumentException::class.java) {
            NotificationPreference(
                preferenceId = "pref-1",
                projectId = "PRJ-01",
                userId = "USER-01",
                notificationType = NotificationType.SECURITY_ALERT,
                channel = NotificationChannel.IN_APP,
                enabled = false // Not allowed for mandatory security alert
            )
        }
    }
}

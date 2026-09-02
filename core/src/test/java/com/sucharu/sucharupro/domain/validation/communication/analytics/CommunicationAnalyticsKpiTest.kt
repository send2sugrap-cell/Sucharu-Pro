package com.sucharu.sucharupro.domain.validation.communication.analytics

import com.sucharu.sucharupro.domain.model.notification.Notification
import com.sucharu.sucharupro.domain.model.notification.NotificationStatus
import com.sucharu.sucharupro.domain.model.notification.NotificationType
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/**
 * 10. CommunicationAnalyticsKpiTest
 *
 * Verifies that KPI metrics (delivery rate, read rate, etc.) are computed accurately
 * based on notification statuses.
 */
class CommunicationAnalyticsKpiTest {

    private fun createNotification(status: NotificationStatus) = Notification(
        notificationId = "N-${Instant.now().toEpochMilli()}",
        notificationNo = "N-1",
        projectId = "PROJ-1",
        recipientUserId = "USR-1",
        createdBy = "USR-1",
        channel = com.sucharu.sucharupro.domain.model.notification.NotificationChannel.IN_APP,
        title = "Test",
        message = "Test message",
        notificationType = NotificationType.GENERAL,
        priority = NotificationPriority.NORMAL,
        status = status
    )

    @Test
    fun `calculateKpiSummary computes rates correctly`() {
        val notifications = listOf(
            createNotification(NotificationStatus.DELIVERED),
            createNotification(NotificationStatus.DELIVERED),
            createNotification(NotificationStatus.READ),
            createNotification(NotificationStatus.READ),
            createNotification(NotificationStatus.READ),
            createNotification(NotificationStatus.FAILED),
            createNotification(NotificationStatus.SENT) // In transit
        )

        val kpi = CommunicationAnalyticsCalculator.calculateKpiSummary(notifications)

        assertEquals("Total communications should be 7", 7, kpi.totalCommunications)
        assertEquals("Delivered count (including READ and ACKNOWLEDGED) should be 5", 5, kpi.deliveredCount)
        assertEquals("Read count (including ACKNOWLEDGED) should be 3", 3, kpi.readCount)
        assertEquals("Failed count should be 1", 1, kpi.failedCount)

        // Rates
        assertEquals("Delivery rate should be 5/7", 5.0 / 7.0, kpi.deliveryRate, 0.01)
        assertEquals("Read rate should be 3/5 of delivered", 3.0 / 5.0, kpi.readRate, 0.01)
    }

    @Test
    fun `calculateKpiSummary handles empty list`() {
        val kpi = CommunicationAnalyticsCalculator.calculateKpiSummary(emptyList())

        assertEquals(0, kpi.totalCommunications)
        assertEquals(0.0, kpi.deliveryRate, 0.01)
    }
}

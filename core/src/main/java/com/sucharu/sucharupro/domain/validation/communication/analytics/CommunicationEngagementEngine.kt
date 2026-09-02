package com.sucharu.sucharupro.domain.validation.communication.analytics

import com.sucharu.sucharupro.domain.model.notification.Notification
import com.sucharu.sucharupro.domain.model.notification.NotificationStatus

object CommunicationEngagementEngine {

    /**
     * Calculates an engagement score between 0.0 and 100.0 based on interaction.
     * Formula:
     * - Weight for READ: 40%
     * - Weight for ACKNOWLEDGEMENT: 60%
     * - Volume dampener: Avoids perfect scores for 1 message.
     */
    fun calculateEngagementScore(notifications: List<Notification>): Double {
        if (notifications.isEmpty()) return 0.0

        var delivered = 0
        var read = 0
        var ack = 0

        for (notification in notifications) {
            when (notification.status) {
                NotificationStatus.DELIVERED -> delivered++
                NotificationStatus.READ -> { delivered++; read++; ack++ }
                else -> { /* Ignore non-delivered for engagement */ }
            }
        }

        if (delivered == 0) return 0.0

        val readRate = read.toDouble() / delivered
        val ackRate = ack.toDouble() / delivered

        val rawScore = (readRate * 40.0) + (ackRate * 60.0)
        
        // Minor penalty for very low volume to require consistent engagement
        val volumeDampener = if (delivered < 3) 0.8 else 1.0

        return rawScore * volumeDampener
    }
}

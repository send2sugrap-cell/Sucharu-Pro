package com.sucharu.sucharupro.domain.validation.communication.analytics

import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationChannelAnalytics
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationKpiSummary
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationTypeAnalytics
import com.sucharu.sucharupro.domain.model.notification.Notification
import com.sucharu.sucharupro.domain.model.notification.NotificationStatus

/**
 * Pure functions for calculating analytics from communication events.
 */
object CommunicationAnalyticsCalculator {

    fun calculateKpiSummary(notifications: List<Notification>): CommunicationKpiSummary {
        val total = notifications.size
        if (total == 0) {
            return emptyKpiSummary()
        }

        var queued = 0
        var sent = 0
        var delivered = 0
        var read = 0
        var ack = 0
        var failed = 0
        var cancelled = 0

        var totalDeliveryTimeMs = 0L
        var deliveryCount = 0
        var totalReadTimeMs = 0L
        var readTimeCount = 0
        var totalAckTimeMs = 0L
        var ackTimeCount = 0

        notifications.forEach {
            when (it.status) {
                NotificationStatus.DRAFT -> {}
                NotificationStatus.QUEUED -> queued++
                NotificationStatus.PROCESSING -> queued++ // Group with queued
                NotificationStatus.SENT -> sent++
                NotificationStatus.DELIVERED -> {
                    sent++
                    delivered++
                }
                NotificationStatus.READ -> {
                    sent++
                    delivered++
                    read++
                    ack++
                }
                NotificationStatus.FAILED -> failed++
                NotificationStatus.CANCELLED -> cancelled++
                else -> {}
            }

            // Calculate latency if available
            val createdAt = it.createdAt
            it.deliveredAt?.let { delTime ->
                val diff = delTime - createdAt
                if (diff >= 0) {
                    totalDeliveryTimeMs += diff
                    deliveryCount++
                }
            }
            it.readAt?.let { rdTime ->
                val delTime = it.deliveredAt ?: createdAt
                val diff = rdTime - delTime
                if (diff >= 0) {
                    totalReadTimeMs += diff
                    readTimeCount++
                }
            }
        }

        val attempts = total - queued - cancelled
        val deliveryRate = if (attempts > 0) delivered.toDouble() / attempts else 0.0
        val readRate = if (delivered > 0) read.toDouble() / delivered else 0.0
        val ackRate = if (read > 0) ack.toDouble() / read else 0.0
        val failRate = if (attempts > 0) failed.toDouble() / attempts else 0.0

        return CommunicationKpiSummary(
            totalCommunications = total,
            queuedCount = queued,
            sentCount = sent,
            deliveredCount = delivered,
            readCount = read,
            acknowledgedCount = ack,
            failedCount = failed,
            cancelledCount = cancelled,
            deliveryRate = deliveryRate,
            readRate = readRate,
            acknowledgementRate = ackRate,
            failureRate = failRate,
            averageDeliveryTimeMs = if (deliveryCount > 0) totalDeliveryTimeMs / deliveryCount else 0L,
            averageReadTimeMs = if (readTimeCount > 0) totalReadTimeMs / readTimeCount else 0L,
            averageAcknowledgementTimeMs = if (ackTimeCount > 0) totalAckTimeMs / ackTimeCount else 0L
        )
    }

    fun calculateChannelAnalytics(notifications: List<Notification>): List<CommunicationChannelAnalytics> {
        return notifications.groupBy { it.channel }.map { (channel, channelNotifications) ->
            val summary = calculateKpiSummary(channelNotifications)
            CommunicationChannelAnalytics(
                channel = channel,
                totalSent = summary.sentCount,
                delivered = summary.deliveredCount,
                failed = summary.failedCount,
                read = summary.readCount,
                acknowledged = summary.acknowledgedCount,
                deliveryRate = summary.deliveryRate,
                readRate = summary.readRate,
                failureRate = summary.failureRate,
                averageDeliveryLatencyMs = summary.averageDeliveryTimeMs
            )
        }
    }

    fun calculateTypeAnalytics(notifications: List<Notification>): List<CommunicationTypeAnalytics> {
        return notifications.groupBy { it.notificationType }.map { (type, typeNotifications) ->
            val summary = calculateKpiSummary(typeNotifications)
            CommunicationTypeAnalytics(
                communicationType = type,
                totalCommunications = summary.totalCommunications,
                delivered = summary.deliveredCount,
                read = summary.readCount,
                failed = summary.failedCount,
                deliveryRate = summary.deliveryRate,
                readRate = summary.readRate
            )
        }
    }

    private fun emptyKpiSummary() = CommunicationKpiSummary(
        0, 0, 0, 0, 0, 0, 0, 0,
        0.0, 0.0, 0.0, 0.0,
        0L, 0L, 0L
    )
}

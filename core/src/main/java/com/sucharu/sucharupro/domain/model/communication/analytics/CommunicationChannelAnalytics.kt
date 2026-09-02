package com.sucharu.sucharupro.domain.model.communication.analytics

import com.sucharu.sucharupro.domain.model.notification.NotificationChannel

data class CommunicationChannelAnalytics(
    val channel: NotificationChannel,
    val totalSent: Int,
    val delivered: Int,
    val failed: Int,
    val read: Int,
    val acknowledged: Int,
    
    val deliveryRate: Double,
    val readRate: Double,
    val failureRate: Double,
    val averageDeliveryLatencyMs: Long
)

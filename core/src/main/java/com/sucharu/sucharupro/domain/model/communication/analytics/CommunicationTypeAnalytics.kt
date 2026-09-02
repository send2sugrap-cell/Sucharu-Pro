package com.sucharu.sucharupro.domain.model.communication.analytics

import com.sucharu.sucharupro.domain.model.notification.NotificationType

data class CommunicationTypeAnalytics(
    val communicationType: NotificationType,
    val totalCommunications: Int,
    val delivered: Int,
    val read: Int,
    val failed: Int,
    
    val deliveryRate: Double,
    val readRate: Double
)

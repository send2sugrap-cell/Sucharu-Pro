package com.sucharu.sucharupro.domain.model.communication.analytics

import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import java.time.Instant

data class CustomerEngagementAnalytics(
    val customerId: String,
    val totalMessages: Int,
    val delivered: Int,
    val read: Int,
    val acknowledged: Int,
    val unread: Int,
    
    val readRate: Double,
    val acknowledgementRate: Double,
    val engagementScore: Double,
    
    val preferredChannel: NotificationChannel?,
    val lastInteractionAt: Instant?,
    val engagementTrend: String // E.g., "STABLE", "INCREASING", "DECREASING"
)

data class InternalCommunicationAnalytics(
    val department: String,
    val role: String,
    
    val totalMessages: Int,
    val sent: Int,
    val delivered: Int,
    val read: Int,
    val acknowledged: Int,
    val unread: Int,
    
    val responseRate: Double,
    val engagementScore: Double,
    val communicationVolume: Int,
    val trend: String
)

data class VendorCommunicationAnalytics(
    val vendorId: String,
    
    val communicationCount: Int,
    val documentCommunicationCount: Int,
    
    val deliveryRate: Double,
    val readRate: Double,
    val acknowledgementRate: Double,
    
    val pendingCommunicationCount: Int,
    val engagementScore: Double,
    val lastCommunicationAt: Instant?
)

package com.sucharu.sucharupro.domain.model.communication.customer

import com.sucharu.sucharupro.domain.model.notification.NotificationPriority

/**
 * Aggregated metrics and counts for customer communications (Module 10 Step 02).
 */
data class CustomerCommunicationSummary(
    val projectId: String,
    val customerId: String? = null,
    val totalCount: Int = 0,
    val unreadCount: Int = 0,
    val readCount: Int = 0,
    val acknowledgedCount: Int = 0,
    val scheduledCount: Int = 0,
    val sentCount: Int = 0,
    val deliveredCount: Int = 0,
    val failedCount: Int = 0,
    val countsByType: Map<CustomerCommunicationType, Int> = emptyMap(),
    val countsByPriority: Map<NotificationPriority, Int> = emptyMap()
)

package com.sucharu.sucharupro.domain.model.communication.analytics

import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignAudienceType
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.notification.NotificationStatus
import com.sucharu.sucharupro.domain.model.notification.NotificationType
import java.time.Instant

data class CommunicationAnalyticsFilter(
    val projectId: String,
    val fromDate: Instant,
    val toDate: Instant,
    val communicationType: NotificationType? = null,
    val channel: NotificationChannel? = null,
    val priority: NotificationPriority? = null,
    val status: NotificationStatus? = null,
    val audienceType: CampaignAudienceType? = null,
    val customerId: String? = null,
    val vendorId: String? = null,
    val department: String? = null,
    val role: String? = null,
    val campaignId: String? = null,
    val automationRuleId: String? = null
) {
    init {
        require(projectId.isNotBlank()) { "projectId must not be blank" }
        require(!fromDate.isAfter(toDate)) { "fromDate cannot be after toDate" }
    }
}

package com.sucharu.sucharupro.domain.model.communication.analytics

import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignStatus

data class CampaignPerformanceAnalytics(
    val campaignId: String,
    val audienceSize: Int,
    val recipients: Int,
    
    val delivered: Int,
    val failed: Int,
    val read: Int,
    val acknowledged: Int,
    
    val deliveryRate: Double,
    val readRate: Double,
    val acknowledgementRate: Double,
    val engagementRate: Double,
    
    val completionStatus: CampaignStatus,
    val campaignDurationMs: Long
)

data class CommunicationAutomationAnalytics(
    val ruleId: String,
    val executionCount: Int,
    val successCount: Int,
    val blockedCount: Int,
    val failedCount: Int,
    val skippedCount: Int,
    val duplicatePreventedCount: Int,
    val notificationGeneratedCount: Int,
    
    val averageExecutionTimeMs: Long,
    val successRate: Double,
    val lastExecutedAt: java.time.Instant?
)

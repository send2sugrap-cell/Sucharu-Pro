package com.sucharu.sucharupro.domain.model.communication.campaign

/**
 * Calculated engagement rates and percentages for campaigns (Module 10 Step 07).
 */
data class CampaignEngagementSummary(
    val deliveryRate: Double = 0.0,
    val readRate: Double = 0.0,
    val acknowledgementRate: Double = 0.0,
    val failureRate: Double = 0.0,
    val engagementRate: Double = 0.0
)

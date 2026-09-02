package com.sucharu.sucharupro.domain.model.communication.campaign

/**
 * Project-scoped high-level summary KPIs for campaigns (Module 10 Step 07).
 */
data class CampaignSummary(
    val projectId: String,
    val totalCampaigns: Int = 0,
    val activeCampaigns: Int = 0,
    val scheduledCampaigns: Int = 0,
    val completedCampaigns: Int = 0,
    val cancelledCampaigns: Int = 0,
    val totalRecipients: Int = 0,
    val sent: Int = 0,
    val delivered: Int = 0,
    val failed: Int = 0,
    val read: Int = 0,
    val acknowledged: Int = 0,
    val deliveryRate: Double = 0.0,
    val readRate: Double = 0.0,
    val acknowledgementRate: Double = 0.0,
    val engagementRate: Double = 0.0
)

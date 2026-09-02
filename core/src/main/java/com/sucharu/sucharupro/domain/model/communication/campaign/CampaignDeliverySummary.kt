package com.sucharu.sucharupro.domain.model.communication.campaign

/**
 * Delivery status breakdown for a specific campaign or project scope (Module 10 Step 07).
 */
data class CampaignDeliverySummary(
    val totalRecipients: Int = 0,
    val queued: Int = 0,
    val sent: Int = 0,
    val delivered: Int = 0,
    val failed: Int = 0,
    val read: Int = 0,
    val acknowledged: Int = 0,
    val cancelled: Int = 0
)

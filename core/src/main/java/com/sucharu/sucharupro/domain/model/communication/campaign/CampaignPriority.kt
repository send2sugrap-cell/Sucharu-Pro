package com.sucharu.sucharupro.domain.model.communication.campaign

/**
 * Priority levels for Campaigns and Broadcasts (Module 10 Step 07).
 */
enum class CampaignPriority(val defaultLabel: String, val level: Int) {
    LOW("Low", 1),
    NORMAL("Normal", 2),
    HIGH("High", 3),
    URGENT("Urgent", 4)
}

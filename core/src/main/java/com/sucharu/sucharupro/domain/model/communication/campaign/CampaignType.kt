package com.sucharu.sucharupro.domain.model.communication.campaign

/**
 * Business-semantic types of Campaigns (Module 10 Step 07).
 */
enum class CampaignType(val defaultLabel: String) {
    PROMOTION("Promotional Campaign"),
    OFFER("Special Offer / Discount"),
    ANNOUNCEMENT("General Announcement"),
    SERVICE_UPDATE("Service / Product Update"),
    INFORMATION("Informational Bulletin"),
    REMINDER("Periodic Reminder"),
    SEASONAL("Seasonal Festival / Greeting"),
    CUSTOMER_RETENTION("Customer Retention & Loyalty"),
    GENERAL("General Communication")
}

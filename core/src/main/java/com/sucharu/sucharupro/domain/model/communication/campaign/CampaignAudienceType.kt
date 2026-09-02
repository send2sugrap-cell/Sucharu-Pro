package com.sucharu.sucharupro.domain.model.communication.campaign

/**
 * Target audience scoping categories for Campaigns, Announcements, and Broadcasts (Module 10 Step 07).
 */
enum class CampaignAudienceType(val defaultLabel: String, val isInternalOnly: Boolean = false) {
    ALL_PROJECT_USERS("All Project Users", false),
    CUSTOMER_SEGMENT("Customer Segment", false),
    SPECIFIC_CUSTOMERS("Specific Customers", false),
    VENDOR_SEGMENT("Vendor Segment", false),
    SPECIFIC_VENDORS("Specific Vendors", false),
    INTERNAL_TEAM("Internal Team", true),
    DEPARTMENT("Department", true),
    ROLE("Role Based", true),
    CUSTOM_RECIPIENTS("Custom Recipients", false)
}

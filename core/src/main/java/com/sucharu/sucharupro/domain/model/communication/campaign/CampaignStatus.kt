package com.sucharu.sucharupro.domain.model.communication.campaign

/**
 * Lifecycle states of a Campaign (Module 10 Step 07).
 */
enum class CampaignStatus(val defaultLabel: String, val isTerminal: Boolean = false) {
    DRAFT("Draft", false),
    PENDING_APPROVAL("Pending Approval", false),
    APPROVED("Approved", false),
    SCHEDULED("Scheduled", false),
    PUBLISHED("Published", false),
    COMPLETED("Completed", true),
    REJECTED("Rejected", true),
    CANCELLED("Cancelled", true)
}

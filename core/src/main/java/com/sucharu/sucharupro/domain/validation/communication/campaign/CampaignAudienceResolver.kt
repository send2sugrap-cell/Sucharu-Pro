package com.sucharu.sucharupro.domain.validation.communication.campaign

import com.sucharu.sucharupro.domain.model.communication.campaign.Campaign
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignAudienceType
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignRecipient
import com.sucharu.sucharupro.domain.model.notification.NotificationStatus

/**
 * Deterministic Audience Resolver for Campaigns, Announcements, and Broadcasts (Module 10 Step 07).
 *
 * Rules:
 * 1. Read-only resolution — zero mutation of source business domains.
 * 2. Strong project isolation — rejects any candidate not belonging to the campaign's projectId.
 * 3. Deduplication — eliminates duplicate recipients deterministically.
 * 4. Boundary enforcement — external actors never receive internal-only announcements.
 */
object CampaignAudienceResolver {

    data class CandidateRecipient(
        val projectId: String,
        val recipientType: String, // "CUSTOMER", "VENDOR", "USER", "STAFF"
        val recipientEntityId: String,
        val userId: String,
        val role: String? = null,
        val departmentId: String? = null,
        val teamId: String? = null,
        val isActive: Boolean = true
    )

    fun resolve(
        campaign: Campaign,
        candidates: List<CandidateRecipient>
    ): List<CampaignRecipient> {
        // 1. Enforce strict project boundary
        val projectCandidates = candidates.filter { it.projectId == campaign.projectId }

        // 2. Filter based on active state if criteria demands it
        val activeFiltered = if (campaign.targetCriteria.activeOnly) {
            projectCandidates.filter { it.isActive }
        } else {
            projectCandidates
        }

        // 3. Filter based on audience type and criteria
        val eligibleCandidates = when (campaign.audienceType) {
            CampaignAudienceType.ALL_PROJECT_USERS -> activeFiltered

            CampaignAudienceType.CUSTOMER_SEGMENT -> {
                activeFiltered.filter { it.recipientType == "CUSTOMER" }
            }

            CampaignAudienceType.SPECIFIC_CUSTOMERS -> {
                val allowed = campaign.targetCriteria.customerIds
                activeFiltered.filter { it.recipientType == "CUSTOMER" && it.recipientEntityId in allowed }
            }

            CampaignAudienceType.VENDOR_SEGMENT -> {
                activeFiltered.filter { it.recipientType == "VENDOR" }
            }

            CampaignAudienceType.SPECIFIC_VENDORS -> {
                val allowed = campaign.targetCriteria.vendorIds
                activeFiltered.filter { it.recipientType == "VENDOR" && it.recipientEntityId in allowed }
            }

            CampaignAudienceType.ROLE -> {
                val allowedRoles = campaign.targetCriteria.roles.map { it.name }
                activeFiltered.filter { it.role != null && it.role in allowedRoles }
            }

            CampaignAudienceType.DEPARTMENT -> {
                val allowedDepts = campaign.targetCriteria.departmentIds
                activeFiltered.filter { it.departmentId != null && it.departmentId in allowedDepts }
            }

            CampaignAudienceType.INTERNAL_TEAM -> {
                val allowedTeams = campaign.targetCriteria.teamIds
                activeFiltered.filter { it.teamId != null && it.teamId in allowedTeams }
            }

            CampaignAudienceType.CUSTOM_RECIPIENTS -> {
                val allowed = campaign.targetCriteria.customRecipientIds
                activeFiltered.filter { it.userId in allowed || it.recipientEntityId in allowed }
            }
        }

        // 4. Boundary Protection: If campaign is internal-only, exclude external actors
        val boundaryChecked = if (campaign.audienceType.isInternalOnly) {
            eligibleCandidates.filter { it.recipientType != "CUSTOMER" && it.recipientType != "VENDOR" }
        } else {
            eligibleCandidates
        }

        // 5. Deduplicate deterministically by (projectId, userId)
        val deduplicated = boundaryChecked.distinctBy { "${it.projectId}:${it.userId}" }

        // 6. Map to CampaignRecipient records
        return deduplicated.mapIndexed { index, candidate ->
            CampaignRecipient(
                recipientId = "rcp-${campaign.campaignId}-${index + 1}",
                campaignId = campaign.campaignId,
                projectId = campaign.projectId,
                recipientType = candidate.recipientType,
                recipientEntityId = candidate.recipientEntityId,
                userId = candidate.userId,
                deliveryStatus = NotificationStatus.DRAFT,
                resolvedAt = System.currentTimeMillis()
            )
        }
    }
}

package com.sucharu.sucharupro.domain.validation.communication.automation

import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationAutomationRule
import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationTriggerEvent
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignAudienceType

/**
 * Deterministic Recipient Resolver for Communication Automation (Module 10 Step 08).
 *
 * Rules:
 * 1. Read-only resolution — zero mutation of source business domains.
 * 2. Strong project isolation — rejects candidate recipients not belonging to trigger's projectId.
 * 3. Boundary protection — external users never receive internal-only alerts.
 */
object CommunicationAutomationRecipientResolver {

    data class CandidateAutomationRecipient(
        val projectId: String,
        val userId: String,
        val entityType: String, // "CUSTOMER", "VENDOR", "STAFF", "USER"
        val entityId: String,
        val role: String? = null,
        val departmentId: String? = null,
        val teamId: String? = null,
        val isActive: Boolean = true
    )

    fun resolveRecipients(
        trigger: CommunicationTriggerEvent,
        rule: CommunicationAutomationRule,
        candidates: List<CandidateAutomationRecipient>
    ): List<String> {
        val projectCandidates = candidates.filter { it.projectId == trigger.projectId && it.isActive }

        val matched = when (rule.audienceType) {
            CampaignAudienceType.ALL_PROJECT_USERS -> projectCandidates

            CampaignAudienceType.CUSTOMER_SEGMENT, CampaignAudienceType.SPECIFIC_CUSTOMERS -> {
                if (trigger.sourceEntityType == "CUSTOMER") {
                    projectCandidates.filter { it.entityType == "CUSTOMER" && it.entityId == trigger.sourceEntityId }
                } else {
                    projectCandidates.filter { it.entityType == "CUSTOMER" }
                }
            }

            CampaignAudienceType.VENDOR_SEGMENT, CampaignAudienceType.SPECIFIC_VENDORS -> {
                if (trigger.sourceEntityType == "VENDOR") {
                    projectCandidates.filter { it.entityType == "VENDOR" && it.entityId == trigger.sourceEntityId }
                } else {
                    projectCandidates.filter { it.entityType == "VENDOR" }
                }
            }

            CampaignAudienceType.ROLE -> {
                // By default for internal notifications match internal staff
                projectCandidates.filter { it.entityType == "STAFF" || it.entityType == "USER" }
            }

            CampaignAudienceType.DEPARTMENT -> {
                val deptId = trigger.payloadMetadata["departmentId"]
                if (deptId != null) {
                    projectCandidates.filter { it.departmentId == deptId }
                } else {
                    projectCandidates.filter { it.entityType == "STAFF" }
                }
            }

            CampaignAudienceType.INTERNAL_TEAM -> {
                val teamId = trigger.payloadMetadata["teamId"]
                if (teamId != null) {
                    projectCandidates.filter { it.teamId == teamId }
                } else {
                    projectCandidates.filter { it.entityType == "STAFF" }
                }
            }

            CampaignAudienceType.CUSTOM_RECIPIENTS -> {
                val explicitUserId = trigger.payloadMetadata["recipientUserId"] ?: trigger.actorUserId
                projectCandidates.filter { it.userId == explicitUserId }
            }
        }

        // Boundary protection: If audience is internal-only, exclude external actors
        val boundaryChecked = if (rule.audienceType.isInternalOnly) {
            matched.filter { it.entityType != "CUSTOMER" && it.entityType != "VENDOR" }
        } else {
            matched
        }

        return boundaryChecked.map { it.userId }.distinct()
    }
}

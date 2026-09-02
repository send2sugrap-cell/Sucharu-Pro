package com.sucharu.sucharupro.domain.validation.communication.campaign

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignStatus

/**
 * Validates lifecycle transitions for Campaigns, Announcements, and Broadcasts (Module 10 Step 07).
 */
object CampaignLifecycleValidator {

    private val allowedTransitions: Map<CampaignStatus, Set<CampaignStatus>> = mapOf(
        CampaignStatus.DRAFT to setOf(
            CampaignStatus.PENDING_APPROVAL,
            CampaignStatus.APPROVED, // Direct approval by ADMIN
            CampaignStatus.SCHEDULED,
            CampaignStatus.PUBLISHED,
            CampaignStatus.CANCELLED
        ),
        CampaignStatus.PENDING_APPROVAL to setOf(
            CampaignStatus.APPROVED,
            CampaignStatus.REJECTED,
            CampaignStatus.CANCELLED
        ),
        CampaignStatus.APPROVED to setOf(
            CampaignStatus.SCHEDULED,
            CampaignStatus.PUBLISHED,
            CampaignStatus.CANCELLED
        ),
        CampaignStatus.SCHEDULED to setOf(
            CampaignStatus.PUBLISHED,
            CampaignStatus.CANCELLED
        ),
        CampaignStatus.PUBLISHED to setOf(
            CampaignStatus.COMPLETED,
            CampaignStatus.CANCELLED
        ),
        CampaignStatus.COMPLETED to emptySet(),
        CampaignStatus.REJECTED to setOf(
            CampaignStatus.DRAFT // Can be revised back to DRAFT
        ),
        CampaignStatus.CANCELLED to emptySet()
    )

    fun validateTransition(
        from: CampaignStatus,
        to: CampaignStatus
    ): DomainResult<Unit> {
        if (from == to) return DomainResult.Success(Unit)

        if (from.isTerminal && from != CampaignStatus.REJECTED) {
            return DomainResult.Error(message = "Cannot transition from terminal status '$from'.")
        }

        val permitted = allowedTransitions[from] ?: emptySet()
        return if (to in permitted) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Illegal campaign transition: cannot transition from '$from' to '$to'.")
        }
    }
}

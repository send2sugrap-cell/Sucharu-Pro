package com.sucharu.sucharupro.domain.validation.communication.campaign

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignAudienceCriteria
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignAudienceType

/**
 * Validates domain constraints, field invariants, and timing logic for Campaigns (Module 10 Step 07).
 */
object CampaignValidator {

    data class ValidationError(val field: String, val message: String)

    fun validateCreation(
        projectId: String,
        title: String,
        content: String,
        audienceType: CampaignAudienceType,
        targetCriteria: CampaignAudienceCriteria,
        createdBy: String,
        scheduledAt: Long? = null,
        startsAt: Long? = null,
        endsAt: Long? = null,
        now: Long = System.currentTimeMillis()
    ): DomainResult<Unit> {
        val errors = mutableListOf<ValidationError>()

        if (projectId.isBlank()) errors.add(ValidationError("projectId", "Project ID cannot be blank."))
        if (title.isBlank()) errors.add(ValidationError("title", "Campaign title cannot be blank."))
        if (title.length > 200) errors.add(ValidationError("title", "Campaign title cannot exceed 200 characters."))
        if (content.isBlank()) errors.add(ValidationError("content", "Campaign content cannot be blank."))
        if (createdBy.isBlank()) errors.add(ValidationError("createdBy", "Creator ID cannot be blank."))

        if (scheduledAt != null && scheduledAt < now) {
            errors.add(ValidationError("scheduledAt", "Scheduled time cannot be in the past."))
        }

        if (startsAt != null && endsAt != null && endsAt < startsAt) {
            errors.add(ValidationError("endsAt", "Campaign end date cannot precede start date."))
        }

        if (audienceType == CampaignAudienceType.SPECIFIC_CUSTOMERS && targetCriteria.customerIds.isEmpty()) {
            errors.add(ValidationError("targetCriteria", "Specific customers targeting requires at least one customer ID."))
        }

        if (audienceType == CampaignAudienceType.SPECIFIC_VENDORS && targetCriteria.vendorIds.isEmpty()) {
            errors.add(ValidationError("targetCriteria", "Specific vendors targeting requires at least one vendor ID."))
        }

        if (audienceType == CampaignAudienceType.ROLE && targetCriteria.roles.isEmpty()) {
            errors.add(ValidationError("targetCriteria", "Role-based targeting requires at least one role."))
        }

        if (audienceType == CampaignAudienceType.DEPARTMENT && targetCriteria.departmentIds.isEmpty()) {
            errors.add(ValidationError("targetCriteria", "Department targeting requires at least one department ID."))
        }

        if (audienceType == CampaignAudienceType.INTERNAL_TEAM && targetCriteria.teamIds.isEmpty()) {
            errors.add(ValidationError("targetCriteria", "Internal team targeting requires at least one team ID."))
        }

        return if (errors.isEmpty()) {
            DomainResult.Success(Unit)
        } else {
            DomainResult.Error(message = "Campaign validation failed: ${errors.joinToString { "${it.field}: ${it.message}" }}")
        }
    }
}

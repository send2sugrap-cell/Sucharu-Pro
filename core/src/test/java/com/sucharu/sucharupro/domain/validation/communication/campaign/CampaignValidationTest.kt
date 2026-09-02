package com.sucharu.sucharupro.domain.validation.communication.campaign

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignAudienceCriteria
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignAudienceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignValidationTest {

    private val validProjectId = "proj-001"
    private val validTitle = "Eid Mubarak Special Print Offer"
    private val validContent = "Get 20% off on all offset printing orders placed before Eid."
    private val validCreator = "user-admin-01"

    @Test
    fun validateCreation_allValidFields_succeeds() {
        val result = CampaignValidator.validateCreation(
            projectId = validProjectId,
            title = validTitle,
            content = validContent,
            audienceType = CampaignAudienceType.ALL_PROJECT_USERS,
            targetCriteria = CampaignAudienceCriteria(),
            createdBy = validCreator
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateCreation_blankProjectId_fails() {
        val result = CampaignValidator.validateCreation(
            projectId = "   ",
            title = validTitle,
            content = validContent,
            audienceType = CampaignAudienceType.ALL_PROJECT_USERS,
            targetCriteria = CampaignAudienceCriteria(),
            createdBy = validCreator
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateCreation_blankTitle_fails() {
        val result = CampaignValidator.validateCreation(
            projectId = validProjectId,
            title = "",
            content = validContent,
            audienceType = CampaignAudienceType.ALL_PROJECT_USERS,
            targetCriteria = CampaignAudienceCriteria(),
            createdBy = validCreator
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateCreation_titleExceeds200Chars_fails() {
        val result = CampaignValidator.validateCreation(
            projectId = validProjectId,
            title = "A".repeat(201),
            content = validContent,
            audienceType = CampaignAudienceType.ALL_PROJECT_USERS,
            targetCriteria = CampaignAudienceCriteria(),
            createdBy = validCreator
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateCreation_blankContent_fails() {
        val result = CampaignValidator.validateCreation(
            projectId = validProjectId,
            title = validTitle,
            content = "   ",
            audienceType = CampaignAudienceType.ALL_PROJECT_USERS,
            targetCriteria = CampaignAudienceCriteria(),
            createdBy = validCreator
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateCreation_scheduledInPast_fails() {
        val past = System.currentTimeMillis() - 5000L
        val result = CampaignValidator.validateCreation(
            projectId = validProjectId,
            title = validTitle,
            content = validContent,
            audienceType = CampaignAudienceType.ALL_PROJECT_USERS,
            targetCriteria = CampaignAudienceCriteria(),
            createdBy = validCreator,
            scheduledAt = past
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateCreation_endsAtBeforeStartsAt_fails() {
        val now = System.currentTimeMillis()
        val result = CampaignValidator.validateCreation(
            projectId = validProjectId,
            title = validTitle,
            content = validContent,
            audienceType = CampaignAudienceType.ALL_PROJECT_USERS,
            targetCriteria = CampaignAudienceCriteria(),
            createdBy = validCreator,
            startsAt = now + 10000L,
            endsAt = now + 5000L
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateCreation_specificCustomersTargeting_emptyList_fails() {
        val result = CampaignValidator.validateCreation(
            projectId = validProjectId,
            title = validTitle,
            content = validContent,
            audienceType = CampaignAudienceType.SPECIFIC_CUSTOMERS,
            targetCriteria = CampaignAudienceCriteria(customerIds = emptySet()),
            createdBy = validCreator
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateCreation_roleTargeting_emptyRoles_fails() {
        val result = CampaignValidator.validateCreation(
            projectId = validProjectId,
            title = validTitle,
            content = validContent,
            audienceType = CampaignAudienceType.ROLE,
            targetCriteria = CampaignAudienceCriteria(roles = emptySet()),
            createdBy = validCreator
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateCreation_roleTargeting_validRoles_succeeds() {
        val result = CampaignValidator.validateCreation(
            projectId = validProjectId,
            title = validTitle,
            content = validContent,
            audienceType = CampaignAudienceType.ROLE,
            targetCriteria = CampaignAudienceCriteria(roles = setOf(UserRole.QC_INSPECTOR)),
            createdBy = validCreator
        )
        assertTrue(result is DomainResult.Success)
    }
}

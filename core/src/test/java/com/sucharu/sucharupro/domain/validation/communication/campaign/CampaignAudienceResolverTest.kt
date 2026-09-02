package com.sucharu.sucharupro.domain.validation.communication.campaign

import com.sucharu.sucharupro.domain.model.communication.campaign.Campaign
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignAudienceCriteria
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignAudienceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignAudienceResolverTest {

    private val candidates = listOf(
        CampaignAudienceResolver.CandidateRecipient(
            projectId = "proj-01",
            recipientType = "CUSTOMER",
            recipientEntityId = "cus-01",
            userId = "user-cus-01",
            isActive = true
        ),
        CampaignAudienceResolver.CandidateRecipient(
            projectId = "proj-01",
            recipientType = "CUSTOMER",
            recipientEntityId = "cus-02",
            userId = "user-cus-02",
            isActive = true
        ),
        CampaignAudienceResolver.CandidateRecipient(
            projectId = "proj-01",
            recipientType = "CUSTOMER",
            recipientEntityId = "cus-03-inactive",
            userId = "user-cus-03",
            isActive = false
        ),
        CampaignAudienceResolver.CandidateRecipient(
            projectId = "proj-01",
            recipientType = "VENDOR",
            recipientEntityId = "ven-01",
            userId = "user-ven-01",
            isActive = true
        ),
        CampaignAudienceResolver.CandidateRecipient(
            projectId = "proj-01",
            recipientType = "STAFF",
            recipientEntityId = "user-staff-01",
            userId = "user-staff-01",
            role = "STAFF",
            departmentId = "dept-press",
            teamId = "team-offset",
            isActive = true
        ),
        CampaignAudienceResolver.CandidateRecipient(
            projectId = "proj-01",
            recipientType = "STAFF",
            recipientEntityId = "user-qc-01",
            userId = "user-qc-01",
            role = "QC_INSPECTOR",
            departmentId = "dept-qc",
            teamId = "team-qc",
            isActive = true
        ),
        // Cross-project candidate (must never be resolved!)
        CampaignAudienceResolver.CandidateRecipient(
            projectId = "proj-02-OTHER",
            recipientType = "CUSTOMER",
            recipientEntityId = "cus-99",
            userId = "user-cus-99",
            isActive = true
        )
    )

    private fun buildCampaign(
        audienceType: CampaignAudienceType,
        criteria: CampaignAudienceCriteria = CampaignAudienceCriteria()
    ) = Campaign(
        campaignId = "cmp-test-01",
        campaignNo = "CMP-2026-00001",
        projectId = "proj-01",
        title = "Test Campaign",
        content = "Test content",
        audienceType = audienceType,
        targetCriteria = criteria,
        createdBy = "user-admin-01"
    )

    @Test
    fun resolve_allProjectUsers_activeOnly_resolvesProjectUsersExcludingInactiveAndOtherProjects() {
        val campaign = buildCampaign(CampaignAudienceType.ALL_PROJECT_USERS)
        val result = CampaignAudienceResolver.resolve(campaign, candidates)

        assertEquals(5, result.size)
        assertTrue(result.none { it.userId == "user-cus-03" }) // Inactive excluded
        assertTrue(result.none { it.projectId == "proj-02-OTHER" }) // Cross-project excluded
    }

    @Test
    fun resolve_customerSegment_resolvesOnlyCustomers() {
        val campaign = buildCampaign(CampaignAudienceType.CUSTOMER_SEGMENT)
        val result = CampaignAudienceResolver.resolve(campaign, candidates)

        assertEquals(2, result.size)
        assertTrue(result.all { it.recipientType == "CUSTOMER" })
    }

    @Test
    fun resolve_specificCustomers_resolvesOnlySpecified() {
        val campaign = buildCampaign(
            CampaignAudienceType.SPECIFIC_CUSTOMERS,
            CampaignAudienceCriteria(customerIds = setOf("cus-01"))
        )
        val result = CampaignAudienceResolver.resolve(campaign, candidates)

        assertEquals(1, result.size)
        assertEquals("cus-01", result[0].recipientEntityId)
    }

    @Test
    fun resolve_vendorSegment_resolvesOnlyVendors() {
        val campaign = buildCampaign(CampaignAudienceType.VENDOR_SEGMENT)
        val result = CampaignAudienceResolver.resolve(campaign, candidates)

        assertEquals(1, result.size)
        assertEquals("user-ven-01", result[0].userId)
    }

    @Test
    fun resolve_roleTargeting_resolvesOnlyMatchingRole() {
        val campaign = buildCampaign(
            CampaignAudienceType.ROLE,
            CampaignAudienceCriteria(roles = setOf(UserRole.QC_INSPECTOR))
        )
        val result = CampaignAudienceResolver.resolve(campaign, candidates)

        assertEquals(1, result.size)
        assertEquals("user-qc-01", result[0].userId)
    }

    @Test
    fun resolve_internalOnlyAudience_neverLeaksToExternalCustomersOrVendors() {
        val campaign = buildCampaign(CampaignAudienceType.INTERNAL_TEAM, CampaignAudienceCriteria(teamIds = setOf("team-offset")))
        val result = CampaignAudienceResolver.resolve(campaign, candidates)

        assertEquals(1, result.size)
        assertEquals("user-staff-01", result[0].userId)
        assertTrue(result.none { it.recipientType == "CUSTOMER" || it.recipientType == "VENDOR" })
    }

    @Test
    fun resolve_deduplicatesDuplicateCandidateEntries() {
        val duplicates = candidates + candidates[0] + candidates[1]
        val campaign = buildCampaign(CampaignAudienceType.CUSTOMER_SEGMENT)
        val result = CampaignAudienceResolver.resolve(campaign, duplicates)

        assertEquals(2, result.size)
    }
}

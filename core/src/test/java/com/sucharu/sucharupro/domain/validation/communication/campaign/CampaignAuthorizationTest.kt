package com.sucharu.sucharupro.domain.validation.communication.campaign

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignAudienceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignAuthorizationTest {

    @Test
    fun create_adminAndManager_canCreateAnyAudience() {
        val audiences = CampaignAudienceType.entries
        audiences.forEach { aud ->
            assertTrue(CampaignAuthorizationValidator.validateCreate(UserRole.ADMIN, aud) is DomainResult.Success)
            assertTrue(CampaignAuthorizationValidator.validateCreate(UserRole.MANAGER, aud) is DomainResult.Success)
        }
    }

    @Test
    fun create_externalRoles_cannotCreateCampaigns() {
        listOf(UserRole.CUSTOMER, UserRole.VENDOR, UserRole.AFFILIATE).forEach { role ->
            val result = CampaignAuthorizationValidator.validateCreate(role, CampaignAudienceType.ALL_PROJECT_USERS)
            assertTrue("$role must be denied from creating campaigns", result is DomainResult.Error)
        }
    }

    @Test
    fun create_staff_cannotCreateInternalBroadcasts() {
        val result = CampaignAuthorizationValidator.validateCreate(UserRole.STAFF, CampaignAudienceType.DEPARTMENT)
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun approval_separationOfDuties_creatorCannotApproveUnlessAdmin() {
        // Manager cannot approve own campaign
        val managerSelfApproval = CampaignAuthorizationValidator.validateApproval(
            callerRole = UserRole.MANAGER,
            creatorUserId = "user-manager-01",
            approverUserId = "user-manager-01"
        )
        assertTrue(managerSelfApproval is DomainResult.Error)

        // Manager can approve someone else's campaign
        val managerOtherApproval = CampaignAuthorizationValidator.validateApproval(
            callerRole = UserRole.MANAGER,
            creatorUserId = "user-staff-01",
            approverUserId = "user-manager-01"
        )
        assertTrue(managerOtherApproval is DomainResult.Success)

        // Admin can approve own campaign (executive override)
        val adminSelfApproval = CampaignAuthorizationValidator.validateApproval(
            callerRole = UserRole.ADMIN,
            creatorUserId = "user-admin-01",
            approverUserId = "user-admin-01"
        )
        assertTrue(adminSelfApproval is DomainResult.Success)
    }

    @Test
    fun publish_separationOfDuties_creatorCannotPublishUnlessAdmin() {
        val managerSelfPublish = CampaignAuthorizationValidator.validatePublish(
            callerRole = UserRole.MANAGER,
            creatorUserId = "user-manager-01",
            publisherUserId = "user-manager-01"
        )
        assertTrue(managerSelfPublish is DomainResult.Error)

        val managerOtherPublish = CampaignAuthorizationValidator.validatePublish(
            callerRole = UserRole.MANAGER,
            creatorUserId = "user-staff-01",
            publisherUserId = "user-manager-01"
        )
        assertTrue(managerOtherPublish is DomainResult.Success)

        val adminSelfPublish = CampaignAuthorizationValidator.validatePublish(
            callerRole = UserRole.ADMIN,
            creatorUserId = "user-admin-01",
            publisherUserId = "user-admin-01"
        )
        assertTrue(adminSelfPublish is DomainResult.Success)
    }

    @Test
    fun cancel_onlyAdminAndManagerCanCancel() {
        assertTrue(CampaignAuthorizationValidator.validateCancel(UserRole.ADMIN) is DomainResult.Success)
        assertTrue(CampaignAuthorizationValidator.validateCancel(UserRole.MANAGER) is DomainResult.Success)
        assertTrue(CampaignAuthorizationValidator.validateCancel(UserRole.STAFF) is DomainResult.Error)
        assertTrue(CampaignAuthorizationValidator.validateCancel(UserRole.CUSTOMER) is DomainResult.Error)
    }
}

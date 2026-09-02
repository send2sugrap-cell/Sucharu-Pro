package com.sucharu.sucharupro.domain.validation.communication.campaign

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.campaign.CampaignAudienceType
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * RBAC and Separation of Duties validator for Campaigns, Announcements, and Broadcasts (Module 10 Step 07).
 */
object CampaignAuthorizationValidator {

    /**
     * Validates whether [callerRole] can create a campaign targeting [audienceType].
     */
    fun validateCreate(callerRole: UserRole, audienceType: CampaignAudienceType): DomainResult<Unit> {
        if (callerRole == UserRole.CUSTOMER || callerRole == UserRole.VENDOR) {
            return DomainResult.Error(message = "External actors ($callerRole) cannot create organizational campaigns.")
        }
        if (callerRole == UserRole.AFFILIATE) {
            return DomainResult.Error(message = "AFFILIATE role cannot create campaigns.")
        }
        if (audienceType.isInternalOnly && callerRole == UserRole.STAFF) {
            // Staff can participate but not create company-wide internal broadcasts
            return DomainResult.Error(message = "STAFF role cannot initiate internal broadcast/department campaigns.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates whether [callerRole] can submit a campaign for approval.
     */
    fun validateSubmit(callerRole: UserRole): DomainResult<Unit> {
        if (!callerRole.isInternal) {
            return DomainResult.Error(message = "External actors cannot submit campaigns.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates whether [callerRole] can approve/reject a campaign, enforcing Separation of Duties.
     */
    fun validateApproval(
        callerRole: UserRole,
        creatorUserId: String,
        approverUserId: String
    ): DomainResult<Unit> {
        if (callerRole != UserRole.ADMIN && callerRole != UserRole.MANAGER) {
            return DomainResult.Error(message = "Role '$callerRole' is not authorized to approve campaigns.")
        }

        // Separation of Duties: Creator cannot approve their own campaign unless they are ADMIN
        if (callerRole != UserRole.ADMIN && creatorUserId == approverUserId) {
            return DomainResult.Error(message = "Separation of Duties violation: Campaign creator cannot approve their own campaign.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates whether [callerRole] can publish/dispatch a campaign, enforcing Separation of Duties.
     */
    fun validatePublish(
        callerRole: UserRole,
        creatorUserId: String,
        publisherUserId: String
    ): DomainResult<Unit> {
        if (callerRole != UserRole.ADMIN && callerRole != UserRole.MANAGER) {
            return DomainResult.Error(message = "Role '$callerRole' is not authorized to publish campaigns.")
        }

        // Separation of Duties: Creator cannot publish their own campaign directly unless they are ADMIN
        if (callerRole != UserRole.ADMIN && creatorUserId == publisherUserId) {
            return DomainResult.Error(message = "Separation of Duties violation: Campaign creator cannot directly publish their own campaign.")
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates whether [callerRole] can cancel a campaign.
     */
    fun validateCancel(callerRole: UserRole): DomainResult<Unit> {
        if (callerRole != UserRole.ADMIN && callerRole != UserRole.MANAGER) {
            return DomainResult.Error(message = "Only ADMIN and MANAGER roles can cancel active campaigns.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates whether [callerRole] can view campaign analytics and performance metrics.
     */
    fun validateViewAnalytics(callerRole: UserRole): DomainResult<Unit> {
        if (!callerRole.isInternal) {
            return DomainResult.Error(message = "External actors cannot view campaign analytics.")
        }
        return DomainResult.Success(Unit)
    }
}

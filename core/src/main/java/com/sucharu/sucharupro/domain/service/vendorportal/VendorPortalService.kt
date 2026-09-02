package com.sucharu.sucharupro.domain.service.vendorportal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Service contract for Vendor Portal Foundation & Access management (Module 13 Step 01).
 */
interface VendorPortalService {

    suspend fun createOrInviteAccount(
        vendorId: String,
        portalCode: String,
        primaryContactEmail: String? = null,
        primaryContactPhone: String? = null,
        tenantId: String,
        projectId: String,
        actorId: String
    ): DomainResult<VendorPortalAccount>

    suspend fun activateAccount(
        portalAccountId: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorPortalAccount>

    suspend fun suspendAccount(
        portalAccountId: String,
        reason: String,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorPortalAccount>

    suspend fun inviteVendorUser(
        portalAccountId: String,
        vendorId: String,
        userId: String,
        role: VendorPortalRole = VendorPortalRole.VENDOR_OPERATOR,
        projectScope: String = "*",
        tenantId: String,
        actorId: String
    ): DomainResult<VendorPortalMembership>

    suspend fun activateMembership(
        invitationToken: String,
        tenantId: String,
        actorId: String,
        isInternalAdmin: Boolean = false
    ): DomainResult<VendorPortalMembership>

    suspend fun updateMembershipStatus(
        membershipId: String,
        newStatus: VendorPortalMembershipStatus,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorPortalMembership>

    suspend fun getAccessContext(
        userId: String,
        vendorId: String,
        tenantId: String,
        clientIp: String? = null
    ): DomainResult<VendorPortalAccessContext>

    suspend fun createSession(
        membershipId: String,
        userId: String,
        vendorId: String,
        tenantId: String,
        projectId: String,
        sessionTokenHash: String,
        ipAddress: String? = null,
        userAgent: String? = null,
        validityMinutes: Long = 1440L // 24 hours
    ): DomainResult<VendorPortalSession>

    suspend fun validateSession(
        tokenHash: String,
        tenantId: String,
        clientIp: String? = null
    ): DomainResult<VendorPortalSession>

    suspend fun revokeSession(
        sessionId: String,
        tenantId: String,
        actorId: String
    ): DomainResult<Boolean>

    suspend fun savePolicy(
        policy: VendorPortalAccessPolicy,
        tenantId: String,
        actorId: String
    ): DomainResult<VendorPortalAccessPolicy>

    suspend fun getPolicy(
        vendorId: String?,
        projectId: String,
        tenantId: String
    ): DomainResult<VendorPortalAccessPolicy>

    suspend fun listMemberships(
        vendorId: String?,
        userId: String?,
        status: VendorPortalMembershipStatus?,
        tenantId: String
    ): DomainResult<List<VendorPortalMembership>>

    suspend fun listAccounts(
        projectId: String?,
        status: VendorPortalAccountStatus?,
        tenantId: String
    ): DomainResult<List<VendorPortalAccount>>

    suspend fun listAuditEvents(
        vendorId: String?,
        actorUserId: String?,
        tenantId: String
    ): DomainResult<List<VendorPortalAuditEvent>>
}

package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Domain repository contract for Vendor Portal Foundation & Secure Access (Module 13 Step 01).
 */
interface VendorPortalRepository {

    // --- Accounts ---
    suspend fun createAccount(account: VendorPortalAccount): DomainResult<VendorPortalAccount>
    suspend fun getAccountById(portalAccountId: String, tenantId: String): DomainResult<VendorPortalAccount?>
    suspend fun getAccountByVendorId(vendorId: String, tenantId: String): DomainResult<VendorPortalAccount?>
    suspend fun getAccountByCode(portalCode: String, tenantId: String): DomainResult<VendorPortalAccount?>
    suspend fun updateAccount(account: VendorPortalAccount): DomainResult<VendorPortalAccount>
    suspend fun listAccounts(projectId: String?, status: VendorPortalAccountStatus?, tenantId: String): DomainResult<List<VendorPortalAccount>>

    // --- Memberships ---
    suspend fun createMembership(membership: VendorPortalMembership): DomainResult<VendorPortalMembership>
    suspend fun getMembershipById(membershipId: String, tenantId: String): DomainResult<VendorPortalMembership?>
    suspend fun getMembershipByVendorAndUser(vendorId: String, userId: String, tenantId: String): DomainResult<VendorPortalMembership?>
    suspend fun getMembershipByToken(invitationToken: String, tenantId: String): DomainResult<VendorPortalMembership?>
    suspend fun updateMembership(membership: VendorPortalMembership): DomainResult<VendorPortalMembership>
    suspend fun listMemberships(vendorId: String?, userId: String?, status: VendorPortalMembershipStatus?, tenantId: String): DomainResult<List<VendorPortalMembership>>

    // --- Access Policies ---
    suspend fun savePolicy(policy: VendorPortalAccessPolicy): DomainResult<VendorPortalAccessPolicy>
    suspend fun getPolicyByVendorId(vendorId: String, tenantId: String): DomainResult<VendorPortalAccessPolicy?>
    suspend fun getDefaultPolicy(projectId: String, tenantId: String): DomainResult<VendorPortalAccessPolicy?>

    // --- Sessions ---
    suspend fun createSession(session: VendorPortalSession): DomainResult<VendorPortalSession>
    suspend fun getSessionById(sessionId: String, tenantId: String): DomainResult<VendorPortalSession?>
    suspend fun getActiveSessionByToken(tokenHash: String, tenantId: String): DomainResult<VendorPortalSession?>
    suspend fun revokeSession(sessionId: String, tenantId: String): DomainResult<Boolean>
    suspend fun terminateUserSessions(userId: String, tenantId: String): DomainResult<Int>

    // --- Audit Events ---
    suspend fun recordAuditEvent(event: VendorPortalAuditEvent): DomainResult<VendorPortalAuditEvent>
    suspend fun listAuditEvents(vendorId: String?, actorUserId: String?, tenantId: String): DomainResult<List<VendorPortalAuditEvent>>
}

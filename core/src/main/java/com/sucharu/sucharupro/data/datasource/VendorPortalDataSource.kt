package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * Data Source contract for Vendor Portal Foundation & Access persistence (Module 13 Step 01).
 */
interface VendorPortalDataSource {

    // --- Accounts ---
    suspend fun insertAccount(account: VendorPortalAccount): VendorPortalAccount
    suspend fun findAccountById(portalAccountId: String, tenantId: String): VendorPortalAccount?
    suspend fun findAccountByVendorId(vendorId: String, tenantId: String): VendorPortalAccount?
    suspend fun findAccountByCode(portalCode: String, tenantId: String): VendorPortalAccount?
    suspend fun updateAccount(account: VendorPortalAccount): VendorPortalAccount
    suspend fun listAccounts(projectId: String?, status: VendorPortalAccountStatus?, tenantId: String): List<VendorPortalAccount>

    // --- Memberships ---
    suspend fun insertMembership(membership: VendorPortalMembership): VendorPortalMembership
    suspend fun findMembershipById(membershipId: String, tenantId: String): VendorPortalMembership?
    suspend fun findMembershipByVendorAndUser(vendorId: String, userId: String, tenantId: String): VendorPortalMembership?
    suspend fun findMembershipByToken(invitationToken: String, tenantId: String): VendorPortalMembership?
    suspend fun updateMembership(membership: VendorPortalMembership): VendorPortalMembership
    suspend fun listMemberships(vendorId: String?, userId: String?, status: VendorPortalMembershipStatus?, tenantId: String): List<VendorPortalMembership>

    // --- Access Policies ---
    suspend fun upsertPolicy(policy: VendorPortalAccessPolicy): VendorPortalAccessPolicy
    suspend fun findPolicyByVendorId(vendorId: String, tenantId: String): VendorPortalAccessPolicy?
    suspend fun findDefaultPolicy(projectId: String, tenantId: String): VendorPortalAccessPolicy?

    // --- Sessions ---
    suspend fun insertSession(session: VendorPortalSession): VendorPortalSession
    suspend fun findSessionById(sessionId: String, tenantId: String): VendorPortalSession?
    suspend fun findActiveSessionByToken(tokenHash: String, tenantId: String): VendorPortalSession?
    suspend fun updateSessionStatus(sessionId: String, status: VendorPortalSessionStatus, tenantId: String): Boolean
    suspend fun terminateUserSessions(userId: String, tenantId: String): Int

    // --- Audit Events ---
    suspend fun appendAuditEvent(event: VendorPortalAuditEvent): VendorPortalAuditEvent
    suspend fun listAuditEvents(vendorId: String?, actorUserId: String?, tenantId: String): List<VendorPortalAuditEvent>
}

package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.vendorportal.*

/**
 * In-memory, thread-safe test implementation of VendorPortalDataSource (Module 13 Step 01).
 */
class FakeVendorPortalDataSource : VendorPortalDataSource {

    private val accounts = mutableListOf<VendorPortalAccount>()
    private val memberships = mutableListOf<VendorPortalMembership>()
    private val policies = mutableListOf<VendorPortalAccessPolicy>()
    private val sessions = mutableListOf<VendorPortalSession>()
    private val auditEvents = mutableListOf<VendorPortalAuditEvent>()

    // --- Accounts ---
    override suspend fun insertAccount(account: VendorPortalAccount): VendorPortalAccount = synchronized(this) {
        val existing = accounts.find { it.tenantId == account.tenantId && it.vendorId == account.vendorId }
        if (existing != null) {
            throw IllegalStateException("Vendor portal account already exists for vendor '${account.vendorId}'")
        }
        val codeExisting = accounts.find { it.tenantId == account.tenantId && it.portalCode == account.portalCode }
        if (codeExisting != null) {
            throw IllegalStateException("Vendor portal code '${account.portalCode}' already taken")
        }
        accounts.add(account)
        account
    }

    override suspend fun findAccountById(portalAccountId: String, tenantId: String): VendorPortalAccount? = synchronized(this) {
        accounts.find { it.portalAccountId == portalAccountId && it.tenantId == tenantId }
    }

    override suspend fun findAccountByVendorId(vendorId: String, tenantId: String): VendorPortalAccount? = synchronized(this) {
        accounts.find { it.vendorId == vendorId && it.tenantId == tenantId }
    }

    override suspend fun findAccountByCode(portalCode: String, tenantId: String): VendorPortalAccount? = synchronized(this) {
        accounts.find { it.portalCode == portalCode && it.tenantId == tenantId }
    }

    override suspend fun updateAccount(account: VendorPortalAccount): VendorPortalAccount = synchronized(this) {
        val idx = accounts.indexOfFirst { it.portalAccountId == account.portalAccountId && it.tenantId == account.tenantId }
        if (idx == -1) {
            throw NoSuchElementException("Vendor portal account '${account.portalAccountId}' not found")
        }
        val current = accounts[idx]
        if (current.version != account.version - 1 && current.version != account.version) {
            throw IllegalStateException("Optimistic locking conflict on account '${account.portalAccountId}'")
        }
        val updated = account.copy(version = current.version + 1, updatedAt = System.currentTimeMillis())
        accounts[idx] = updated
        updated
    }

    override suspend fun listAccounts(
        projectId: String?,
        status: VendorPortalAccountStatus?,
        tenantId: String
    ): List<VendorPortalAccount> = synchronized(this) {
        accounts.filter {
            it.tenantId == tenantId &&
            (projectId == null || it.projectId == projectId) &&
            (status == null || it.status == status)
        }.sortedByDescending { it.createdAt }
    }

    // --- Memberships ---
    override suspend fun insertMembership(membership: VendorPortalMembership): VendorPortalMembership = synchronized(this) {
        val existing = memberships.find {
            it.tenantId == membership.tenantId &&
            it.vendorId == membership.vendorId &&
            it.userId == membership.userId
        }
        if (existing != null) {
            throw IllegalStateException("User '${membership.userId}' already has a membership for vendor '${membership.vendorId}'")
        }
        memberships.add(membership)
        membership
    }

    override suspend fun findMembershipById(membershipId: String, tenantId: String): VendorPortalMembership? = synchronized(this) {
        memberships.find { it.membershipId == membershipId && it.tenantId == tenantId }
    }

    override suspend fun findMembershipByVendorAndUser(
        vendorId: String,
        userId: String,
        tenantId: String
    ): VendorPortalMembership? = synchronized(this) {
        memberships.find { it.vendorId == vendorId && it.userId == userId && it.tenantId == tenantId }
    }

    override suspend fun findMembershipByToken(invitationToken: String, tenantId: String): VendorPortalMembership? = synchronized(this) {
        memberships.find { it.invitationToken == invitationToken && it.tenantId == tenantId }
    }

    override suspend fun updateMembership(membership: VendorPortalMembership): VendorPortalMembership = synchronized(this) {
        val idx = memberships.indexOfFirst { it.membershipId == membership.membershipId && it.tenantId == membership.tenantId }
        if (idx == -1) {
            throw NoSuchElementException("Vendor portal membership '${membership.membershipId}' not found")
        }
        val current = memberships[idx]
        if (current.version != membership.version - 1 && current.version != membership.version) {
            throw IllegalStateException("Optimistic locking conflict on membership '${membership.membershipId}'")
        }
        val updated = membership.copy(version = current.version + 1, updatedAt = System.currentTimeMillis())
        memberships[idx] = updated
        updated
    }

    override suspend fun listMemberships(
        vendorId: String?,
        userId: String?,
        status: VendorPortalMembershipStatus?,
        tenantId: String
    ): List<VendorPortalMembership> = synchronized(this) {
        memberships.filter {
            it.tenantId == tenantId &&
            (vendorId == null || it.vendorId == vendorId) &&
            (userId == null || it.userId == userId) &&
            (status == null || it.status == status)
        }.sortedByDescending { it.createdAt }
    }

    // --- Access Policies ---
    override suspend fun upsertPolicy(policy: VendorPortalAccessPolicy): VendorPortalAccessPolicy = synchronized(this) {
        val idx = policies.indexOfFirst {
            it.tenantId == policy.tenantId &&
            it.projectId == policy.projectId &&
            it.vendorId == policy.vendorId
        }
        if (idx != -1) {
            val current = policies[idx]
            val updated = policy.copy(policyId = current.policyId, version = current.version + 1, updatedAt = System.currentTimeMillis())
            policies[idx] = updated
            updated
        } else {
            policies.add(policy)
            policy
        }
    }

    override suspend fun findPolicyByVendorId(vendorId: String, tenantId: String): VendorPortalAccessPolicy? = synchronized(this) {
        policies.find { it.vendorId == vendorId && it.tenantId == tenantId }
    }

    override suspend fun findDefaultPolicy(projectId: String, tenantId: String): VendorPortalAccessPolicy? = synchronized(this) {
        policies.find { it.vendorId == null && it.projectId == projectId && it.tenantId == tenantId }
    }

    // --- Sessions ---
    override suspend fun insertSession(session: VendorPortalSession): VendorPortalSession = synchronized(this) {
        sessions.add(session)
        session
    }

    override suspend fun findSessionById(sessionId: String, tenantId: String): VendorPortalSession? = synchronized(this) {
        sessions.find { it.sessionId == sessionId && it.tenantId == tenantId }
    }

    override suspend fun findActiveSessionByToken(tokenHash: String, tenantId: String): VendorPortalSession? = synchronized(this) {
        sessions.find {
            it.sessionTokenHash == tokenHash &&
            it.tenantId == tenantId &&
            it.status == VendorPortalSessionStatus.ACTIVE &&
            it.expiresAt > System.currentTimeMillis()
        }
    }

    override suspend fun updateSessionStatus(
        sessionId: String,
        status: VendorPortalSessionStatus,
        tenantId: String
    ): Boolean = synchronized(this) {
        val idx = sessions.indexOfFirst { it.sessionId == sessionId && it.tenantId == tenantId }
        if (idx != -1) {
            sessions[idx] = sessions[idx].copy(status = status)
            true
        } else false
    }

    override suspend fun terminateUserSessions(userId: String, tenantId: String): Int = synchronized(this) {
        var count = 0
        for (i in sessions.indices) {
            if (sessions[i].userId == userId && sessions[i].tenantId == tenantId && sessions[i].status == VendorPortalSessionStatus.ACTIVE) {
                sessions[i] = sessions[i].copy(status = VendorPortalSessionStatus.TERMINATED)
                count++
            }
        }
        count
    }

    // --- Audit Events ---
    override suspend fun appendAuditEvent(event: VendorPortalAuditEvent): VendorPortalAuditEvent = synchronized(this) {
        auditEvents.add(event)
        event
    }

    override suspend fun listAuditEvents(
        vendorId: String?,
        actorUserId: String?,
        tenantId: String
    ): List<VendorPortalAuditEvent> = synchronized(this) {
        auditEvents.filter {
            it.tenantId == tenantId &&
            (vendorId == null || it.vendorId == vendorId) &&
            (actorUserId == null || it.actorUserId == actorUserId)
        }.sortedByDescending { it.timestamp }
    }
}

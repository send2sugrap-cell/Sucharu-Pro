package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorPortalDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.repository.VendorPortalRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe implementation of VendorPortalRepository (Module 13 Step 01).
 */
class VendorPortalRepositoryImpl(
    private val dataSource: VendorPortalDataSource
) : VendorPortalRepository {

    private val mutex = Mutex()

    // --- Accounts ---
    override suspend fun createAccount(account: VendorPortalAccount): DomainResult<VendorPortalAccount> = mutex.withLock {
        try {
            val saved = dataSource.insertAccount(account)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getAccountById(portalAccountId: String, tenantId: String): DomainResult<VendorPortalAccount?> = mutex.withLock {
        try {
            val account = dataSource.findAccountById(portalAccountId, tenantId)
            DomainResult.Success(account)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getAccountByVendorId(vendorId: String, tenantId: String): DomainResult<VendorPortalAccount?> = mutex.withLock {
        try {
            val account = dataSource.findAccountByVendorId(vendorId, tenantId)
            DomainResult.Success(account)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getAccountByCode(portalCode: String, tenantId: String): DomainResult<VendorPortalAccount?> = mutex.withLock {
        try {
            val account = dataSource.findAccountByCode(portalCode, tenantId)
            DomainResult.Success(account)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun updateAccount(account: VendorPortalAccount): DomainResult<VendorPortalAccount> = mutex.withLock {
        try {
            val updated = dataSource.updateAccount(account)
            DomainResult.Success(updated)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listAccounts(
        projectId: String?,
        status: VendorPortalAccountStatus?,
        tenantId: String
    ): DomainResult<List<VendorPortalAccount>> = mutex.withLock {
        try {
            val list = dataSource.listAccounts(projectId, status, tenantId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    // --- Memberships ---
    override suspend fun createMembership(membership: VendorPortalMembership): DomainResult<VendorPortalMembership> = mutex.withLock {
        try {
            val saved = dataSource.insertMembership(membership)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getMembershipById(membershipId: String, tenantId: String): DomainResult<VendorPortalMembership?> = mutex.withLock {
        try {
            val membership = dataSource.findMembershipById(membershipId, tenantId)
            DomainResult.Success(membership)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getMembershipByVendorAndUser(
        vendorId: String,
        userId: String,
        tenantId: String
    ): DomainResult<VendorPortalMembership?> = mutex.withLock {
        try {
            val membership = dataSource.findMembershipByVendorAndUser(vendorId, userId, tenantId)
            DomainResult.Success(membership)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getMembershipByToken(invitationToken: String, tenantId: String): DomainResult<VendorPortalMembership?> = mutex.withLock {
        try {
            val membership = dataSource.findMembershipByToken(invitationToken, tenantId)
            DomainResult.Success(membership)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun updateMembership(membership: VendorPortalMembership): DomainResult<VendorPortalMembership> = mutex.withLock {
        try {
            val updated = dataSource.updateMembership(membership)
            DomainResult.Success(updated)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listMemberships(
        vendorId: String?,
        userId: String?,
        status: VendorPortalMembershipStatus?,
        tenantId: String
    ): DomainResult<List<VendorPortalMembership>> = mutex.withLock {
        try {
            val list = dataSource.listMemberships(vendorId, userId, status, tenantId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    // --- Access Policies ---
    override suspend fun savePolicy(policy: VendorPortalAccessPolicy): DomainResult<VendorPortalAccessPolicy> = mutex.withLock {
        try {
            val saved = dataSource.upsertPolicy(policy)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getPolicyByVendorId(vendorId: String, tenantId: String): DomainResult<VendorPortalAccessPolicy?> = mutex.withLock {
        try {
            val policy = dataSource.findPolicyByVendorId(vendorId, tenantId)
            DomainResult.Success(policy)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getDefaultPolicy(projectId: String, tenantId: String): DomainResult<VendorPortalAccessPolicy?> = mutex.withLock {
        try {
            val policy = dataSource.findDefaultPolicy(projectId, tenantId)
            DomainResult.Success(policy)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    // --- Sessions ---
    override suspend fun createSession(session: VendorPortalSession): DomainResult<VendorPortalSession> = mutex.withLock {
        try {
            val saved = dataSource.insertSession(session)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getSessionById(sessionId: String, tenantId: String): DomainResult<VendorPortalSession?> = mutex.withLock {
        try {
            val session = dataSource.findSessionById(sessionId, tenantId)
            DomainResult.Success(session)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun getActiveSessionByToken(tokenHash: String, tenantId: String): DomainResult<VendorPortalSession?> = mutex.withLock {
        try {
            val session = dataSource.findActiveSessionByToken(tokenHash, tenantId)
            DomainResult.Success(session)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun revokeSession(sessionId: String, tenantId: String): DomainResult<Boolean> = mutex.withLock {
        try {
            val ok = dataSource.updateSessionStatus(sessionId, VendorPortalSessionStatus.REVOKED, tenantId)
            DomainResult.Success(ok)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun terminateUserSessions(userId: String, tenantId: String): DomainResult<Int> = mutex.withLock {
        try {
            val count = dataSource.terminateUserSessions(userId, tenantId)
            DomainResult.Success(count)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    // --- Audit Events ---
    override suspend fun recordAuditEvent(event: VendorPortalAuditEvent): DomainResult<VendorPortalAuditEvent> = mutex.withLock {
        try {
            val saved = dataSource.appendAuditEvent(event)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }

    override suspend fun listAuditEvents(
        vendorId: String?,
        actorUserId: String?,
        tenantId: String
    ): DomainResult<List<VendorPortalAuditEvent>> = mutex.withLock {
        try {
            val list = dataSource.listAuditEvents(vendorId, actorUserId, tenantId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(e)
        }
    }
}

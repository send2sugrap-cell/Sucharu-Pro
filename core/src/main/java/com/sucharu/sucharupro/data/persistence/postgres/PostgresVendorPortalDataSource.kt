package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.VendorPortalDataSource
import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.sql.ResultSet

/**
 * PostgreSQL JDBC implementation of VendorPortalDataSource with RLS (Module 13 Step 01).
 */
class PostgresVendorPortalDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : VendorPortalDataSource {

    private fun mapAccountRow(rs: ResultSet): VendorPortalAccount {
        val actAt = rs.getLong("activated_at")
        val susAt = rs.getLong("suspended_at")
        return VendorPortalAccount(
            portalAccountId = rs.getString("portal_account_id"),
            vendorId = rs.getString("vendor_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            projectId = rs.getString("project_id"),
            status = VendorPortalAccountStatus.valueOf(rs.getString("status")),
            portalCode = rs.getString("portal_code"),
            primaryContactEmail = rs.getString("primary_contact_email"),
            primaryContactPhone = rs.getString("primary_contact_phone"),
            activatedAt = if (rs.wasNull() || actAt == 0L) null else actAt,
            activatedBy = rs.getString("activated_by"),
            suspendedAt = if (rs.wasNull() || susAt == 0L) null else susAt,
            suspendedBy = rs.getString("suspended_by"),
            suspensionReason = rs.getString("suspension_reason"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by") ?: "system",
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by") ?: "system",
            version = rs.getLong("version")
        )
    }

    private fun mapMembershipRow(rs: ResultSet): VendorPortalMembership {
        val expAt = rs.getLong("invitation_expires_at")
        val actAt = rs.getLong("activated_at")
        val lastAcc = rs.getLong("last_access_at")
        return VendorPortalMembership(
            membershipId = rs.getString("membership_id"),
            portalAccountId = rs.getString("portal_account_id"),
            vendorId = rs.getString("vendor_id"),
            userId = rs.getString("user_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            projectScope = rs.getString("project_scope") ?: "*",
            role = VendorPortalRole.valueOf(rs.getString("role")),
            status = VendorPortalMembershipStatus.valueOf(rs.getString("status")),
            invitationToken = rs.getString("invitation_token"),
            invitationExpiresAt = if (rs.wasNull() || expAt == 0L) null else expAt,
            activatedAt = if (rs.wasNull() || actAt == 0L) null else actAt,
            lastAccessAt = if (rs.wasNull() || lastAcc == 0L) null else lastAcc,
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by") ?: "system",
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by") ?: "system",
            version = rs.getLong("version")
        )
    }

    private fun mapPolicyRow(rs: ResultSet): VendorPortalAccessPolicy {
        return VendorPortalAccessPolicy(
            policyId = rs.getString("policy_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            allowRfqSubmission = rs.getBoolean("allow_rfq_submission"),
            allowPoAcknowledgement = rs.getBoolean("allow_po_acknowledgement"),
            allowInvoiceSubmission = rs.getBoolean("allow_invoice_submission"),
            allowQualityDispute = rs.getBoolean("allow_quality_dispute"),
            requireTwoFactorAuth = rs.getBoolean("require_two_factor_auth"),
            ipWhitelist = rs.getString("ip_whitelist"),
            sessionInactivityTimeoutMinutes = rs.getInt("session_inactivity_timeout_minutes"),
            maxActiveSessionsPerUser = rs.getInt("max_active_sessions_per_user"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by") ?: "system",
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by") ?: "system",
            version = rs.getLong("version")
        )
    }

    private fun mapSessionRow(rs: ResultSet): VendorPortalSession {
        return VendorPortalSession(
            sessionId = rs.getString("session_id"),
            membershipId = rs.getString("membership_id"),
            userId = rs.getString("user_id"),
            vendorId = rs.getString("vendor_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            projectId = rs.getString("project_id"),
            sessionTokenHash = rs.getString("session_token_hash"),
            ipAddress = rs.getString("ip_address"),
            userAgent = rs.getString("user_agent"),
            status = VendorPortalSessionStatus.valueOf(rs.getString("status")),
            expiresAt = rs.getLong("expires_at"),
            lastActivityAt = rs.getLong("last_activity_at"),
            createdAt = rs.getLong("created_at")
        )
    }

    private fun mapAuditRow(rs: ResultSet): VendorPortalAuditEvent {
        return VendorPortalAuditEvent(
            eventId = rs.getString("event_id"),
            tenantId = rs.getString("tenant_id") ?: defaultTenantId,
            projectId = rs.getString("project_id"),
            vendorId = rs.getString("vendor_id"),
            membershipId = rs.getString("membership_id"),
            actorUserId = rs.getString("actor_user_id"),
            eventType = VendorPortalAuditEventType.valueOf(rs.getString("event_type")),
            action = rs.getString("action"),
            targetId = rs.getString("target_id"),
            result = rs.getString("result") ?: "SUCCESS",
            details = rs.getString("details") ?: "",
            ipAddress = rs.getString("ip_address"),
            correlationId = rs.getString("correlation_id"),
            timestamp = rs.getLong("timestamp")
        )
    }

    // --- Accounts ---
    override suspend fun insertAccount(account: VendorPortalAccount): VendorPortalAccount {
        val tenant = TenantContext(account.projectId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_accounts (
                    portal_account_id, vendor_id, tenant_id, project_id, status,
                    portal_code, primary_contact_email, primary_contact_phone,
                    activated_at, activated_by, suspended_at, suspended_by, suspension_reason,
                    created_at, created_by, updated_at, updated_by, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, account.portalAccountId)
                stmt.setString(2, account.vendorId)
                stmt.setString(3, account.tenantId)
                stmt.setString(4, account.projectId)
                stmt.setString(5, account.status.name)
                stmt.setString(6, account.portalCode)
                stmt.setString(7, account.primaryContactEmail)
                stmt.setString(8, account.primaryContactPhone)
                if (account.activatedAt != null) stmt.setLong(9, account.activatedAt) else stmt.setNull(9, java.sql.Types.BIGINT)
                stmt.setString(10, account.activatedBy)
                if (account.suspendedAt != null) stmt.setLong(11, account.suspendedAt) else stmt.setNull(11, java.sql.Types.BIGINT)
                stmt.setString(12, account.suspendedBy)
                stmt.setString(13, account.suspensionReason)
                stmt.setLong(14, account.createdAt)
                stmt.setString(15, account.createdBy)
                stmt.setLong(16, account.updatedAt)
                stmt.setString(17, account.updatedBy)
                stmt.setLong(18, account.version)
                stmt.executeUpdate()
            }
            account
        }
    }

    override suspend fun findAccountById(portalAccountId: String, tenantId: String): VendorPortalAccount? {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = "SELECT * FROM vendor_portal_accounts WHERE portal_account_id = ? AND tenant_id = ?"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, portalAccountId)
                stmt.setString(2, tenantId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapAccountRow(rs) else null
                }
            }
        }
    }

    override suspend fun findAccountByVendorId(vendorId: String, tenantId: String): VendorPortalAccount? {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = "SELECT * FROM vendor_portal_accounts WHERE vendor_id = ? AND tenant_id = ?"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, vendorId)
                stmt.setString(2, tenantId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapAccountRow(rs) else null
                }
            }
        }
    }

    override suspend fun findAccountByCode(portalCode: String, tenantId: String): VendorPortalAccount? {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = "SELECT * FROM vendor_portal_accounts WHERE portal_code = ? AND tenant_id = ?"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, portalCode)
                stmt.setString(2, tenantId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapAccountRow(rs) else null
                }
            }
        }
    }

    override suspend fun updateAccount(account: VendorPortalAccount): VendorPortalAccount {
        val tenant = TenantContext(account.projectId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                UPDATE vendor_portal_accounts SET
                    status = ?, portal_code = ?, primary_contact_email = ?, primary_contact_phone = ?,
                    activated_at = ?, activated_by = ?, suspended_at = ?, suspended_by = ?,
                    suspension_reason = ?, updated_at = ?, updated_by = ?, version = ?
                WHERE portal_account_id = ? AND tenant_id = ? AND version = ?
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, account.status.name)
                stmt.setString(2, account.portalCode)
                stmt.setString(3, account.primaryContactEmail)
                stmt.setString(4, account.primaryContactPhone)
                if (account.activatedAt != null) stmt.setLong(5, account.activatedAt) else stmt.setNull(5, java.sql.Types.BIGINT)
                stmt.setString(6, account.activatedBy)
                if (account.suspendedAt != null) stmt.setLong(7, account.suspendedAt) else stmt.setNull(7, java.sql.Types.BIGINT)
                stmt.setString(8, account.suspendedBy)
                stmt.setString(9, account.suspensionReason)
                stmt.setLong(10, account.updatedAt)
                stmt.setString(11, account.updatedBy)
                stmt.setLong(12, account.version)
                stmt.setString(13, account.portalAccountId)
                stmt.setString(14, account.tenantId)
                stmt.setLong(15, account.version - 1)
                val updated = stmt.executeUpdate()
                if (updated == 0) {
                    throw IllegalStateException("Optimistic locking conflict on account '${account.portalAccountId}'")
                }
            }
            account
        }
    }

    override suspend fun listAccounts(
        projectId: String?,
        status: VendorPortalAccountStatus?,
        tenantId: String
    ): List<VendorPortalAccount> {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val query = StringBuilder("SELECT * FROM vendor_portal_accounts WHERE tenant_id = ?")
            val params = mutableListOf<Any>(tenantId)
            if (projectId != null) {
                query.append(" AND project_id = ?")
                params.add(projectId)
            }
            if (status != null) {
                query.append(" AND status = ?")
                params.add(status.name)
            }
            query.append(" ORDER BY created_at DESC")

            val list = mutableListOf<VendorPortalAccount>()
            ctx.connection.prepareStatement(query.toString()).use { stmt ->
                params.forEachIndexed { i, p -> stmt.setObject(i + 1, p) }
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(mapAccountRow(rs))
                    }
                }
            }
            list
        }
    }

    // --- Memberships ---
    override suspend fun insertMembership(membership: VendorPortalMembership): VendorPortalMembership {
        val tenant = TenantContext(membership.tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_memberships (
                    membership_id, portal_account_id, vendor_id, user_id, tenant_id,
                    project_scope, role, status, invitation_token, invitation_expires_at,
                    activated_at, last_access_at, created_at, created_by, updated_at, updated_by, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, membership.membershipId)
                stmt.setString(2, membership.portalAccountId)
                stmt.setString(3, membership.vendorId)
                stmt.setString(4, membership.userId)
                stmt.setString(5, membership.tenantId)
                stmt.setString(6, membership.projectScope)
                stmt.setString(7, membership.role.name)
                stmt.setString(8, membership.status.name)
                stmt.setString(9, membership.invitationToken)
                if (membership.invitationExpiresAt != null) stmt.setLong(10, membership.invitationExpiresAt) else stmt.setNull(10, java.sql.Types.BIGINT)
                if (membership.activatedAt != null) stmt.setLong(11, membership.activatedAt) else stmt.setNull(11, java.sql.Types.BIGINT)
                if (membership.lastAccessAt != null) stmt.setLong(12, membership.lastAccessAt) else stmt.setNull(12, java.sql.Types.BIGINT)
                stmt.setLong(13, membership.createdAt)
                stmt.setString(14, membership.createdBy)
                stmt.setLong(15, membership.updatedAt)
                stmt.setString(16, membership.updatedBy)
                stmt.setLong(17, membership.version)
                stmt.executeUpdate()
            }
            membership
        }
    }

    override suspend fun findMembershipById(membershipId: String, tenantId: String): VendorPortalMembership? {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = "SELECT * FROM vendor_portal_memberships WHERE membership_id = ? AND tenant_id = ?"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, membershipId)
                stmt.setString(2, tenantId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapMembershipRow(rs) else null
                }
            }
        }
    }

    override suspend fun findMembershipByVendorAndUser(
        vendorId: String,
        userId: String,
        tenantId: String
    ): VendorPortalMembership? {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = "SELECT * FROM vendor_portal_memberships WHERE vendor_id = ? AND user_id = ? AND tenant_id = ?"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, vendorId)
                stmt.setString(2, userId)
                stmt.setString(3, tenantId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapMembershipRow(rs) else null
                }
            }
        }
    }

    override suspend fun findMembershipByToken(invitationToken: String, tenantId: String): VendorPortalMembership? {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = "SELECT * FROM vendor_portal_memberships WHERE invitation_token = ? AND tenant_id = ?"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, invitationToken)
                stmt.setString(2, tenantId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapMembershipRow(rs) else null
                }
            }
        }
    }

    override suspend fun updateMembership(membership: VendorPortalMembership): VendorPortalMembership {
        val tenant = TenantContext(membership.tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                UPDATE vendor_portal_memberships SET
                    project_scope = ?, role = ?, status = ?, invitation_token = ?,
                    invitation_expires_at = ?, activated_at = ?, last_access_at = ?,
                    updated_at = ?, updated_by = ?, version = ?
                WHERE membership_id = ? AND tenant_id = ? AND version = ?
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, membership.projectScope)
                stmt.setString(2, membership.role.name)
                stmt.setString(3, membership.status.name)
                stmt.setString(4, membership.invitationToken)
                if (membership.invitationExpiresAt != null) stmt.setLong(5, membership.invitationExpiresAt) else stmt.setNull(5, java.sql.Types.BIGINT)
                if (membership.activatedAt != null) stmt.setLong(6, membership.activatedAt) else stmt.setNull(6, java.sql.Types.BIGINT)
                if (membership.lastAccessAt != null) stmt.setLong(7, membership.lastAccessAt) else stmt.setNull(7, java.sql.Types.BIGINT)
                stmt.setLong(8, membership.updatedAt)
                stmt.setString(9, membership.updatedBy)
                stmt.setLong(10, membership.version)
                stmt.setString(11, membership.membershipId)
                stmt.setString(12, membership.tenantId)
                stmt.setLong(13, membership.version - 1)
                val updated = stmt.executeUpdate()
                if (updated == 0) {
                    throw IllegalStateException("Optimistic locking conflict on membership '${membership.membershipId}'")
                }
            }
            membership
        }
    }

    override suspend fun listMemberships(
        vendorId: String?,
        userId: String?,
        status: VendorPortalMembershipStatus?,
        tenantId: String
    ): List<VendorPortalMembership> {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val query = StringBuilder("SELECT * FROM vendor_portal_memberships WHERE tenant_id = ?")
            val params = mutableListOf<Any>(tenantId)
            if (vendorId != null) {
                query.append(" AND vendor_id = ?")
                params.add(vendorId)
            }
            if (userId != null) {
                query.append(" AND user_id = ?")
                params.add(userId)
            }
            if (status != null) {
                query.append(" AND status = ?")
                params.add(status.name)
            }
            query.append(" ORDER BY created_at DESC")

            val list = mutableListOf<VendorPortalMembership>()
            ctx.connection.prepareStatement(query.toString()).use { stmt ->
                params.forEachIndexed { i, p -> stmt.setObject(i + 1, p) }
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(mapMembershipRow(rs))
                    }
                }
            }
            list
        }
    }

    // --- Access Policies ---
    override suspend fun upsertPolicy(policy: VendorPortalAccessPolicy): VendorPortalAccessPolicy {
        val tenant = TenantContext(policy.projectId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_access_policies (
                    policy_id, tenant_id, project_id, vendor_id, allow_rfq_submission,
                    allow_po_acknowledgement, allow_invoice_submission, allow_quality_dispute,
                    require_two_factor_auth, ip_whitelist, session_inactivity_timeout_minutes,
                    max_active_sessions_per_user, created_at, created_by, updated_at, updated_by, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, project_id, vendor_id) DO UPDATE SET
                    allow_rfq_submission = EXCLUDED.allow_rfq_submission,
                    allow_po_acknowledgement = EXCLUDED.allow_po_acknowledgement,
                    allow_invoice_submission = EXCLUDED.allow_invoice_submission,
                    allow_quality_dispute = EXCLUDED.allow_quality_dispute,
                    require_two_factor_auth = EXCLUDED.require_two_factor_auth,
                    ip_whitelist = EXCLUDED.ip_whitelist,
                    session_inactivity_timeout_minutes = EXCLUDED.session_inactivity_timeout_minutes,
                    max_active_sessions_per_user = EXCLUDED.max_active_sessions_per_user,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    version = vendor_portal_access_policies.version + 1
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, policy.policyId)
                stmt.setString(2, policy.tenantId)
                stmt.setString(3, policy.projectId)
                stmt.setString(4, policy.vendorId)
                stmt.setBoolean(5, policy.allowRfqSubmission)
                stmt.setBoolean(6, policy.allowPoAcknowledgement)
                stmt.setBoolean(7, policy.allowInvoiceSubmission)
                stmt.setBoolean(8, policy.allowQualityDispute)
                stmt.setBoolean(9, policy.requireTwoFactorAuth)
                stmt.setString(10, policy.ipWhitelist)
                stmt.setInt(11, policy.sessionInactivityTimeoutMinutes)
                stmt.setInt(12, policy.maxActiveSessionsPerUser)
                stmt.setLong(13, policy.createdAt)
                stmt.setString(14, policy.createdBy)
                stmt.setLong(15, policy.updatedAt)
                stmt.setString(16, policy.updatedBy)
                stmt.setLong(17, policy.version)
                stmt.executeUpdate()
            }
            policy
        }
    }

    override suspend fun findPolicyByVendorId(vendorId: String, tenantId: String): VendorPortalAccessPolicy? {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = "SELECT * FROM vendor_portal_access_policies WHERE vendor_id = ? AND tenant_id = ?"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, vendorId)
                stmt.setString(2, tenantId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapPolicyRow(rs) else null
                }
            }
        }
    }

    override suspend fun findDefaultPolicy(projectId: String, tenantId: String): VendorPortalAccessPolicy? {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = "SELECT * FROM vendor_portal_access_policies WHERE vendor_id IS NULL AND project_id = ? AND tenant_id = ?"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, projectId)
                stmt.setString(2, tenantId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapPolicyRow(rs) else null
                }
            }
        }
    }

    // --- Sessions ---
    override suspend fun insertSession(session: VendorPortalSession): VendorPortalSession {
        val tenant = TenantContext(session.projectId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_sessions (
                    session_id, membership_id, user_id, vendor_id, tenant_id,
                    project_id, session_token_hash, ip_address, user_agent,
                    status, expires_at, last_activity_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, session.sessionId)
                stmt.setString(2, session.membershipId)
                stmt.setString(3, session.userId)
                stmt.setString(4, session.vendorId)
                stmt.setString(5, session.tenantId)
                stmt.setString(6, session.projectId)
                stmt.setString(7, session.sessionTokenHash)
                stmt.setString(8, session.ipAddress)
                stmt.setString(9, session.userAgent)
                stmt.setString(10, session.status.name)
                stmt.setLong(11, session.expiresAt)
                stmt.setLong(12, session.lastActivityAt)
                stmt.setLong(13, session.createdAt)
                stmt.executeUpdate()
            }
            session
        }
    }

    override suspend fun findSessionById(sessionId: String, tenantId: String): VendorPortalSession? {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = "SELECT * FROM vendor_portal_sessions WHERE session_id = ? AND tenant_id = ?"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, sessionId)
                stmt.setString(2, tenantId)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapSessionRow(rs) else null
                }
            }
        }
    }

    override suspend fun findActiveSessionByToken(tokenHash: String, tenantId: String): VendorPortalSession? {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        val now = System.currentTimeMillis()
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = "SELECT * FROM vendor_portal_sessions WHERE session_token_hash = ? AND tenant_id = ? AND status = 'ACTIVE' AND expires_at > ?"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tokenHash)
                stmt.setString(2, tenantId)
                stmt.setLong(3, now)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) mapSessionRow(rs) else null
                }
            }
        }
    }

    override suspend fun updateSessionStatus(
        sessionId: String,
        status: VendorPortalSessionStatus,
        tenantId: String
    ): Boolean {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = "UPDATE vendor_portal_sessions SET status = ? WHERE session_id = ? AND tenant_id = ?"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, status.name)
                stmt.setString(2, sessionId)
                stmt.setString(3, tenantId)
                stmt.executeUpdate() > 0
            }
        }
    }

    override suspend fun terminateUserSessions(userId: String, tenantId: String): Int {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = "UPDATE vendor_portal_sessions SET status = 'TERMINATED' WHERE user_id = ? AND tenant_id = ? AND status = 'ACTIVE'"
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, userId)
                stmt.setString(2, tenantId)
                stmt.executeUpdate()
            }
        }
    }

    // --- Audit Events ---
    override suspend fun appendAuditEvent(event: VendorPortalAuditEvent): VendorPortalAuditEvent {
        val tenant = TenantContext(event.projectId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val sql = """
                INSERT INTO vendor_portal_audit_events (
                    event_id, tenant_id, project_id, vendor_id, membership_id,
                    actor_user_id, event_type, action, target_id, result,
                    details, ip_address, correlation_id, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            ctx.connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, event.eventId)
                stmt.setString(2, event.tenantId)
                stmt.setString(3, event.projectId)
                stmt.setString(4, event.vendorId)
                stmt.setString(5, event.membershipId)
                stmt.setString(6, event.actorUserId)
                stmt.setString(7, event.eventType.name)
                stmt.setString(8, event.action)
                stmt.setString(9, event.targetId)
                stmt.setString(10, event.result)
                stmt.setString(11, event.details)
                stmt.setString(12, event.ipAddress)
                stmt.setString(13, event.correlationId)
                stmt.setLong(14, event.timestamp)
                stmt.executeUpdate()
            }
            event
        }
    }

    override suspend fun listAuditEvents(
        vendorId: String?,
        actorUserId: String?,
        tenantId: String
    ): List<VendorPortalAuditEvent> {
        val tenant = TenantContext(tenantId.ifBlank { defaultTenantId })
        return transactionManager.inTransaction(tenant) { ctx ->
            val query = StringBuilder("SELECT * FROM vendor_portal_audit_events WHERE tenant_id = ?")
            val params = mutableListOf<Any>(tenantId)
            if (vendorId != null) {
                query.append(" AND vendor_id = ?")
                params.add(vendorId)
            }
            if (actorUserId != null) {
                query.append(" AND actor_user_id = ?")
                params.add(actorUserId)
            }
            query.append(" ORDER BY timestamp DESC")

            val list = mutableListOf<VendorPortalAuditEvent>()
            ctx.connection.prepareStatement(query.toString()).use { stmt ->
                params.forEachIndexed { i, p -> stmt.setObject(i + 1, p) }
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(mapAuditRow(rs))
                    }
                }
            }
            list
        }
    }
}

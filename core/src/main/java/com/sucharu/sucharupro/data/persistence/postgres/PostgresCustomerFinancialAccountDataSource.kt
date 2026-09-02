package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.CustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountAuditEvent
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import java.sql.ResultSet

/**
 * Production-grade PostgreSQL DataSource for Customer Financial Accounts.
 */
class PostgresCustomerFinancialAccountDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : CustomerFinancialAccountDataSource {

    private fun mapAccount(rs: ResultSet): CustomerFinancialAccount {
        return CustomerFinancialAccount(
            financialAccountId = rs.getString("financial_account_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            accountNumber = rs.getString("account_number"),
            currency = rs.getString("currency") ?: "BDT",
            status = rs.getEnumByName("status", CustomerFinancialAccountStatus.ACTIVE),
            suspensionReason = rs.getString("suspension_reason"),
            closedReason = rs.getString("closed_reason"),
            notes = rs.getString("notes"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by") ?: "system",
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by") ?: "system",
            version = rs.getLong("version")
        )
    }

    private fun mapAuditEvent(rs: ResultSet): CustomerFinancialAccountAuditEvent {
        return CustomerFinancialAccountAuditEvent(
            auditId = rs.getString("audit_id"),
            financialAccountId = rs.getString("financial_account_id"),
            customerId = rs.getString("customer_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            action = rs.getString("action"),
            previousStatus = rs.getString("previous_status")?.let { CustomerFinancialAccountStatus.valueOf(it) },
            newStatus = rs.getString("new_status")?.let { CustomerFinancialAccountStatus.valueOf(it) },
            reason = rs.getString("reason"),
            occurredAt = rs.getLong("occurred_at"),
            metadataJson = rs.getString("metadata_json")
        )
    }

    override suspend fun insertAccount(account: CustomerFinancialAccount): DomainResult<CustomerFinancialAccount> {
        val tenantContext = TenantContext(projectId = account.projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    INSERT INTO customer_financial_accounts (
                        financial_account_id, tenant_id, project_id, customer_id, account_number,
                        currency, status, suspension_reason, closed_reason, notes,
                        created_at, created_by, updated_at, updated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, account.financialAccountId)
                    stmt.setString(2, account.tenantId)
                    stmt.setString(3, account.projectId)
                    stmt.setString(4, account.customerId)
                    stmt.setString(5, account.accountNumber)
                    stmt.setString(6, account.currency)
                    stmt.setString(7, account.status.name)
                    stmt.setString(8, account.suspensionReason)
                    stmt.setString(9, account.closedReason)
                    stmt.setString(10, account.notes)
                    stmt.setLong(11, account.createdAt)
                    stmt.setString(12, account.createdBy)
                    stmt.setLong(13, account.updatedAt)
                    stmt.setString(14, account.updatedBy)
                    stmt.setLong(15, account.version)
                    stmt.executeUpdate()
                }
                DomainResult.Success(account)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to insert customer financial account")
        }
    }

    override suspend fun findAccountById(
        tenantId: String,
        projectId: String,
        financialAccountId: String
    ): DomainResult<CustomerFinancialAccount> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_financial_accounts WHERE tenant_id = ? AND project_id = ? AND financial_account_id = ?"
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, financialAccountId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        DomainResult.Success(mapAccount(rs))
                    } else {
                        DomainResult.Error(NoSuchElementException("CustomerFinancialAccount '$financialAccountId' not found"))
                    }
                }
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to find account by ID")
        }
    }

    override suspend fun findAccountByCustomerId(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerFinancialAccount> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_financial_accounts WHERE tenant_id = ? AND project_id = ? AND customer_id = ?"
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, customerId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        DomainResult.Success(mapAccount(rs))
                    } else {
                        DomainResult.Error(NoSuchElementException("CustomerFinancialAccount for customer '$customerId' not found"))
                    }
                }
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to find account by customer ID")
        }
    }

    override suspend fun listAccounts(
        tenantId: String,
        projectId: String,
        status: CustomerFinancialAccountStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerFinancialAccount>> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val baseSql = StringBuilder("SELECT * FROM customer_financial_accounts WHERE tenant_id = ? AND project_id = ?")
                if (status != null) {
                    baseSql.append(" AND status = ?")
                }
                baseSql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?")
                tx.connection.prepareStatement(baseSql.toString()).use { stmt ->
                    var idx = 1
                    stmt.setString(idx++, tenantId)
                    stmt.setString(idx++, projectId)
                    if (status != null) {
                        stmt.setString(idx++, status.name)
                    }
                    stmt.setInt(idx++, limit)
                    stmt.setInt(idx++, offset)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<CustomerFinancialAccount>()
                    while (rs.next()) {
                        list.add(mapAccount(rs))
                    }
                    DomainResult.Success(list)
                }
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to list customer financial accounts")
        }
    }

    override suspend fun updateStatus(
        tenantId: String,
        projectId: String,
        financialAccountId: String,
        newStatus: CustomerFinancialAccountStatus,
        reason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerFinancialAccount> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val now = System.currentTimeMillis()
                val sql = """
                    UPDATE customer_financial_accounts
                    SET status = ?,
                        suspension_reason = CASE WHEN ? = 'SUSPENDED' THEN ? ELSE suspension_reason END,
                        closed_reason = CASE WHEN ? = 'CLOSED' THEN ? ELSE closed_reason END,
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE tenant_id = ? AND project_id = ? AND financial_account_id = ? AND version = ?
                """.trimIndent()
                val rows = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, newStatus.name)
                    stmt.setString(2, newStatus.name)
                    stmt.setString(3, reason)
                    stmt.setString(4, newStatus.name)
                    stmt.setString(5, reason)
                    stmt.setLong(6, now)
                    stmt.setString(7, actorId)
                    stmt.setString(8, tenantId)
                    stmt.setString(9, projectId)
                    stmt.setString(10, financialAccountId)
                    stmt.setLong(11, expectedVersion)
                    stmt.executeUpdate()
                }
                if (rows == 0) {
                    return@inTransaction DomainResult.Error(
                        IllegalStateException("Update failed for account '$financialAccountId'. It may not exist or version conflict occurred.")
                    )
                }
                // Fetch updated
                findAccountById(tenantId, projectId, financialAccountId)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to update account status")
        }
    }

    override suspend fun updateNotes(
        tenantId: String,
        projectId: String,
        financialAccountId: String,
        notes: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerFinancialAccount> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val now = System.currentTimeMillis()
                val sql = """
                    UPDATE customer_financial_accounts
                    SET notes = ?,
                        updated_at = ?,
                        updated_by = ?,
                        version = version + 1
                    WHERE tenant_id = ? AND project_id = ? AND financial_account_id = ? AND version = ?
                """.trimIndent()
                val rows = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, notes)
                    stmt.setLong(2, now)
                    stmt.setString(3, actorId)
                    stmt.setString(4, tenantId)
                    stmt.setString(5, projectId)
                    stmt.setString(6, financialAccountId)
                    stmt.setLong(7, expectedVersion)
                    stmt.executeUpdate()
                }
                if (rows == 0) {
                    return@inTransaction DomainResult.Error(
                        IllegalStateException("Update notes failed for account '$financialAccountId'. Version conflict or not found.")
                    )
                }
                findAccountById(tenantId, projectId, financialAccountId)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to update account notes")
        }
    }

    override suspend fun insertAuditEvent(event: CustomerFinancialAccountAuditEvent): DomainResult<CustomerFinancialAccountAuditEvent> {
        val tenantContext = TenantContext(projectId = event.projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    INSERT INTO customer_financial_account_audit_events (
                        audit_id, financial_account_id, customer_id, tenant_id, project_id,
                        actor_id, actor_role, action, previous_status, new_status,
                        reason, occurred_at, metadata_json
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, event.auditId)
                    stmt.setString(2, event.financialAccountId)
                    stmt.setString(3, event.customerId)
                    stmt.setString(4, event.tenantId)
                    stmt.setString(5, event.projectId)
                    stmt.setString(6, event.actorId)
                    stmt.setString(7, event.actorRole)
                    stmt.setString(8, event.action)
                    stmt.setString(9, event.previousStatus?.name)
                    stmt.setString(10, event.newStatus?.name)
                    stmt.setString(11, event.reason)
                    stmt.setLong(12, event.occurredAt)
                    stmt.setString(13, event.metadataJson)
                    stmt.executeUpdate()
                }
                DomainResult.Success(event)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to record audit event")
        }
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        financialAccountId: String
    ): DomainResult<List<CustomerFinancialAccountAuditEvent>> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = """
                    SELECT * FROM customer_financial_account_audit_events
                    WHERE tenant_id = ? AND project_id = ? AND financial_account_id = ?
                    ORDER BY occurred_at DESC
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, financialAccountId)
                    val rs = stmt.executeQuery()
                    val list = mutableListOf<CustomerFinancialAccountAuditEvent>()
                    while (rs.next()) {
                        list.add(mapAuditEvent(rs))
                    }
                    DomainResult.Success(list)
                }
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to get audit events")
        }
    }
}

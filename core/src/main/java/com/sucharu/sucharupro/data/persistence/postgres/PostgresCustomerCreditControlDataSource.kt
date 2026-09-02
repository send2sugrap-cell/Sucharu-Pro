package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.customercreditcontrol.CustomerCreditControlDataSource
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditControlAuditEvent
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditProfileEntity
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerPaymentTermsType
import java.math.BigDecimal
import java.sql.ResultSet

/**
 * Production PostgreSQL DataSource for Customer Credit Profiles (Module 14 Step 07).
 */
class PostgresCustomerCreditControlDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : CustomerCreditControlDataSource {

    private fun mapProfile(rs: ResultSet): CustomerCreditProfileEntity {
        return CustomerCreditProfileEntity(
            profileId = rs.getString("profile_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            creditLimit = rs.getBigDecimal("credit_limit") ?: BigDecimal.ZERO,
            currency = rs.getString("currency") ?: "BDT",
            paymentTermsType = rs.getEnumByName("payment_terms_type", CustomerPaymentTermsType.DUE_ON_RECEIPT),
            creditDays = rs.getInt("credit_days"),
            requiresAdvance = rs.getBoolean("requires_advance"),
            financialHold = rs.getBoolean("financial_hold"),
            holdReason = rs.getString("hold_reason"),
            holdPlacedAt = rs.getObject("hold_placed_at") as? Long,
            holdPlacedBy = rs.getString("hold_placed_by"),
            effectiveFrom = rs.getObject("effective_from") as? Long,
            effectiveUntil = rs.getObject("effective_until") as? Long,
            notes = rs.getString("notes"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by") ?: "system",
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by") ?: "system",
            version = rs.getLong("version")
        )
    }

    private fun mapAuditEvent(rs: ResultSet): CustomerCreditControlAuditEvent {
        return CustomerCreditControlAuditEvent(
            auditId = rs.getString("audit_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            action = rs.getString("action"),
            previousValueJson = rs.getString("previous_value_json"),
            newValueJson = rs.getString("new_value_json"),
            reason = rs.getString("reason"),
            occurredAt = rs.getLong("occurred_at"),
            metadataJson = rs.getString("metadata_json")
        )
    }

    override suspend fun saveProfile(profile: CustomerCreditProfileEntity): DomainResult<CustomerCreditProfileEntity> {
        val tenantContext = TenantContext(projectId = profile.projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val upsertSql = """
                    INSERT INTO customer_credit_profiles (
                        profile_id, tenant_id, project_id, customer_id, credit_limit,
                        currency, payment_terms_type, credit_days, requires_advance,
                        financial_hold, hold_reason, hold_placed_at, hold_placed_by,
                        effective_from, effective_until, notes, created_at, created_by,
                        updated_at, updated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (tenant_id, project_id, customer_id)
                    DO UPDATE SET
                        credit_limit = EXCLUDED.credit_limit,
                        currency = EXCLUDED.currency,
                        payment_terms_type = EXCLUDED.payment_terms_type,
                        credit_days = EXCLUDED.credit_days,
                        requires_advance = EXCLUDED.requires_advance,
                        financial_hold = EXCLUDED.financial_hold,
                        hold_reason = EXCLUDED.hold_reason,
                        hold_placed_at = EXCLUDED.hold_placed_at,
                        hold_placed_by = EXCLUDED.hold_placed_by,
                        effective_from = EXCLUDED.effective_from,
                        effective_until = EXCLUDED.effective_until,
                        notes = EXCLUDED.notes,
                        updated_at = EXCLUDED.updated_at,
                        updated_by = EXCLUDED.updated_by,
                        version = customer_credit_profiles.version + 1
                """.trimIndent()

                tx.connection.prepareStatement(upsertSql).use { stmt ->
                    stmt.setString(1, profile.profileId)
                    stmt.setString(2, profile.tenantId)
                    stmt.setString(3, profile.projectId)
                    stmt.setString(4, profile.customerId)
                    stmt.setBigDecimal(5, profile.creditLimit)
                    stmt.setString(6, profile.currency)
                    stmt.setString(7, profile.paymentTermsType.name)
                    stmt.setInt(8, profile.creditDays)
                    stmt.setBoolean(9, profile.requiresAdvance)
                    stmt.setBoolean(10, profile.financialHold)
                    stmt.setString(11, profile.holdReason)
                    stmt.setObject(12, profile.holdPlacedAt)
                    stmt.setString(13, profile.holdPlacedBy)
                    stmt.setObject(14, profile.effectiveFrom)
                    stmt.setObject(15, profile.effectiveUntil)
                    stmt.setString(16, profile.notes)
                    stmt.setLong(17, profile.createdAt)
                    stmt.setString(18, profile.createdBy)
                    stmt.setLong(19, profile.updatedAt)
                    stmt.setString(20, profile.updatedBy)
                    stmt.setLong(21, profile.version)
                    stmt.executeUpdate()
                }

                val selectSql = "SELECT * FROM customer_credit_profiles WHERE tenant_id = ? AND project_id = ? AND customer_id = ?"
                val saved = tx.connection.prepareStatement(selectSql).use { stmt ->
                    stmt.setString(1, profile.tenantId)
                    stmt.setString(2, profile.projectId)
                    stmt.setString(3, profile.customerId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapProfile(rs) else profile
                }
                DomainResult.Success(saved)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to save customer credit profile")
        }
    }

    override suspend fun getProfileByCustomerId(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerCreditProfileEntity?> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = "SELECT * FROM customer_credit_profiles WHERE tenant_id = ? AND project_id = ? AND customer_id = ?"
                val profile = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, customerId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapProfile(rs) else null
                }
                DomainResult.Success(profile)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to fetch customer credit profile")
        }
    }

    override suspend fun listProfiles(
        tenantId: String,
        projectId: String,
        financialHold: Boolean?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerCreditProfileEntity>> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
                val params = mutableListOf<Any>(tenantId, projectId)

                if (financialHold != null) {
                    conditions.add("financial_hold = ?")
                    params.add(financialHold)
                }

                val sql = """
                    SELECT * FROM customer_credit_profiles
                    WHERE ${conditions.joinToString(" AND ")}
                    ORDER BY updated_at DESC
                    LIMIT ? OFFSET ?
                """.trimIndent()

                val list = tx.connection.prepareStatement(sql).use { stmt ->
                    var idx = 1
                    for (p in params) {
                        when (p) {
                            is String -> stmt.setString(idx++, p)
                            is Boolean -> stmt.setBoolean(idx++, p)
                        }
                    }
                    stmt.setInt(idx++, limit)
                    stmt.setInt(idx, offset)

                    val resultList = mutableListOf<CustomerCreditProfileEntity>()
                    val rs = stmt.executeQuery()
                    while (rs.next()) {
                        resultList.add(mapProfile(rs))
                    }
                    resultList
                }
                DomainResult.Success(list)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to list customer credit profiles")
        }
    }

    override suspend fun recordAuditEvent(event: CustomerCreditControlAuditEvent): DomainResult<CustomerCreditControlAuditEvent> {
        val tenantContext = TenantContext(projectId = event.projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    INSERT INTO customer_credit_control_audit_events (
                        audit_id, tenant_id, project_id, customer_id, actor_id,
                        actor_role, action, previous_value_json, new_value_json,
                        reason, occurred_at, metadata_json
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, event.auditId)
                    stmt.setString(2, event.tenantId)
                    stmt.setString(3, event.projectId)
                    stmt.setString(4, event.customerId)
                    stmt.setString(5, event.actorId)
                    stmt.setString(6, event.actorRole)
                    stmt.setString(7, event.action)
                    stmt.setString(8, event.previousValueJson)
                    stmt.setString(9, event.newValueJson)
                    stmt.setString(10, event.reason)
                    stmt.setLong(11, event.occurredAt)
                    stmt.setString(12, event.metadataJson)
                    stmt.executeUpdate()
                }
                DomainResult.Success(event)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to record credit control audit event")
        }
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<List<CustomerCreditControlAuditEvent>> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = """
                    SELECT * FROM customer_credit_control_audit_events
                    WHERE tenant_id = ? AND project_id = ? AND customer_id = ?
                    ORDER BY occurred_at DESC
                """.trimIndent()
                val list = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, customerId)
                    val rs = stmt.executeQuery()
                    val resultList = mutableListOf<CustomerCreditControlAuditEvent>()
                    while (rs.next()) {
                        resultList.add(mapAuditEvent(rs))
                    }
                    resultList
                }
                DomainResult.Success(list)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to get credit control audit events")
        }
    }
}

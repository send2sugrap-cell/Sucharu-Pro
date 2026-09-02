package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.customercollection.CustomerCollectionDataSource
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercollection.*
import java.math.BigDecimal
import java.sql.ResultSet

/**
 * Production PostgreSQL DataSource for Customer Collection Management (Module 14 Step 08).
 */
class PostgresCustomerCollectionDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : CustomerCollectionDataSource {

    private fun mapAction(rs: ResultSet): CustomerCollectionAction {
        return CustomerCollectionAction(
            actionId = rs.getString("action_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            invoiceId = rs.getString("invoice_id"),
            actionType = rs.getEnumByName("action_type", CollectionActionType.REMINDER),
            priority = rs.getEnumByName("priority", CollectionPriority.NORMAL),
            status = rs.getEnumByName("status", CollectionActionStatus.SCHEDULED),
            scheduledAt = rs.getLong("scheduled_at"),
            performedAt = rs.getObject("performed_at") as? Long,
            nextFollowUpAt = rs.getObject("next_follow_up_at") as? Long,
            assignedUserId = rs.getString("assigned_user_id"),
            outcome = rs.getString("outcome")?.let {
                try { CollectionOutcomeType.valueOf(it) } catch (e: Exception) { null }
            },
            outcomeNotes = rs.getString("outcome_notes"),
            cancellationReason = rs.getString("cancellation_reason"),
            notes = rs.getString("notes"),
            idempotencyKey = rs.getString("idempotency_key"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by") ?: "system",
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by") ?: "system",
            version = rs.getLong("version")
        )
    }

    private fun mapPromise(rs: ResultSet): CustomerPaymentPromise {
        return CustomerPaymentPromise(
            promiseId = rs.getString("promise_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            invoiceId = rs.getString("invoice_id"),
            actionId = rs.getString("action_id"),
            promisedAmount = rs.getBigDecimal("promised_amount") ?: BigDecimal.ZERO,
            promisedDate = rs.getLong("promised_date"),
            status = rs.getEnumByName("status", PaymentPromiseStatus.PENDING),
            notes = rs.getString("notes"),
            fulfilledAt = rs.getObject("fulfilled_at") as? Long,
            fulfilledPaymentId = rs.getString("fulfilled_payment_id"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by") ?: "system",
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by") ?: "system",
            version = rs.getLong("version")
        )
    }

    private fun mapAuditEvent(rs: ResultSet): CustomerCollectionAuditEvent {
        return CustomerCollectionAuditEvent(
            auditId = rs.getString("audit_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            actionId = rs.getString("action_id"),
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

    override suspend fun saveAction(action: CustomerCollectionAction): DomainResult<CustomerCollectionAction> {
        val tenantContext = TenantContext(projectId = action.projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    INSERT INTO customer_collection_actions (
                        action_id, tenant_id, project_id, customer_id, invoice_id,
                        action_type, priority, status, scheduled_at, performed_at,
                        next_follow_up_at, assigned_user_id, outcome, outcome_notes,
                        cancellation_reason, notes, idempotency_key, created_at,
                        created_by, updated_at, updated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (action_id) DO UPDATE SET
                        action_type = EXCLUDED.action_type,
                        priority = EXCLUDED.priority,
                        status = EXCLUDED.status,
                        scheduled_at = EXCLUDED.scheduled_at,
                        performed_at = EXCLUDED.performed_at,
                        next_follow_up_at = EXCLUDED.next_follow_up_at,
                        assigned_user_id = EXCLUDED.assigned_user_id,
                        outcome = EXCLUDED.outcome,
                        outcome_notes = EXCLUDED.outcome_notes,
                        cancellation_reason = EXCLUDED.cancellation_reason,
                        notes = EXCLUDED.notes,
                        updated_at = EXCLUDED.updated_at,
                        updated_by = EXCLUDED.updated_by,
                        version = customer_collection_actions.version + 1
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, action.actionId)
                    stmt.setString(2, action.tenantId)
                    stmt.setString(3, action.projectId)
                    stmt.setString(4, action.customerId)
                    stmt.setString(5, action.invoiceId)
                    stmt.setString(6, action.actionType.name)
                    stmt.setString(7, action.priority.name)
                    stmt.setString(8, action.status.name)
                    stmt.setLong(9, action.scheduledAt)
                    stmt.setObject(10, action.performedAt)
                    stmt.setObject(11, action.nextFollowUpAt)
                    stmt.setString(12, action.assignedUserId)
                    stmt.setString(13, action.outcome?.name)
                    stmt.setString(14, action.outcomeNotes)
                    stmt.setString(15, action.cancellationReason)
                    stmt.setString(16, action.notes)
                    stmt.setString(17, action.idempotencyKey)
                    stmt.setLong(18, action.createdAt)
                    stmt.setString(19, action.createdBy)
                    stmt.setLong(20, action.updatedAt)
                    stmt.setString(21, action.updatedBy)
                    stmt.setLong(22, action.version)
                    stmt.executeUpdate()
                }
                DomainResult.Success(action)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to save collection action")
        }
    }

    override suspend fun getActionById(
        tenantId: String,
        projectId: String,
        actionId: String
    ): DomainResult<CustomerCollectionAction?> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = """
                    SELECT * FROM customer_collection_actions
                    WHERE tenant_id = ? AND project_id = ? AND action_id = ?
                """.trimIndent()
                val action = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, actionId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapAction(rs) else null
                }
                DomainResult.Success(action)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to get collection action")
        }
    }

    override suspend fun getActionByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerCollectionAction?> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = """
                    SELECT * FROM customer_collection_actions
                    WHERE tenant_id = ? AND project_id = ? AND idempotency_key = ?
                """.trimIndent()
                val action = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, idempotencyKey)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapAction(rs) else null
                }
                DomainResult.Success(action)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to get collection action by idempotency key")
        }
    }

    override suspend fun listActions(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CollectionActionStatus?,
        assignedUserId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerCollectionAction>> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
                val params = mutableListOf<Any>(tenantId, projectId)

                if (!customerId.isNullOrBlank()) {
                    conditions.add("customer_id = ?")
                    params.add(customerId)
                }
                if (status != null) {
                    conditions.add("status = ?")
                    params.add(status.name)
                }
                if (!assignedUserId.isNullOrBlank()) {
                    conditions.add("assigned_user_id = ?")
                    params.add(assignedUserId)
                }

                val whereClause = conditions.joinToString(" AND ")
                val sql = """
                    SELECT * FROM customer_collection_actions
                    WHERE $whereClause
                    ORDER BY scheduled_at DESC
                    LIMIT ? OFFSET ?
                """.trimIndent()

                val list = tx.connection.prepareStatement(sql).use { stmt ->
                    var idx = 1
                    for (param in params) {
                        stmt.setObject(idx++, param)
                    }
                    stmt.setInt(idx++, limit)
                    stmt.setInt(idx, offset)

                    val rs = stmt.executeQuery()
                    val result = mutableListOf<CustomerCollectionAction>()
                    while (rs.next()) {
                        result.add(mapAction(rs))
                    }
                    result
                }
                DomainResult.Success(list)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to list collection actions")
        }
    }

    override suspend fun savePaymentPromise(promise: CustomerPaymentPromise): DomainResult<CustomerPaymentPromise> {
        val tenantContext = TenantContext(projectId = promise.projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    INSERT INTO customer_payment_promises (
                        promise_id, tenant_id, project_id, customer_id, invoice_id,
                        action_id, promised_amount, promised_date, status, notes,
                        fulfilled_at, fulfilled_payment_id, created_at, created_by,
                        updated_at, updated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (promise_id) DO UPDATE SET
                        status = EXCLUDED.status,
                        notes = EXCLUDED.notes,
                        fulfilled_at = EXCLUDED.fulfilled_at,
                        fulfilled_payment_id = EXCLUDED.fulfilled_payment_id,
                        updated_at = EXCLUDED.updated_at,
                        updated_by = EXCLUDED.updated_by,
                        version = customer_payment_promises.version + 1
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, promise.promiseId)
                    stmt.setString(2, promise.tenantId)
                    stmt.setString(3, promise.projectId)
                    stmt.setString(4, promise.customerId)
                    stmt.setString(5, promise.invoiceId)
                    stmt.setString(6, promise.actionId)
                    stmt.setBigDecimal(7, promise.promisedAmount)
                    stmt.setLong(8, promise.promisedDate)
                    stmt.setString(9, promise.status.name)
                    stmt.setString(10, promise.notes)
                    stmt.setObject(11, promise.fulfilledAt)
                    stmt.setString(12, promise.fulfilledPaymentId)
                    stmt.setLong(13, promise.createdAt)
                    stmt.setString(14, promise.createdBy)
                    stmt.setLong(15, promise.updatedAt)
                    stmt.setString(16, promise.updatedBy)
                    stmt.setLong(17, promise.version)
                    stmt.executeUpdate()
                }
                DomainResult.Success(promise)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to save payment promise")
        }
    }

    override suspend fun getPaymentPromiseById(
        tenantId: String,
        projectId: String,
        promiseId: String
    ): DomainResult<CustomerPaymentPromise?> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val sql = """
                    SELECT * FROM customer_payment_promises
                    WHERE tenant_id = ? AND project_id = ? AND promise_id = ?
                """.trimIndent()
                val promise = tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, promiseId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapPromise(rs) else null
                }
                DomainResult.Success(promise)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to get payment promise")
        }
    }

    override suspend fun listPaymentPromises(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: PaymentPromiseStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerPaymentPromise>> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
                val params = mutableListOf<Any>(tenantId, projectId)

                if (!customerId.isNullOrBlank()) {
                    conditions.add("customer_id = ?")
                    params.add(customerId)
                }
                if (status != null) {
                    conditions.add("status = ?")
                    params.add(status.name)
                }

                val whereClause = conditions.joinToString(" AND ")
                val sql = """
                    SELECT * FROM customer_payment_promises
                    WHERE $whereClause
                    ORDER BY promised_date DESC
                    LIMIT ? OFFSET ?
                """.trimIndent()

                val list = tx.connection.prepareStatement(sql).use { stmt ->
                    var idx = 1
                    for (param in params) {
                        stmt.setObject(idx++, param)
                    }
                    stmt.setInt(idx++, limit)
                    stmt.setInt(idx, offset)

                    val rs = stmt.executeQuery()
                    val result = mutableListOf<CustomerPaymentPromise>()
                    while (rs.next()) {
                        result.add(mapPromise(rs))
                    }
                    result
                }
                DomainResult.Success(list)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to list payment promises")
        }
    }

    override suspend fun recordAuditEvent(event: CustomerCollectionAuditEvent): DomainResult<Unit> {
        val tenantContext = TenantContext(projectId = event.projectId)
        return try {
            transactionManager.inTransaction(tenantContext) { tx ->
                val sql = """
                    INSERT INTO customer_collection_action_audit_events (
                        audit_id, tenant_id, project_id, customer_id, action_id,
                        actor_id, actor_role, action, previous_value_json,
                        new_value_json, reason, occurred_at, metadata_json
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, event.auditId)
                    stmt.setString(2, event.tenantId)
                    stmt.setString(3, event.projectId)
                    stmt.setString(4, event.customerId)
                    stmt.setString(5, event.actionId)
                    stmt.setString(6, event.actorId)
                    stmt.setString(7, event.actorRole)
                    stmt.setString(8, event.action)
                    stmt.setString(9, event.previousValueJson)
                    stmt.setString(10, event.newValueJson)
                    stmt.setString(11, event.reason)
                    stmt.setLong(12, event.occurredAt)
                    stmt.setString(13, event.metadataJson)
                    stmt.executeUpdate()
                }
                DomainResult.Success(Unit)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to record collection audit event")
        }
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        customerId: String?,
        actionId: String?,
        limit: Int
    ): DomainResult<List<CustomerCollectionAuditEvent>> {
        val tenantContext = TenantContext(projectId = projectId)
        return try {
            transactionManager.inReadOnly(tenantContext) { tx ->
                val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
                val params = mutableListOf<Any>(tenantId, projectId)

                if (!customerId.isNullOrBlank()) {
                    conditions.add("customer_id = ?")
                    params.add(customerId)
                }
                if (!actionId.isNullOrBlank()) {
                    conditions.add("action_id = ?")
                    params.add(actionId)
                }

                val whereClause = conditions.joinToString(" AND ")
                val sql = """
                    SELECT * FROM customer_collection_action_audit_events
                    WHERE $whereClause
                    ORDER BY occurred_at DESC
                    LIMIT ?
                """.trimIndent()

                val list = tx.connection.prepareStatement(sql).use { stmt ->
                    var idx = 1
                    for (param in params) {
                        stmt.setObject(idx++, param)
                    }
                    stmt.setInt(idx, limit)

                    val rs = stmt.executeQuery()
                    val result = mutableListOf<CustomerCollectionAuditEvent>()
                    while (rs.next()) {
                        result.add(mapAuditEvent(rs))
                    }
                    result
                }
                DomainResult.Success(list)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to get collection audit events")
        }
    }
}

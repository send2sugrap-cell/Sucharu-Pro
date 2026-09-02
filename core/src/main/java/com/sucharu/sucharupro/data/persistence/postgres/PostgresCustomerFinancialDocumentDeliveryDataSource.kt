package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.customerfinancialreporting.CustomerFinancialDocumentDeliveryDataSource
import com.sucharu.sucharupro.data.persistence.postgres.RowMappers.getEnumByName
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerfinancialreporting.*
import java.sql.ResultSet
import java.util.concurrent.ConcurrentHashMap

/**
 * Production PostgreSQL DataSource for Customer Financial Document Delivery (Module 14 Step 11).
 */
class PostgresCustomerFinancialDocumentDeliveryDataSource(
    private val transactionManager: TransactionManager,
    private val defaultTenantId: String = "TENANT-001"
) : CustomerFinancialDocumentDeliveryDataSource {

    private val inMemoryPayloads = ConcurrentHashMap<String, ByteArray>()

    private fun mapDelivery(rs: ResultSet): CustomerFinancialDocumentDelivery {
        return CustomerFinancialDocumentDelivery(
            deliveryId = rs.getString("delivery_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            documentId = rs.getString("document_id"),
            documentType = rs.getEnumByName("document_type", CustomerFinancialReportType.CUSTOMER_STATEMENT),
            documentFormat = rs.getEnumByName("document_format", CustomerFinancialReportFormat.JSON),
            documentName = rs.getString("document_name"),
            storageReference = rs.getString("storage_reference"),
            checksum = rs.getString("checksum"),
            fileSize = rs.getLong("file_size"),
            mimeType = rs.getString("mime_type") ?: "application/octet-stream",
            deliveryStatus = rs.getEnumByName("delivery_status", CustomerFinancialDeliveryStatus.CREATED),
            accessCount = rs.getInt("access_count"),
            lastAccessedAt = rs.getObject("last_accessed_at") as? Long,
            lastAccessedBy = rs.getString("last_accessed_by"),
            expiresAt = rs.getObject("expires_at") as? Long,
            isRevoked = rs.getBoolean("is_revoked"),
            revokedAt = rs.getObject("revoked_at") as? Long,
            revokedBy = rs.getString("revoked_by"),
            revocationReason = rs.getString("revocation_reason"),
            notificationStatus = rs.getEnumByName("notification_status", CustomerFinancialNotificationStatus.PENDING),
            notifiedAt = rs.getObject("notified_at") as? Long,
            notificationId = rs.getString("notification_id"),
            failureReason = rs.getString("failure_reason"),
            idempotencyKey = rs.getString("idempotency_key"),
            metadataJson = rs.getString("metadata_json"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by") ?: "system",
            updatedAt = rs.getLong("updated_at"),
            updatedBy = rs.getString("updated_by") ?: "system",
            version = rs.getLong("version")
        )
    }

    private fun mapAuditEvent(rs: ResultSet): CustomerFinancialDocumentDeliveryAuditEvent {
        return CustomerFinancialDocumentDeliveryAuditEvent(
            auditId = rs.getString("audit_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            customerId = rs.getString("customer_id"),
            deliveryId = rs.getString("delivery_id"),
            documentId = rs.getString("document_id"),
            eventType = rs.getEnumByName("event_type", CustomerFinancialDeliveryEventType.DOCUMENT_CREATED),
            actorId = rs.getString("actor_id"),
            actorRole = rs.getString("actor_role"),
            timestamp = rs.getLong("timestamp"),
            correlationId = rs.getString("correlation_id"),
            detailsJson = rs.getString("details_json"),
            checksum = rs.getString("checksum")
        )
    }

    override suspend fun saveDelivery(delivery: CustomerFinancialDocumentDelivery): DomainResult<CustomerFinancialDocumentDelivery> {
        return try {
            transactionManager.inTransaction(TenantContext(delivery.projectId)) { tx ->
                val sql = """
                    INSERT INTO customer_financial_document_deliveries (
                        delivery_id, tenant_id, project_id, customer_id, document_id,
                        document_type, document_format, document_name, storage_reference,
                        checksum, file_size, mime_type, delivery_status, access_count,
                        last_accessed_at, last_accessed_by, expires_at, is_revoked,
                        revoked_at, revoked_by, revocation_reason, notification_status,
                        notified_at, notification_id, failure_reason, idempotency_key,
                        metadata_json, created_at, created_by, updated_at, updated_by, version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (delivery_id) DO UPDATE SET
                        delivery_status = EXCLUDED.delivery_status,
                        access_count = EXCLUDED.access_count,
                        last_accessed_at = EXCLUDED.last_accessed_at,
                        last_accessed_by = EXCLUDED.last_accessed_by,
                        is_revoked = EXCLUDED.is_revoked,
                        revoked_at = EXCLUDED.revoked_at,
                        revoked_by = EXCLUDED.revoked_by,
                        revocation_reason = EXCLUDED.revocation_reason,
                        notification_status = EXCLUDED.notification_status,
                        notified_at = EXCLUDED.notified_at,
                        notification_id = EXCLUDED.notification_id,
                        failure_reason = EXCLUDED.failure_reason,
                        updated_at = EXCLUDED.updated_at,
                        updated_by = EXCLUDED.updated_by,
                        version = customer_financial_document_deliveries.version + 1
                """.trimIndent()

                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, delivery.deliveryId)
                    stmt.setString(2, delivery.tenantId)
                    stmt.setString(3, delivery.projectId)
                    stmt.setString(4, delivery.customerId)
                    stmt.setString(5, delivery.documentId)
                    stmt.setString(6, delivery.documentType.name)
                    stmt.setString(7, delivery.documentFormat.name)
                    stmt.setString(8, delivery.documentName)
                    stmt.setString(9, delivery.storageReference)
                    stmt.setString(10, delivery.checksum)
                    stmt.setLong(11, delivery.fileSize)
                    stmt.setString(12, delivery.mimeType)
                    stmt.setString(13, delivery.deliveryStatus.name)
                    stmt.setInt(14, delivery.accessCount)
                    stmt.setObject(15, delivery.lastAccessedAt)
                    stmt.setString(16, delivery.lastAccessedBy)
                    stmt.setObject(17, delivery.expiresAt)
                    stmt.setBoolean(18, delivery.isRevoked)
                    stmt.setObject(19, delivery.revokedAt)
                    stmt.setString(20, delivery.revokedBy)
                    stmt.setString(21, delivery.revocationReason)
                    stmt.setString(22, delivery.notificationStatus.name)
                    stmt.setObject(23, delivery.notifiedAt)
                    stmt.setString(24, delivery.notificationId)
                    stmt.setString(25, delivery.failureReason)
                    stmt.setString(26, delivery.idempotencyKey)
                    stmt.setString(27, delivery.metadataJson)
                    stmt.setLong(28, delivery.createdAt)
                    stmt.setString(29, delivery.createdBy)
                    stmt.setLong(30, delivery.updatedAt)
                    stmt.setString(31, delivery.updatedBy)
                    stmt.setLong(32, delivery.version)
                    stmt.executeUpdate()
                }
                DomainResult.Success(delivery)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to save customer financial document delivery")
        }
    }

    override suspend fun getDeliveryById(
        tenantId: String,
        projectId: String,
        deliveryId: String
    ): DomainResult<CustomerFinancialDocumentDelivery?> {
        return try {
            transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
                val sql = "SELECT * FROM customer_financial_document_deliveries WHERE tenant_id = ? AND project_id = ? AND delivery_id = ?"
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, deliveryId)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) DomainResult.Success(mapDelivery(rs)) else DomainResult.Success(null)
                    }
                }
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to get document delivery by ID")
        }
    }

    override suspend fun getDeliveryByDocumentId(
        tenantId: String,
        projectId: String,
        documentId: String
    ): DomainResult<CustomerFinancialDocumentDelivery?> {
        return try {
            transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
                val sql = "SELECT * FROM customer_financial_document_deliveries WHERE tenant_id = ? AND project_id = ? AND document_id = ?"
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, documentId)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) DomainResult.Success(mapDelivery(rs)) else DomainResult.Success(null)
                    }
                }
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to get document delivery by document ID")
        }
    }

    override suspend fun getDeliveryByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerFinancialDocumentDelivery?> {
        return try {
            transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
                val sql = "SELECT * FROM customer_financial_document_deliveries WHERE tenant_id = ? AND project_id = ? AND idempotency_key = ?"
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, idempotencyKey)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) DomainResult.Success(mapDelivery(rs)) else DomainResult.Success(null)
                    }
                }
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to get document delivery by idempotency key")
        }
    }

    override suspend fun listDeliveries(
        tenantId: String,
        projectId: String,
        customerId: String?,
        documentType: CustomerFinancialReportType?,
        status: CustomerFinancialDeliveryStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerFinancialDocumentDelivery>> {
        return try {
            transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
                val sb = StringBuilder("SELECT * FROM customer_financial_document_deliveries WHERE tenant_id = ? AND project_id = ?")
                val params = mutableListOf<Any>(tenantId, projectId)

                if (customerId != null) {
                    sb.append(" AND customer_id = ?")
                    params.add(customerId)
                }
                if (documentType != null) {
                    sb.append(" AND document_type = ?")
                    params.add(documentType.name)
                }
                if (status != null) {
                    sb.append(" AND delivery_status = ?")
                    params.add(status.name)
                }

                sb.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?")
                params.add(limit)
                params.add(offset)

                tx.connection.prepareStatement(sb.toString()).use { stmt ->
                    params.forEachIndexed { idx, p ->
                        when (p) {
                            is String -> stmt.setString(idx + 1, p)
                            is Int -> stmt.setInt(idx + 1, p)
                            is Long -> stmt.setLong(idx + 1, p)
                            else -> stmt.setObject(idx + 1, p)
                        }
                    }
                    stmt.executeQuery().use { rs ->
                        val list = mutableListOf<CustomerFinancialDocumentDelivery>()
                        while (rs.next()) {
                            list.add(mapDelivery(rs))
                        }
                        DomainResult.Success(list)
                    }
                }
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to list customer financial document deliveries")
        }
    }

    override suspend fun saveAuditEvent(event: CustomerFinancialDocumentDeliveryAuditEvent): DomainResult<CustomerFinancialDocumentDeliveryAuditEvent> {
        return try {
            transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
                val sql = """
                    INSERT INTO customer_financial_document_delivery_audit_events (
                        audit_id, tenant_id, project_id, customer_id, delivery_id,
                        document_id, event_type, actor_id, actor_role, timestamp,
                        correlation_id, details_json, checksum
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, event.auditId)
                    stmt.setString(2, event.tenantId)
                    stmt.setString(3, event.projectId)
                    stmt.setString(4, event.customerId)
                    stmt.setString(5, event.deliveryId)
                    stmt.setString(6, event.documentId)
                    stmt.setString(7, event.eventType.name)
                    stmt.setString(8, event.actorId)
                    stmt.setString(9, event.actorRole)
                    stmt.setLong(10, event.timestamp)
                    stmt.setString(11, event.correlationId)
                    stmt.setString(12, event.detailsJson)
                    stmt.setString(13, event.checksum)
                    stmt.executeUpdate()
                }
                DomainResult.Success(event)
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to save document delivery audit event")
        }
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        deliveryId: String
    ): DomainResult<List<CustomerFinancialDocumentDeliveryAuditEvent>> {
        return try {
            transactionManager.inReadOnly(TenantContext(projectId)) { tx ->
                val sql = "SELECT * FROM customer_financial_document_delivery_audit_events WHERE tenant_id = ? AND project_id = ? AND delivery_id = ? ORDER BY timestamp ASC"
                tx.connection.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setString(3, deliveryId)
                    stmt.executeQuery().use { rs ->
                        val list = mutableListOf<CustomerFinancialDocumentDeliveryAuditEvent>()
                        while (rs.next()) {
                            list.add(mapAuditEvent(rs))
                        }
                        DomainResult.Success(list)
                    }
                }
            }
        } catch (e: Exception) {
            DomainResult.Error(e, e.message ?: "Failed to list document delivery audit events")
        }
    }

    override suspend fun saveDocumentPayload(documentId: String, content: ByteArray): DomainResult<Unit> {
        inMemoryPayloads[documentId] = content
        return DomainResult.Success(Unit)
    }

    override suspend fun getDocumentPayload(documentId: String): DomainResult<ByteArray?> {
        return DomainResult.Success(inMemoryPayloads[documentId])
    }
}

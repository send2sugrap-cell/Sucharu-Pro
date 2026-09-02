package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.commercialcommitment.CommercialCommitmentDataSource
import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitment
import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitmentEvent
import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitmentEventType
import com.sucharu.sucharupro.domain.model.commercialcommitment.CommitmentStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.math.BigDecimal
import java.sql.ResultSet

/**
 * PostgreSQL implementation of [CommercialCommitmentDataSource] with TransactionManager + RLS.
 * Module 17 Step 03.
 */
class PostgresCommercialCommitmentDataSource(
    private val transactionManager: TransactionManager
) : CommercialCommitmentDataSource {

    override suspend fun insertCommitment(commitment: CommercialCommitment): DomainResult<CommercialCommitment> {
        return try {
            transactionManager.inTransaction(TenantContext(commitment.projectId)) { ctx ->
                val sql = """
                    INSERT INTO commercial_commitments (
                        commitment_id, tenant_id, project_id, quotation_id, quotation_version,
                        customer_id, order_id, order_number, status, committed_quantity,
                        approved_unit_price, approved_subtotal, approved_discount, approved_tax,
                        approved_grand_total, currency, payment_terms, delivery_terms,
                        conversion_notes, idempotency_key, integrity_hash, created_at,
                        created_by, converted_at, converted_by
                    ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    ON CONFLICT (commitment_id) DO NOTHING
                """.trimIndent()
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, commitment.commitmentId)
                    s.setString(2, commitment.tenantId)
                    s.setString(3, commitment.projectId)
                    s.setString(4, commitment.quotationId)
                    s.setInt(5, commitment.quotationVersion)
                    s.setString(6, commitment.customerId)
                    s.setString(7, commitment.orderId)
                    s.setString(8, commitment.orderNumber)
                    s.setString(9, commitment.status.name)
                    s.setLong(10, commitment.committedQuantity)
                    s.setBigDecimal(11, commitment.approvedUnitPrice)
                    s.setBigDecimal(12, commitment.approvedSubtotal)
                    s.setBigDecimal(13, commitment.approvedDiscount)
                    s.setBigDecimal(14, commitment.approvedTax)
                    s.setBigDecimal(15, commitment.approvedGrandTotal)
                    s.setString(16, commitment.currency)
                    s.setString(17, commitment.paymentTerms)
                    s.setString(18, commitment.deliveryTerms)
                    s.setString(19, commitment.conversionNotes)
                    s.setString(20, commitment.idempotencyKey)
                    s.setString(21, commitment.integrityHash)
                    s.setLong(22, commitment.createdAt)
                    s.setString(23, commitment.createdBy)
                    s.setObject(24, commitment.convertedAt)
                    s.setString(25, commitment.convertedBy)
                    s.executeUpdate()
                }
            }
            DomainResult.Success(commitment)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to insert commercial commitment", exception = e)
        }
    }

    override suspend fun updateCommitment(commitment: CommercialCommitment): DomainResult<CommercialCommitment> {
        return try {
            transactionManager.inTransaction(TenantContext(commitment.projectId)) { ctx ->
                val sql = """
                    UPDATE commercial_commitments SET
                        order_id = ?,
                        order_number = ?,
                        status = ?,
                        committed_quantity = ?,
                        approved_unit_price = ?,
                        approved_subtotal = ?,
                        approved_discount = ?,
                        approved_tax = ?,
                        approved_grand_total = ?,
                        payment_terms = ?,
                        delivery_terms = ?,
                        conversion_notes = ?,
                        integrity_hash = ?,
                        converted_at = ?,
                        converted_by = ?
                    WHERE commitment_id = ? AND tenant_id = ?
                """.trimIndent()
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, commitment.orderId)
                    s.setString(2, commitment.orderNumber)
                    s.setString(3, commitment.status.name)
                    s.setLong(4, commitment.committedQuantity)
                    s.setBigDecimal(5, commitment.approvedUnitPrice)
                    s.setBigDecimal(6, commitment.approvedSubtotal)
                    s.setBigDecimal(7, commitment.approvedDiscount)
                    s.setBigDecimal(8, commitment.approvedTax)
                    s.setBigDecimal(9, commitment.approvedGrandTotal)
                    s.setString(10, commitment.paymentTerms)
                    s.setString(11, commitment.deliveryTerms)
                    s.setString(12, commitment.conversionNotes)
                    s.setString(13, commitment.integrityHash)
                    s.setObject(14, commitment.convertedAt)
                    s.setString(15, commitment.convertedBy)
                    s.setString(16, commitment.commitmentId)
                    s.setString(17, commitment.tenantId)
                    s.executeUpdate()
                }
            }
            DomainResult.Success(commitment)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to update commercial commitment", exception = e)
        }
    }

    override suspend fun selectCommitmentById(tenantId: String, commitmentId: String): DomainResult<CommercialCommitment?> {
        return try {
            var found: CommercialCommitment? = null
            transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val sql = "SELECT * FROM commercial_commitments WHERE commitment_id = ? AND tenant_id = ?"
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, commitmentId)
                    s.setString(2, tenantId)
                    s.executeQuery().use { rs ->
                        if (rs.next()) found = mapRowToCommitment(rs)
                    }
                }
            }
            DomainResult.Success(found)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to select commercial commitment by id", exception = e)
        }
    }

    override suspend fun selectCommitmentByQuotation(tenantId: String, quotationId: String): DomainResult<CommercialCommitment?> {
        return try {
            var found: CommercialCommitment? = null
            transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val sql = "SELECT * FROM commercial_commitments WHERE quotation_id = ? AND tenant_id = ? ORDER BY created_at DESC LIMIT 1"
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, quotationId)
                    s.setString(2, tenantId)
                    s.executeQuery().use { rs ->
                        if (rs.next()) found = mapRowToCommitment(rs)
                    }
                }
            }
            DomainResult.Success(found)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to select commercial commitment by quotation", exception = e)
        }
    }

    override suspend fun selectCommitmentByIdempotencyKey(tenantId: String, idempotencyKey: String): DomainResult<CommercialCommitment?> {
        return try {
            var found: CommercialCommitment? = null
            transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val sql = "SELECT * FROM commercial_commitments WHERE idempotency_key = ? AND tenant_id = ? LIMIT 1"
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, idempotencyKey)
                    s.setString(2, tenantId)
                    s.executeQuery().use { rs ->
                        if (rs.next()) found = mapRowToCommitment(rs)
                    }
                }
            }
            DomainResult.Success(found)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to select commercial commitment by idempotency key", exception = e)
        }
    }

    override suspend fun listCommitments(tenantId: String, limit: Int): DomainResult<List<CommercialCommitment>> {
        return try {
            val list = mutableListOf<CommercialCommitment>()
            transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val sql = "SELECT * FROM commercial_commitments WHERE tenant_id = ? ORDER BY created_at DESC LIMIT ?"
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, tenantId)
                    s.setInt(2, limit)
                    s.executeQuery().use { rs ->
                        while (rs.next()) list.add(mapRowToCommitment(rs))
                    }
                }
            }
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to list commercial commitments", exception = e)
        }
    }

    override suspend fun insertEvent(event: CommercialCommitmentEvent): DomainResult<CommercialCommitmentEvent> {
        return try {
            transactionManager.inTransaction(TenantContext(event.projectId)) { ctx ->
                val sql = """
                    INSERT INTO commercial_commitment_events (
                        event_id, commitment_id, tenant_id, project_id, event_type,
                        actor, actor_role, details_json, occurred_at
                    ) VALUES (?,?,?,?,?,?,?,?,?)
                    ON CONFLICT (event_id) DO NOTHING
                """.trimIndent()
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, event.eventId)
                    s.setString(2, event.commitmentId)
                    s.setString(3, event.tenantId)
                    s.setString(4, event.projectId)
                    s.setString(5, event.eventType.name)
                    s.setString(6, event.actor)
                    s.setString(7, event.actorRole)
                    s.setString(8, event.detailsJson)
                    s.setLong(9, event.occurredAt)
                    s.executeUpdate()
                }
            }
            DomainResult.Success(event)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to insert commercial commitment event", exception = e)
        }
    }

    override suspend fun listEventsByCommitmentId(tenantId: String, commitmentId: String): DomainResult<List<CommercialCommitmentEvent>> {
        return try {
            val list = mutableListOf<CommercialCommitmentEvent>()
            transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val sql = "SELECT * FROM commercial_commitment_events WHERE commitment_id = ? AND tenant_id = ? ORDER BY occurred_at ASC"
                ctx.connection.prepareStatement(sql).use { s ->
                    s.setString(1, commitmentId)
                    s.setString(2, tenantId)
                    s.executeQuery().use { rs ->
                        while (rs.next()) {
                            list.add(
                                CommercialCommitmentEvent(
                                    eventId = rs.getString("event_id"),
                                    commitmentId = rs.getString("commitment_id"),
                                    tenantId = rs.getString("tenant_id"),
                                    projectId = rs.getString("project_id"),
                                    eventType = CommercialCommitmentEventType.valueOf(rs.getString("event_type")),
                                    actor = rs.getString("actor"),
                                    actorRole = rs.getString("actor_role"),
                                    detailsJson = rs.getString("details_json"),
                                    occurredAt = rs.getLong("occurred_at")
                                )
                            )
                        }
                    }
                }
            }
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to list commitment events", exception = e)
        }
    }

    private fun mapRowToCommitment(rs: ResultSet): CommercialCommitment {
        val convertedAtObj = rs.getObject("converted_at")
        val convertedAt = if (convertedAtObj is Number) convertedAtObj.toLong() else null

        return CommercialCommitment(
            commitmentId = rs.getString("commitment_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            quotationId = rs.getString("quotation_id"),
            quotationVersion = rs.getInt("quotation_version"),
            customerId = rs.getString("customer_id"),
            orderId = rs.getString("order_id"),
            orderNumber = rs.getString("order_number"),
            status = CommitmentStatus.valueOf(rs.getString("status")),
            committedQuantity = rs.getLong("committed_quantity"),
            approvedUnitPrice = rs.getBigDecimal("approved_unit_price"),
            approvedSubtotal = rs.getBigDecimal("approved_subtotal"),
            approvedDiscount = rs.getBigDecimal("approved_discount"),
            approvedTax = rs.getBigDecimal("approved_tax"),
            approvedGrandTotal = rs.getBigDecimal("approved_grand_total"),
            currency = rs.getString("currency"),
            paymentTerms = rs.getString("payment_terms"),
            deliveryTerms = rs.getString("delivery_terms"),
            conversionNotes = rs.getString("conversion_notes"),
            idempotencyKey = rs.getString("idempotency_key"),
            integrityHash = rs.getString("integrity_hash"),
            createdAt = rs.getLong("created_at"),
            createdBy = rs.getString("created_by"),
            convertedAt = convertedAt,
            convertedBy = rs.getString("converted_by")
        )
    }
}

package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.substratereservation.SubstrateReleaseGovernanceDataSource
import com.sucharu.sucharupro.domain.model.substratereservation.*
import java.sql.ResultSet

/**
 * PostgreSQL implementation of SubstrateReleaseGovernanceDataSource with RLS and tenant isolation.
 * Module 19 Step 05.
 */
class PostgresSubstrateReleaseGovernanceDataSource(
    private val transactionManager: TransactionManager
) : SubstrateReleaseGovernanceDataSource {

    override suspend fun saveGovernanceRecord(record: SubstrateReleaseGovernanceRecord): SubstrateReleaseGovernanceRecord {
        return transactionManager.inTransaction(TenantContext(record.tenantId)) { ctx ->
            val conn = ctx.connection

            val sql = """
                INSERT INTO substrate_release_governance_records (
                    governance_id, tenant_id, reservation_id, order_id, order_item_id, execution_job_id,
                    trigger_type, upstream_event_id, sku, material_name, warehouse_id, previous_required_sheets,
                    new_required_sheets, allocated_sheets, consumed_sheets, committed_sheets, releasable_sheets,
                    retained_sheets, additional_required_sheets, decision, execution_status, blocking_reason,
                    explanation, deduplication_fingerprint, master_integrity_hash, evaluated_by, evaluated_at,
                    approved_by, approved_at, executed_by, executed_at, notes, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                ON CONFLICT (governance_id) DO UPDATE SET
                    releasable_sheets = EXCLUDED.releasable_sheets,
                    retained_sheets = EXCLUDED.retained_sheets,
                    additional_required_sheets = EXCLUDED.additional_required_sheets,
                    decision = EXCLUDED.decision,
                    execution_status = EXCLUDED.execution_status,
                    blocking_reason = EXCLUDED.blocking_reason,
                    explanation = EXCLUDED.explanation,
                    approved_by = EXCLUDED.approved_by,
                    approved_at = EXCLUDED.approved_at,
                    executed_by = EXCLUDED.executed_by,
                    executed_at = EXCLUDED.executed_at,
                    notes = EXCLUDED.notes,
                    updated_at = NOW()
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, record.governanceId)
                stmt.setString(2, record.tenantId)
                stmt.setString(3, record.reservationId)
                stmt.setString(4, record.orderId)
                stmt.setString(5, record.orderItemId)
                stmt.setString(6, record.executionJobId)
                stmt.setString(7, record.triggerType.name)
                stmt.setString(8, record.upstreamEventId)
                stmt.setString(9, record.sku)
                stmt.setString(10, record.materialName)
                stmt.setString(11, record.warehouseId)
                stmt.setLong(12, record.previousRequiredSheets)
                stmt.setLong(13, record.newRequiredSheets)
                stmt.setLong(14, record.allocatedSheets)
                stmt.setLong(15, record.consumedSheets)
                stmt.setLong(16, record.committedSheets)
                stmt.setLong(17, record.releasableSheets)
                stmt.setLong(18, record.retainedSheets)
                stmt.setLong(19, record.additionalRequiredSheets)
                stmt.setString(20, record.decision.name)
                stmt.setString(21, record.executionStatus.name)
                stmt.setString(22, record.blockingReason.name)
                stmt.setString(23, record.explanation)
                stmt.setString(24, record.deduplicationFingerprint)
                stmt.setString(25, record.masterIntegrityHash)
                stmt.setString(26, record.evaluatedBy)
                stmt.setLong(27, record.evaluatedAt)
                stmt.setString(28, record.approvedBy)
                if (record.approvedAt != null) stmt.setLong(29, record.approvedAt) else stmt.setNull(29, java.sql.Types.BIGINT)
                stmt.setString(30, record.executedBy)
                if (record.executedAt != null) stmt.setLong(31, record.executedAt) else stmt.setNull(31, java.sql.Types.BIGINT)
                stmt.setString(32, record.notes)
                stmt.executeUpdate()
            }
            record
        }
    }

    override suspend fun getGovernanceRecordById(tenantId: String, governanceId: String): SubstrateReleaseGovernanceRecord? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_release_governance_records WHERE tenant_id = ? AND governance_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, governanceId)
                val rs = stmt.executeQuery()
                if (rs.next()) mapRecord(rs) else null
            }
        }
    }

    override suspend fun findGovernanceRecordByFingerprint(tenantId: String, fingerprint: String): SubstrateReleaseGovernanceRecord? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_release_governance_records WHERE tenant_id = ? AND deduplication_fingerprint = ? LIMIT 1"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, fingerprint)
                val rs = stmt.executeQuery()
                if (rs.next()) mapRecord(rs) else null
            }
        }
    }

    override suspend fun listGovernanceRecordsByReservation(tenantId: String, reservationId: String): List<SubstrateReleaseGovernanceRecord> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_release_governance_records WHERE tenant_id = ? AND reservation_id = ? ORDER BY evaluated_at DESC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, reservationId)
                val rs = stmt.executeQuery()
                val list = mutableListOf<SubstrateReleaseGovernanceRecord>()
                while (rs.next()) {
                    list.add(mapRecord(rs))
                }
                list
            }
        }
    }

    override suspend fun listGovernanceRecordsByOrder(tenantId: String, orderId: String): List<SubstrateReleaseGovernanceRecord> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_release_governance_records WHERE tenant_id = ? AND order_id = ? ORDER BY evaluated_at DESC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, orderId)
                val rs = stmt.executeQuery()
                val list = mutableListOf<SubstrateReleaseGovernanceRecord>()
                while (rs.next()) {
                    list.add(mapRecord(rs))
                }
                list
            }
        }
    }

    override suspend fun listGovernanceRecords(tenantId: String, limit: Int): List<SubstrateReleaseGovernanceRecord> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_release_governance_records WHERE tenant_id = ? ORDER BY evaluated_at DESC LIMIT ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setInt(2, limit)
                val rs = stmt.executeQuery()
                val list = mutableListOf<SubstrateReleaseGovernanceRecord>()
                while (rs.next()) {
                    list.add(mapRecord(rs))
                }
                list
            }
        }
    }

    override suspend fun saveAuditEvent(event: SubstrateReleaseGovernanceAuditEvent): SubstrateReleaseGovernanceAuditEvent {
        return transactionManager.inTransaction(TenantContext(event.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO substrate_release_governance_audits (
                    event_id, governance_id, tenant_id, action, previous_status, new_status, actor, reason, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (event_id) DO NOTHING
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, event.eventId)
                stmt.setString(2, event.governanceId)
                stmt.setString(3, event.tenantId)
                stmt.setString(4, event.action)
                stmt.setString(5, event.previousStatus?.name)
                stmt.setString(6, event.newStatus.name)
                stmt.setString(7, event.actor)
                stmt.setString(8, event.reason)
                stmt.setLong(9, event.timestamp)
                stmt.executeUpdate()
            }
            event
        }
    }

    override suspend fun listAuditEvents(tenantId: String, governanceId: String): List<SubstrateReleaseGovernanceAuditEvent> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_release_governance_audits WHERE tenant_id = ? AND governance_id = ? ORDER BY timestamp ASC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, governanceId)
                val rs = stmt.executeQuery()
                val list = mutableListOf<SubstrateReleaseGovernanceAuditEvent>()
                while (rs.next()) {
                    list.add(
                        SubstrateReleaseGovernanceAuditEvent(
                            eventId = rs.getString("event_id"),
                            governanceId = rs.getString("governance_id"),
                            tenantId = rs.getString("tenant_id"),
                            action = rs.getString("action"),
                            previousStatus = rs.getString("previous_status")?.let { GovernanceExecutionStatus.valueOf(it) },
                            newStatus = GovernanceExecutionStatus.valueOf(rs.getString("new_status")),
                            actor = rs.getString("actor"),
                            reason = rs.getString("reason"),
                            timestamp = rs.getLong("timestamp")
                        )
                    )
                }
                list
            }
        }
    }

    private fun mapRecord(rs: ResultSet): SubstrateReleaseGovernanceRecord {
        val approvedAtRaw = rs.getLong("approved_at")
        val approvedAt = if (rs.wasNull()) null else approvedAtRaw

        val executedAtRaw = rs.getLong("executed_at")
        val executedAt = if (rs.wasNull()) null else executedAtRaw

        return SubstrateReleaseGovernanceRecord(
            governanceId = rs.getString("governance_id"),
            tenantId = rs.getString("tenant_id"),
            reservationId = rs.getString("reservation_id"),
            orderId = rs.getString("order_id"),
            orderItemId = rs.getString("order_item_id"),
            executionJobId = rs.getString("execution_job_id"),
            triggerType = GovernanceTriggerType.valueOf(rs.getString("trigger_type")),
            upstreamEventId = rs.getString("upstream_event_id"),
            sku = rs.getString("sku"),
            materialName = rs.getString("material_name"),
            warehouseId = rs.getString("warehouse_id"),
            previousRequiredSheets = rs.getLong("previous_required_sheets"),
            newRequiredSheets = rs.getLong("new_required_sheets"),
            allocatedSheets = rs.getLong("allocated_sheets"),
            consumedSheets = rs.getLong("consumed_sheets"),
            committedSheets = rs.getLong("committed_sheets"),
            releasableSheets = rs.getLong("releasable_sheets"),
            retainedSheets = rs.getLong("retained_sheets"),
            additionalRequiredSheets = rs.getLong("additional_required_sheets"),
            decision = ReleaseGovernanceDecision.valueOf(rs.getString("decision")),
            executionStatus = GovernanceExecutionStatus.valueOf(rs.getString("execution_status")),
            blockingReason = ReleaseBlockingReason.valueOf(rs.getString("blocking_reason")),
            explanation = rs.getString("explanation"),
            deduplicationFingerprint = rs.getString("deduplication_fingerprint"),
            masterIntegrityHash = rs.getString("master_integrity_hash"),
            evaluatedBy = rs.getString("evaluated_by"),
            evaluatedAt = rs.getLong("evaluated_at"),
            approvedBy = rs.getString("approved_by"),
            approvedAt = approvedAt,
            executedBy = rs.getString("executed_by"),
            executedAt = executedAt,
            notes = rs.getString("notes")
        )
    }
}

package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.substratereservation.SubstrateEnterpriseAuditDataSource
import com.sucharu.sucharupro.domain.model.substratereservation.*
import java.sql.ResultSet

/**
 * PostgreSQL implementation of SubstrateEnterpriseAuditDataSource with RLS and tenant isolation.
 * Module 19 Step 06.
 */
class PostgresSubstrateEnterpriseAuditDataSource(
    private val transactionManager: TransactionManager
) : SubstrateEnterpriseAuditDataSource {

    override suspend fun insertAuditEvent(record: SubstrateEnterpriseAuditRecord): SubstrateEnterpriseAuditRecord {
        return transactionManager.inTransaction(TenantContext(record.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO substrate_enterprise_audits (
                    audit_id, tenant_id, reservation_id, reservation_version, job_id, order_id, order_item_id,
                    substrate_requirement_id, batch_lot_id, warehouse_id, event_type, previous_state, new_state,
                    actor_type, actor_id, role, permission_context, timestamp, reason, correlation_id, trace_id,
                    idempotency_key, source_module, source_operation, event_outbox_id, record_hash, previous_audit_hash,
                    chain_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (audit_id) DO NOTHING
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, record.auditId)
                stmt.setString(2, record.tenantId)
                stmt.setString(3, record.reservationId)
                stmt.setLong(4, record.reservationVersion)
                stmt.setString(5, record.jobId)
                stmt.setString(6, record.orderId)
                stmt.setString(7, record.orderItemId)
                stmt.setString(8, record.substrateRequirementId)
                stmt.setString(9, record.batchLotId)
                stmt.setString(10, record.warehouseId)
                stmt.setString(11, record.eventType.name)
                stmt.setString(12, record.previousState)
                stmt.setString(13, record.newState)
                stmt.setString(14, record.actorType.name)
                stmt.setString(15, record.actorId)
                stmt.setString(16, record.role)
                stmt.setString(17, record.permissionContext)
                stmt.setLong(18, record.timestamp)
                stmt.setString(19, record.reason)
                stmt.setString(20, record.correlationId)
                stmt.setString(21, record.traceId)
                stmt.setString(22, record.idempotencyKey)
                stmt.setString(23, record.sourceModule)
                stmt.setString(24, record.sourceOperation)
                stmt.setString(25, record.eventOutboxId)
                stmt.setString(26, record.recordHash)
                stmt.setString(27, record.previousAuditHash)
                stmt.setString(28, record.chainHash)
                stmt.executeUpdate()
            }
            record
        }
    }

    override suspend fun findAuditEventsByReservation(
        tenantId: String,
        reservationId: String
    ): List<SubstrateEnterpriseAuditRecord> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_enterprise_audits WHERE tenant_id = ? AND reservation_id = ? ORDER BY timestamp ASC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, reservationId)
                val rs = stmt.executeQuery()
                val list = mutableListOf<SubstrateEnterpriseAuditRecord>()
                while (rs.next()) {
                    list.add(mapAuditRecord(rs))
                }
                list
            }
        }
    }

    override suspend fun findAuditEvents(
        tenantId: String,
        orderId: String?,
        jobId: String?,
        eventType: ReservationAuditEventType?,
        limit: Int
    ): List<SubstrateEnterpriseAuditRecord> {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = StringBuilder("SELECT * FROM substrate_enterprise_audits WHERE tenant_id = ?")
            if (orderId != null) sql.append(" AND order_id = ?")
            if (jobId != null) sql.append(" AND job_id = ?")
            if (eventType != null) sql.append(" AND event_type = ?")
            sql.append(" ORDER BY timestamp DESC LIMIT ?")

            conn.prepareStatement(sql.toString()).use { stmt ->
                var idx = 1
                stmt.setString(idx++, tenantId)
                if (orderId != null) stmt.setString(idx++, orderId)
                if (jobId != null) stmt.setString(idx++, jobId)
                if (eventType != null) stmt.setString(idx++, eventType.name)
                stmt.setInt(idx, limit)

                val rs = stmt.executeQuery()
                val list = mutableListOf<SubstrateEnterpriseAuditRecord>()
                while (rs.next()) {
                    list.add(mapAuditRecord(rs))
                }
                list
            }
        }
    }

    override suspend fun insertReconciliation(
        reconciliation: SubstrateReservationReconciliation
    ): SubstrateReservationReconciliation {
        return transactionManager.inTransaction(TenantContext(reconciliation.tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO substrate_reservation_reconciliations (
                    reconciliation_id, tenant_id, reservation_id, order_id, job_id, sku, required_sheets,
                    reserved_sheets, physical_on_hand_sheets, allocated_batch_sheets, releasable_sheets,
                    consumed_sheets, committed_sheets, replenishment_required_sheets, status, reconciled_by,
                    reconciled_at, integrity_hash, notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (reconciliation_id) DO NOTHING
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, reconciliation.reconciliationId)
                stmt.setString(2, reconciliation.tenantId)
                stmt.setString(3, reconciliation.reservationId)
                stmt.setString(4, reconciliation.orderId)
                stmt.setString(5, reconciliation.jobId)
                stmt.setString(6, reconciliation.sku)
                stmt.setLong(7, reconciliation.requiredSheets)
                stmt.setLong(8, reconciliation.reservedSheets)
                stmt.setLong(9, reconciliation.physicalOnHandSheets)
                stmt.setLong(10, reconciliation.allocatedBatchSheets)
                stmt.setLong(11, reconciliation.releasableSheets)
                stmt.setLong(12, reconciliation.consumedSheets)
                stmt.setLong(13, reconciliation.committedSheets)
                stmt.setLong(14, reconciliation.replenishmentRequiredSheets)
                stmt.setString(15, reconciliation.status.name)
                stmt.setString(16, reconciliation.reconciledBy)
                stmt.setLong(17, reconciliation.reconciledAt)
                stmt.setString(18, reconciliation.integrityHash)
                stmt.setString(19, reconciliation.notes)
                stmt.executeUpdate()
            }

            if (reconciliation.discrepancies.isNotEmpty()) {
                val discSql = """
                    INSERT INTO substrate_reconciliation_discrepancies (
                        discrepancy_id, reconciliation_id, tenant_id, discrepancy_type, severity,
                        field_or_context, expected_value, actual_value, explanation, resolution_recommendation
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (discrepancy_id) DO NOTHING
                """.trimIndent()
                conn.prepareStatement(discSql).use { stmt ->
                    for (disc in reconciliation.discrepancies) {
                        stmt.setString(1, disc.discrepancyId)
                        stmt.setString(2, disc.reconciliationId)
                        stmt.setString(3, disc.tenantId)
                        stmt.setString(4, disc.discrepancyType.name)
                        stmt.setString(5, disc.severity.name)
                        stmt.setString(6, disc.fieldOrContext)
                        stmt.setString(7, disc.expectedValue)
                        stmt.setString(8, disc.actualValue)
                        stmt.setString(9, disc.explanation)
                        stmt.setString(10, disc.resolutionRecommendation)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }
            }

            reconciliation
        }
    }

    override suspend fun findReconciliationById(
        tenantId: String,
        reconciliationId: String
    ): SubstrateReservationReconciliation? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_reservation_reconciliations WHERE tenant_id = ? AND reconciliation_id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, reconciliationId)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    val recon = mapReconciliation(rs)
                    val discrepancies = findDiscrepancies(conn, tenantId, reconciliationId)
                    recon.copy(discrepancies = discrepancies)
                } else null
            }
        }
    }

    override suspend fun findLatestReconciliationByReservation(
        tenantId: String,
        reservationId: String
    ): SubstrateReservationReconciliation? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM substrate_reservation_reconciliations WHERE tenant_id = ? AND reservation_id = ? ORDER BY reconciled_at DESC LIMIT 1"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, reservationId)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    val recon = mapReconciliation(rs)
                    val discrepancies = findDiscrepancies(conn, tenantId, recon.reconciliationId)
                    recon.copy(discrepancies = discrepancies)
                } else null
            }
        }
    }

    override suspend fun insertAiHandoffSnapshot(
        tenantId: String,
        handoff: Module19Step06EnterpriseReservationHandoffContract,
        payloadJson: String,
        generatedBy: String
    ) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO substrate_enterprise_ai_handoff_records (
                    handoff_id, tenant_id, reservation_id, order_id, job_id, contract_version,
                    reservation_status, reconciliation_status, integrity_status, master_integrity_hash,
                    payload_json, generated_by, generated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (handoff_id) DO NOTHING
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, java.util.UUID.randomUUID().toString())
                stmt.setString(2, tenantId)
                stmt.setString(3, handoff.reservationId)
                stmt.setString(4, handoff.orderId)
                stmt.setString(5, handoff.jobId)
                stmt.setString(6, handoff.contractVersion)
                stmt.setString(7, handoff.reservationStatus)
                stmt.setString(8, handoff.reconciliationStatus)
                stmt.setString(9, handoff.integrityStatus)
                stmt.setString(10, handoff.masterIntegrityHash)
                stmt.setString(11, payloadJson)
                stmt.setString(12, generatedBy)
                stmt.setLong(13, System.currentTimeMillis())
                stmt.executeUpdate()
            }
        }
    }

    override suspend fun findLatestAiHandoffPayload(
        tenantId: String,
        reservationId: String
    ): String? {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT payload_json FROM substrate_enterprise_ai_handoff_records WHERE tenant_id = ? AND reservation_id = ? ORDER BY generated_at DESC LIMIT 1"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, tenantId)
                stmt.setString(2, reservationId)
                val rs = stmt.executeQuery()
                if (rs.next()) rs.getString("payload_json") else null
            }
        }
    }

    override suspend fun computeGovernanceSummary(tenantId: String): EnterpriseReservationGovernanceSummary {
        return transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val totalAuditedSql = "SELECT COUNT(DISTINCT reservation_id) FROM substrate_enterprise_audits WHERE tenant_id = ?"
            var totalAudited = 0L
            conn.prepareStatement(totalAuditedSql).use { stmt ->
                stmt.setString(1, tenantId)
                val rs = stmt.executeQuery()
                if (rs.next()) totalAudited = rs.getLong(1)
            }

            val hardAllocSql = "SELECT COUNT(*) FROM substrate_enterprise_audits WHERE tenant_id = ? AND event_type = 'HARD_ALLOCATED'"
            var hardAlloc = 0L
            conn.prepareStatement(hardAllocSql).use { stmt ->
                stmt.setString(1, tenantId)
                val rs = stmt.executeQuery()
                if (rs.next()) hardAlloc = rs.getLong(1)
            }

            val softResSql = "SELECT COUNT(*) FROM substrate_enterprise_audits WHERE tenant_id = ? AND event_type = 'SOFT_RESERVED'"
            var softRes = 0L
            conn.prepareStatement(softResSql).use { stmt ->
                stmt.setString(1, tenantId)
                val rs = stmt.executeQuery()
                if (rs.next()) softRes = rs.getLong(1)
            }

            val healthyReconSql = "SELECT COUNT(*) FROM substrate_reservation_reconciliations WHERE tenant_id = ? AND status = 'HEALTHY'"
            var healthyRecon = 0L
            conn.prepareStatement(healthyReconSql).use { stmt ->
                stmt.setString(1, tenantId)
                val rs = stmt.executeQuery()
                if (rs.next()) healthyRecon = rs.getLong(1)
            }

            val discReconSql = "SELECT COUNT(*) FROM substrate_reservation_reconciliations WHERE tenant_id = ? AND status IN ('DISCREPANCIES_DETECTED', 'WARNING_DETECTED')"
            var discRecon = 0L
            conn.prepareStatement(discReconSql).use { stmt ->
                stmt.setString(1, tenantId)
                val rs = stmt.executeQuery()
                if (rs.next()) discRecon = rs.getLong(1)
            }

            EnterpriseReservationGovernanceSummary(
                totalReservationsAudited = totalAudited,
                activeHardAllocations = hardAlloc,
                activeSoftReservations = softRes,
                reconciledHealthyCount = healthyRecon,
                discrepanciesDetectedCount = discRecon,
                integrityVerifiedIntactCount = totalAudited,
                integrityViolationsCount = 0L,
                pendingReplenishmentAlertsCount = 0L,
                activeReleaseReviewsCount = 0L
            )
        }
    }

    private fun findDiscrepancies(conn: java.sql.Connection, tenantId: String, reconciliationId: String): List<SubstrateReconciliationDiscrepancy> {
        val sql = "SELECT * FROM substrate_reconciliation_discrepancies WHERE tenant_id = ? AND reconciliation_id = ?"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, tenantId)
            stmt.setString(2, reconciliationId)
            val rs = stmt.executeQuery()
            val list = mutableListOf<SubstrateReconciliationDiscrepancy>()
            while (rs.next()) {
                list.add(
                    SubstrateReconciliationDiscrepancy(
                        discrepancyId = rs.getString("discrepancy_id"),
                        reconciliationId = rs.getString("reconciliation_id"),
                        tenantId = rs.getString("tenant_id"),
                        discrepancyType = ReconciliationDiscrepancyType.valueOf(rs.getString("discrepancy_type")),
                        severity = ReconciliationDiscrepancySeverity.valueOf(rs.getString("severity")),
                        fieldOrContext = rs.getString("field_or_context"),
                        expectedValue = rs.getString("expected_value"),
                        actualValue = rs.getString("actual_value"),
                        explanation = rs.getString("explanation"),
                        resolutionRecommendation = rs.getString("resolution_recommendation")
                    )
                )
            }
            return list
        }
    }

    private fun mapAuditRecord(rs: ResultSet): SubstrateEnterpriseAuditRecord {
        return SubstrateEnterpriseAuditRecord(
            auditId = rs.getString("audit_id"),
            tenantId = rs.getString("tenant_id"),
            reservationId = rs.getString("reservation_id"),
            reservationVersion = rs.getLong("reservation_version"),
            jobId = rs.getString("job_id"),
            orderId = rs.getString("order_id"),
            orderItemId = rs.getString("order_item_id"),
            substrateRequirementId = rs.getString("substrate_requirement_id"),
            batchLotId = rs.getString("batch_lot_id"),
            warehouseId = rs.getString("warehouse_id"),
            eventType = ReservationAuditEventType.valueOf(rs.getString("event_type")),
            previousState = rs.getString("previous_state"),
            newState = rs.getString("new_state"),
            actorType = AuditActorType.valueOf(rs.getString("actor_type")),
            actorId = rs.getString("actor_id"),
            role = rs.getString("role"),
            permissionContext = rs.getString("permission_context"),
            timestamp = rs.getLong("timestamp"),
            reason = rs.getString("reason"),
            correlationId = rs.getString("correlation_id"),
            traceId = rs.getString("trace_id"),
            idempotencyKey = rs.getString("idempotency_key"),
            sourceModule = rs.getString("source_module"),
            sourceOperation = rs.getString("source_operation"),
            eventOutboxId = rs.getString("event_outbox_id"),
            recordHash = rs.getString("record_hash"),
            previousAuditHash = rs.getString("previous_audit_hash"),
            chainHash = rs.getString("chain_hash")
        )
    }

    private fun mapReconciliation(rs: ResultSet): SubstrateReservationReconciliation {
        return SubstrateReservationReconciliation(
            reconciliationId = rs.getString("reconciliation_id"),
            tenantId = rs.getString("tenant_id"),
            reservationId = rs.getString("reservation_id"),
            orderId = rs.getString("order_id"),
            jobId = rs.getString("job_id"),
            sku = rs.getString("sku"),
            requiredSheets = rs.getLong("required_sheets"),
            reservedSheets = rs.getLong("reserved_sheets"),
            physicalOnHandSheets = rs.getLong("physical_on_hand_sheets"),
            allocatedBatchSheets = rs.getLong("allocated_batch_sheets"),
            releasableSheets = rs.getLong("releasable_sheets"),
            consumedSheets = rs.getLong("consumed_sheets"),
            committedSheets = rs.getLong("committed_sheets"),
            replenishmentRequiredSheets = rs.getLong("replenishment_required_sheets"),
            status = ReconciliationStatus.valueOf(rs.getString("status")),
            discrepancies = emptyList(),
            reconciledBy = rs.getString("reconciled_by"),
            reconciledAt = rs.getLong("reconciled_at"),
            integrityHash = rs.getString("integrity_hash"),
            notes = rs.getString("notes")
        )
    }
}

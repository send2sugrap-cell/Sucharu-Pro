package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.profitability.ProfitabilityDataSource
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.sql.ResultSet

class PostgresProfitabilityDataSource(
    private val transactionManager: TransactionManager
) : ProfitabilityDataSource {

    override suspend fun saveSnapshot(snapshot: ProfitabilitySnapshot): ProfitabilitySnapshot {
        return transactionManager.inTransaction(TenantContext(snapshot.projectId)) { tx ->
            val sqlSnapshot = """
                INSERT INTO profitability_analysis_snapshots (
                    id, tenant_id, project_id, scope, target_entity_id, period_id, currency,
                    revenue, direct_cost, indirect_cost, total_cost, gross_profit, gross_margin_percentage,
                    baseline_cost, cost_variance, revenue_variance, margin_variance,
                    calculation_version, source_integrity_status, financial_handoff_verified,
                    handoff_checksum, integrity_notes, cost_breakdown_json, generated_by, generated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    revenue = EXCLUDED.revenue,
                    direct_cost = EXCLUDED.direct_cost,
                    indirect_cost = EXCLUDED.indirect_cost,
                    total_cost = EXCLUDED.total_cost,
                    gross_profit = EXCLUDED.gross_profit,
                    gross_margin_percentage = EXCLUDED.gross_margin_percentage,
                    baseline_cost = EXCLUDED.baseline_cost,
                    cost_variance = EXCLUDED.cost_variance,
                    revenue_variance = EXCLUDED.revenue_variance,
                    margin_variance = EXCLUDED.margin_variance,
                    calculation_version = EXCLUDED.calculation_version,
                    source_integrity_status = EXCLUDED.source_integrity_status,
                    financial_handoff_verified = EXCLUDED.financial_handoff_verified,
                    handoff_checksum = EXCLUDED.handoff_checksum,
                    integrity_notes = EXCLUDED.integrity_notes,
                    cost_breakdown_json = EXCLUDED.cost_breakdown_json,
                    generated_by = EXCLUDED.generated_by,
                    generated_at = EXCLUDED.generated_at
            """.trimIndent()

            tx.connection.prepareStatement(sqlSnapshot).use { ps ->
                ps.setString(1, snapshot.id)
                ps.setString(2, snapshot.tenantId)
                ps.setString(3, snapshot.projectId)
                ps.setString(4, snapshot.scope.name)
                ps.setString(5, snapshot.targetEntityId)
                ps.setString(6, snapshot.periodId)
                ps.setString(7, snapshot.currency)
                ps.setBigDecimal(8, snapshot.metrics.revenue)
                ps.setBigDecimal(9, snapshot.metrics.directCost)
                ps.setBigDecimal(10, snapshot.metrics.indirectCost)
                ps.setBigDecimal(11, snapshot.metrics.totalCost)
                ps.setBigDecimal(12, snapshot.metrics.grossProfit)
                ps.setBigDecimal(13, snapshot.metrics.grossMarginPercentage)
                ps.setBigDecimal(14, snapshot.metrics.baselineCost)
                ps.setBigDecimal(15, snapshot.metrics.costVariance)
                ps.setBigDecimal(16, snapshot.metrics.revenueVariance)
                ps.setBigDecimal(17, snapshot.metrics.marginVariance)
                ps.setString(18, snapshot.calculationVersion)
                ps.setString(19, snapshot.sourceIntegrityStatus.name)
                ps.setBoolean(20, snapshot.financialHandoffVerified)
                ps.setString(21, snapshot.handoffChecksum)
                ps.setString(22, snapshot.integrityNotes.joinToString("||"))
                ps.setString(23, null)
                ps.setString(24, snapshot.generatedBy)
                ps.setLong(25, snapshot.generatedAt)
                ps.executeUpdate()
            }

            // Save attributions
            if (snapshot.costAttributions.isNotEmpty()) {
                val sqlAttribution = """
                    INSERT INTO profitability_cost_attributions (
                        id, snapshot_id, tenant_id, project_id, source_type, source_id,
                        component_type, job_id, order_id, product_id, customer_id, vendor_id,
                        period_id, attribution_basis, source_amount, attributable_amount,
                        currency, recorded_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO NOTHING
                """.trimIndent()
                tx.connection.prepareStatement(sqlAttribution).use { ps ->
                    for (attr in snapshot.costAttributions) {
                        ps.setString(1, attr.id)
                        ps.setString(2, snapshot.id)
                        ps.setString(3, attr.tenantId)
                        ps.setString(4, attr.projectId)
                        ps.setString(5, attr.sourceType.name)
                        ps.setString(6, attr.sourceId)
                        ps.setString(7, attr.componentType.name)
                        ps.setString(8, attr.jobId)
                        ps.setString(9, attr.orderId)
                        ps.setString(10, attr.productId)
                        ps.setString(11, attr.customerId)
                        ps.setString(12, attr.vendorId)
                        ps.setString(13, attr.periodId)
                        ps.setString(14, attr.attributionBasis)
                        ps.setBigDecimal(15, attr.sourceAmount)
                        ps.setBigDecimal(16, attr.attributableAmount)
                        ps.setString(17, attr.currency)
                        ps.setLong(18, attr.recordedAt)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }

            // Save revenue provenances
            if (snapshot.revenueProvenances.isNotEmpty()) {
                val sqlRevenue = """
                    INSERT INTO profitability_revenue_provenances (
                        id, snapshot_id, tenant_id, project_id, canonical_source_type,
                        canonical_source_id, customer_id, order_id, job_id, period_id,
                        recognized_amount, currency, recognition_state, source_timestamp
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO NOTHING
                """.trimIndent()
                tx.connection.prepareStatement(sqlRevenue).use { ps ->
                    for (rev in snapshot.revenueProvenances) {
                        ps.setString(1, rev.id)
                        ps.setString(2, snapshot.id)
                        ps.setString(3, rev.tenantId)
                        ps.setString(4, rev.projectId)
                        ps.setString(5, rev.canonicalSourceType.name)
                        ps.setString(6, rev.canonicalSourceId)
                        ps.setString(7, rev.customerId)
                        ps.setString(8, rev.orderId)
                        ps.setString(9, rev.jobId)
                        ps.setString(10, rev.periodId)
                        ps.setBigDecimal(11, rev.recognizedAmount)
                        ps.setString(12, rev.currency)
                        ps.setString(13, rev.recognitionState)
                        ps.setLong(14, rev.sourceTimestamp)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }

            snapshot
        }
    }

    override suspend fun findSnapshotById(tenantId: String, projectId: String, id: String): ProfitabilitySnapshot? {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val sql = "SELECT * FROM profitability_analysis_snapshots WHERE tenant_id = ? AND project_id = ? AND id = ?"
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, projectId)
                ps.setString(3, id)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        mapSnapshot(rs)
                    } else null
                }
            }
        }
    }

    override suspend fun listSnapshots(
        tenantId: String,
        projectId: String,
        scope: ProfitabilityScope?,
        targetEntityId: String?,
        periodId: String?,
        limit: Int,
        offset: Int
    ): List<ProfitabilitySnapshot> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (scope != null) {
                conditions.add("scope = ?")
                params.add(scope.name)
            }
            if (targetEntityId != null) {
                conditions.add("target_entity_id = ?")
                params.add(targetEntityId)
            }
            if (periodId != null) {
                conditions.add("period_id = ?")
                params.add(periodId)
            }

            val sql = "SELECT * FROM profitability_analysis_snapshots WHERE ${conditions.joinToString(" AND ")} ORDER BY generated_at DESC LIMIT ? OFFSET ?"
            tx.connection.prepareStatement(sql).use { ps ->
                var idx = 1
                for (p in params) {
                    when (p) {
                        is String -> ps.setString(idx++, p)
                        is Long -> ps.setLong(idx++, p)
                        is Int -> ps.setInt(idx++, p)
                        else -> ps.setObject(idx++, p)
                    }
                }
                ps.setInt(idx++, limit)
                ps.setInt(idx, offset)

                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProfitabilitySnapshot>()
                    while (rs.next()) {
                        list.add(mapSnapshot(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun saveReconciliationEvent(event: ProfitabilityReconciliationEvent): ProfitabilityReconciliationEvent {
        return transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            val sql = """
                INSERT INTO profitability_reconciliation_events (
                    id, tenant_id, project_id, snapshot_id, scope, target_entity_id,
                    period_id, is_reconciled, canonical_revenue_total, snapshot_revenue_total,
                    revenue_difference, canonical_cost_total, snapshot_cost_total, cost_difference,
                    discrepancies, checked_by, checked_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, event.id)
                ps.setString(2, event.tenantId)
                ps.setString(3, event.projectId)
                ps.setString(4, event.snapshotId)
                ps.setString(5, event.scope.name)
                ps.setString(6, event.targetEntityId)
                ps.setString(7, event.periodId)
                ps.setBoolean(8, event.isReconciled)
                ps.setBigDecimal(9, event.canonicalRevenueTotal)
                ps.setBigDecimal(10, event.snapshotRevenueTotal)
                ps.setBigDecimal(11, event.revenueDifference)
                ps.setBigDecimal(12, event.canonicalCostTotal)
                ps.setBigDecimal(13, event.snapshotCostTotal)
                ps.setBigDecimal(14, event.costDifference)
                ps.setString(15, event.discrepancies.joinToString("||"))
                ps.setString(16, event.checkedBy)
                ps.setLong(17, event.checkedAt)
                ps.executeUpdate()
            }
            event
        }
    }

    override suspend fun listReconciliationEvents(
        tenantId: String,
        projectId: String,
        snapshotId: String?,
        limit: Int,
        offset: Int
    ): List<ProfitabilityReconciliationEvent> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (snapshotId != null) {
                conditions.add("snapshot_id = ?")
                params.add(snapshotId)
            }

            val sql = "SELECT * FROM profitability_reconciliation_events WHERE ${conditions.joinToString(" AND ")} ORDER BY checked_at DESC LIMIT ? OFFSET ?"
            tx.connection.prepareStatement(sql).use { ps ->
                var idx = 1
                for (p in params) {
                    when (p) {
                        is String -> ps.setString(idx++, p)
                        else -> ps.setObject(idx++, p)
                    }
                }
                ps.setInt(idx++, limit)
                ps.setInt(idx, offset)

                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProfitabilityReconciliationEvent>()
                    while (rs.next()) {
                        list.add(mapReconciliationEvent(rs))
                    }
                    list
                }
            }
        }
    }

    override suspend fun recordAuditEvent(event: ProfitabilityAuditEvent): ProfitabilityAuditEvent {
        return transactionManager.inTransaction(TenantContext(event.projectId)) { tx ->
            val sql = """
                INSERT INTO profitability_audit_events (
                    id, tenant_id, project_id, snapshot_id, action, scope,
                    target_entity_id, outcome, details, actor, timestamp, correlation_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
            """.trimIndent()
            tx.connection.prepareStatement(sql).use { ps ->
                ps.setString(1, event.id)
                ps.setString(2, event.tenantId)
                ps.setString(3, event.projectId)
                ps.setString(4, event.snapshotId)
                ps.setString(5, event.action)
                ps.setString(6, event.scope?.name)
                ps.setString(7, event.targetEntityId)
                ps.setString(8, event.outcome)
                ps.setString(9, event.details)
                ps.setString(10, event.actor)
                ps.setLong(11, event.timestamp)
                ps.setString(12, event.correlationId)
                ps.executeUpdate()
            }
            event
        }
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        snapshotId: String?,
        limit: Int,
        offset: Int
    ): List<ProfitabilityAuditEvent> {
        return transactionManager.inTransaction(TenantContext(projectId)) { tx ->
            val conditions = mutableListOf("tenant_id = ?", "project_id = ?")
            val params = mutableListOf<Any>(tenantId, projectId)

            if (snapshotId != null) {
                conditions.add("snapshot_id = ?")
                params.add(snapshotId)
            }

            val sql = "SELECT * FROM profitability_audit_events WHERE ${conditions.joinToString(" AND ")} ORDER BY timestamp DESC LIMIT ? OFFSET ?"
            tx.connection.prepareStatement(sql).use { ps ->
                var idx = 1
                for (p in params) {
                    when (p) {
                        is String -> ps.setString(idx++, p)
                        else -> ps.setObject(idx++, p)
                    }
                }
                ps.setInt(idx++, limit)
                ps.setInt(idx, offset)

                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ProfitabilityAuditEvent>()
                    while (rs.next()) {
                        list.add(mapAuditEvent(rs))
                    }
                    list
                }
            }
        }
    }

    private fun mapSnapshot(rs: ResultSet): ProfitabilitySnapshot {
        val notesStr = rs.getString("integrity_notes")
        val notes = if (!notesStr.isNullOrBlank()) notesStr.split("||") else emptyList()

        return ProfitabilitySnapshot(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            scope = ProfitabilityScope.valueOf(rs.getString("scope")),
            targetEntityId = rs.getString("target_entity_id"),
            periodId = rs.getString("period_id"),
            currency = rs.getString("currency") ?: "BDT",
            metrics = ProfitabilityMetric(
                revenue = rs.getBigDecimal("revenue") ?: BigDecimal.ZERO,
                directCost = rs.getBigDecimal("direct_cost") ?: BigDecimal.ZERO,
                indirectCost = rs.getBigDecimal("indirect_cost") ?: BigDecimal.ZERO,
                totalCost = rs.getBigDecimal("total_cost") ?: BigDecimal.ZERO,
                grossProfit = rs.getBigDecimal("gross_profit") ?: BigDecimal.ZERO,
                grossMarginPercentage = rs.getBigDecimal("gross_margin_percentage") ?: BigDecimal.ZERO,
                baselineCost = rs.getBigDecimal("baseline_cost"),
                costVariance = rs.getBigDecimal("cost_variance"),
                revenueVariance = rs.getBigDecimal("revenue_variance"),
                marginVariance = rs.getBigDecimal("margin_variance")
            ),
            calculationVersion = rs.getString("calculation_version") ?: "1.0.0",
            sourceIntegrityStatus = SourceIntegrityStatus.valueOf(rs.getString("source_integrity_status") ?: "VERIFIED"),
            financialHandoffVerified = rs.getBoolean("financial_handoff_verified"),
            handoffChecksum = rs.getString("handoff_checksum"),
            integrityNotes = notes,
            generatedBy = rs.getString("generated_by"),
            generatedAt = rs.getLong("generated_at")
        )
    }

    private fun mapReconciliationEvent(rs: ResultSet): ProfitabilityReconciliationEvent {
        val discStr = rs.getString("discrepancies")
        val disc = if (!discStr.isNullOrBlank()) discStr.split("||") else emptyList()

        return ProfitabilityReconciliationEvent(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            snapshotId = rs.getString("snapshot_id"),
            scope = ProfitabilityScope.valueOf(rs.getString("scope")),
            targetEntityId = rs.getString("target_entity_id"),
            periodId = rs.getString("period_id"),
            isReconciled = rs.getBoolean("is_reconciled"),
            canonicalRevenueTotal = rs.getBigDecimal("canonical_revenue_total") ?: BigDecimal.ZERO,
            snapshotRevenueTotal = rs.getBigDecimal("snapshot_revenue_total") ?: BigDecimal.ZERO,
            revenueDifference = rs.getBigDecimal("revenue_difference") ?: BigDecimal.ZERO,
            canonicalCostTotal = rs.getBigDecimal("canonical_cost_total") ?: BigDecimal.ZERO,
            snapshotCostTotal = rs.getBigDecimal("snapshot_cost_total") ?: BigDecimal.ZERO,
            costDifference = rs.getBigDecimal("cost_difference") ?: BigDecimal.ZERO,
            discrepancies = disc,
            checkedBy = rs.getString("checked_by"),
            checkedAt = rs.getLong("checked_at")
        )
    }

    private fun mapAuditEvent(rs: ResultSet): ProfitabilityAuditEvent {
        return ProfitabilityAuditEvent(
            id = rs.getString("id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            snapshotId = rs.getString("snapshot_id"),
            action = rs.getString("action"),
            scope = rs.getString("scope")?.let { ProfitabilityScope.valueOf(it) },
            targetEntityId = rs.getString("target_entity_id"),
            outcome = rs.getString("outcome"),
            details = rs.getString("details"),
            actor = rs.getString("actor"),
            timestamp = rs.getLong("timestamp"),
            correlationId = rs.getString("correlation_id")
        )
    }
}

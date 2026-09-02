package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.profitability.PeriodProfitabilityDataSource
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet
import java.util.UUID

/**
 * PostgreSQL implementation of PeriodProfitabilityDataSource with parameterized queries.
 * Module 16 Step 06.
 */
class PostgresPeriodProfitabilityDataSource(
    private val transactionManager: TransactionManager
) : PeriodProfitabilityDataSource {

    override suspend fun saveSnapshot(snapshot: PeriodProfitabilitySnapshot): PeriodProfitabilitySnapshot {
        return transactionManager.inTransaction(TenantContext(snapshot.projectId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO period_profitability_snapshots (
                    snapshot_id, tenant_id, project_id, period_id, period_type, period_start, period_end,
                    timezone, period_key, fiscal_period_id, period_status, currency, calculation_version,
                    snapshot_version, supersedes_snapshot_id, superseded_by_snapshot_id, generated_at,
                    generated_by, source_as_of, revenue, total_actual_cost, gross_profit, gross_margin_percentage,
                    cost_to_revenue_percentage, direct_cost, indirect_cost, contribution_amount,
                    contribution_margin_percentage, baseline_revenue, baseline_cost, revenue_variance,
                    revenue_variance_percentage, cost_variance, cost_variance_percentage, profit_variance,
                    profit_variance_percentage, job_count, completed_job_count, product_count, customer_count,
                    vendor_count, total_units, average_revenue_per_job, average_profit_per_job,
                    average_revenue_per_unit, average_cost_per_unit, average_profit_per_unit,
                    profitability_classification, trend_direction, source_readiness, provenance_fingerprints,
                    integrity_hash, is_certified, certified_at, certificate_id, warnings, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (snapshot_id) DO UPDATE SET
                    period_status = EXCLUDED.period_status,
                    is_certified = EXCLUDED.is_certified,
                    certified_at = EXCLUDED.certified_at,
                    certificate_id = EXCLUDED.certificate_id,
                    superseded_by_snapshot_id = EXCLUDED.superseded_by_snapshot_id
            """.trimIndent()

            conn.prepareStatement(sql).use { ps ->
                var i = 1
                ps.setString(i++, snapshot.snapshotId)
                ps.setString(i++, snapshot.tenantId)
                ps.setString(i++, snapshot.projectId)
                ps.setString(i++, snapshot.periodId)
                ps.setString(i++, snapshot.periodType.name)
                ps.setLong(i++, snapshot.periodStart)
                ps.setLong(i++, snapshot.periodEnd)
                ps.setString(i++, snapshot.timezone)
                ps.setString(i++, snapshot.periodKey)
                ps.setString(i++, snapshot.fiscalPeriodId)
                ps.setString(i++, snapshot.periodStatus.name)
                ps.setString(i++, snapshot.currency)
                ps.setString(i++, snapshot.calculationVersion)
                ps.setInt(i++, snapshot.snapshotVersion)
                ps.setString(i++, snapshot.supersedesSnapshotId)
                ps.setString(i++, snapshot.supersededBySnapshotId)
                ps.setLong(i++, snapshot.generatedAt)
                ps.setString(i++, snapshot.generatedBy)
                ps.setLong(i++, snapshot.sourceAsOf)
                ps.setBigDecimal(i++, snapshot.revenue)
                ps.setBigDecimal(i++, snapshot.totalActualCost)
                ps.setBigDecimal(i++, snapshot.grossProfit)
                ps.setObject(i++, snapshot.grossMarginPercentage)
                ps.setObject(i++, snapshot.costToRevenuePercentage)
                ps.setBigDecimal(i++, snapshot.directCost)
                ps.setBigDecimal(i++, snapshot.indirectCost)
                ps.setBigDecimal(i++, snapshot.contributionAmount)
                ps.setObject(i++, snapshot.contributionMarginPercentage)
                ps.setObject(i++, snapshot.baselineRevenue)
                ps.setObject(i++, snapshot.baselineCost)
                ps.setObject(i++, snapshot.revenueVariance)
                ps.setObject(i++, snapshot.revenueVariancePercentage)
                ps.setObject(i++, snapshot.costVariance)
                ps.setObject(i++, snapshot.costVariancePercentage)
                ps.setObject(i++, snapshot.profitVariance)
                ps.setObject(i++, snapshot.profitVariancePercentage)
                ps.setInt(i++, snapshot.jobCount)
                ps.setInt(i++, snapshot.completedJobCount)
                ps.setInt(i++, snapshot.productCount)
                ps.setInt(i++, snapshot.customerCount)
                ps.setInt(i++, snapshot.vendorCount)
                ps.setLong(i++, snapshot.totalUnits)
                ps.setObject(i++, snapshot.averageRevenuePerJob)
                ps.setObject(i++, snapshot.averageProfitPerJob)
                ps.setObject(i++, snapshot.averageRevenuePerUnit)
                ps.setObject(i++, snapshot.averageCostPerUnit)
                ps.setObject(i++, snapshot.averageProfitPerUnit)
                ps.setString(i++, snapshot.profitabilityClassification.name)
                ps.setString(i++, snapshot.trendDirection.name)
                ps.setString(i++, snapshot.sourceReadiness.name)
                ps.setArray(i++, conn.createArrayOf("text", snapshot.provenanceFingerprints.toTypedArray()))
                ps.setString(i++, snapshot.integrityHash)
                ps.setBoolean(i++, snapshot.isCertified)
                ps.setObject(i++, snapshot.certifiedAt)
                ps.setString(i++, snapshot.certificateId)
                ps.setArray(i++, conn.createArrayOf("text", snapshot.warnings.toTypedArray()))
                ps.setLong(i++, snapshot.generatedAt)
                ps.executeUpdate()
            }

            // Save Cost Components
            if (snapshot.costBreakdown.isNotEmpty()) {
                val compSql = """
                    INSERT INTO period_profitability_cost_components (
                        component_id, snapshot_id, tenant_id, period_id, component_type,
                        amount, percentage_of_total_cost, percentage_of_revenue, source_attribution_count
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                conn.prepareStatement(compSql).use { ps ->
                    for (item in snapshot.costBreakdown) {
                        ps.setString(1, UUID.randomUUID().toString())
                        ps.setString(2, snapshot.snapshotId)
                        ps.setString(3, snapshot.tenantId)
                        ps.setString(4, snapshot.periodId)
                        ps.setString(5, item.componentType.name)
                        ps.setBigDecimal(6, item.amount)
                        ps.setBigDecimal(7, item.percentageOfTotalCost)
                        ps.setObject(8, item.percentageOfRevenue)
                        ps.setInt(9, item.sourceAttributionCount)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }

            // Save Revenue Attributions
            if (snapshot.revenueAttributions.isNotEmpty()) {
                val revSql = """
                    INSERT INTO period_profitability_revenue_attributions (
                        attribution_id, snapshot_id, tenant_id, period_id, attribution_dimension,
                        dimension_id, dimension_name, amount, percentage_of_total_revenue,
                        source_module, source_entity_type, source_entity_id
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                conn.prepareStatement(revSql).use { ps ->
                    for (item in snapshot.revenueAttributions) {
                        ps.setString(1, UUID.randomUUID().toString())
                        ps.setString(2, snapshot.snapshotId)
                        ps.setString(3, snapshot.tenantId)
                        ps.setString(4, snapshot.periodId)
                        ps.setString(5, item.attributionDimension)
                        ps.setString(6, item.dimensionId)
                        ps.setString(7, item.dimensionName)
                        ps.setBigDecimal(8, item.amount)
                        ps.setBigDecimal(9, item.percentageOfTotalRevenue)
                        ps.setString(10, item.sourceModule)
                        ps.setString(11, item.sourceEntityType)
                        ps.setString(12, item.sourceEntityId)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }

            snapshot
        }
    }

    override suspend fun findSnapshotById(tenantId: String, snapshotId: String): PeriodProfitabilitySnapshot? {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM period_profitability_snapshots WHERE tenant_id = ? AND snapshot_id = ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, snapshotId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        val snap = mapSnapshot(rs)
                        val comps = loadCostComponents(conn, snap.snapshotId)
                        val revs = loadRevenueAttributions(conn, snap.snapshotId)
                        snap.copy(costBreakdown = comps, revenueAttributions = revs)
                    } else null
                }
            }
        }
    }

    override suspend fun findLatestSnapshotByPeriodId(tenantId: String, periodId: String): PeriodProfitabilitySnapshot? {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM period_profitability_snapshots WHERE tenant_id = ? AND period_id = ? ORDER BY generated_at DESC LIMIT 1"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, periodId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        val snap = mapSnapshot(rs)
                        val comps = loadCostComponents(conn, snap.snapshotId)
                        val revs = loadRevenueAttributions(conn, snap.snapshotId)
                        snap.copy(costBreakdown = comps, revenueAttributions = revs)
                    } else null
                }
            }
        }
    }

    override suspend fun listSnapshots(tenantId: String, filter: PeriodProfitabilityFilter): List<PeriodProfitabilitySnapshot> {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = StringBuilder("SELECT * FROM period_profitability_snapshots WHERE tenant_id = ?")
            val params = mutableListOf<Any>(tenantId)

            if (filter.periodType != null) {
                sql.append(" AND period_type = ?")
                params.add(filter.periodType.name)
            }
            if (filter.status != null) {
                sql.append(" AND period_status = ?")
                params.add(filter.status.name)
            }
            if (filter.periodStartFrom != null) {
                sql.append(" AND period_start >= ?")
                params.add(filter.periodStartFrom)
            }
            if (filter.periodEndTo != null) {
                sql.append(" AND period_end <= ?")
                params.add(filter.periodEndTo)
            }
            sql.append(" ORDER BY generated_at DESC LIMIT ? OFFSET ?")
            params.add(filter.limit)
            params.add(filter.offset)

            val results = mutableListOf<PeriodProfitabilitySnapshot>()
            conn.prepareStatement(sql.toString()).use { ps ->
                params.forEachIndexed { idx, param -> ps.setObject(idx + 1, param) }
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        results.add(mapSnapshot(rs))
                    }
                }
            }
            results
        }
    }

    override suspend fun saveProvenanceRecords(records: List<PeriodProfitabilityProvenanceRecord>) {
        if (records.isEmpty()) return
        transactionManager.inTransaction(TenantContext(records.first().projectId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO period_profitability_provenances (
                    provenance_id, tenant_id, project_id, period_id, source_module,
                    source_entity_type, source_entity_id, source_transaction_id, source_snapshot_id,
                    amount, component_type, attribution_dimension, fingerprint, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                for (r in records) {
                    ps.setString(1, r.provenanceId)
                    ps.setString(2, r.tenantId)
                    ps.setString(3, r.projectId)
                    ps.setString(4, r.periodId)
                    ps.setString(5, r.sourceModule)
                    ps.setString(6, r.sourceEntityType)
                    ps.setString(7, r.sourceEntityId)
                    ps.setString(8, r.sourceTransactionId)
                    ps.setString(9, r.sourceSnapshotId)
                    ps.setBigDecimal(10, r.amount)
                    ps.setString(11, r.componentType?.name)
                    ps.setString(12, r.attributionDimension)
                    ps.setString(13, r.fingerprint)
                    ps.setLong(14, r.createdAt)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }

    override suspend fun listProvenanceByPeriodId(tenantId: String, periodId: String): List<PeriodProfitabilityProvenanceRecord> {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM period_profitability_provenances WHERE tenant_id = ? AND period_id = ? ORDER BY created_at ASC"
            val list = mutableListOf<PeriodProfitabilityProvenanceRecord>()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, periodId)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            PeriodProfitabilityProvenanceRecord(
                                provenanceId = rs.getString("provenance_id"),
                                tenantId = rs.getString("tenant_id"),
                                projectId = rs.getString("project_id"),
                                periodId = rs.getString("period_id"),
                                sourceModule = rs.getString("source_module"),
                                sourceEntityType = rs.getString("source_entity_type"),
                                sourceEntityId = rs.getString("source_entity_id"),
                                sourceTransactionId = rs.getString("source_transaction_id"),
                                sourceSnapshotId = rs.getString("source_snapshot_id"),
                                amount = rs.getBigDecimal("amount"),
                                componentType = rs.getString("component_type")?.let { JobCostComponentType.valueOf(it) },
                                attributionDimension = rs.getString("attribution_dimension"),
                                fingerprint = rs.getString("fingerprint"),
                                createdAt = rs.getLong("created_at")
                            )
                        )
                    }
                }
            }
            list
        }
    }

    override suspend fun saveReconciliationEvent(event: PeriodProfitabilityReconciliationEvent): PeriodProfitabilityReconciliationEvent {
        return transactionManager.inTransaction(TenantContext(event.projectId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO period_profitability_reconciliations (
                    event_id, tenant_id, project_id, period_id, snapshot_id, is_balanced,
                    revenue_difference, cost_difference, profit_difference, margin_difference,
                    contribution_difference, child_aggregation_difference, cross_dimensional_difference,
                    assertions_json, error_details, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, event.eventId)
                ps.setString(2, event.tenantId)
                ps.setString(3, event.projectId)
                ps.setString(4, event.periodId)
                ps.setString(5, event.snapshotId)
                ps.setBoolean(6, event.isBalanced)
                ps.setBigDecimal(7, event.revenueDifference)
                ps.setBigDecimal(8, event.costDifference)
                ps.setBigDecimal(9, event.profitDifference)
                ps.setBigDecimal(10, event.marginDifference)
                ps.setBigDecimal(11, event.contributionDifference)
                ps.setBigDecimal(12, event.childAggregationDifference)
                ps.setBigDecimal(13, event.crossDimensionalDifference)
                ps.setString(14, "[]")
                ps.setArray(15, conn.createArrayOf("text", event.errorDetails.toTypedArray()))
                ps.setLong(16, event.timestamp)
                ps.executeUpdate()
            }
            event
        }
    }

    override suspend fun listReconciliationEventsByPeriodId(tenantId: String, periodId: String): List<PeriodProfitabilityReconciliationEvent> {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM period_profitability_reconciliations WHERE tenant_id = ? AND period_id = ? ORDER BY timestamp DESC"
            val list = mutableListOf<PeriodProfitabilityReconciliationEvent>()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, periodId)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        @Suppress("UNCHECKED_CAST")
                        val errs = (rs.getArray("error_details")?.array as? Array<String>)?.toList() ?: emptyList()
                        list.add(
                            PeriodProfitabilityReconciliationEvent(
                                eventId = rs.getString("event_id"),
                                tenantId = rs.getString("tenant_id"),
                                projectId = rs.getString("project_id"),
                                periodId = rs.getString("period_id"),
                                snapshotId = rs.getString("snapshot_id"),
                                isBalanced = rs.getBoolean("is_balanced"),
                                revenueDifference = rs.getBigDecimal("revenue_difference"),
                                costDifference = rs.getBigDecimal("cost_difference"),
                                profitDifference = rs.getBigDecimal("profit_difference"),
                                marginDifference = rs.getBigDecimal("margin_difference"),
                                contributionDifference = rs.getBigDecimal("contribution_difference"),
                                childAggregationDifference = rs.getBigDecimal("child_aggregation_difference"),
                                crossDimensionalDifference = rs.getBigDecimal("cross_dimensional_difference"),
                                errorDetails = errs,
                                timestamp = rs.getLong("timestamp")
                            )
                        )
                    }
                }
            }
            list
        }
    }

    override suspend fun saveAuditEvent(event: PeriodProfitabilityAuditEvent): PeriodProfitabilityAuditEvent {
        return transactionManager.inTransaction(TenantContext(event.projectId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO period_profitability_audit_events (
                    audit_id, tenant_id, project_id, period_id, action, actor_id, actor_role,
                    snapshot_id, calculation_version, previous_state, resulting_state, details,
                    integrity_hash, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, event.auditId)
                ps.setString(2, event.tenantId)
                ps.setString(3, event.projectId)
                ps.setString(4, event.periodId)
                ps.setString(5, event.action)
                ps.setString(6, event.actorId)
                ps.setString(7, event.actorRole)
                ps.setString(8, event.snapshotId)
                ps.setString(9, event.calculationVersion)
                ps.setString(10, event.previousState)
                ps.setString(11, event.resultingState)
                ps.setString(12, event.details)
                ps.setString(13, event.integrityHash)
                ps.setLong(14, event.timestamp)
                ps.executeUpdate()
            }
            event
        }
    }

    override suspend fun listAuditEventsByPeriodId(tenantId: String, periodId: String): List<PeriodProfitabilityAuditEvent> {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT * FROM period_profitability_audit_events WHERE tenant_id = ? AND period_id = ? ORDER BY timestamp DESC"
            val list = mutableListOf<PeriodProfitabilityAuditEvent>()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, periodId)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            PeriodProfitabilityAuditEvent(
                                auditId = rs.getString("audit_id"),
                                tenantId = rs.getString("tenant_id"),
                                projectId = rs.getString("project_id"),
                                periodId = rs.getString("period_id"),
                                action = rs.getString("action"),
                                actorId = rs.getString("actor_id"),
                                actorRole = rs.getString("actor_role"),
                                snapshotId = rs.getString("snapshot_id"),
                                calculationVersion = rs.getString("calculation_version"),
                                previousState = rs.getString("previous_state"),
                                resultingState = rs.getString("resulting_state"),
                                details = rs.getString("details"),
                                integrityHash = rs.getString("integrity_hash"),
                                timestamp = rs.getLong("timestamp")
                            )
                        )
                    }
                }
            }
            list
        }
    }

    override suspend fun saveUnattributedItems(items: List<PeriodUnattributedItem>) {
        if (items.isEmpty()) return
        transactionManager.inTransaction(TenantContext(items.first().projectId)) { ctx ->
            val conn = ctx.connection
            val sql = """
                INSERT INTO period_profitability_unattributed_items (
                    unattributed_id, tenant_id, project_id, period_id, item_type,
                    source_module, source_entity_type, source_entity_id, amount, reason, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                for (item in items) {
                    ps.setString(1, item.unattributedId)
                    ps.setString(2, item.tenantId)
                    ps.setString(3, item.projectId)
                    ps.setString(4, item.periodId)
                    ps.setString(5, item.itemType)
                    ps.setString(6, item.sourceModule)
                    ps.setString(7, item.sourceEntityType)
                    ps.setString(8, item.sourceEntityId)
                    ps.setBigDecimal(9, item.amount)
                    ps.setString(10, item.reason)
                    ps.setLong(11, item.createdAt)
                    ps.addBatch()
                }
                ps.executeBatch()
            }
        }
    }

    override suspend fun listUnattributedItems(tenantId: String, periodId: String?): List<PeriodUnattributedItem> {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = if (periodId != null) {
                "SELECT * FROM period_profitability_unattributed_items WHERE tenant_id = ? AND period_id = ? ORDER BY created_at DESC"
            } else {
                "SELECT * FROM period_profitability_unattributed_items WHERE tenant_id = ? ORDER BY created_at DESC"
            }
            val list = mutableListOf<PeriodUnattributedItem>()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                if (periodId != null) ps.setString(2, periodId)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            PeriodUnattributedItem(
                                unattributedId = rs.getString("unattributed_id"),
                                tenantId = rs.getString("tenant_id"),
                                projectId = rs.getString("project_id"),
                                periodId = rs.getString("period_id"),
                                itemType = rs.getString("item_type"),
                                sourceModule = rs.getString("source_module"),
                                sourceEntityType = rs.getString("source_entity_type"),
                                sourceEntityId = rs.getString("source_entity_id"),
                                amount = rs.getBigDecimal("amount"),
                                reason = rs.getString("reason"),
                                createdAt = rs.getLong("created_at")
                            )
                        )
                    }
                }
            }
            list
        }
    }

    override suspend fun getIdempotentSnapshotId(tenantId: String, idempotencyKey: String): String? {
        return transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "SELECT snapshot_id FROM period_profitability_idempotency_records WHERE tenant_id = ? AND idempotency_key = ?"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, idempotencyKey)
                ps.executeQuery().use { rs ->
                    if (rs.next()) rs.getString("snapshot_id") else null
                }
            }
        }
    }

    override suspend fun saveIdempotencyRecord(tenantId: String, idempotencyKey: String, snapshotId: String) {
        transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
            val conn = ctx.connection
            val sql = "INSERT INTO period_profitability_idempotency_records (tenant_id, idempotency_key, snapshot_id) VALUES (?, ?, ?) ON CONFLICT DO NOTHING"
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, tenantId)
                ps.setString(2, idempotencyKey)
                ps.setString(3, snapshotId)
                ps.executeUpdate()
            }
        }
    }

    private fun loadCostComponents(conn: Connection, snapshotId: String): List<PeriodCostBreakdownItem> {
        val sql = "SELECT * FROM period_profitability_cost_components WHERE snapshot_id = ? ORDER BY amount DESC"
        val list = mutableListOf<PeriodCostBreakdownItem>()
        conn.prepareStatement(sql).use { ps ->
            ps.setString(1, snapshotId)
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    list.add(
                        PeriodCostBreakdownItem(
                            componentType = JobCostComponentType.valueOf(rs.getString("component_type")),
                            amount = rs.getBigDecimal("amount"),
                            percentageOfTotalCost = rs.getBigDecimal("percentage_of_total_cost"),
                            percentageOfRevenue = rs.getBigDecimal("percentage_of_revenue"),
                            sourceAttributionCount = rs.getInt("source_attribution_count")
                        )
                    )
                }
            }
        }
        return list
    }

    private fun loadRevenueAttributions(conn: Connection, snapshotId: String): List<PeriodRevenueAttributionItem> {
        val sql = "SELECT * FROM period_profitability_revenue_attributions WHERE snapshot_id = ? ORDER BY amount DESC"
        val list = mutableListOf<PeriodRevenueAttributionItem>()
        conn.prepareStatement(sql).use { ps ->
            ps.setString(1, snapshotId)
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    list.add(
                        PeriodRevenueAttributionItem(
                            attributionDimension = rs.getString("attribution_dimension"),
                            dimensionId = rs.getString("dimension_id"),
                            dimensionName = rs.getString("dimension_name"),
                            amount = rs.getBigDecimal("amount"),
                            percentageOfTotalRevenue = rs.getBigDecimal("percentage_of_total_revenue"),
                            sourceModule = rs.getString("source_module"),
                            sourceEntityType = rs.getString("source_entity_type"),
                            sourceEntityId = rs.getString("source_entity_id")
                        )
                    )
                }
            }
        }
        return list
    }

    private fun mapSnapshot(rs: ResultSet): PeriodProfitabilitySnapshot {
        @Suppress("UNCHECKED_CAST")
        val fingerprints = (rs.getArray("provenance_fingerprints")?.array as? Array<String>)?.toList() ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val warnings = (rs.getArray("warnings")?.array as? Array<String>)?.toList() ?: emptyList()

        return PeriodProfitabilitySnapshot(
            snapshotId = rs.getString("snapshot_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            periodId = rs.getString("period_id"),
            periodType = PeriodType.valueOf(rs.getString("period_type")),
            periodStart = rs.getLong("period_start"),
            periodEnd = rs.getLong("period_end"),
            timezone = rs.getString("timezone"),
            periodKey = rs.getString("period_key"),
            fiscalPeriodId = rs.getString("fiscal_period_id"),
            periodStatus = PeriodStatus.valueOf(rs.getString("period_status")),
            currency = rs.getString("currency"),
            calculationVersion = rs.getString("calculation_version"),
            snapshotVersion = rs.getInt("snapshot_version"),
            supersedesSnapshotId = rs.getString("supersedes_snapshot_id"),
            supersededBySnapshotId = rs.getString("superseded_by_snapshot_id"),
            generatedAt = rs.getLong("generated_at"),
            generatedBy = rs.getString("generated_by"),
            sourceAsOf = rs.getLong("source_as_of"),
            revenue = rs.getBigDecimal("revenue"),
            totalActualCost = rs.getBigDecimal("total_actual_cost"),
            grossProfit = rs.getBigDecimal("gross_profit"),
            grossMarginPercentage = rs.getBigDecimal("gross_margin_percentage"),
            costToRevenuePercentage = rs.getBigDecimal("cost_to_revenue_percentage"),
            directCost = rs.getBigDecimal("direct_cost"),
            indirectCost = rs.getBigDecimal("indirect_cost"),
            contributionAmount = rs.getBigDecimal("contribution_amount"),
            contributionMarginPercentage = rs.getBigDecimal("contribution_margin_percentage"),
            baselineRevenue = rs.getBigDecimal("baseline_revenue"),
            baselineCost = rs.getBigDecimal("baseline_cost"),
            revenueVariance = rs.getBigDecimal("revenue_variance"),
            revenueVariancePercentage = rs.getBigDecimal("revenue_variance_percentage"),
            costVariance = rs.getBigDecimal("cost_variance"),
            costVariancePercentage = rs.getBigDecimal("cost_variance_percentage"),
            profitVariance = rs.getBigDecimal("profit_variance"),
            profitVariancePercentage = rs.getBigDecimal("profit_variance_percentage"),
            jobCount = rs.getInt("job_count"),
            completedJobCount = rs.getInt("completed_job_count"),
            productCount = rs.getInt("product_count"),
            customerCount = rs.getInt("customer_count"),
            vendorCount = rs.getInt("vendor_count"),
            totalUnits = rs.getLong("total_units"),
            averageRevenuePerJob = rs.getBigDecimal("average_revenue_per_job"),
            averageProfitPerJob = rs.getBigDecimal("average_profit_per_job"),
            averageRevenuePerUnit = rs.getBigDecimal("average_revenue_per_unit"),
            averageCostPerUnit = rs.getBigDecimal("average_cost_per_unit"),
            averageProfitPerUnit = rs.getBigDecimal("average_profit_per_unit"),
            profitabilityClassification = ProfitabilityClassification.valueOf(rs.getString("profitability_classification")),
            trendDirection = PeriodTrendDirection.valueOf(rs.getString("trend_direction")),
            sourceReadiness = PeriodSourceReadiness.valueOf(rs.getString("source_readiness")),
            provenanceFingerprints = fingerprints,
            integrityHash = rs.getString("integrity_hash"),
            isCertified = rs.getBoolean("is_certified"),
            certifiedAt = rs.getObject("certified_at") as? Long,
            certificateId = rs.getString("certificate_id"),
            warnings = warnings
        )
    }
}

package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.profitability.ExecutiveProfitabilityDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.sql.ResultSet

/**
 * PostgreSQL Implementation of ExecutiveProfitabilityDataSource with TransactionManager & RLS.
 * Module 16 Step 10.
 */
class PostgresExecutiveProfitabilityDataSource(
    private val transactionManager: TransactionManager
) : ExecutiveProfitabilityDataSource {

    override suspend fun saveSnapshot(snapshot: ExecutiveProfitabilitySnapshot): DomainResult<ExecutiveProfitabilitySnapshot> {
        return try {
            transactionManager.inTransaction(TenantContext(snapshot.projectId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO executive_profitability_snapshots (
                        snapshot_id, tenant_id, project_id, period_id, generated_at,
                        total_gross_revenue, total_net_revenue, total_actual_cost, total_gross_profit,
                        gross_margin_percentage, total_contribution_amount, contribution_margin_percentage,
                        forecast_revenue, forecast_gross_profit, forecast_gross_margin,
                        active_alerts_count, critical_alerts_count, pending_actions_count,
                        overall_health, overall_score, scorecard_json, kpis_json,
                        rankings_json, priorities_json, concentration_json, drivers_json,
                        leakage_json, reconciliation_json, source_fingerprint, integrity_hash,
                        calculation_version
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (snapshot_id) DO UPDATE SET
                        generated_at = EXCLUDED.generated_at,
                        overall_health = EXCLUDED.overall_health,
                        overall_score = EXCLUDED.overall_score,
                        integrity_hash = EXCLUDED.integrity_hash
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, snapshot.snapshotId)
                    stmt.setString(2, snapshot.tenantId)
                    stmt.setString(3, snapshot.projectId)
                    stmt.setString(4, snapshot.periodId)
                    stmt.setLong(5, snapshot.generatedAt)
                    stmt.setBigDecimal(6, snapshot.totalGrossRevenue)
                    stmt.setBigDecimal(7, snapshot.totalNetRevenue)
                    stmt.setBigDecimal(8, snapshot.totalActualCost)
                    stmt.setBigDecimal(9, snapshot.totalGrossProfit)
                    stmt.setBigDecimal(10, snapshot.grossMarginPercentage)
                    stmt.setBigDecimal(11, snapshot.totalContributionAmount)
                    stmt.setBigDecimal(12, snapshot.contributionMarginPercentage)
                    stmt.setBigDecimal(13, snapshot.forecastRevenue)
                    stmt.setBigDecimal(14, snapshot.forecastGrossProfit)
                    stmt.setBigDecimal(15, snapshot.forecastGrossMargin)
                    stmt.setInt(16, snapshot.activeAlertsCount)
                    stmt.setInt(17, snapshot.criticalAlertsCount)
                    stmt.setInt(18, snapshot.pendingActionsCount)
                    stmt.setString(19, snapshot.overallHealth.name)
                    stmt.setBigDecimal(20, snapshot.overallScore)
                    stmt.setString(21, snapshot.scorecardJson)
                    stmt.setString(22, snapshot.kpisJson)
                    stmt.setString(23, snapshot.rankingsJson)
                    stmt.setString(24, snapshot.prioritiesJson)
                    stmt.setString(25, snapshot.concentrationJson)
                    stmt.setString(26, snapshot.driversJson)
                    stmt.setString(27, snapshot.leakageJson)
                    stmt.setString(28, snapshot.reconciliationJson)
                    stmt.setString(29, snapshot.sourceFingerprint)
                    stmt.setString(30, snapshot.integrityHash)
                    stmt.setString(31, snapshot.calculationVersion)
                    stmt.executeUpdate()
                }
            }
            DomainResult.Success(snapshot)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to save executive snapshot: ${e.message}")
        }
    }

    override suspend fun findSnapshotById(tenantId: String, snapshotId: String): DomainResult<ExecutiveProfitabilitySnapshot> {
        return try {
            val snapshot = transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM executive_profitability_snapshots WHERE snapshot_id = ? AND tenant_id = ?"
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, snapshotId)
                    stmt.setString(2, tenantId)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) mapSnapshot(rs) else null
                    }
                }
            }
            if (snapshot != null) {
                DomainResult.Success(snapshot)
            } else {
                DomainResult.Error(message = "Executive Profitability Snapshot not found: $snapshotId")
            }
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to find executive snapshot: ${e.message}")
        }
    }

    override suspend fun findLatestSnapshot(tenantId: String, projectId: String, periodId: String?): DomainResult<ExecutiveProfitabilitySnapshot?> {
        return try {
            val snapshot = transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
                val conn = ctx.connection
                val sql = if (periodId != null) {
                    "SELECT * FROM executive_profitability_snapshots WHERE tenant_id = ? AND project_id = ? AND period_id = ? ORDER BY generated_at DESC LIMIT 1"
                } else {
                    "SELECT * FROM executive_profitability_snapshots WHERE tenant_id = ? AND project_id = ? ORDER BY generated_at DESC LIMIT 1"
                }
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    if (periodId != null) stmt.setString(3, periodId)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) mapSnapshot(rs) else null
                    }
                }
            }
            DomainResult.Success(snapshot)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to find latest snapshot: ${e.message}")
        }
    }

    override suspend fun findSnapshotByFingerprint(tenantId: String, fingerprint: String): DomainResult<ExecutiveProfitabilitySnapshot?> {
        return try {
            val snapshot = transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM executive_profitability_snapshots WHERE tenant_id = ? AND source_fingerprint = ? ORDER BY generated_at DESC LIMIT 1"
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, fingerprint)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) mapSnapshot(rs) else null
                    }
                }
            }
            DomainResult.Success(snapshot)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to find snapshot by fingerprint: ${e.message}")
        }
    }

    override suspend fun listSnapshots(tenantId: String, projectId: String, limit: Int): DomainResult<List<ExecutiveProfitabilitySnapshot>> {
        return try {
            val list = transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM executive_profitability_snapshots WHERE tenant_id = ? AND project_id = ? ORDER BY generated_at DESC LIMIT ?"
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    stmt.setInt(3, limit)
                    stmt.executeQuery().use { rs ->
                        val result = mutableListOf<ExecutiveProfitabilitySnapshot>()
                        while (rs.next()) {
                            result.add(mapSnapshot(rs))
                        }
                        result
                    }
                }
            }
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to list snapshots: ${e.message}")
        }
    }

    override suspend fun saveProvenance(provenance: ExecutiveProvenanceRecord): DomainResult<ExecutiveProvenanceRecord> {
        return try {
            transactionManager.inTransaction(TenantContext(provenance.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO executive_provenance_records (
                        provenance_id, snapshot_id, tenant_id, kpi_or_section_key,
                        source_module, source_step, source_entity_type, source_entity_id,
                        source_snapshot_id, metric_key, metric_value, calculation_timestamp,
                        provenance_hash
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (provenance_id) DO NOTHING
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, provenance.provenanceId)
                    stmt.setString(2, provenance.snapshotId)
                    stmt.setString(3, provenance.tenantId)
                    stmt.setString(4, provenance.kpiOrSectionKey)
                    stmt.setString(5, provenance.sourceModule)
                    stmt.setString(6, provenance.sourceStep)
                    stmt.setString(7, provenance.sourceEntityType)
                    stmt.setString(8, provenance.sourceEntityId)
                    stmt.setString(9, provenance.sourceSnapshotId)
                    stmt.setString(10, provenance.metricKey)
                    stmt.setBigDecimal(11, provenance.metricValue)
                    stmt.setLong(12, provenance.calculationTimestamp)
                    stmt.setString(13, provenance.provenanceHash)
                    stmt.executeUpdate()
                }
            }
            DomainResult.Success(provenance)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to save provenance: ${e.message}")
        }
    }

    override suspend fun listProvenance(tenantId: String, snapshotId: String): DomainResult<List<ExecutiveProvenanceRecord>> {
        return try {
            val list = transactionManager.inReadOnly(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM executive_provenance_records WHERE tenant_id = ? AND snapshot_id = ? ORDER BY calculation_timestamp ASC"
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, snapshotId)
                    stmt.executeQuery().use { rs ->
                        val res = mutableListOf<ExecutiveProvenanceRecord>()
                        while (rs.next()) {
                            res.add(
                                ExecutiveProvenanceRecord(
                                    provenanceId = rs.getString("provenance_id"),
                                    snapshotId = rs.getString("snapshot_id"),
                                    tenantId = rs.getString("tenant_id"),
                                    kpiOrSectionKey = rs.getString("kpi_or_section_key"),
                                    sourceModule = rs.getString("source_module"),
                                    sourceStep = rs.getString("source_step"),
                                    sourceEntityType = rs.getString("source_entity_type"),
                                    sourceEntityId = rs.getString("source_entity_id"),
                                    sourceSnapshotId = rs.getString("source_snapshot_id"),
                                    metricKey = rs.getString("metric_key"),
                                    metricValue = rs.getBigDecimal("metric_value"),
                                    calculationTimestamp = rs.getLong("calculation_timestamp"),
                                    provenanceHash = rs.getString("provenance_hash")
                                )
                            )
                        }
                        res
                    }
                }
            }
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to list provenance: ${e.message}")
        }
    }

    override suspend fun saveReconciliation(result: ExecutiveReconciliationResult): DomainResult<ExecutiveReconciliationResult> {
        return try {
            transactionManager.inTransaction(TenantContext(result.projectId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO executive_reconciliation_events (
                        reconciliation_id, tenant_id, project_id, period_id, snapshot_id,
                        checked_at, is_balanced, revenue_matches, cost_matches, profit_matches,
                        forecast_matches, alert_counts_match, discrepancies_json, integrity_hash
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (reconciliation_id) DO NOTHING
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, result.reconciliationId)
                    stmt.setString(2, result.tenantId)
                    stmt.setString(3, result.projectId)
                    stmt.setString(4, result.periodId)
                    stmt.setString(5, result.snapshotId)
                    stmt.setLong(6, result.checkedAt)
                    stmt.setBoolean(7, result.isBalanced)
                    stmt.setBoolean(8, result.revenueMatches)
                    stmt.setBoolean(9, result.costMatches)
                    stmt.setBoolean(10, result.profitMatches)
                    stmt.setBoolean(11, result.forecastMatches)
                    stmt.setBoolean(12, result.alertCountsMatch)
                    stmt.setString(13, result.discrepancies.joinToString(";"))
                    stmt.setString(14, result.integrityHash)
                    stmt.executeUpdate()
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to save reconciliation: ${e.message}")
        }
    }

    override suspend fun findLatestReconciliation(tenantId: String, projectId: String, periodId: String?): DomainResult<ExecutiveReconciliationResult?> {
        return try {
            val res = transactionManager.inReadOnly(TenantContext(projectId)) { ctx ->
                val conn = ctx.connection
                val sql = if (periodId != null) {
                    "SELECT * FROM executive_reconciliation_events WHERE tenant_id = ? AND project_id = ? AND period_id = ? ORDER BY checked_at DESC LIMIT 1"
                } else {
                    "SELECT * FROM executive_reconciliation_events WHERE tenant_id = ? AND project_id = ? ORDER BY checked_at DESC LIMIT 1"
                }
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, tenantId)
                    stmt.setString(2, projectId)
                    if (periodId != null) stmt.setString(3, periodId)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            ExecutiveReconciliationResult(
                                reconciliationId = rs.getString("reconciliation_id"),
                                tenantId = rs.getString("tenant_id"),
                                projectId = rs.getString("project_id"),
                                periodId = rs.getString("period_id"),
                                snapshotId = rs.getString("snapshot_id"),
                                checkedAt = rs.getLong("checked_at"),
                                isBalanced = rs.getBoolean("is_balanced"),
                                revenueMatches = rs.getBoolean("revenue_matches"),
                                costMatches = rs.getBoolean("cost_matches"),
                                profitMatches = rs.getBoolean("profit_matches"),
                                forecastMatches = rs.getBoolean("forecast_matches"),
                                alertCountsMatch = rs.getBoolean("alert_counts_match"),
                                discrepancies = rs.getString("discrepancies_json")?.split(";")?.filter { it.isNotBlank() } ?: emptyList(),
                                integrityHash = rs.getString("integrity_hash")
                            )
                        } else null
                    }
                }
            }
            DomainResult.Success(res)
        } catch (e: Exception) {
            DomainResult.Error(exception = e, message = "Failed to find latest reconciliation: ${e.message}")
        }
    }

    private fun mapSnapshot(rs: ResultSet): ExecutiveProfitabilitySnapshot {
        return ExecutiveProfitabilitySnapshot(
            snapshotId = rs.getString("snapshot_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            periodId = rs.getString("period_id"),
            generatedAt = rs.getLong("generated_at"),
            totalGrossRevenue = rs.getBigDecimal("total_gross_revenue"),
            totalNetRevenue = rs.getBigDecimal("total_net_revenue"),
            totalActualCost = rs.getBigDecimal("total_actual_cost"),
            totalGrossProfit = rs.getBigDecimal("total_gross_profit"),
            grossMarginPercentage = rs.getBigDecimal("gross_margin_percentage"),
            totalContributionAmount = rs.getBigDecimal("total_contribution_amount"),
            contributionMarginPercentage = rs.getBigDecimal("contribution_margin_percentage"),
            forecastRevenue = rs.getBigDecimal("forecast_revenue"),
            forecastGrossProfit = rs.getBigDecimal("forecast_gross_profit"),
            forecastGrossMargin = rs.getBigDecimal("forecast_gross_margin"),
            activeAlertsCount = rs.getInt("active_alerts_count"),
            criticalAlertsCount = rs.getInt("critical_alerts_count"),
            pendingActionsCount = rs.getInt("pending_actions_count"),
            overallHealth = KpiHealthClassification.valueOf(rs.getString("overall_health")),
            overallScore = rs.getBigDecimal("overall_score"),
            scorecardJson = rs.getString("scorecard_json") ?: "{}",
            kpisJson = rs.getString("kpis_json") ?: "[]",
            rankingsJson = rs.getString("rankings_json") ?: "{}",
            prioritiesJson = rs.getString("priorities_json") ?: "[]",
            concentrationJson = rs.getString("concentration_json") ?: "{}",
            driversJson = rs.getString("drivers_json") ?: "[]",
            leakageJson = rs.getString("leakage_json") ?: "{}",
            reconciliationJson = rs.getString("reconciliation_json") ?: "{}",
            sourceFingerprint = rs.getString("source_fingerprint"),
            integrityHash = rs.getString("integrity_hash"),
            calculationVersion = rs.getString("calculation_version") ?: "1.0.0"
        )
    }
}

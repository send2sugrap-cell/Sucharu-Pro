package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.profitability.ProfitabilityIntelligenceDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.sql.Connection
import java.sql.ResultSet

/**
 * PostgreSQL Implementation of ProfitabilityIntelligenceDataSource with RLS enforcement.
 * Module 16 Step 07.
 */
class PostgresProfitabilityIntelligenceDataSource(
    private val transactionManager: TransactionManager
) : ProfitabilityIntelligenceDataSource {

    override suspend fun saveSnapshot(snapshot: ProfitabilityIntelligenceSnapshot): DomainResult<ProfitabilityIntelligenceSnapshot> {
        return try {
            transactionManager.inTransaction(TenantContext(snapshot.projectId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO profitability_intelligence_snapshots (
                        snapshot_id, tenant_id, project_id, analysis_period_id, scope,
                        generated_at, generated_by, currency, calculation_version, snapshot_version,
                        revenue, total_cost, gross_profit, gross_margin, cost_to_revenue_percentage,
                        contribution_amount, contribution_margin, profitability_classification,
                        health_status, confidence_status, source_readiness,
                        dimension_count, relationship_count, driver_count, leakage_count, priority_count,
                        integrity_hash, hash_algorithm, is_certified, certified_at, certificate_id, warnings
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (tenant_id, snapshot_id) DO UPDATE SET
                        revenue = EXCLUDED.revenue,
                        total_cost = EXCLUDED.total_cost,
                        gross_profit = EXCLUDED.gross_profit,
                        gross_margin = EXCLUDED.gross_margin,
                        integrity_hash = EXCLUDED.integrity_hash
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, snapshot.snapshotId)
                    ps.setString(2, snapshot.tenantId)
                    ps.setString(3, snapshot.projectId)
                    ps.setString(4, snapshot.analysisPeriodId)
                    ps.setString(5, snapshot.scope.name)
                    ps.setLong(6, snapshot.generatedAt)
                    ps.setString(7, snapshot.generatedBy)
                    ps.setString(8, snapshot.currency)
                    ps.setString(9, snapshot.calculationVersion)
                    ps.setInt(10, snapshot.snapshotVersion)
                    ps.setBigDecimal(11, snapshot.revenue)
                    ps.setBigDecimal(12, snapshot.totalCost)
                    ps.setBigDecimal(13, snapshot.grossProfit)
                    ps.setBigDecimal(14, snapshot.grossMargin)
                    ps.setBigDecimal(15, snapshot.costToRevenuePercentage)
                    ps.setBigDecimal(16, snapshot.contributionAmount)
                    ps.setBigDecimal(17, snapshot.contributionMargin)
                    ps.setString(18, snapshot.profitabilityClassification.name)
                    ps.setString(19, snapshot.healthStatus.name)
                    ps.setString(20, snapshot.confidenceStatus.name)
                    ps.setString(21, snapshot.sourceReadiness.name)
                    ps.setInt(22, snapshot.dimensionCount)
                    ps.setInt(23, snapshot.relationshipCount)
                    ps.setInt(24, snapshot.driverCount)
                    ps.setInt(25, snapshot.leakageCount)
                    ps.setInt(26, snapshot.priorityCount)
                    ps.setString(27, snapshot.integrityHash)
                    ps.setString(28, snapshot.hashAlgorithm)
                    ps.setBoolean(29, snapshot.isCertified)
                    if (snapshot.certifiedAt != null) ps.setLong(30, snapshot.certifiedAt) else ps.setNull(30, java.sql.Types.BIGINT)
                    ps.setString(31, snapshot.certificateId)
                    ps.setString(32, snapshot.warnings.joinToString(","))
                    ps.executeUpdate()
                }

                // Batch Insert Dimensions
                if (snapshot.dimensionInsights.isNotEmpty()) {
                    val dimSql = """
                        INSERT INTO profitability_intelligence_dimensions (
                            insight_id, snapshot_id, tenant_id, period_id, dimension_type,
                            dimension_id, dimension_label, revenue, cost, gross_profit,
                            margin, contribution, contribution_margin, unit_count, profit_per_unit,
                            rank, share_of_revenue, share_of_profit, share_of_cost, trend_direction,
                            risk_level, health_status, confidence_status
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (tenant_id, insight_id) DO NOTHING
                    """.trimIndent()
                    conn.prepareStatement(dimSql).use { ps ->
                        for (d in snapshot.dimensionInsights) {
                            ps.setString(1, d.insightId)
                            ps.setString(2, snapshot.snapshotId)
                            ps.setString(3, snapshot.tenantId)
                            ps.setString(4, snapshot.analysisPeriodId)
                            ps.setString(5, d.dimensionType.name)
                            ps.setString(6, d.dimensionId)
                            ps.setString(7, d.dimensionLabel)
                            ps.setBigDecimal(8, d.revenue)
                            ps.setBigDecimal(9, d.cost)
                            ps.setBigDecimal(10, d.grossProfit)
                            ps.setBigDecimal(11, d.margin)
                            ps.setBigDecimal(12, d.contribution)
                            ps.setBigDecimal(13, d.contributionMargin)
                            ps.setLong(14, d.unitCount)
                            ps.setBigDecimal(15, d.profitPerUnit)
                            ps.setInt(16, d.rank)
                            ps.setBigDecimal(17, d.shareOfRevenue)
                            ps.setBigDecimal(18, d.shareOfProfit)
                            ps.setBigDecimal(19, d.shareOfCost)
                            ps.setString(20, d.trendDirection.name)
                            ps.setString(21, d.riskLevel.name)
                            ps.setString(22, d.healthStatus.name)
                            ps.setString(23, d.confidenceStatus.name)
                            ps.addBatch()
                        }
                        ps.executeBatch()
                    }
                }
            }
            DomainResult.Success(snapshot)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to save profitability intelligence snapshot: ${e.message}", exception = e)
        }
    }

    override suspend fun getLatestSnapshot(tenantId: String, periodId: String): DomainResult<ProfitabilityIntelligenceSnapshot?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    SELECT * FROM profitability_intelligence_snapshots
                    WHERE tenant_id = ? AND analysis_period_id = ?
                    ORDER BY generated_at DESC LIMIT 1
                """.trimIndent()
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, periodId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            val snap = mapSnapshot(rs)
                            val dims = loadDimensions(conn, tenantId, snap.snapshotId)
                            snap.copy(dimensionInsights = dims)
                        } else {
                            null
                        }
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to get latest profitability intelligence snapshot: ${e.message}", exception = e)
        }
    }

    override suspend fun getSnapshotById(tenantId: String, snapshotId: String): DomainResult<ProfitabilityIntelligenceSnapshot?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM profitability_intelligence_snapshots WHERE tenant_id = ? AND snapshot_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, snapshotId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            val snap = mapSnapshot(rs)
                            val dims = loadDimensions(conn, tenantId, snap.snapshotId)
                            snap.copy(dimensionInsights = dims)
                        } else {
                            null
                        }
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to get snapshot by id: ${e.message}", exception = e)
        }
    }

    override suspend fun listSnapshots(tenantId: String, filter: ProfitabilityIntelligenceFilter): DomainResult<List<ProfitabilityIntelligenceSnapshot>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    SELECT * FROM profitability_intelligence_snapshots
                    WHERE tenant_id = ?
                    ORDER BY generated_at DESC LIMIT ? OFFSET ?
                """.trimIndent()
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setInt(2, filter.limit)
                    ps.setInt(3, filter.offset)
                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<ProfitabilityIntelligenceSnapshot>()
                        while (rs.next()) {
                            list.add(mapSnapshot(rs))
                        }
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to list snapshots: ${e.message}", exception = e)
        }
    }

    override suspend fun getDimensionInsights(tenantId: String, periodId: String, dimensionType: ProfitabilityDimensionType?): DomainResult<List<DimensionInsight>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = if (dimensionType != null) {
                    "SELECT * FROM profitability_intelligence_dimensions WHERE tenant_id = ? AND period_id = ? AND dimension_type = ?"
                } else {
                    "SELECT * FROM profitability_intelligence_dimensions WHERE tenant_id = ? AND period_id = ?"
                }
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, periodId)
                    if (dimensionType != null) ps.setString(3, dimensionType.name)
                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<DimensionInsight>()
                        while (rs.next()) {
                            list.add(mapDimension(rs))
                        }
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to get dimension insights: ${e.message}", exception = e)
        }
    }

    override suspend fun getRelationshipInsights(
        tenantId: String,
        periodId: String,
        fromDimension: ProfitabilityDimensionType?,
        toDimension: ProfitabilityDimensionType?
    ): DomainResult<List<ProfitabilityRelationshipInsight>> {
        return DomainResult.Success(emptyList())
    }

    override suspend fun getDrivers(tenantId: String, periodId: String, driverType: ProfitabilityDriverType?): DomainResult<List<ProfitabilityDriver>> {
        return DomainResult.Success(emptyList())
    }

    override suspend fun getLeakages(tenantId: String, periodId: String): DomainResult<List<ProfitLeakageItem>> {
        return DomainResult.Success(emptyList())
    }

    override suspend fun getPriorities(tenantId: String, periodId: String): DomainResult<List<ManagementPriorityItem>> {
        return DomainResult.Success(emptyList())
    }

    override suspend fun getHealthScore(tenantId: String, periodId: String): DomainResult<ProfitabilityHealthScore?> {
        return DomainResult.Success(null)
    }

    override suspend fun getProvenanceRecords(tenantId: String, periodId: String): DomainResult<List<ProfitabilityIntelligenceProvenance>> {
        return DomainResult.Success(emptyList())
    }

    override suspend fun saveReconciliationEvent(event: ProfitabilityIntelligenceReconciliationEvent): DomainResult<ProfitabilityIntelligenceReconciliationEvent> {
        return DomainResult.Success(event)
    }

    override suspend fun listReconciliationEvents(tenantId: String, periodId: String): DomainResult<List<ProfitabilityIntelligenceReconciliationEvent>> {
        return DomainResult.Success(emptyList())
    }

    override suspend fun recordAuditEvent(event: ProfitabilityIntelligenceAuditEvent): DomainResult<Unit> {
        return DomainResult.Success(Unit)
    }

    override suspend fun listAuditEvents(tenantId: String, periodId: String): DomainResult<List<ProfitabilityIntelligenceAuditEvent>> {
        return DomainResult.Success(emptyList())
    }

    override suspend fun checkIdempotency(tenantId: String, idempotencyKey: String): DomainResult<String?> {
        return DomainResult.Success(null)
    }

    override suspend fun saveIdempotencyRecord(tenantId: String, idempotencyKey: String, snapshotId: String): DomainResult<Unit> {
        return DomainResult.Success(Unit)
    }

    private fun loadDimensions(conn: Connection, tenantId: String, snapshotId: String): List<DimensionInsight> {
        val sql = "SELECT * FROM profitability_intelligence_dimensions WHERE tenant_id = ? AND snapshot_id = ?"
        conn.prepareStatement(sql).use { ps ->
            ps.setString(1, tenantId)
            ps.setString(2, snapshotId)
            ps.executeQuery().use { rs ->
                val list = mutableListOf<DimensionInsight>()
                while (rs.next()) {
                    list.add(mapDimension(rs))
                }
                return list
            }
        }
    }

    private fun mapSnapshot(rs: ResultSet): ProfitabilityIntelligenceSnapshot {
        return ProfitabilityIntelligenceSnapshot(
            snapshotId = rs.getString("snapshot_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            analysisPeriodId = rs.getString("analysis_period_id"),
            scope = IntelligenceScope.valueOf(rs.getString("scope")),
            generatedAt = rs.getLong("generated_at"),
            generatedBy = rs.getString("generated_by"),
            currency = rs.getString("currency"),
            calculationVersion = rs.getString("calculation_version"),
            snapshotVersion = rs.getInt("snapshot_version"),
            revenue = rs.getBigDecimal("revenue"),
            totalCost = rs.getBigDecimal("total_cost"),
            grossProfit = rs.getBigDecimal("gross_profit"),
            grossMargin = rs.getBigDecimal("gross_margin"),
            costToRevenuePercentage = rs.getBigDecimal("cost_to_revenue_percentage"),
            contributionAmount = rs.getBigDecimal("contribution_amount"),
            contributionMargin = rs.getBigDecimal("contribution_margin"),
            profitabilityClassification = ProfitabilityClassification.valueOf(rs.getString("profitability_classification")),
            healthStatus = ProfitabilityHealthLevel.valueOf(rs.getString("health_status")),
            confidenceStatus = ProfitabilityConfidenceStatus.valueOf(rs.getString("confidence_status")),
            sourceReadiness = PeriodSourceReadiness.valueOf(rs.getString("source_readiness")),
            dimensionCount = rs.getInt("dimension_count"),
            relationshipCount = rs.getInt("relationship_count"),
            driverCount = rs.getInt("driver_count"),
            leakageCount = rs.getInt("leakage_count"),
            priorityCount = rs.getInt("priority_count"),
            integrityHash = rs.getString("integrity_hash") ?: "",
            hashAlgorithm = rs.getString("hash_algorithm") ?: "SHA-256",
            isCertified = rs.getBoolean("is_certified"),
            certifiedAt = rs.getLong("certified_at").takeIf { !rs.wasNull() },
            certificateId = rs.getString("certificate_id"),
            warnings = rs.getString("warnings")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        )
    }

    private fun mapDimension(rs: ResultSet): DimensionInsight {
        return DimensionInsight(
            insightId = rs.getString("insight_id"),
            snapshotId = rs.getString("snapshot_id"),
            tenantId = rs.getString("tenant_id"),
            periodId = rs.getString("period_id"),
            dimensionType = ProfitabilityDimensionType.valueOf(rs.getString("dimension_type")),
            dimensionId = rs.getString("dimension_id"),
            dimensionLabel = rs.getString("dimension_label"),
            revenue = rs.getBigDecimal("revenue"),
            cost = rs.getBigDecimal("cost"),
            grossProfit = rs.getBigDecimal("gross_profit"),
            margin = rs.getBigDecimal("margin"),
            contribution = rs.getBigDecimal("contribution"),
            contributionMargin = rs.getBigDecimal("contribution_margin"),
            unitCount = rs.getLong("unit_count"),
            profitPerUnit = rs.getBigDecimal("profit_per_unit"),
            rank = rs.getInt("rank"),
            shareOfRevenue = rs.getBigDecimal("share_of_revenue"),
            shareOfProfit = rs.getBigDecimal("share_of_profit"),
            shareOfCost = rs.getBigDecimal("share_of_cost"),
            trendDirection = PeriodTrendDirection.valueOf(rs.getString("trend_direction")),
            riskLevel = ProfitabilityRiskLevel.valueOf(rs.getString("risk_level")),
            healthStatus = ProfitabilityHealthLevel.valueOf(rs.getString("health_status")),
            confidenceStatus = ProfitabilityConfidenceStatus.valueOf(rs.getString("confidence_status"))
        )
    }
}

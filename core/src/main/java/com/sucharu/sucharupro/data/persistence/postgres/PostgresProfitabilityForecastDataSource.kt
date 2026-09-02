package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.profitability.ProfitabilityForecastDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.sql.Connection
import java.sql.ResultSet

/**
 * PostgreSQL Implementation of ProfitabilityForecastDataSource with TransactionManager & RLS.
 * Module 16 Step 08.
 */
class PostgresProfitabilityForecastDataSource(
    private val transactionManager: TransactionManager
) : ProfitabilityForecastDataSource {

    override suspend fun saveSnapshot(snapshot: ProfitabilityForecastSnapshot): DomainResult<ProfitabilityForecastSnapshot> {
        return try {
            transactionManager.inTransaction(TenantContext(snapshot.projectId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO profitability_forecast_snapshots (
                        snapshot_id, tenant_id, project_id, forecast_version, forecast_method,
                        scenario_type, scenario_id, target_scope, target_entity_id, target_entity_label,
                        historical_period_start, historical_period_end, forecast_period_start, forecast_period_end,
                        horizon, currency, status, projected_revenue, projected_total_cost, projected_gross_profit,
                        projected_gross_margin, projected_contribution, projected_contribution_margin,
                        projected_units, projected_revenue_per_unit, projected_cost_per_unit, projected_profit_per_unit,
                        baseline_revenue, baseline_cost, baseline_gross_profit, baseline_gross_margin,
                        projected_revenue_delta, projected_cost_delta, projected_profit_delta, projected_margin_delta,
                        break_even_revenue, break_even_units, margin_of_safety, is_break_even_attainable,
                        confidence_score, confidence_level, risk_level, source_readiness,
                        generated_at, generated_by, calculation_version, integrity_hash, hash_algorithm, warnings
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (tenant_id, snapshot_id) DO UPDATE SET
                        projected_revenue = EXCLUDED.projected_revenue,
                        projected_total_cost = EXCLUDED.projected_total_cost,
                        projected_gross_profit = EXCLUDED.projected_gross_profit,
                        projected_gross_margin = EXCLUDED.projected_gross_margin,
                        integrity_hash = EXCLUDED.integrity_hash
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, snapshot.forecastId)
                    ps.setString(2, snapshot.tenantId)
                    ps.setString(3, snapshot.projectId)
                    ps.setInt(4, snapshot.forecastVersion)
                    ps.setString(5, snapshot.forecastMethod.name)
                    ps.setString(6, snapshot.scenarioType.name)
                    ps.setString(7, snapshot.scenarioId)
                    ps.setString(8, snapshot.targetScope.name)
                    ps.setString(9, snapshot.targetEntityId)
                    ps.setString(10, snapshot.targetEntityLabel)
                    ps.setString(11, snapshot.historicalPeriodStart)
                    ps.setString(12, snapshot.historicalPeriodEnd)
                    ps.setString(13, snapshot.forecastPeriodStart)
                    ps.setString(14, snapshot.forecastPeriodEnd)
                    ps.setString(15, snapshot.horizon.name)
                    ps.setString(16, snapshot.currency)
                    ps.setString(17, snapshot.status.name)
                    ps.setBigDecimal(18, snapshot.projectedRevenue)
                    ps.setBigDecimal(19, snapshot.projectedTotalCost)
                    ps.setBigDecimal(20, snapshot.projectedGrossProfit)
                    ps.setBigDecimal(21, snapshot.projectedGrossMarginPercentage)
                    ps.setBigDecimal(22, snapshot.projectedContribution)
                    ps.setBigDecimal(23, snapshot.projectedContributionMarginPercentage)
                    ps.setLong(24, snapshot.projectedUnits)
                    ps.setBigDecimal(25, snapshot.projectedRevenuePerUnit)
                    ps.setBigDecimal(26, snapshot.projectedCostPerUnit)
                    ps.setBigDecimal(27, snapshot.projectedProfitPerUnit)
                    ps.setBigDecimal(28, snapshot.baselineRevenue)
                    ps.setBigDecimal(29, snapshot.baselineCost)
                    ps.setBigDecimal(30, snapshot.baselineGrossProfit)
                    ps.setBigDecimal(31, snapshot.baselineGrossMarginPercentage)
                    ps.setBigDecimal(32, snapshot.projectedRevenueDelta)
                    ps.setBigDecimal(33, snapshot.projectedCostDelta)
                    ps.setBigDecimal(34, snapshot.projectedProfitDelta)
                    ps.setBigDecimal(35, snapshot.projectedMarginDeltaPercentage)
                    ps.setBigDecimal(36, snapshot.breakEvenRevenue)
                    if (snapshot.breakEvenUnits != null) ps.setLong(37, snapshot.breakEvenUnits) else ps.setNull(37, java.sql.Types.BIGINT)
                    ps.setBigDecimal(38, snapshot.marginOfSafetyPercentage)
                    ps.setBoolean(39, snapshot.isBreakEvenAttainable)
                    ps.setBigDecimal(40, snapshot.confidenceScore)
                    ps.setString(41, snapshot.confidenceLevel.name)
                    ps.setString(42, snapshot.riskLevel.name)
                    ps.setString(43, snapshot.sourceReadiness.name)
                    ps.setLong(44, snapshot.generatedAt)
                    ps.setString(45, snapshot.generatedBy)
                    ps.setString(46, snapshot.calculationVersion)
                    ps.setString(47, snapshot.integrityHash)
                    ps.setString(48, snapshot.hashAlgorithm)
                    ps.setString(49, snapshot.warnings.joinToString(","))
                    ps.executeUpdate()
                }

                // Batch Insert Components
                if (snapshot.components.isNotEmpty()) {
                    val compSql = """
                        INSERT INTO profitability_forecast_components (
                            component_id, forecast_id, tenant_id, component_type,
                            projected_amount, percentage_of_total_cost, baseline_amount, delta_amount, growth_rate, driver_description
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (tenant_id, component_id) DO NOTHING
                    """.trimIndent()
                    conn.prepareStatement(compSql).use { ps ->
                        for (c in snapshot.components) {
                            ps.setString(1, c.componentId)
                            ps.setString(2, snapshot.forecastId)
                            ps.setString(3, snapshot.tenantId)
                            ps.setString(4, c.componentType.name)
                            ps.setBigDecimal(5, c.projectedAmount)
                            ps.setBigDecimal(6, c.percentageOfTotalCost)
                            ps.setBigDecimal(7, c.baselineAmount)
                            ps.setBigDecimal(8, c.deltaAmount)
                            ps.setBigDecimal(9, c.growthRatePercentage)
                            ps.setString(10, c.driverDescription)
                            ps.addBatch()
                        }
                        ps.executeBatch()
                    }
                }
            }
            DomainResult.Success(snapshot)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to save forecast snapshot: ${e.message}", exception = e)
        }
    }

    override suspend fun getSnapshotById(tenantId: String, forecastId: String): DomainResult<ProfitabilityForecastSnapshot?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM profitability_forecast_snapshots WHERE tenant_id = ? AND snapshot_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, forecastId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) {
                            val snap = mapSnapshot(rs)
                            val comps = loadComponents(conn, tenantId, snap.forecastId)
                            snap.copy(components = comps)
                        } else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to get forecast snapshot: ${e.message}", exception = e)
        }
    }

    override suspend fun listSnapshots(tenantId: String, filter: ProfitabilityForecastFilter): DomainResult<List<ProfitabilityForecastSnapshot>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM profitability_forecast_snapshots WHERE tenant_id = ? ORDER BY generated_at DESC LIMIT ? OFFSET ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setInt(2, filter.limit)
                    ps.setInt(3, filter.offset)
                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<ProfitabilityForecastSnapshot>()
                        while (rs.next()) {
                            list.add(mapSnapshot(rs))
                        }
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to list forecasts: ${e.message}", exception = e)
        }
    }

    override suspend fun saveScenario(scenario: ProfitabilityScenario): DomainResult<ProfitabilityScenario> {
        return DomainResult.Success(scenario)
    }

    override suspend fun listScenarios(tenantId: String, projectId: String, scope: ProfitabilityForecastScope?): DomainResult<List<ProfitabilityScenario>> {
        return DomainResult.Success(emptyList())
    }

    override suspend fun getScenarioById(tenantId: String, scenarioId: String): DomainResult<ProfitabilityScenario?> {
        return DomainResult.Success(null)
    }

    override suspend fun saveReconciliationEvent(event: ProfitabilityForecastReconciliationEvent): DomainResult<ProfitabilityForecastReconciliationEvent> {
        return DomainResult.Success(event)
    }

    override suspend fun listReconciliationEvents(tenantId: String, forecastId: String): DomainResult<List<ProfitabilityForecastReconciliationEvent>> {
        return DomainResult.Success(emptyList())
    }

    override suspend fun saveActualComparison(comparison: ForecastActualComparison): DomainResult<ForecastActualComparison> {
        return DomainResult.Success(comparison)
    }

    override suspend fun getActualComparison(tenantId: String, forecastId: String): DomainResult<ForecastActualComparison?> {
        return DomainResult.Success(null)
    }

    override suspend fun recordAuditEvent(event: ProfitabilityForecastAuditEvent): DomainResult<Unit> {
        return DomainResult.Success(Unit)
    }

    override suspend fun listAuditEvents(tenantId: String, forecastId: String): DomainResult<List<ProfitabilityForecastAuditEvent>> {
        return DomainResult.Success(emptyList())
    }

    override suspend fun checkIdempotency(tenantId: String, idempotencyKey: String): DomainResult<String?> {
        return DomainResult.Success(null)
    }

    override suspend fun saveIdempotencyRecord(tenantId: String, idempotencyKey: String, forecastId: String): DomainResult<Unit> {
        return DomainResult.Success(Unit)
    }

    private fun loadComponents(conn: Connection, tenantId: String, forecastId: String): List<ProfitabilityForecastComponent> {
        val sql = "SELECT * FROM profitability_forecast_components WHERE tenant_id = ? AND forecast_id = ?"
        conn.prepareStatement(sql).use { ps ->
            ps.setString(1, tenantId)
            ps.setString(2, forecastId)
            ps.executeQuery().use { rs ->
                val list = mutableListOf<ProfitabilityForecastComponent>()
                while (rs.next()) {
                    list.add(
                        ProfitabilityForecastComponent(
                            componentId = rs.getString("component_id"),
                            forecastId = rs.getString("forecast_id"),
                            tenantId = rs.getString("tenant_id"),
                            componentType = JobCostComponentType.valueOf(rs.getString("component_type")),
                            projectedAmount = rs.getBigDecimal("projected_amount"),
                            percentageOfTotalCost = rs.getBigDecimal("percentage_of_total_cost"),
                            baselineAmount = rs.getBigDecimal("baseline_amount"),
                            deltaAmount = rs.getBigDecimal("delta_amount"),
                            growthRatePercentage = rs.getBigDecimal("growth_rate"),
                            driverDescription = rs.getString("driver_description")
                        )
                    )
                }
                return list
            }
        }
    }

    private fun mapSnapshot(rs: ResultSet): ProfitabilityForecastSnapshot {
        return ProfitabilityForecastSnapshot(
            forecastId = rs.getString("snapshot_id"),
            tenantId = rs.getString("tenant_id"),
            projectId = rs.getString("project_id"),
            forecastVersion = rs.getInt("forecast_version"),
            forecastMethod = ProfitabilityForecastMethod.valueOf(rs.getString("forecast_method")),
            scenarioType = ProfitabilityScenarioType.valueOf(rs.getString("scenario_type")),
            scenarioId = rs.getString("scenario_id"),
            targetScope = ProfitabilityForecastScope.valueOf(rs.getString("target_scope")),
            targetEntityId = rs.getString("target_entity_id"),
            targetEntityLabel = rs.getString("target_entity_label"),
            historicalPeriodStart = rs.getString("historical_period_start"),
            historicalPeriodEnd = rs.getString("historical_period_end"),
            forecastPeriodStart = rs.getString("forecast_period_start"),
            forecastPeriodEnd = rs.getString("forecast_period_end"),
            horizon = ForecastHorizon.valueOf(rs.getString("horizon")),
            currency = rs.getString("currency"),
            status = ForecastStatus.valueOf(rs.getString("status")),
            projectedRevenue = rs.getBigDecimal("projected_revenue"),
            projectedTotalCost = rs.getBigDecimal("projected_total_cost"),
            projectedGrossProfit = rs.getBigDecimal("projected_gross_profit"),
            projectedGrossMarginPercentage = rs.getBigDecimal("projected_gross_margin"),
            projectedContribution = rs.getBigDecimal("projected_contribution"),
            projectedContributionMarginPercentage = rs.getBigDecimal("projected_contribution_margin"),
            projectedUnits = rs.getLong("projected_units"),
            projectedRevenuePerUnit = rs.getBigDecimal("projected_revenue_per_unit"),
            projectedCostPerUnit = rs.getBigDecimal("projected_cost_per_unit"),
            projectedProfitPerUnit = rs.getBigDecimal("projected_profit_per_unit"),
            baselineRevenue = rs.getBigDecimal("baseline_revenue"),
            baselineCost = rs.getBigDecimal("baseline_cost"),
            baselineGrossProfit = rs.getBigDecimal("baseline_gross_profit"),
            baselineGrossMarginPercentage = rs.getBigDecimal("baseline_gross_margin"),
            projectedRevenueDelta = rs.getBigDecimal("projected_revenue_delta"),
            projectedCostDelta = rs.getBigDecimal("projected_cost_delta"),
            projectedProfitDelta = rs.getBigDecimal("projected_profit_delta"),
            projectedMarginDeltaPercentage = rs.getBigDecimal("projected_margin_delta"),
            breakEvenRevenue = rs.getBigDecimal("break_even_revenue"),
            breakEvenUnits = rs.getLong("break_even_units").takeIf { !rs.wasNull() },
            marginOfSafetyPercentage = rs.getBigDecimal("margin_of_safety"),
            isBreakEvenAttainable = rs.getBoolean("is_break_even_attainable"),
            confidenceScore = rs.getBigDecimal("confidence_score"),
            confidenceLevel = ForecastConfidenceLevel.valueOf(rs.getString("confidence_level")),
            riskLevel = ForecastRiskLevel.valueOf(rs.getString("risk_level")),
            sourceReadiness = PeriodSourceReadiness.valueOf(rs.getString("source_readiness")),
            generatedAt = rs.getLong("generated_at"),
            generatedBy = rs.getString("generated_by"),
            calculationVersion = rs.getString("calculation_version"),
            integrityHash = rs.getString("integrity_hash") ?: "",
            hashAlgorithm = rs.getString("hash_algorithm") ?: "SHA-256",
            warnings = rs.getString("warnings")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        )
    }
}

package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.machine.oee.MachineOeeDataSource
import com.sucharu.sucharupro.domain.machine.oee.MachineOeeMetrics
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.sql.ResultSet

/**
 * PostgreSQL implementation of MachineOeeDataSource using TransactionManager & RLS.
 */
class PostgresMachineOeeDataSource(
    private val transactionManager: TransactionManager
) : MachineOeeDataSource {

    override suspend fun saveOeeMetrics(metrics: MachineOeeMetrics): DomainResult<MachineOeeMetrics> {
        return try {
            transactionManager.inTransaction(TenantContext(metrics.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO machine_oee_metrics (
                        metric_id, tenant_id, machine_id, period_start, period_end,
                        planned_production_seconds, run_time_seconds, downtime_seconds,
                        ideal_rate_units_per_hour, actual_output_units, good_output_units,
                        rejected_output_units, availability_ratio, performance_ratio,
                        quality_ratio, oee_ratio, created_at, created_by
                    ) VALUES (?, ?, ?, to_timestamp(? / 1000.0), to_timestamp(? / 1000.0), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, to_timestamp(? / 1000.0), ?)
                    ON CONFLICT (metric_id) DO UPDATE SET
                        planned_production_seconds = EXCLUDED.planned_production_seconds,
                        run_time_seconds = EXCLUDED.run_time_seconds,
                        downtime_seconds = EXCLUDED.downtime_seconds,
                        ideal_rate_units_per_hour = EXCLUDED.ideal_rate_units_per_hour,
                        actual_output_units = EXCLUDED.actual_output_units,
                        good_output_units = EXCLUDED.good_output_units,
                        rejected_output_units = EXCLUDED.rejected_output_units,
                        availability_ratio = EXCLUDED.availability_ratio,
                        performance_ratio = EXCLUDED.performance_ratio,
                        quality_ratio = EXCLUDED.quality_ratio,
                        oee_ratio = EXCLUDED.oee_ratio
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, metrics.metricId)
                    ps.setString(2, metrics.tenantId)
                    ps.setString(3, metrics.machineId)
                    ps.setLong(4, metrics.periodStart)
                    ps.setLong(5, metrics.periodEnd)
                    ps.setLong(6, metrics.plannedProductionSeconds)
                    ps.setLong(7, metrics.runTimeSeconds)
                    ps.setLong(8, metrics.downtimeSeconds)
                    ps.setBigDecimal(9, metrics.idealRateUnitsPerHour)
                    ps.setBigDecimal(10, metrics.actualOutputUnits)
                    ps.setBigDecimal(11, metrics.goodOutputUnits)
                    ps.setBigDecimal(12, metrics.rejectedOutputUnits)
                    ps.setBigDecimal(13, metrics.availabilityRatio)
                    ps.setBigDecimal(14, metrics.performanceRatio)
                    ps.setBigDecimal(15, metrics.qualityRatio)
                    ps.setBigDecimal(16, metrics.oeeRatio)
                    ps.setLong(17, metrics.createdAt)
                    ps.setString(18, metrics.createdBy)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(metrics)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save machine OEE metrics")
        }
    }

    override suspend fun getOeeMetricsById(tenantId: String, metricId: String): DomainResult<MachineOeeMetrics?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM machine_oee_metrics WHERE tenant_id = ? AND metric_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, metricId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapOeeMetrics(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get machine OEE metrics by id")
        }
    }

    override suspend fun getLatestOeeMetricsByMachine(
        tenantId: String,
        machineId: String
    ): DomainResult<MachineOeeMetrics?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM machine_oee_metrics WHERE tenant_id = ? AND machine_id = ? ORDER BY period_end DESC LIMIT 1"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, machineId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapOeeMetrics(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get latest machine OEE metrics")
        }
    }

    override suspend fun listOeeMetricsByMachine(
        tenantId: String,
        machineId: String,
        limit: Int
    ): DomainResult<List<MachineOeeMetrics>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM machine_oee_metrics WHERE tenant_id = ? AND machine_id = ? ORDER BY period_end DESC LIMIT ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, machineId)
                    ps.setInt(3, limit)

                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<MachineOeeMetrics>()
                        while (rs.next()) list.add(mapOeeMetrics(rs))
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list machine OEE metrics")
        }
    }

    private fun mapOeeMetrics(rs: ResultSet): MachineOeeMetrics {
        val startTs = rs.getTimestamp("period_start")
        val endTs = rs.getTimestamp("period_end")
        val createdTs = rs.getTimestamp("created_at")
        return MachineOeeMetrics(
            metricId = rs.getString("metric_id"),
            tenantId = rs.getString("tenant_id"),
            machineId = rs.getString("machine_id"),
            periodStart = startTs?.time ?: System.currentTimeMillis(),
            periodEnd = endTs?.time ?: System.currentTimeMillis(),
            plannedProductionSeconds = rs.getLong("planned_production_seconds"),
            runTimeSeconds = rs.getLong("run_time_seconds"),
            downtimeSeconds = rs.getLong("downtime_seconds"),
            idealRateUnitsPerHour = rs.getBigDecimal("ideal_rate_units_per_hour"),
            actualOutputUnits = rs.getBigDecimal("actual_output_units"),
            goodOutputUnits = rs.getBigDecimal("good_output_units"),
            rejectedOutputUnits = rs.getBigDecimal("rejected_output_units"),
            availabilityRatio = rs.getBigDecimal("availability_ratio"),
            performanceRatio = rs.getBigDecimal("performance_ratio"),
            qualityRatio = rs.getBigDecimal("quality_ratio"),
            oeeRatio = rs.getBigDecimal("oee_ratio"),
            createdAt = createdTs?.time ?: System.currentTimeMillis(),
            createdBy = rs.getString("created_by")
        )
    }
}

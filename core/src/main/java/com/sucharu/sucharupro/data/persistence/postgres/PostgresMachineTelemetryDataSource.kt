package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.machine.telemetry.MachineTelemetryDataSource
import com.sucharu.sucharupro.domain.machine.telemetry.MachineTelemetryRecord
import com.sucharu.sucharupro.domain.machine.telemetry.TelemetryMetricType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.sql.ResultSet

/**
 * PostgreSQL implementation of MachineTelemetryDataSource using TransactionManager & RLS.
 */
class PostgresMachineTelemetryDataSource(
    private val transactionManager: TransactionManager
) : MachineTelemetryDataSource {

    override suspend fun saveTelemetryRecord(record: MachineTelemetryRecord): DomainResult<MachineTelemetryRecord> {
        return try {
            transactionManager.inTransaction(TenantContext(record.tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO machine_telemetry_records (
                        telemetry_id, tenant_id, machine_id, source_device_id,
                        metric_type, metric_value, unit, event_timestamp,
                        ingested_at, metadata_json, idempotency_key, created_by
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, to_timestamp(? / 1000.0), to_timestamp(? / 1000.0), ?, ?, ?)
                    ON CONFLICT (telemetry_id) DO NOTHING
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, record.telemetryId)
                    ps.setString(2, record.tenantId)
                    ps.setString(3, record.machineId)
                    ps.setString(4, record.sourceDeviceId)
                    ps.setString(5, record.metricType.name)
                    ps.setBigDecimal(6, record.metricValue)
                    ps.setString(7, record.unit)
                    ps.setLong(8, record.eventTimestamp)
                    ps.setLong(9, record.ingestedAt)
                    ps.setString(10, record.metadataJson)
                    ps.setString(11, record.idempotencyKey)
                    ps.setString(12, record.createdBy)
                    ps.executeUpdate()
                }
            }
            DomainResult.Success(record)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save telemetry record")
        }
    }

    override suspend fun saveTelemetryBatch(records: List<MachineTelemetryRecord>): DomainResult<List<MachineTelemetryRecord>> {
        if (records.isEmpty()) return DomainResult.Success(emptyList())
        val tenantId = records.first().tenantId
        return try {
            transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = """
                    INSERT INTO machine_telemetry_records (
                        telemetry_id, tenant_id, machine_id, source_device_id,
                        metric_type, metric_value, unit, event_timestamp,
                        ingested_at, metadata_json, idempotency_key, created_by
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, to_timestamp(? / 1000.0), to_timestamp(? / 1000.0), ?, ?, ?)
                    ON CONFLICT (telemetry_id) DO NOTHING
                """.trimIndent()

                conn.prepareStatement(sql).use { ps ->
                    for (rec in records) {
                        ps.setString(1, rec.telemetryId)
                        ps.setString(2, rec.tenantId)
                        ps.setString(3, rec.machineId)
                        ps.setString(4, rec.sourceDeviceId)
                        ps.setString(5, rec.metricType.name)
                        ps.setBigDecimal(6, rec.metricValue)
                        ps.setString(7, rec.unit)
                        ps.setLong(8, rec.eventTimestamp)
                        ps.setLong(9, rec.ingestedAt)
                        ps.setString(10, rec.metadataJson)
                        ps.setString(11, rec.idempotencyKey)
                        ps.setString(12, rec.createdBy)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }
            DomainResult.Success(records)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "save telemetry batch")
        }
    }

    override suspend fun getTelemetryById(tenantId: String, telemetryId: String): DomainResult<MachineTelemetryRecord?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM machine_telemetry_records WHERE tenant_id = ? AND telemetry_id = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, telemetryId)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapRecord(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get telemetry by id")
        }
    }

    override suspend fun getTelemetryByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): DomainResult<MachineTelemetryRecord?> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = "SELECT * FROM machine_telemetry_records WHERE tenant_id = ? AND idempotency_key = ?"
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, tenantId)
                    ps.setString(2, idempotencyKey)
                    ps.executeQuery().use { rs ->
                        if (rs.next()) mapRecord(rs) else null
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "get telemetry by idempotency key")
        }
    }

    override suspend fun listTelemetryByMachine(
        tenantId: String,
        machineId: String,
        metricType: TelemetryMetricType?,
        limit: Int
    ): DomainResult<List<MachineTelemetryRecord>> {
        return try {
            val result = transactionManager.inTransaction(TenantContext(tenantId)) { ctx ->
                val conn = ctx.connection
                val sql = StringBuilder("SELECT * FROM machine_telemetry_records WHERE tenant_id = ? AND machine_id = ?")
                if (metricType != null) sql.append(" AND metric_type = ?")
                sql.append(" ORDER BY event_timestamp DESC LIMIT ?")

                conn.prepareStatement(sql.toString()).use { ps ->
                    var idx = 1
                    ps.setString(idx++, tenantId)
                    ps.setString(idx++, machineId)
                    if (metricType != null) ps.setString(idx++, metricType.name)
                    ps.setInt(idx, limit)

                    ps.executeQuery().use { rs ->
                        val list = mutableListOf<MachineTelemetryRecord>()
                        while (rs.next()) {
                            list.add(mapRecord(rs))
                        }
                        list
                    }
                }
            }
            DomainResult.Success(result)
        } catch (e: Exception) {
            PostgresErrorTranslator.translate(e, "list telemetry by machine")
        }
    }

    private fun mapRecord(rs: ResultSet): MachineTelemetryRecord {
        val eventTs = rs.getTimestamp("event_timestamp")
        val ingestedAtTs = rs.getTimestamp("ingested_at")
        return MachineTelemetryRecord(
            telemetryId = rs.getString("telemetry_id"),
            tenantId = rs.getString("tenant_id"),
            machineId = rs.getString("machine_id"),
            sourceDeviceId = rs.getString("source_device_id"),
            metricType = try { TelemetryMetricType.valueOf(rs.getString("metric_type")) } catch (_: Exception) { TelemetryMetricType.OTHER },
            metricValue = rs.getBigDecimal("metric_value"),
            unit = rs.getString("unit"),
            eventTimestamp = eventTs?.time ?: System.currentTimeMillis(),
            ingestedAt = ingestedAtTs?.time ?: System.currentTimeMillis(),
            metadataJson = rs.getString("metadata_json"),
            idempotencyKey = rs.getString("idempotency_key"),
            createdBy = rs.getString("created_by")
        )
    }
}

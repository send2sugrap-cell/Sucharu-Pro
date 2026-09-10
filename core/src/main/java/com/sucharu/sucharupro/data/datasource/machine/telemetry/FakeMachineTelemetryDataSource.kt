package com.sucharu.sucharupro.data.datasource.machine.telemetry

import com.sucharu.sucharupro.domain.machine.telemetry.MachineTelemetryRecord
import com.sucharu.sucharupro.domain.machine.telemetry.TelemetryMetricType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe fake in-memory data source for Machine Telemetry testing.
 */
class FakeMachineTelemetryDataSource : MachineTelemetryDataSource {

    private val telemetryRecords = ConcurrentHashMap<String, MachineTelemetryRecord>()

    override suspend fun saveTelemetryRecord(record: MachineTelemetryRecord): DomainResult<MachineTelemetryRecord> {
        val key = "${record.tenantId}:${record.telemetryId}"
        telemetryRecords[key] = record
        return DomainResult.Success(record)
    }

    override suspend fun saveTelemetryBatch(records: List<MachineTelemetryRecord>): DomainResult<List<MachineTelemetryRecord>> {
        records.forEach { rec ->
            telemetryRecords["${rec.tenantId}:${rec.telemetryId}"] = rec
        }
        return DomainResult.Success(records)
    }

    override suspend fun getTelemetryById(tenantId: String, telemetryId: String): DomainResult<MachineTelemetryRecord?> {
        val key = "$tenantId:$telemetryId"
        return DomainResult.Success(telemetryRecords[key])
    }

    override suspend fun getTelemetryByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): DomainResult<MachineTelemetryRecord?> {
        val found = telemetryRecords.values.firstOrNull {
            it.tenantId == tenantId &&
            it.idempotencyKey != null &&
            it.idempotencyKey.equals(idempotencyKey, ignoreCase = true)
        }
        return DomainResult.Success(found)
    }

    override suspend fun listTelemetryByMachine(
        tenantId: String,
        machineId: String,
        metricType: TelemetryMetricType?,
        limit: Int
    ): DomainResult<List<MachineTelemetryRecord>> {
        val filtered = telemetryRecords.values.filter { rec ->
            rec.tenantId == tenantId &&
            rec.machineId == machineId &&
            (metricType == null || rec.metricType == metricType)
        }.sortedByDescending { it.eventTimestamp }.take(limit)
        return DomainResult.Success(filtered)
    }
}

package com.sucharu.sucharupro.data.datasource.machine.telemetry

import com.sucharu.sucharupro.domain.machine.telemetry.MachineTelemetryRecord
import com.sucharu.sucharupro.domain.machine.telemetry.TelemetryMetricType
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Data Source interface for Machine Telemetry Ingestion persistence.
 */
interface MachineTelemetryDataSource {
    suspend fun saveTelemetryRecord(record: MachineTelemetryRecord): DomainResult<MachineTelemetryRecord>
    suspend fun saveTelemetryBatch(records: List<MachineTelemetryRecord>): DomainResult<List<MachineTelemetryRecord>>
    suspend fun getTelemetryById(tenantId: String, telemetryId: String): DomainResult<MachineTelemetryRecord?>
    suspend fun getTelemetryByIdempotencyKey(tenantId: String, idempotencyKey: String): DomainResult<MachineTelemetryRecord?>
    suspend fun listTelemetryByMachine(tenantId: String, machineId: String, metricType: TelemetryMetricType? = null, limit: Int = 100): DomainResult<List<MachineTelemetryRecord>>
}

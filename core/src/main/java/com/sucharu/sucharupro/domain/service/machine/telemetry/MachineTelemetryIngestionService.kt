package com.sucharu.sucharupro.domain.service.machine.telemetry

import com.sucharu.sucharupro.domain.machine.telemetry.MachineTelemetryRecord
import com.sucharu.sucharupro.domain.machine.telemetry.TelemetryMetricType
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Service interface for Machine Telemetry Ingestion orchestrations.
 */
interface MachineTelemetryIngestionService {
    suspend fun ingestTelemetry(record: MachineTelemetryRecord): DomainResult<MachineTelemetryRecord>
    suspend fun ingestTelemetryBatch(records: List<MachineTelemetryRecord>): DomainResult<List<MachineTelemetryRecord>>
    suspend fun listMachineTelemetry(tenantId: String, machineId: String, metricType: TelemetryMetricType? = null, limit: Int = 100): DomainResult<List<MachineTelemetryRecord>>
}

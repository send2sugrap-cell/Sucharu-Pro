package com.sucharu.sucharupro.data.repository.machine.telemetry

import com.sucharu.sucharupro.data.datasource.machine.telemetry.MachineTelemetryDataSource
import com.sucharu.sucharupro.domain.machine.telemetry.MachineTelemetryRecord
import com.sucharu.sucharupro.domain.machine.telemetry.MachineTelemetryValidator
import com.sucharu.sucharupro.domain.machine.telemetry.TelemetryMetricType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.machine.telemetry.MachineTelemetryRepository

/**
 * Production Repository implementation delegating to MachineTelemetryDataSource with validation & idempotency checks.
 */
class MachineTelemetryRepositoryImpl(
    private val dataSource: MachineTelemetryDataSource
) : MachineTelemetryRepository {

    override suspend fun saveTelemetryRecord(record: MachineTelemetryRecord): DomainResult<MachineTelemetryRecord> {
        val validation = MachineTelemetryValidator.validateRecord(record)
        if (validation is DomainResult.Error) {
            return validation
        }

        // Idempotency check if idempotencyKey is present
        if (!record.idempotencyKey.isNullOrBlank()) {
            val existing = dataSource.getTelemetryByIdempotencyKey(record.tenantId, record.idempotencyKey)
            if (existing is DomainResult.Success && existing.data != null) {
                return DomainResult.Success(existing.data)
            }
        }

        return dataSource.saveTelemetryRecord(record)
    }

    override suspend fun saveTelemetryBatch(records: List<MachineTelemetryRecord>): DomainResult<List<MachineTelemetryRecord>> {
        for (rec in records) {
            val validation = MachineTelemetryValidator.validateRecord(rec)
            if (validation is DomainResult.Error) {
                return validation
            }
        }
        return dataSource.saveTelemetryBatch(records)
    }

    override suspend fun getTelemetryById(tenantId: String, telemetryId: String): DomainResult<MachineTelemetryRecord?> {
        if (tenantId.isBlank() || telemetryId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Telemetry ID cannot be blank.")
        }
        return dataSource.getTelemetryById(tenantId, telemetryId)
    }

    override suspend fun getTelemetryByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): DomainResult<MachineTelemetryRecord?> {
        if (tenantId.isBlank() || idempotencyKey.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Idempotency Key cannot be blank.")
        }
        return dataSource.getTelemetryByIdempotencyKey(tenantId, idempotencyKey)
    }

    override suspend fun listTelemetryByMachine(
        tenantId: String,
        machineId: String,
        metricType: TelemetryMetricType?,
        limit: Int
    ): DomainResult<List<MachineTelemetryRecord>> {
        if (tenantId.isBlank() || machineId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Machine ID cannot be blank.")
        }
        return dataSource.listTelemetryByMachine(tenantId, machineId, metricType, limit)
    }
}

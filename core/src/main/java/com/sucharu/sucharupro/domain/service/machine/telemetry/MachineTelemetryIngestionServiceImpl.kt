package com.sucharu.sucharupro.domain.service.machine.telemetry

import com.sucharu.sucharupro.domain.machine.telemetry.MachineTelemetryRecord
import com.sucharu.sucharupro.domain.machine.telemetry.TelemetryMetricType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository
import com.sucharu.sucharupro.domain.repository.machine.telemetry.MachineTelemetryRepository

/**
 * Domain Service implementation for Machine Telemetry Ingestion verifying machine existence & tenant boundaries.
 */
class MachineTelemetryIngestionServiceImpl(
    private val telemetryRepository: MachineTelemetryRepository,
    private val machineRegistryRepository: MachineRegistryRepository
) : MachineTelemetryIngestionService {

    override suspend fun ingestTelemetry(record: MachineTelemetryRecord): DomainResult<MachineTelemetryRecord> {
        // Verify target machine exists in tenant's machine registry
        val machineCheck = machineRegistryRepository.getMachineById(record.tenantId, record.machineId)
        if (machineCheck is DomainResult.Error) {
            return DomainResult.Error(message = machineCheck.message)
        }
        val machine = (machineCheck as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Target machine '${record.machineId}' not found in registry for tenant '${record.tenantId}'.")

        if (!machine.isActive) {
            return DomainResult.Error(message = "Cannot ingest telemetry: machine '${record.machineId}' is inactive.")
        }

        return telemetryRepository.saveTelemetryRecord(record)
    }

    override suspend fun ingestTelemetryBatch(records: List<MachineTelemetryRecord>): DomainResult<List<MachineTelemetryRecord>> {
        if (records.isEmpty()) return DomainResult.Success(emptyList())

        val tenantId = records.first().tenantId
        for (rec in records) {
            val machineCheck = machineRegistryRepository.getMachineById(tenantId, rec.machineId)
            if (machineCheck is DomainResult.Error) {
                return DomainResult.Error(message = machineCheck.message)
            }
            if ((machineCheck as? DomainResult.Success)?.data == null) {
                return DomainResult.Error(message = "Target machine '${rec.machineId}' not found in registry for tenant '$tenantId'.")
            }
        }

        return telemetryRepository.saveTelemetryBatch(records)
    }

    override suspend fun listMachineTelemetry(
        tenantId: String,
        machineId: String,
        metricType: TelemetryMetricType?,
        limit: Int
    ): DomainResult<List<MachineTelemetryRecord>> {
        return telemetryRepository.listTelemetryByMachine(tenantId, machineId, metricType, limit)
    }
}

package com.sucharu.sucharupro.domain.service.machine.health

import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.health.MachineHealthEvaluator
import com.sucharu.sucharupro.domain.machine.health.MachineHealthPolicy
import com.sucharu.sucharupro.domain.machine.health.MachineHealthSnapshot
import com.sucharu.sucharupro.domain.machine.health.MachineOperationalState
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository
import com.sucharu.sucharupro.domain.repository.machine.telemetry.MachineTelemetryRepository

/**
 * Domain Service implementation for Machine Status & Health Monitoring.
 */
class MachineStatusMonitoringServiceImpl(
    private val machineRegistryRepository: MachineRegistryRepository,
    private val machineTelemetryRepository: MachineTelemetryRepository
) : MachineStatusMonitoringService {

    override suspend fun evaluateMachineHealth(
        tenantId: String,
        machineId: String,
        policy: MachineHealthPolicy
    ): DomainResult<MachineHealthSnapshot> {
        val machineResult = machineRegistryRepository.getMachineById(tenantId, machineId)
        if (machineResult is DomainResult.Error) {
            return DomainResult.Error(message = machineResult.message)
        }
        val machine = (machineResult as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Machine '$machineId' not found in registry for tenant '$tenantId'.")

        val telemetryResult = machineTelemetryRepository.listTelemetryByMachine(tenantId, machineId, limit = 10)
        val telemetry = (telemetryResult as? DomainResult.Success)?.data ?: emptyList()

        val snapshot = MachineHealthEvaluator.evaluate(machine, telemetry, policy)
        return DomainResult.Success(snapshot)
    }

    override suspend fun listMachinesHealth(
        tenantId: String,
        type: MachineType?,
        operationalState: MachineOperationalState?,
        policy: MachineHealthPolicy
    ): DomainResult<List<MachineHealthSnapshot>> {
        val machinesResult = machineRegistryRepository.listMachines(tenantId, type = type)
        if (machinesResult is DomainResult.Error) {
            return DomainResult.Error(message = machinesResult.message)
        }
        val machines = (machinesResult as? DomainResult.Success)?.data ?: emptyList()

        val snapshots = mutableListOf<MachineHealthSnapshot>()
        for (m in machines) {
            val telemetryResult = machineTelemetryRepository.listTelemetryByMachine(tenantId, m.machineId, limit = 10)
            val telemetry = (telemetryResult as? DomainResult.Success)?.data ?: emptyList()

            val snapshot = MachineHealthEvaluator.evaluate(m, telemetry, policy)
            if (operationalState == null || snapshot.operationalState == operationalState) {
                snapshots.add(snapshot)
            }
        }

        return DomainResult.Success(snapshots)
    }
}

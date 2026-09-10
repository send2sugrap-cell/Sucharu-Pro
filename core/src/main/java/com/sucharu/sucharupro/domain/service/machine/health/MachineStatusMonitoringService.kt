package com.sucharu.sucharupro.domain.service.machine.health

import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.health.MachineHealthPolicy
import com.sucharu.sucharupro.domain.machine.health.MachineHealthSnapshot
import com.sucharu.sucharupro.domain.machine.health.MachineOperationalState
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Service interface for Machine Operational Status & Health Monitoring.
 */
interface MachineStatusMonitoringService {
    suspend fun evaluateMachineHealth(
        tenantId: String,
        machineId: String,
        policy: MachineHealthPolicy = MachineHealthPolicy()
    ): DomainResult<MachineHealthSnapshot>

    suspend fun listMachinesHealth(
        tenantId: String,
        type: MachineType? = null,
        operationalState: MachineOperationalState? = null,
        policy: MachineHealthPolicy = MachineHealthPolicy()
    ): DomainResult<List<MachineHealthSnapshot>>
}

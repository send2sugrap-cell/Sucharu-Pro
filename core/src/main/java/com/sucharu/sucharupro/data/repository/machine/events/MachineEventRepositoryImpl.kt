package com.sucharu.sucharupro.data.repository.machine.events

import com.sucharu.sucharupro.data.datasource.machine.events.MachineEventDataSource
import com.sucharu.sucharupro.domain.machine.events.DowntimeStatus
import com.sucharu.sucharupro.domain.machine.events.FaultStatus
import com.sucharu.sucharupro.domain.machine.events.MachineDowntimeEvent
import com.sucharu.sucharupro.domain.machine.events.MachineEventValidator
import com.sucharu.sucharupro.domain.machine.events.MachineFaultEvent
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.machine.events.MachineEventRepository

/**
 * Production Repository implementation delegating to MachineEventDataSource with validation.
 */
class MachineEventRepositoryImpl(
    private val dataSource: MachineEventDataSource
) : MachineEventRepository {

    override suspend fun saveFaultEvent(event: MachineFaultEvent): DomainResult<MachineFaultEvent> {
        val validation = MachineEventValidator.validateFaultEvent(event)
        if (validation is DomainResult.Error) {
            return validation
        }
        return dataSource.saveFaultEvent(event)
    }

    override suspend fun getFaultEventById(tenantId: String, faultEventId: String): DomainResult<MachineFaultEvent?> {
        if (tenantId.isBlank() || faultEventId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Fault Event ID cannot be blank.")
        }
        return dataSource.getFaultEventById(tenantId, faultEventId)
    }

    override suspend fun listFaultEventsByMachine(
        tenantId: String,
        machineId: String,
        status: FaultStatus?,
        limit: Int
    ): DomainResult<List<MachineFaultEvent>> {
        if (tenantId.isBlank() || machineId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Machine ID cannot be blank.")
        }
        return dataSource.listFaultEventsByMachine(tenantId, machineId, status, limit)
    }

    override suspend fun saveDowntimeEvent(event: MachineDowntimeEvent): DomainResult<MachineDowntimeEvent> {
        val validation = MachineEventValidator.validateDowntimeEvent(event)
        if (validation is DomainResult.Error) {
            return validation
        }
        return dataSource.saveDowntimeEvent(event)
    }

    override suspend fun getDowntimeEventById(tenantId: String, downtimeId: String): DomainResult<MachineDowntimeEvent?> {
        if (tenantId.isBlank() || downtimeId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Downtime ID cannot be blank.")
        }
        return dataSource.getDowntimeEventById(tenantId, downtimeId)
    }

    override suspend fun listDowntimeEventsByMachine(
        tenantId: String,
        machineId: String,
        status: DowntimeStatus?,
        limit: Int
    ): DomainResult<List<MachineDowntimeEvent>> {
        if (tenantId.isBlank() || machineId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Machine ID cannot be blank.")
        }
        return dataSource.listDowntimeEventsByMachine(tenantId, machineId, status, limit)
    }
}

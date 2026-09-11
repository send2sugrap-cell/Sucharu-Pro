package com.sucharu.sucharupro.domain.service.machine.events

import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.events.DowntimeStatus
import com.sucharu.sucharupro.domain.machine.events.MachineDowntimeEvent
import com.sucharu.sucharupro.domain.machine.events.MachineEventValidator
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository
import com.sucharu.sucharupro.domain.repository.machine.events.MachineEventRepository

/**
 * Domain Service implementation for Machine Downtime Event operations.
 */
class MachineDowntimeServiceImpl(
    private val eventRepository: MachineEventRepository,
    private val machineRegistryRepository: MachineRegistryRepository
) : MachineDowntimeService {

    override suspend fun startDowntime(event: MachineDowntimeEvent): DomainResult<MachineDowntimeEvent> {
        val machineCheck = machineRegistryRepository.getMachineById(event.tenantId, event.machineId)
        if (machineCheck is DomainResult.Error) return machineCheck
        val machine = (machineCheck as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Machine '${event.machineId}' not found in registry.")

        if (machine.status == MachineStatus.DECOMMISSIONED) {
            return DomainResult.Error(message = "Cannot record downtime event for decommissioned machine '${event.machineId}'.")
        }

        val validation = MachineEventValidator.validateDowntimeEvent(event)
        if (validation is DomainResult.Error) return validation

        return eventRepository.saveDowntimeEvent(event)
    }

    override suspend fun endDowntime(
        tenantId: String,
        machineId: String,
        downtimeId: String,
        endedAt: Long,
        actorId: String
    ): DomainResult<MachineDowntimeEvent> {
        val eventCheck = eventRepository.getDowntimeEventById(tenantId, downtimeId)
        if (eventCheck is DomainResult.Error) return eventCheck
        val existing = (eventCheck as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Downtime event '$downtimeId' not found.")

        if (existing.machineId != machineId) {
            return DomainResult.Error(message = "Machine ID mismatch for downtime event '$downtimeId'.")
        }

        if (existing.status.isTerminal) {
            return DomainResult.Error(message = "Downtime event '$downtimeId' is already terminal ('${existing.status.name}').")
        }

        if (endedAt < existing.startedAt) {
            return DomainResult.Error(message = "End timestamp cannot be earlier than start timestamp.")
        }

        val durationSec = (endedAt - existing.startedAt) / 1000L
        val updated = existing.copy(
            status = DowntimeStatus.ENDED,
            endedAt = endedAt,
            durationSeconds = durationSec,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId
        )
        return eventRepository.saveDowntimeEvent(updated)
    }

    override suspend fun getDowntimeDetails(
        tenantId: String,
        downtimeId: String
    ): DomainResult<MachineDowntimeEvent?> {
        return eventRepository.getDowntimeEventById(tenantId, downtimeId)
    }

    override suspend fun listDowntimeEvents(
        tenantId: String,
        machineId: String,
        status: DowntimeStatus?,
        limit: Int
    ): DomainResult<List<MachineDowntimeEvent>> {
        return eventRepository.listDowntimeEventsByMachine(tenantId, machineId, status, limit)
    }
}

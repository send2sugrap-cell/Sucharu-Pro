package com.sucharu.sucharupro.domain.service.machine.events

import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.events.FaultStatus
import com.sucharu.sucharupro.domain.machine.events.MachineEventValidator
import com.sucharu.sucharupro.domain.machine.events.MachineFaultEvent
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository
import com.sucharu.sucharupro.domain.repository.machine.events.MachineEventRepository

/**
 * Domain Service implementation for Machine Fault Event operations.
 */
class MachineFaultEventServiceImpl(
    private val eventRepository: MachineEventRepository,
    private val machineRegistryRepository: MachineRegistryRepository
) : MachineFaultEventService {

    override suspend fun recordFault(event: MachineFaultEvent): DomainResult<MachineFaultEvent> {
        val machineCheck = machineRegistryRepository.getMachineById(event.tenantId, event.machineId)
        if (machineCheck is DomainResult.Error) return machineCheck
        val machine = (machineCheck as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Machine '${event.machineId}' not found in registry.")

        if (machine.status == MachineStatus.DECOMMISSIONED) {
            return DomainResult.Error(message = "Cannot record fault event for decommissioned machine '${event.machineId}'.")
        }

        return eventRepository.saveFaultEvent(event)
    }

    override suspend fun acknowledgeFault(
        tenantId: String,
        machineId: String,
        faultEventId: String,
        actorId: String
    ): DomainResult<MachineFaultEvent> {
        val eventCheck = eventRepository.getFaultEventById(tenantId, faultEventId)
        if (eventCheck is DomainResult.Error) return eventCheck
        val existing = (eventCheck as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Fault event '$faultEventId' not found.")

        if (existing.machineId != machineId) {
            return DomainResult.Error(message = "Machine ID mismatch for fault event '$faultEventId'.")
        }

        val transitionValidation = MachineEventValidator.validateFaultStatusTransition(existing.status, FaultStatus.ACKNOWLEDGED)
        if (transitionValidation is DomainResult.Error) return transitionValidation

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = FaultStatus.ACKNOWLEDGED,
            acknowledgedAt = now,
            acknowledgedBy = actorId,
            updatedAt = now,
            updatedBy = actorId
        )
        return eventRepository.saveFaultEvent(updated)
    }

    override suspend fun resolveFault(
        tenantId: String,
        machineId: String,
        faultEventId: String,
        resolutionNotes: String?,
        maintenanceRecordId: String?,
        actorId: String
    ): DomainResult<MachineFaultEvent> {
        val eventCheck = eventRepository.getFaultEventById(tenantId, faultEventId)
        if (eventCheck is DomainResult.Error) return eventCheck
        val existing = (eventCheck as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Fault event '$faultEventId' not found.")

        if (existing.machineId != machineId) {
            return DomainResult.Error(message = "Machine ID mismatch for fault event '$faultEventId'.")
        }

        val transitionValidation = MachineEventValidator.validateFaultStatusTransition(existing.status, FaultStatus.RESOLVED)
        if (transitionValidation is DomainResult.Error) return transitionValidation

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = FaultStatus.RESOLVED,
            resolvedAt = now,
            resolvedBy = actorId,
            resolutionNotes = resolutionNotes ?: existing.resolutionNotes,
            maintenanceRecordId = maintenanceRecordId ?: existing.maintenanceRecordId,
            updatedAt = now,
            updatedBy = actorId
        )
        return eventRepository.saveFaultEvent(updated)
    }

    override suspend fun getFaultEventDetails(
        tenantId: String,
        faultEventId: String
    ): DomainResult<MachineFaultEvent?> {
        return eventRepository.getFaultEventById(tenantId, faultEventId)
    }

    override suspend fun listFaultEvents(
        tenantId: String,
        machineId: String,
        status: FaultStatus?,
        limit: Int
    ): DomainResult<List<MachineFaultEvent>> {
        return eventRepository.listFaultEventsByMachine(tenantId, machineId, status, limit)
    }
}

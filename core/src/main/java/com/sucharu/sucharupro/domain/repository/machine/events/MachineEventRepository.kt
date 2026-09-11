package com.sucharu.sucharupro.domain.repository.machine.events

import com.sucharu.sucharupro.domain.machine.events.DowntimeStatus
import com.sucharu.sucharupro.domain.machine.events.FaultStatus
import com.sucharu.sucharupro.domain.machine.events.MachineDowntimeEvent
import com.sucharu.sucharupro.domain.machine.events.MachineFaultEvent
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Repository interface for Machine Fault and Downtime Events.
 */
interface MachineEventRepository {
    suspend fun saveFaultEvent(event: MachineFaultEvent): DomainResult<MachineFaultEvent>
    suspend fun getFaultEventById(tenantId: String, faultEventId: String): DomainResult<MachineFaultEvent?>
    suspend fun listFaultEventsByMachine(tenantId: String, machineId: String, status: FaultStatus? = null, limit: Int = 100): DomainResult<List<MachineFaultEvent>>
    suspend fun saveDowntimeEvent(event: MachineDowntimeEvent): DomainResult<MachineDowntimeEvent>
    suspend fun getDowntimeEventById(tenantId: String, downtimeId: String): DomainResult<MachineDowntimeEvent?>
    suspend fun listDowntimeEventsByMachine(tenantId: String, machineId: String, status: DowntimeStatus? = null, limit: Int = 100): DomainResult<List<MachineDowntimeEvent>>
}

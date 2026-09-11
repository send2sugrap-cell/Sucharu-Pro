package com.sucharu.sucharupro.domain.service.machine.events

import com.sucharu.sucharupro.domain.machine.events.DowntimeStatus
import com.sucharu.sucharupro.domain.machine.events.MachineDowntimeEvent
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Service interface for Machine Downtime Events operations.
 */
interface MachineDowntimeService {
    suspend fun startDowntime(event: MachineDowntimeEvent): DomainResult<MachineDowntimeEvent>
    suspend fun endDowntime(tenantId: String, machineId: String, downtimeId: String, endedAt: Long = System.currentTimeMillis(), actorId: String): DomainResult<MachineDowntimeEvent>
    suspend fun getDowntimeDetails(tenantId: String, downtimeId: String): DomainResult<MachineDowntimeEvent?>
    suspend fun listDowntimeEvents(tenantId: String, machineId: String, status: DowntimeStatus? = null, limit: Int = 100): DomainResult<List<MachineDowntimeEvent>>
}

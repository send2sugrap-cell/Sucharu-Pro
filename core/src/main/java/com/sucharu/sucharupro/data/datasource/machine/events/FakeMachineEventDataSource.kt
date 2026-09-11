package com.sucharu.sucharupro.data.datasource.machine.events

import com.sucharu.sucharupro.domain.machine.events.DowntimeStatus
import com.sucharu.sucharupro.domain.machine.events.FaultStatus
import com.sucharu.sucharupro.domain.machine.events.MachineDowntimeEvent
import com.sucharu.sucharupro.domain.machine.events.MachineFaultEvent
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe fake in-memory data source for Machine Fault Events and Downtime Events testing.
 */
class FakeMachineEventDataSource : MachineEventDataSource {

    private val faultEvents = ConcurrentHashMap<String, MachineFaultEvent>()
    private val downtimeEvents = ConcurrentHashMap<String, MachineDowntimeEvent>()

    override suspend fun saveFaultEvent(event: MachineFaultEvent): DomainResult<MachineFaultEvent> {
        val key = "${event.tenantId}:${event.faultEventId}"
        faultEvents[key] = event
        return DomainResult.Success(event)
    }

    override suspend fun getFaultEventById(tenantId: String, faultEventId: String): DomainResult<MachineFaultEvent?> {
        val key = "$tenantId:$faultEventId"
        return DomainResult.Success(faultEvents[key])
    }

    override suspend fun listFaultEventsByMachine(
        tenantId: String,
        machineId: String,
        status: FaultStatus?,
        limit: Int
    ): DomainResult<List<MachineFaultEvent>> {
        val filtered = faultEvents.values.filter { f ->
            f.tenantId == tenantId &&
            f.machineId == machineId &&
            (status == null || f.status == status)
        }.sortedByDescending { it.occurredAt }.take(limit)
        return DomainResult.Success(filtered)
    }

    override suspend fun saveDowntimeEvent(event: MachineDowntimeEvent): DomainResult<MachineDowntimeEvent> {
        val key = "${event.tenantId}:${event.downtimeId}"
        downtimeEvents[key] = event
        return DomainResult.Success(event)
    }

    override suspend fun getDowntimeEventById(tenantId: String, downtimeId: String): DomainResult<MachineDowntimeEvent?> {
        val key = "$tenantId:$downtimeId"
        return DomainResult.Success(downtimeEvents[key])
    }

    override suspend fun listDowntimeEventsByMachine(
        tenantId: String,
        machineId: String,
        status: DowntimeStatus?,
        limit: Int
    ): DomainResult<List<MachineDowntimeEvent>> {
        val filtered = downtimeEvents.values.filter { d ->
            d.tenantId == tenantId &&
            d.machineId == machineId &&
            (status == null || d.status == status)
        }.sortedByDescending { it.startedAt }.take(limit)
        return DomainResult.Success(filtered)
    }
}

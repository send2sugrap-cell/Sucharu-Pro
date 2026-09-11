package com.sucharu.sucharupro.domain.service.machine.events

import com.sucharu.sucharupro.domain.machine.events.FaultStatus
import com.sucharu.sucharupro.domain.machine.events.MachineFaultEvent
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Service interface for Machine Fault Events operations.
 */
interface MachineFaultEventService {
    suspend fun recordFault(event: MachineFaultEvent): DomainResult<MachineFaultEvent>
    suspend fun acknowledgeFault(tenantId: String, machineId: String, faultEventId: String, actorId: String): DomainResult<MachineFaultEvent>
    suspend fun resolveFault(tenantId: String, machineId: String, faultEventId: String, resolutionNotes: String? = null, maintenanceRecordId: String? = null, actorId: String): DomainResult<MachineFaultEvent>
    suspend fun getFaultEventDetails(tenantId: String, faultEventId: String): DomainResult<MachineFaultEvent?>
    suspend fun listFaultEvents(tenantId: String, machineId: String, status: FaultStatus? = null, limit: Int = 100): DomainResult<List<MachineFaultEvent>>
}

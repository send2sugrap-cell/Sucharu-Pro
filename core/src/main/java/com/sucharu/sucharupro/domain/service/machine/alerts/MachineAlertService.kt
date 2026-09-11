package com.sucharu.sucharupro.domain.service.machine.alerts

import com.sucharu.sucharupro.domain.machine.alerts.MachineAlertStatus
import com.sucharu.sucharupro.domain.machine.alerts.MachineOperationalAlert
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Service interface for Machine Operational Alert operations.
 */
interface MachineAlertService {
    suspend fun raiseAlert(alert: MachineOperationalAlert): DomainResult<MachineOperationalAlert>
    suspend fun acknowledgeAlert(tenantId: String, machineId: String, alertId: String, actorId: String): DomainResult<MachineOperationalAlert>
    suspend fun resolveAlert(tenantId: String, machineId: String, alertId: String, resolutionNotes: String?, actorId: String): DomainResult<MachineOperationalAlert>
    suspend fun dismissAlert(tenantId: String, machineId: String, alertId: String, reason: String?, actorId: String): DomainResult<MachineOperationalAlert>
    suspend fun getAlertDetails(tenantId: String, alertId: String): DomainResult<MachineOperationalAlert?>
    suspend fun listAlerts(tenantId: String, machineId: String, status: MachineAlertStatus? = null, limit: Int = 100): DomainResult<List<MachineOperationalAlert>>
}

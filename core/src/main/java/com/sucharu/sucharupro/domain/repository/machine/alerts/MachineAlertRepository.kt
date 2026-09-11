package com.sucharu.sucharupro.domain.repository.machine.alerts

import com.sucharu.sucharupro.domain.machine.alerts.MachineAlertStatus
import com.sucharu.sucharupro.domain.machine.alerts.MachineOperationalAlert
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Repository interface for Machine Operational Alerts.
 */
interface MachineAlertRepository {
    suspend fun saveAlert(alert: MachineOperationalAlert): DomainResult<MachineOperationalAlert>
    suspend fun getAlertById(tenantId: String, alertId: String): DomainResult<MachineOperationalAlert?>
    suspend fun getAlertByCorrelationKey(tenantId: String, correlationKey: String): DomainResult<MachineOperationalAlert?>
    suspend fun listAlertsByMachine(tenantId: String, machineId: String, status: MachineAlertStatus? = null, limit: Int = 100): DomainResult<List<MachineOperationalAlert>>
}

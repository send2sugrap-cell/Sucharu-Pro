package com.sucharu.sucharupro.data.datasource.machine.alerts

import com.sucharu.sucharupro.domain.machine.alerts.MachineAlertStatus
import com.sucharu.sucharupro.domain.machine.alerts.MachineOperationalAlert
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe fake in-memory data source for Machine Operational Alerts testing.
 */
class FakeMachineAlertDataSource : MachineAlertDataSource {

    private val alerts = ConcurrentHashMap<String, MachineOperationalAlert>()

    override suspend fun saveAlert(alert: MachineOperationalAlert): DomainResult<MachineOperationalAlert> {
        val key = "${alert.tenantId}:${alert.alertId}"
        alerts[key] = alert
        return DomainResult.Success(alert)
    }

    override suspend fun getAlertById(tenantId: String, alertId: String): DomainResult<MachineOperationalAlert?> {
        val key = "$tenantId:$alertId"
        return DomainResult.Success(alerts[key])
    }

    override suspend fun getAlertByCorrelationKey(
        tenantId: String,
        correlationKey: String
    ): DomainResult<MachineOperationalAlert?> {
        val alert = alerts.values.find { a -> a.tenantId == tenantId && a.correlationKey == correlationKey && !a.status.isTerminal }
        return DomainResult.Success(alert)
    }

    override suspend fun listAlertsByMachine(
        tenantId: String,
        machineId: String,
        status: MachineAlertStatus?,
        limit: Int
    ): DomainResult<List<MachineOperationalAlert>> {
        val filtered = alerts.values.filter { a ->
            a.tenantId == tenantId &&
            a.machineId == machineId &&
            (status == null || a.status == status)
        }.sortedByDescending { it.occurredAt }.take(limit)
        return DomainResult.Success(filtered)
    }
}

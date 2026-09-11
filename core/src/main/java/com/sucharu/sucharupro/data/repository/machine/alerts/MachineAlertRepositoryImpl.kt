package com.sucharu.sucharupro.data.repository.machine.alerts

import com.sucharu.sucharupro.data.datasource.machine.alerts.MachineAlertDataSource
import com.sucharu.sucharupro.domain.machine.alerts.MachineAlertStatus
import com.sucharu.sucharupro.domain.machine.alerts.MachineAlertValidator
import com.sucharu.sucharupro.domain.machine.alerts.MachineOperationalAlert
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.machine.alerts.MachineAlertRepository

/**
 * Production Repository implementation delegating to MachineAlertDataSource with validation.
 */
class MachineAlertRepositoryImpl(
    private val dataSource: MachineAlertDataSource
) : MachineAlertRepository {

    override suspend fun saveAlert(alert: MachineOperationalAlert): DomainResult<MachineOperationalAlert> {
        val validation = MachineAlertValidator.validateAlert(alert)
        if (validation is DomainResult.Error) {
            return validation
        }
        return dataSource.saveAlert(alert)
    }

    override suspend fun getAlertById(tenantId: String, alertId: String): DomainResult<MachineOperationalAlert?> {
        if (tenantId.isBlank() || alertId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Alert ID cannot be blank.")
        }
        return dataSource.getAlertById(tenantId, alertId)
    }

    override suspend fun getAlertByCorrelationKey(
        tenantId: String,
        correlationKey: String
    ): DomainResult<MachineOperationalAlert?> {
        if (tenantId.isBlank() || correlationKey.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and correlation key cannot be blank.")
        }
        return dataSource.getAlertByCorrelationKey(tenantId, correlationKey)
    }

    override suspend fun listAlertsByMachine(
        tenantId: String,
        machineId: String,
        status: MachineAlertStatus?,
        limit: Int
    ): DomainResult<List<MachineOperationalAlert>> {
        if (tenantId.isBlank() || machineId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Machine ID cannot be blank.")
        }
        return dataSource.listAlertsByMachine(tenantId, machineId, status, limit)
    }
}

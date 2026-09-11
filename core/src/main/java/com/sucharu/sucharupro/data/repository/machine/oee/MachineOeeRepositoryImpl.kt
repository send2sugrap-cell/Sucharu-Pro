package com.sucharu.sucharupro.data.repository.machine.oee

import com.sucharu.sucharupro.data.datasource.machine.oee.MachineOeeDataSource
import com.sucharu.sucharupro.domain.machine.oee.MachineOeeMetrics
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.machine.oee.MachineOeeRepository

/**
 * Production Repository implementation delegating to MachineOeeDataSource.
 */
class MachineOeeRepositoryImpl(
    private val dataSource: MachineOeeDataSource
) : MachineOeeRepository {

    override suspend fun saveOeeMetrics(metrics: MachineOeeMetrics): DomainResult<MachineOeeMetrics> {
        if (metrics.tenantId.isBlank() || metrics.machineId.isBlank() || metrics.metricId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID, Machine ID, and Metric ID cannot be blank.")
        }
        if (metrics.periodEnd < metrics.periodStart) {
            return DomainResult.Error(message = "Period end cannot be earlier than period start.")
        }
        return dataSource.saveOeeMetrics(metrics)
    }

    override suspend fun getOeeMetricsById(tenantId: String, metricId: String): DomainResult<MachineOeeMetrics?> {
        if (tenantId.isBlank() || metricId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Metric ID cannot be blank.")
        }
        return dataSource.getOeeMetricsById(tenantId, metricId)
    }

    override suspend fun getLatestOeeMetricsByMachine(
        tenantId: String,
        machineId: String
    ): DomainResult<MachineOeeMetrics?> {
        if (tenantId.isBlank() || machineId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Machine ID cannot be blank.")
        }
        return dataSource.getLatestOeeMetricsByMachine(tenantId, machineId)
    }

    override suspend fun listOeeMetricsByMachine(
        tenantId: String,
        machineId: String,
        limit: Int
    ): DomainResult<List<MachineOeeMetrics>> {
        if (tenantId.isBlank() || machineId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Machine ID cannot be blank.")
        }
        return dataSource.listOeeMetricsByMachine(tenantId, machineId, limit)
    }
}

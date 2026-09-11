package com.sucharu.sucharupro.domain.repository.machine.oee

import com.sucharu.sucharupro.domain.machine.oee.MachineOeeMetrics
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Repository interface for Machine OEE Metrics operations.
 */
interface MachineOeeRepository {
    suspend fun saveOeeMetrics(metrics: MachineOeeMetrics): DomainResult<MachineOeeMetrics>
    suspend fun getOeeMetricsById(tenantId: String, metricId: String): DomainResult<MachineOeeMetrics?>
    suspend fun getLatestOeeMetricsByMachine(tenantId: String, machineId: String): DomainResult<MachineOeeMetrics?>
    suspend fun listOeeMetricsByMachine(tenantId: String, machineId: String, limit: Int = 100): DomainResult<List<MachineOeeMetrics>>
}

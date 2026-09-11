package com.sucharu.sucharupro.data.datasource.machine.oee

import com.sucharu.sucharupro.domain.machine.oee.MachineOeeMetrics
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe fake in-memory data source for Machine OEE Metrics testing.
 */
class FakeMachineOeeDataSource : MachineOeeDataSource {

    private val metricsMap = ConcurrentHashMap<String, MachineOeeMetrics>()

    override suspend fun saveOeeMetrics(metrics: MachineOeeMetrics): DomainResult<MachineOeeMetrics> {
        val key = "${metrics.tenantId}:${metrics.metricId}"
        metricsMap[key] = metrics
        return DomainResult.Success(metrics)
    }

    override suspend fun getOeeMetricsById(tenantId: String, metricId: String): DomainResult<MachineOeeMetrics?> {
        val key = "$tenantId:$metricId"
        return DomainResult.Success(metricsMap[key])
    }

    override suspend fun getLatestOeeMetricsByMachine(
        tenantId: String,
        machineId: String
    ): DomainResult<MachineOeeMetrics?> {
        val latest = metricsMap.values.filter { m ->
            m.tenantId == tenantId && m.machineId == machineId
        }.maxByOrNull { it.periodEnd }
        return DomainResult.Success(latest)
    }

    override suspend fun listOeeMetricsByMachine(
        tenantId: String,
        machineId: String,
        limit: Int
    ): DomainResult<List<MachineOeeMetrics>> {
        val filtered = metricsMap.values.filter { m ->
            m.tenantId == tenantId && m.machineId == machineId
        }.sortedByDescending { it.periodEnd }.take(limit)
        return DomainResult.Success(filtered)
    }
}

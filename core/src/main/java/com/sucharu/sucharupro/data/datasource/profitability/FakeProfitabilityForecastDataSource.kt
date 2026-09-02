package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe In-Memory Data Source for Profitability Forecasting unit and integration testing.
 * Module 16 Step 08.
 */
class FakeProfitabilityForecastDataSource : ProfitabilityForecastDataSource {

    private val snapshots = ConcurrentHashMap<String, ProfitabilityForecastSnapshot>()
    private val scenarios = ConcurrentHashMap<String, ProfitabilityScenario>()
    private val reconciliations = ConcurrentHashMap<String, MutableList<ProfitabilityForecastReconciliationEvent>>()
    private val comparisons = ConcurrentHashMap<String, ForecastActualComparison>()
    private val audits = ConcurrentHashMap<String, MutableList<ProfitabilityForecastAuditEvent>>()
    private val idempotency = ConcurrentHashMap<String, String>()

    override suspend fun saveSnapshot(snapshot: ProfitabilityForecastSnapshot): DomainResult<ProfitabilityForecastSnapshot> {
        val key = "${snapshot.tenantId}:${snapshot.forecastId}"
        snapshots[key] = snapshot
        return DomainResult.Success(snapshot)
    }

    override suspend fun getSnapshotById(tenantId: String, forecastId: String): DomainResult<ProfitabilityForecastSnapshot?> {
        val key = "$tenantId:$forecastId"
        return DomainResult.Success(snapshots[key])
    }

    override suspend fun listSnapshots(tenantId: String, filter: ProfitabilityForecastFilter): DomainResult<List<ProfitabilityForecastSnapshot>> {
        var filtered = snapshots.values.filter { it.tenantId == tenantId }
        if (filter.targetScope != null) filtered = filtered.filter { it.targetScope == filter.targetScope }
        if (filter.targetEntityId != null) filtered = filtered.filter { it.targetEntityId == filter.targetEntityId }
        if (filter.forecastMethod != null) filtered = filtered.filter { it.forecastMethod == filter.forecastMethod }
        if (filter.scenarioType != null) filtered = filtered.filter { it.scenarioType == filter.scenarioType }
        if (filter.status != null) filtered = filtered.filter { it.status == filter.status }
        val paged = filtered.sortedByDescending { it.generatedAt }.drop(filter.offset).take(filter.limit)
        return DomainResult.Success(paged)
    }

    override suspend fun saveScenario(scenario: ProfitabilityScenario): DomainResult<ProfitabilityScenario> {
        val key = "${scenario.tenantId}:${scenario.scenarioId}"
        scenarios[key] = scenario
        return DomainResult.Success(scenario)
    }

    override suspend fun listScenarios(tenantId: String, projectId: String, scope: ProfitabilityForecastScope?): DomainResult<List<ProfitabilityScenario>> {
        var filtered = scenarios.values.filter { it.tenantId == tenantId && it.projectId == projectId }
        if (scope != null) filtered = filtered.filter { it.targetScope == scope }
        return DomainResult.Success(filtered.toList())
    }

    override suspend fun getScenarioById(tenantId: String, scenarioId: String): DomainResult<ProfitabilityScenario?> {
        val key = "$tenantId:$scenarioId"
        return DomainResult.Success(scenarios[key])
    }

    override suspend fun saveReconciliationEvent(event: ProfitabilityForecastReconciliationEvent): DomainResult<ProfitabilityForecastReconciliationEvent> {
        val key = "${event.tenantId}:${event.forecastId}"
        reconciliations.computeIfAbsent(key) { mutableListOf() }.add(event)
        return DomainResult.Success(event)
    }

    override suspend fun listReconciliationEvents(tenantId: String, forecastId: String): DomainResult<List<ProfitabilityForecastReconciliationEvent>> {
        val key = "$tenantId:$forecastId"
        return DomainResult.Success(reconciliations[key]?.toList() ?: emptyList())
    }

    override suspend fun saveActualComparison(comparison: ForecastActualComparison): DomainResult<ForecastActualComparison> {
        val key = "${comparison.tenantId}:${comparison.forecastId}"
        comparisons[key] = comparison
        return DomainResult.Success(comparison)
    }

    override suspend fun getActualComparison(tenantId: String, forecastId: String): DomainResult<ForecastActualComparison?> {
        val key = "$tenantId:$forecastId"
        return DomainResult.Success(comparisons[key])
    }

    override suspend fun recordAuditEvent(event: ProfitabilityForecastAuditEvent): DomainResult<Unit> {
        val key = "${event.tenantId}:${event.forecastId}"
        audits.computeIfAbsent(key) { mutableListOf() }.add(event)
        return DomainResult.Success(Unit)
    }

    override suspend fun listAuditEvents(tenantId: String, forecastId: String): DomainResult<List<ProfitabilityForecastAuditEvent>> {
        val key = "$tenantId:$forecastId"
        return DomainResult.Success(audits[key]?.toList() ?: emptyList())
    }

    override suspend fun checkIdempotency(tenantId: String, idempotencyKey: String): DomainResult<String?> {
        val key = "$tenantId:$idempotencyKey"
        return DomainResult.Success(idempotency[key])
    }

    override suspend fun saveIdempotencyRecord(tenantId: String, idempotencyKey: String, forecastId: String): DomainResult<Unit> {
        val key = "$tenantId:$idempotencyKey"
        idempotency[key] = forecastId
        return DomainResult.Success(Unit)
    }
}

package com.sucharu.sucharupro.data.repository.profitability

import com.sucharu.sucharupro.data.datasource.profitability.ProfitabilityForecastDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Implementation of ProfitabilityForecastRepository delegating to ProfitabilityForecastDataSource.
 * Module 16 Step 08.
 */
class ProfitabilityForecastRepositoryImpl(
    private val dataSource: ProfitabilityForecastDataSource
) : ProfitabilityForecastRepository {

    override suspend fun saveSnapshot(snapshot: ProfitabilityForecastSnapshot): DomainResult<ProfitabilityForecastSnapshot> {
        return dataSource.saveSnapshot(snapshot)
    }

    override suspend fun getSnapshotById(tenantId: String, forecastId: String): DomainResult<ProfitabilityForecastSnapshot?> {
        return dataSource.getSnapshotById(tenantId, forecastId)
    }

    override suspend fun listSnapshots(tenantId: String, filter: ProfitabilityForecastFilter): DomainResult<List<ProfitabilityForecastSnapshot>> {
        return dataSource.listSnapshots(tenantId, filter)
    }

    override suspend fun saveScenario(scenario: ProfitabilityScenario): DomainResult<ProfitabilityScenario> {
        return dataSource.saveScenario(scenario)
    }

    override suspend fun listScenarios(tenantId: String, projectId: String, scope: ProfitabilityForecastScope?): DomainResult<List<ProfitabilityScenario>> {
        return dataSource.listScenarios(tenantId, projectId, scope)
    }

    override suspend fun getScenarioById(tenantId: String, scenarioId: String): DomainResult<ProfitabilityScenario?> {
        return dataSource.getScenarioById(tenantId, scenarioId)
    }

    override suspend fun saveReconciliationEvent(event: ProfitabilityForecastReconciliationEvent): DomainResult<ProfitabilityForecastReconciliationEvent> {
        return dataSource.saveReconciliationEvent(event)
    }

    override suspend fun listReconciliationEvents(tenantId: String, forecastId: String): DomainResult<List<ProfitabilityForecastReconciliationEvent>> {
        return dataSource.listReconciliationEvents(tenantId, forecastId)
    }

    override suspend fun saveActualComparison(comparison: ForecastActualComparison): DomainResult<ForecastActualComparison> {
        return dataSource.saveActualComparison(comparison)
    }

    override suspend fun getActualComparison(tenantId: String, forecastId: String): DomainResult<ForecastActualComparison?> {
        return dataSource.getActualComparison(tenantId, forecastId)
    }

    override suspend fun recordAuditEvent(event: ProfitabilityForecastAuditEvent): DomainResult<Unit> {
        return dataSource.recordAuditEvent(event)
    }

    override suspend fun listAuditEvents(tenantId: String, forecastId: String): DomainResult<List<ProfitabilityForecastAuditEvent>> {
        return dataSource.listAuditEvents(tenantId, forecastId)
    }

    override suspend fun checkIdempotency(tenantId: String, idempotencyKey: String): DomainResult<String?> {
        return dataSource.checkIdempotency(tenantId, idempotencyKey)
    }

    override suspend fun saveIdempotencyRecord(tenantId: String, idempotencyKey: String, forecastId: String): DomainResult<Unit> {
        return dataSource.saveIdempotencyRecord(tenantId, idempotencyKey, forecastId)
    }
}

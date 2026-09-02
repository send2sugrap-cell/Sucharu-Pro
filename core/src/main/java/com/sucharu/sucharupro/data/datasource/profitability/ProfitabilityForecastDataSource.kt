package com.sucharu.sucharupro.data.datasource.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Data Source Interface for Profitability Forecasting & Scenario Modelling.
 * Module 16 Step 08.
 */
interface ProfitabilityForecastDataSource {

    suspend fun saveSnapshot(snapshot: ProfitabilityForecastSnapshot): DomainResult<ProfitabilityForecastSnapshot>

    suspend fun getSnapshotById(tenantId: String, forecastId: String): DomainResult<ProfitabilityForecastSnapshot?>

    suspend fun listSnapshots(tenantId: String, filter: ProfitabilityForecastFilter): DomainResult<List<ProfitabilityForecastSnapshot>>

    suspend fun saveScenario(scenario: ProfitabilityScenario): DomainResult<ProfitabilityScenario>

    suspend fun listScenarios(tenantId: String, projectId: String, scope: ProfitabilityForecastScope?): DomainResult<List<ProfitabilityScenario>>

    suspend fun getScenarioById(tenantId: String, scenarioId: String): DomainResult<ProfitabilityScenario?>

    suspend fun saveReconciliationEvent(event: ProfitabilityForecastReconciliationEvent): DomainResult<ProfitabilityForecastReconciliationEvent>

    suspend fun listReconciliationEvents(tenantId: String, forecastId: String): DomainResult<List<ProfitabilityForecastReconciliationEvent>>

    suspend fun saveActualComparison(comparison: ForecastActualComparison): DomainResult<ForecastActualComparison>

    suspend fun getActualComparison(tenantId: String, forecastId: String): DomainResult<ForecastActualComparison?>

    suspend fun recordAuditEvent(event: ProfitabilityForecastAuditEvent): DomainResult<Unit>

    suspend fun listAuditEvents(tenantId: String, forecastId: String): DomainResult<List<ProfitabilityForecastAuditEvent>>

    suspend fun checkIdempotency(tenantId: String, idempotencyKey: String): DomainResult<String?>

    suspend fun saveIdempotencyRecord(tenantId: String, idempotencyKey: String, forecastId: String): DomainResult<Unit>
}

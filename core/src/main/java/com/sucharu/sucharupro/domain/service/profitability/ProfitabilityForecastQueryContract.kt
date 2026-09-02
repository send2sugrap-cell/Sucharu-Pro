package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*

/**
 * Stable, Verified Query & Orchestration Contract for Forward-Looking Profitability.
 * Consumed by REST API Controllers, Backend UseCases, and AI-Agent Handoff.
 * Module 16 Step 08.
 */
interface ProfitabilityForecastQueryContract {

    suspend fun generateForecast(
        tenantId: String,
        projectId: String,
        targetScope: ProfitabilityForecastScope,
        targetEntityId: String,
        targetEntityLabel: String,
        historicalPeriodStart: String,
        historicalPeriodEnd: String,
        forecastPeriodStart: String,
        forecastPeriodEnd: String,
        horizon: ForecastHorizon,
        forecastMethod: ProfitabilityForecastMethod,
        scenarioType: ProfitabilityScenarioType,
        scenarioId: String?,
        idempotencyKey: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<ProfitabilityForecastSnapshot>

    suspend fun getForecastById(
        tenantId: String,
        forecastId: String
    ): DomainResult<ProfitabilityForecastSnapshot?>

    suspend fun listForecasts(
        tenantId: String,
        filter: ProfitabilityForecastFilter
    ): DomainResult<List<ProfitabilityForecastSnapshot>>

    suspend fun getForecastComponents(
        tenantId: String,
        forecastId: String
    ): DomainResult<List<ProfitabilityForecastComponent>>

    suspend fun getForecastAssumptions(
        tenantId: String,
        forecastId: String
    ): DomainResult<List<ProfitabilityScenarioAssumption>>

    suspend fun getForecastInsights(
        tenantId: String,
        forecastId: String
    ): DomainResult<List<ForecastManagementInsight>>

    suspend fun getForecastRisk(
        tenantId: String,
        forecastId: String
    ): DomainResult<ForecastRiskLevel?>

    suspend fun getForecastConfidence(
        tenantId: String,
        forecastId: String
    ): DomainResult<ConfidenceAndRiskEvaluation?>

    suspend fun getForecastProvenance(
        tenantId: String,
        forecastId: String
    ): DomainResult<List<ProfitabilityForecastProvenance>>

    suspend fun reconcileForecast(
        tenantId: String,
        projectId: String,
        forecastId: String
    ): DomainResult<ProfitabilityForecastReconciliationEvent>

    suspend fun compareWithActual(
        tenantId: String,
        projectId: String,
        forecastId: String,
        actualRevenue: java.math.BigDecimal,
        actualCost: java.math.BigDecimal,
        actualUnits: Long,
        actualPeriodId: String
    ): DomainResult<ForecastActualComparison>

    suspend fun createScenario(
        tenantId: String,
        projectId: String,
        scenario: ProfitabilityScenario
    ): DomainResult<ProfitabilityScenario>

    suspend fun listScenarios(
        tenantId: String,
        projectId: String,
        scope: ProfitabilityForecastScope?
    ): DomainResult<List<ProfitabilityScenario>>

    suspend fun getScenarioById(
        tenantId: String,
        scenarioId: String
    ): DomainResult<ProfitabilityScenario?>

    suspend fun compareScenarios(
        tenantId: String,
        projectId: String,
        forecastId: String,
        scenarioIds: List<String>?
    ): DomainResult<ProfitabilityScenarioComparison>

    suspend fun getForecastTrends(
        tenantId: String,
        scope: ProfitabilityForecastScope,
        targetEntityId: String
    ): DomainResult<List<ProfitabilityForecastSnapshot>>

    suspend fun getForecastRankings(
        tenantId: String,
        scope: ProfitabilityForecastScope,
        horizon: ForecastHorizon
    ): DomainResult<List<ProfitabilityForecastSnapshot>>

    suspend fun listAuditEvents(
        tenantId: String,
        forecastId: String
    ): DomainResult<List<ProfitabilityForecastAuditEvent>>

    suspend fun exportHandoffContract(
        tenantId: String,
        projectId: String,
        forecastId: String
    ): DomainResult<Module16Step08ProfitabilityForecastHandoffContract>
}

interface ProfitabilityForecastService : ProfitabilityForecastQueryContract

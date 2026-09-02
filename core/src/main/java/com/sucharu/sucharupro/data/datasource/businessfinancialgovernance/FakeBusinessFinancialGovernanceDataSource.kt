package com.sucharu.sucharupro.data.datasource.businessfinancialgovernance

import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.*
import com.sucharu.sucharupro.domain.repository.businessfinancialgovernance.BusinessFinancialBudgetFilter
import com.sucharu.sucharupro.domain.repository.businessfinancialgovernance.GovernanceAlertFilter
import com.sucharu.sucharupro.domain.repository.businessfinancialgovernance.GovernanceAuditFilter
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe In-Memory fake data source for Business Financial Governance unit and integration testing.
 */
class FakeBusinessFinancialGovernanceDataSource : BusinessFinancialGovernanceDataSource {

    private val budgets = ConcurrentHashMap<String, BusinessFinancialBudget>()
    private val revisions = ConcurrentHashMap<String, BusinessFinancialBudgetRevision>()
    private val thresholds = ConcurrentHashMap<String, BusinessFinancialBudgetThreshold>()
    private val forecasts = ConcurrentHashMap<String, BusinessFinancialForecast>()
    private val scenarios = ConcurrentHashMap<String, BusinessFinancialForecastScenario>()
    private val alerts = ConcurrentHashMap<String, BusinessFinancialGovernanceAlert>()
    private val auditEvents = ConcurrentHashMap<String, BusinessFinancialGovernanceAuditEvent>()

    // --- Budgets ---

    override suspend fun saveBudget(budget: BusinessFinancialBudget): BusinessFinancialBudget {
        budgets[budget.id] = budget
        return budget
    }

    override suspend fun updateBudget(budget: BusinessFinancialBudget): BusinessFinancialBudget {
        budgets[budget.id] = budget
        return budget
    }

    override suspend fun findBudgetById(tenantId: String, projectId: String, budgetId: String): BusinessFinancialBudget? {
        return budgets[budgetId]?.takeIf { it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun findBudgetByDimension(
        tenantId: String,
        projectId: String,
        periodId: String,
        dimensionType: BusinessFinancialBudgetDimensionType,
        dimensionId: String
    ): BusinessFinancialBudget? {
        return budgets.values.find {
            it.tenantId == tenantId &&
            it.projectId == projectId &&
            it.periodId == periodId &&
            it.dimensionType == dimensionType &&
            it.dimensionId == dimensionId
        }
    }

    override suspend fun listBudgets(tenantId: String, projectId: String, filter: BusinessFinancialBudgetFilter): List<BusinessFinancialBudget> {
        return budgets.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { filter.periodId == null || it.periodId == filter.periodId }
            .filter { filter.dimensionType == null || it.dimensionType == filter.dimensionType }
            .filter { filter.dimensionId == null || it.dimensionId == filter.dimensionId }
            .filter { filter.status == null || it.status == filter.status }
            .filter { filter.currency == null || it.currency == filter.currency }
            .sortedByDescending { it.createdAt }
            .drop(filter.offset)
            .take(filter.limit)
    }

    override suspend fun deleteDraftBudget(tenantId: String, projectId: String, budgetId: String): Boolean {
        val existing = budgets[budgetId]
        if (existing != null && existing.tenantId == tenantId && existing.projectId == projectId && existing.status == BusinessFinancialBudgetStatus.DRAFT) {
            budgets.remove(budgetId)
            return true
        }
        return false
    }

    // --- Revisions ---

    override suspend fun saveBudgetRevision(revision: BusinessFinancialBudgetRevision): BusinessFinancialBudgetRevision {
        revisions[revision.id] = revision
        return revision
    }

    override suspend fun listBudgetRevisions(tenantId: String, projectId: String, budgetId: String): List<BusinessFinancialBudgetRevision> {
        return revisions.values
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.budgetId == budgetId }
            .sortedByDescending { it.revisedAt }
    }

    // --- Thresholds ---

    override suspend fun saveThreshold(threshold: BusinessFinancialBudgetThreshold): BusinessFinancialBudgetThreshold {
        thresholds[threshold.id] = threshold
        return threshold
    }

    override suspend fun findThreshold(
        tenantId: String,
        projectId: String,
        dimensionType: BusinessFinancialBudgetDimensionType,
        dimensionId: String
    ): BusinessFinancialBudgetThreshold? {
        return thresholds.values.find {
            it.tenantId == tenantId &&
            it.projectId == projectId &&
            it.dimensionType == dimensionType &&
            it.dimensionId == dimensionId
        } ?: thresholds.values.find {
            it.tenantId == tenantId &&
            it.projectId == projectId &&
            it.dimensionType == BusinessFinancialBudgetDimensionType.OVERALL_BUSINESS &&
            it.dimensionId == "ALL"
        }
    }

    override suspend fun listThresholds(tenantId: String, projectId: String): List<BusinessFinancialBudgetThreshold> {
        return thresholds.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .sortedBy { it.thresholdName }
    }

    // --- Forecasts ---

    override suspend fun saveForecast(forecast: BusinessFinancialForecast): BusinessFinancialForecast {
        forecasts[forecast.id] = forecast
        return forecast
    }

    override suspend fun findForecastById(tenantId: String, projectId: String, forecastId: String): BusinessFinancialForecast? {
        return forecasts[forecastId]?.takeIf { it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun listForecasts(tenantId: String, projectId: String, periodId: String?): List<BusinessFinancialForecast> {
        return forecasts.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { periodId == null || it.periodId == periodId }
            .sortedByDescending { it.generatedAt }
    }

    override suspend fun saveForecastScenario(scenario: BusinessFinancialForecastScenario): BusinessFinancialForecastScenario {
        scenarios[scenario.id] = scenario
        return scenario
    }

    override suspend fun listForecastScenarios(tenantId: String, projectId: String, forecastId: String): List<BusinessFinancialForecastScenario> {
        return scenarios.values
            .filter { it.tenantId == tenantId && it.projectId == projectId && it.forecastId == forecastId }
            .sortedBy { it.scenarioType.ordinal }
    }

    // --- Alerts ---

    override suspend fun saveAlert(alert: BusinessFinancialGovernanceAlert): BusinessFinancialGovernanceAlert {
        alerts[alert.id] = alert
        return alert
    }

    override suspend fun updateAlert(alert: BusinessFinancialGovernanceAlert): BusinessFinancialGovernanceAlert {
        alerts[alert.id] = alert
        return alert
    }

    override suspend fun findAlertById(tenantId: String, projectId: String, alertId: String): BusinessFinancialGovernanceAlert? {
        return alerts[alertId]?.takeIf { it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun findOpenAlert(
        tenantId: String,
        projectId: String,
        alertType: GovernanceAlertType,
        dimensionType: BusinessFinancialBudgetDimensionType,
        dimensionId: String,
        periodId: String?
    ): BusinessFinancialGovernanceAlert? {
        return alerts.values.find {
            it.tenantId == tenantId &&
            it.projectId == projectId &&
            it.alertType == alertType &&
            it.sourceDimensionType == dimensionType &&
            it.sourceDimensionId == dimensionId &&
            it.status == GovernanceAlertStatus.OPEN &&
            (periodId == null || it.periodId == periodId)
        }
    }

    override suspend fun listAlerts(tenantId: String, projectId: String, filter: GovernanceAlertFilter): List<BusinessFinancialGovernanceAlert> {
        return alerts.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { filter.status == null || it.status == filter.status }
            .filter { filter.severity == null || it.severity == filter.severity }
            .filter { filter.alertType == null || it.alertType == filter.alertType }
            .filter { filter.periodId == null || it.periodId == filter.periodId }
            .filter { filter.dimensionType == null || it.sourceDimensionType == filter.dimensionType }
            .filter { filter.dimensionId == null || it.sourceDimensionId == filter.dimensionId }
            .sortedByDescending { it.createdAt }
            .drop(filter.offset)
            .take(filter.limit)
    }

    // --- Audit Trail ---

    override suspend fun saveAuditEvent(event: BusinessFinancialGovernanceAuditEvent): BusinessFinancialGovernanceAuditEvent {
        auditEvents[event.id] = event
        return event
    }

    override suspend fun listAuditEvents(tenantId: String, projectId: String, filter: GovernanceAuditFilter): List<BusinessFinancialGovernanceAuditEvent> {
        return auditEvents.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { filter.targetId == null || it.targetId == filter.targetId }
            .filter { filter.eventType == null || it.eventType == filter.eventType }
            .filter { filter.actorId == null || it.actorId == filter.actorId }
            .filter { filter.fromTimestamp == null || it.timestamp >= filter.fromTimestamp }
            .filter { filter.toTimestamp == null || it.timestamp <= filter.toTimestamp }
            .sortedByDescending { it.timestamp }
            .drop(filter.offset)
            .take(filter.limit)
    }
}

package com.sucharu.sucharupro.domain.repository.businessfinancialgovernance

import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.*
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Filter criteria for querying financial budgets.
 */
data class BusinessFinancialBudgetFilter(
    val periodId: String? = null,
    val dimensionType: BusinessFinancialBudgetDimensionType? = null,
    val dimensionId: String? = null,
    val status: BusinessFinancialBudgetStatus? = null,
    val currency: String? = null,
    val limit: Int = 100,
    val offset: Int = 0
)

/**
 * Filter criteria for querying governance alerts.
 */
data class GovernanceAlertFilter(
    val status: GovernanceAlertStatus? = null,
    val severity: GovernanceAlertSeverity? = null,
    val alertType: GovernanceAlertType? = null,
    val periodId: String? = null,
    val dimensionType: BusinessFinancialBudgetDimensionType? = null,
    val dimensionId: String? = null,
    val limit: Int = 100,
    val offset: Int = 0
)

/**
 * Filter criteria for querying governance audit events.
 */
data class GovernanceAuditFilter(
    val targetId: String? = null,
    val eventType: String? = null,
    val actorId: String? = null,
    val fromTimestamp: Long? = null,
    val toTimestamp: Long? = null,
    val limit: Int = 100,
    val offset: Int = 0
)

/**
 * Repository interface for Business Financial Governance & Budget Control persistence.
 */
interface BusinessFinancialGovernanceRepository {

    // --- Budgets ---
    suspend fun createBudget(budget: BusinessFinancialBudget): DomainResult<BusinessFinancialBudget>
    suspend fun updateBudget(budget: BusinessFinancialBudget): DomainResult<BusinessFinancialBudget>
    suspend fun getBudgetById(tenantId: String, projectId: String, budgetId: String): DomainResult<BusinessFinancialBudget?>
    suspend fun findBudgetByDimension(
        tenantId: String,
        projectId: String,
        periodId: String,
        dimensionType: BusinessFinancialBudgetDimensionType,
        dimensionId: String
    ): DomainResult<BusinessFinancialBudget?>
    suspend fun listBudgets(tenantId: String, projectId: String, filter: BusinessFinancialBudgetFilter): DomainResult<List<BusinessFinancialBudget>>
    suspend fun deleteDraftBudget(tenantId: String, projectId: String, budgetId: String): DomainResult<Boolean>

    // --- Budget Revisions ---
    suspend fun recordBudgetRevision(revision: BusinessFinancialBudgetRevision): DomainResult<BusinessFinancialBudgetRevision>
    suspend fun listBudgetRevisions(tenantId: String, projectId: String, budgetId: String): DomainResult<List<BusinessFinancialBudgetRevision>>

    // --- Thresholds ---
    suspend fun upsertThreshold(threshold: BusinessFinancialBudgetThreshold): DomainResult<BusinessFinancialBudgetThreshold>
    suspend fun getThreshold(
        tenantId: String,
        projectId: String,
        dimensionType: BusinessFinancialBudgetDimensionType,
        dimensionId: String
    ): DomainResult<BusinessFinancialBudgetThreshold?>
    suspend fun listThresholds(tenantId: String, projectId: String): DomainResult<List<BusinessFinancialBudgetThreshold>>

    // --- Forecasts ---
    suspend fun saveForecast(forecast: BusinessFinancialForecast): DomainResult<BusinessFinancialForecast>
    suspend fun getForecastById(tenantId: String, projectId: String, forecastId: String): DomainResult<BusinessFinancialForecast?>
    suspend fun listForecasts(tenantId: String, projectId: String, periodId: String? = null): DomainResult<List<BusinessFinancialForecast>>
    suspend fun saveForecastScenario(scenario: BusinessFinancialForecastScenario): DomainResult<BusinessFinancialForecastScenario>
    suspend fun listForecastScenarios(tenantId: String, projectId: String, forecastId: String): DomainResult<List<BusinessFinancialForecastScenario>>

    // --- Alerts ---
    suspend fun createAlert(alert: BusinessFinancialGovernanceAlert): DomainResult<BusinessFinancialGovernanceAlert>
    suspend fun updateAlert(alert: BusinessFinancialGovernanceAlert): DomainResult<BusinessFinancialGovernanceAlert>
    suspend fun getAlertById(tenantId: String, projectId: String, alertId: String): DomainResult<BusinessFinancialGovernanceAlert?>
    suspend fun findOpenAlert(
        tenantId: String,
        projectId: String,
        alertType: GovernanceAlertType,
        dimensionType: BusinessFinancialBudgetDimensionType,
        dimensionId: String,
        periodId: String?
    ): DomainResult<BusinessFinancialGovernanceAlert?>
    suspend fun listAlerts(tenantId: String, projectId: String, filter: GovernanceAlertFilter): DomainResult<List<BusinessFinancialGovernanceAlert>>

    // --- Audit Trail ---
    suspend fun recordAuditEvent(event: BusinessFinancialGovernanceAuditEvent): DomainResult<BusinessFinancialGovernanceAuditEvent>
    suspend fun listAuditEvents(tenantId: String, projectId: String, filter: GovernanceAuditFilter): DomainResult<List<BusinessFinancialGovernanceAuditEvent>>
}

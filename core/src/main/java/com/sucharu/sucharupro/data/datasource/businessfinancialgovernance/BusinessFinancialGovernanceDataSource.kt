package com.sucharu.sucharupro.data.datasource.businessfinancialgovernance

import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.*
import com.sucharu.sucharupro.domain.repository.businessfinancialgovernance.BusinessFinancialBudgetFilter
import com.sucharu.sucharupro.domain.repository.businessfinancialgovernance.GovernanceAlertFilter
import com.sucharu.sucharupro.domain.repository.businessfinancialgovernance.GovernanceAuditFilter

/**
 * Low-level data source interface for Business Financial Governance & Budget Control.
 */
interface BusinessFinancialGovernanceDataSource {

    // --- Budgets ---
    suspend fun saveBudget(budget: BusinessFinancialBudget): BusinessFinancialBudget
    suspend fun updateBudget(budget: BusinessFinancialBudget): BusinessFinancialBudget
    suspend fun findBudgetById(tenantId: String, projectId: String, budgetId: String): BusinessFinancialBudget?
    suspend fun findBudgetByDimension(
        tenantId: String,
        projectId: String,
        periodId: String,
        dimensionType: BusinessFinancialBudgetDimensionType,
        dimensionId: String
    ): BusinessFinancialBudget?
    suspend fun listBudgets(tenantId: String, projectId: String, filter: BusinessFinancialBudgetFilter): List<BusinessFinancialBudget>
    suspend fun deleteDraftBudget(tenantId: String, projectId: String, budgetId: String): Boolean

    // --- Budget Revisions ---
    suspend fun saveBudgetRevision(revision: BusinessFinancialBudgetRevision): BusinessFinancialBudgetRevision
    suspend fun listBudgetRevisions(tenantId: String, projectId: String, budgetId: String): List<BusinessFinancialBudgetRevision>

    // --- Thresholds ---
    suspend fun saveThreshold(threshold: BusinessFinancialBudgetThreshold): BusinessFinancialBudgetThreshold
    suspend fun findThreshold(
        tenantId: String,
        projectId: String,
        dimensionType: BusinessFinancialBudgetDimensionType,
        dimensionId: String
    ): BusinessFinancialBudgetThreshold?
    suspend fun listThresholds(tenantId: String, projectId: String): List<BusinessFinancialBudgetThreshold>

    // --- Forecasts ---
    suspend fun saveForecast(forecast: BusinessFinancialForecast): BusinessFinancialForecast
    suspend fun findForecastById(tenantId: String, projectId: String, forecastId: String): BusinessFinancialForecast?
    suspend fun listForecasts(tenantId: String, projectId: String, periodId: String? = null): List<BusinessFinancialForecast>
    suspend fun saveForecastScenario(scenario: BusinessFinancialForecastScenario): BusinessFinancialForecastScenario
    suspend fun listForecastScenarios(tenantId: String, projectId: String, forecastId: String): List<BusinessFinancialForecastScenario>

    // --- Alerts ---
    suspend fun saveAlert(alert: BusinessFinancialGovernanceAlert): BusinessFinancialGovernanceAlert
    suspend fun updateAlert(alert: BusinessFinancialGovernanceAlert): BusinessFinancialGovernanceAlert
    suspend fun findAlertById(tenantId: String, projectId: String, alertId: String): BusinessFinancialGovernanceAlert?
    suspend fun findOpenAlert(
        tenantId: String,
        projectId: String,
        alertType: GovernanceAlertType,
        dimensionType: BusinessFinancialBudgetDimensionType,
        dimensionId: String,
        periodId: String?
    ): BusinessFinancialGovernanceAlert?
    suspend fun listAlerts(tenantId: String, projectId: String, filter: GovernanceAlertFilter): List<BusinessFinancialGovernanceAlert>

    // --- Audit Trail ---
    suspend fun saveAuditEvent(event: BusinessFinancialGovernanceAuditEvent): BusinessFinancialGovernanceAuditEvent
    suspend fun listAuditEvents(tenantId: String, projectId: String, filter: GovernanceAuditFilter): List<BusinessFinancialGovernanceAuditEvent>
}

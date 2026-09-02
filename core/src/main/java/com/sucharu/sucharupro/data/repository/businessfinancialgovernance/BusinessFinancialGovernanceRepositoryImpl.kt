package com.sucharu.sucharupro.data.repository.businessfinancialgovernance

import com.sucharu.sucharupro.data.datasource.businessfinancialgovernance.BusinessFinancialGovernanceDataSource
import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.businessfinancialgovernance.*

/**
 * Canonical Repository implementation for Business Financial Governance & Budget Control.
 */
class BusinessFinancialGovernanceRepositoryImpl(
    private val dataSource: BusinessFinancialGovernanceDataSource
) : BusinessFinancialGovernanceRepository {

    // --- Budgets ---

    override suspend fun createBudget(budget: BusinessFinancialBudget): DomainResult<BusinessFinancialBudget> {
        return try {
            val saved = dataSource.saveBudget(budget)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to save financial budget.")
        }
    }

    override suspend fun updateBudget(budget: BusinessFinancialBudget): DomainResult<BusinessFinancialBudget> {
        return try {
            val updated = dataSource.updateBudget(budget)
            DomainResult.Success(updated)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to update financial budget.")
        }
    }

    override suspend fun getBudgetById(tenantId: String, projectId: String, budgetId: String): DomainResult<BusinessFinancialBudget?> {
        return try {
            val budget = dataSource.findBudgetById(tenantId, projectId, budgetId)
            DomainResult.Success(budget)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to retrieve budget.")
        }
    }

    override suspend fun findBudgetByDimension(
        tenantId: String,
        projectId: String,
        periodId: String,
        dimensionType: BusinessFinancialBudgetDimensionType,
        dimensionId: String
    ): DomainResult<BusinessFinancialBudget?> {
        return try {
            val budget = dataSource.findBudgetByDimension(tenantId, projectId, periodId, dimensionType, dimensionId)
            DomainResult.Success(budget)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to find budget by dimension.")
        }
    }

    override suspend fun listBudgets(tenantId: String, projectId: String, filter: BusinessFinancialBudgetFilter): DomainResult<List<BusinessFinancialBudget>> {
        return try {
            val list = dataSource.listBudgets(tenantId, projectId, filter)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to list budgets.")
        }
    }

    override suspend fun deleteDraftBudget(tenantId: String, projectId: String, budgetId: String): DomainResult<Boolean> {
        return try {
            val deleted = dataSource.deleteDraftBudget(tenantId, projectId, budgetId)
            DomainResult.Success(deleted)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to delete draft budget.")
        }
    }

    // --- Budget Revisions ---

    override suspend fun recordBudgetRevision(revision: BusinessFinancialBudgetRevision): DomainResult<BusinessFinancialBudgetRevision> {
        return try {
            val saved = dataSource.saveBudgetRevision(revision)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to save budget revision.")
        }
    }

    override suspend fun listBudgetRevisions(tenantId: String, projectId: String, budgetId: String): DomainResult<List<BusinessFinancialBudgetRevision>> {
        return try {
            val list = dataSource.listBudgetRevisions(tenantId, projectId, budgetId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to list budget revisions.")
        }
    }

    // --- Thresholds ---

    override suspend fun upsertThreshold(threshold: BusinessFinancialBudgetThreshold): DomainResult<BusinessFinancialBudgetThreshold> {
        return try {
            val saved = dataSource.saveThreshold(threshold)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to configure threshold.")
        }
    }

    override suspend fun getThreshold(
        tenantId: String,
        projectId: String,
        dimensionType: BusinessFinancialBudgetDimensionType,
        dimensionId: String
    ): DomainResult<BusinessFinancialBudgetThreshold?> {
        return try {
            val threshold = dataSource.findThreshold(tenantId, projectId, dimensionType, dimensionId)
            DomainResult.Success(threshold)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to retrieve threshold.")
        }
    }

    override suspend fun listThresholds(tenantId: String, projectId: String): DomainResult<List<BusinessFinancialBudgetThreshold>> {
        return try {
            val list = dataSource.listThresholds(tenantId, projectId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to list thresholds.")
        }
    }

    // --- Forecasts ---

    override suspend fun saveForecast(forecast: BusinessFinancialForecast): DomainResult<BusinessFinancialForecast> {
        return try {
            val saved = dataSource.saveForecast(forecast)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to save forecast.")
        }
    }

    override suspend fun getForecastById(tenantId: String, projectId: String, forecastId: String): DomainResult<BusinessFinancialForecast?> {
        return try {
            val forecast = dataSource.findForecastById(tenantId, projectId, forecastId)
            DomainResult.Success(forecast)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to retrieve forecast.")
        }
    }

    override suspend fun listForecasts(tenantId: String, projectId: String, periodId: String?): DomainResult<List<BusinessFinancialForecast>> {
        return try {
            val list = dataSource.listForecasts(tenantId, projectId, periodId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to list forecasts.")
        }
    }

    override suspend fun saveForecastScenario(scenario: BusinessFinancialForecastScenario): DomainResult<BusinessFinancialForecastScenario> {
        return try {
            val saved = dataSource.saveForecastScenario(scenario)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to save forecast scenario.")
        }
    }

    override suspend fun listForecastScenarios(tenantId: String, projectId: String, forecastId: String): DomainResult<List<BusinessFinancialForecastScenario>> {
        return try {
            val list = dataSource.listForecastScenarios(tenantId, projectId, forecastId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to list forecast scenarios.")
        }
    }

    // --- Alerts ---

    override suspend fun createAlert(alert: BusinessFinancialGovernanceAlert): DomainResult<BusinessFinancialGovernanceAlert> {
        return try {
            val saved = dataSource.saveAlert(alert)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to create governance alert.")
        }
    }

    override suspend fun updateAlert(alert: BusinessFinancialGovernanceAlert): DomainResult<BusinessFinancialGovernanceAlert> {
        return try {
            val updated = dataSource.updateAlert(alert)
            DomainResult.Success(updated)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to update governance alert.")
        }
    }

    override suspend fun getAlertById(tenantId: String, projectId: String, alertId: String): DomainResult<BusinessFinancialGovernanceAlert?> {
        return try {
            val alert = dataSource.findAlertById(tenantId, projectId, alertId)
            DomainResult.Success(alert)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to retrieve alert.")
        }
    }

    override suspend fun findOpenAlert(
        tenantId: String,
        projectId: String,
        alertType: GovernanceAlertType,
        dimensionType: BusinessFinancialBudgetDimensionType,
        dimensionId: String,
        periodId: String?
    ): DomainResult<BusinessFinancialGovernanceAlert?> {
        return try {
            val alert = dataSource.findOpenAlert(tenantId, projectId, alertType, dimensionType, dimensionId, periodId)
            DomainResult.Success(alert)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to query open alerts.")
        }
    }

    override suspend fun listAlerts(tenantId: String, projectId: String, filter: GovernanceAlertFilter): DomainResult<List<BusinessFinancialGovernanceAlert>> {
        return try {
            val list = dataSource.listAlerts(tenantId, projectId, filter)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to list governance alerts.")
        }
    }

    // --- Audit Trail ---

    override suspend fun recordAuditEvent(event: BusinessFinancialGovernanceAuditEvent): DomainResult<BusinessFinancialGovernanceAuditEvent> {
        return try {
            val saved = dataSource.saveAuditEvent(event)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to record governance audit event.")
        }
    }

    override suspend fun listAuditEvents(tenantId: String, projectId: String, filter: GovernanceAuditFilter): DomainResult<List<BusinessFinancialGovernanceAuditEvent>> {
        return try {
            val list = dataSource.listAuditEvents(tenantId, projectId, filter)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to list governance audit events.")
        }
    }
}

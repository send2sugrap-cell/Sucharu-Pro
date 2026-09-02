package com.sucharu.sucharupro.domain.service.businessfinancialgovernance

import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.businessfinancialgovernance.BusinessFinancialBudgetFilter
import com.sucharu.sucharupro.domain.repository.businessfinancialgovernance.GovernanceAlertFilter
import com.sucharu.sucharupro.domain.repository.businessfinancialgovernance.GovernanceAuditFilter
import java.math.BigDecimal

/**
 * Service interface for Business Financial Governance, Budget Control, Forecast & Decision Intelligence.
 */
interface BusinessFinancialGovernanceService {

    // --- Budget Lifecycle ---
    suspend fun createBudget(budget: BusinessFinancialBudget, actorId: String, actorRole: String): DomainResult<BusinessFinancialBudget>
    suspend fun submitBudget(tenantId: String, projectId: String, budgetId: String, actorId: String, actorRole: String): DomainResult<BusinessFinancialBudget>
    suspend fun reviewBudget(tenantId: String, projectId: String, budgetId: String, actorId: String, actorRole: String): DomainResult<BusinessFinancialBudget>
    suspend fun approveBudget(tenantId: String, projectId: String, budgetId: String, actorId: String, actorRole: String): DomainResult<BusinessFinancialBudget>
    suspend fun activateBudget(tenantId: String, projectId: String, budgetId: String, actorId: String, actorRole: String): DomainResult<BusinessFinancialBudget>
    suspend fun rejectBudget(tenantId: String, projectId: String, budgetId: String, actorId: String, actorRole: String, rejectionReason: String): DomainResult<BusinessFinancialBudget>
    suspend fun reviseBudget(tenantId: String, projectId: String, budgetId: String, newAmount: BigDecimal, revisionReason: String, actorId: String, actorRole: String): DomainResult<BusinessFinancialBudget>
    suspend fun closeBudget(tenantId: String, projectId: String, budgetId: String, actorId: String, actorRole: String): DomainResult<BusinessFinancialBudget>
    suspend fun getBudgetById(tenantId: String, projectId: String, budgetId: String): DomainResult<BusinessFinancialBudget?>
    suspend fun listBudgets(tenantId: String, projectId: String, filter: BusinessFinancialBudgetFilter): DomainResult<List<BusinessFinancialBudget>>
    suspend fun listBudgetRevisions(tenantId: String, projectId: String, budgetId: String): DomainResult<List<BusinessFinancialBudgetRevision>>

    // --- Variance & Projections (Consumes Steps 01–08 Canonical Sources) ---
    suspend fun calculateBudgetVsActual(tenantId: String, projectId: String, budgetId: String, currency: String = "BDT"): DomainResult<BudgetVsActualComparison>
    suspend fun calculateAllBudgetVsActual(tenantId: String, projectId: String, periodId: String? = null, currency: String = "BDT"): DomainResult<List<BudgetVsActualComparison>>
    suspend fun getExecutiveGovernanceOverview(tenantId: String, projectId: String, periodId: String? = null, currency: String = "BDT"): DomainResult<ExecutiveGovernanceOverview>

    // --- Forecast Foundation ---
    suspend fun generateDeterministicForecast(
        tenantId: String,
        projectId: String,
        periodId: String,
        dimensionType: BusinessFinancialBudgetDimensionType,
        dimensionId: String,
        currency: String = "BDT",
        actorId: String = "system"
    ): DomainResult<Pair<BusinessFinancialForecast, List<BusinessFinancialForecastScenario>>>
    suspend fun listForecasts(tenantId: String, projectId: String, periodId: String? = null): DomainResult<List<BusinessFinancialForecast>>
    suspend fun getForecastScenarios(tenantId: String, projectId: String, forecastId: String): DomainResult<List<BusinessFinancialForecastScenario>>

    // --- Spending Thresholds & Alert Evaluation ---
    suspend fun upsertThreshold(threshold: BusinessFinancialBudgetThreshold, actorId: String, actorRole: String): DomainResult<BusinessFinancialBudgetThreshold>
    suspend fun listThresholds(tenantId: String, projectId: String): DomainResult<List<BusinessFinancialBudgetThreshold>>
    suspend fun evaluateGovernanceThresholdsAndGenerateAlerts(tenantId: String, projectId: String, periodId: String? = null, currency: String = "BDT", actorId: String = "system"): DomainResult<List<BusinessFinancialGovernanceAlert>>

    // --- Alert Lifecycle ---
    suspend fun acknowledgeAlert(tenantId: String, projectId: String, alertId: String, notes: String?, actorId: String, actorRole: String): DomainResult<BusinessFinancialGovernanceAlert>
    suspend fun resolveAlert(tenantId: String, projectId: String, alertId: String, notes: String?, actorId: String, actorRole: String): DomainResult<BusinessFinancialGovernanceAlert>
    suspend fun dismissAlert(tenantId: String, projectId: String, alertId: String, dismissalReason: String, actorId: String, actorRole: String): DomainResult<BusinessFinancialGovernanceAlert>
    suspend fun listAlerts(tenantId: String, projectId: String, filter: GovernanceAlertFilter): DomainResult<List<BusinessFinancialGovernanceAlert>>

    // --- Audit Trail ---
    suspend fun listAuditEvents(tenantId: String, projectId: String, filter: GovernanceAuditFilter): DomainResult<List<BusinessFinancialGovernanceAuditEvent>>
}

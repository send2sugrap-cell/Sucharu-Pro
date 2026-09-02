package com.sucharu.sucharupro.domain.service.businessfinancialgovernance

import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepository
import com.sucharu.sucharupro.data.repository.businessfinancialadjustment.BusinessFinancialAdjustmentRepository
import com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepository
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessCostAccrualStatus
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessCostCommitmentStatus
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessFinancialPeriod
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorpayable.VendorPayable
import com.sucharu.sucharupro.domain.model.vendorpayable.VendorPayableStatus
import com.sucharu.sucharupro.domain.repository.businesscost.BusinessCostManagementRepository
import com.sucharu.sucharupro.domain.repository.businessexpense.BusinessExpenseRepository
import com.sucharu.sucharupro.domain.repository.businessfinancialgovernance.*
import com.sucharu.sucharupro.domain.repository.businessledger.BusinessLedgerRepository
import com.sucharu.sucharupro.domain.repository.vendorpayable.VendorPayableRepository
import com.sucharu.sucharupro.domain.validation.businessfinancialgovernance.BusinessFinancialGovernanceValidator
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Production implementation of BusinessFinancialGovernanceService.
 * Strictly operates as a CONTROL / PLANNING / GOVERNANCE layer projecting actual financial values
 * directly from canonical Steps 01–08 repositories.
 */
class BusinessFinancialGovernanceServiceImpl(
    private val governanceRepository: BusinessFinancialGovernanceRepository,
    private val expenseRepository: BusinessExpenseRepository,
    private val payableRepository: VendorPayableRepository,
    private val ledgerRepository: BusinessLedgerRepository,
    private val costManagementRepository: BusinessCostManagementRepository,
    private val costControlRepository: BusinessCostControlRepository,
    private val reconciliationRepository: BusinessFinancialReconciliationRepository,
    private val adjustmentRepository: BusinessFinancialAdjustmentRepository,
    private val defaultTenantId: String = "TENANT-001"
) : BusinessFinancialGovernanceService {

    // =========================================================================
    // 1. BUDGET LIFECYCLE MANAGEMENT
    // =========================================================================

    override suspend fun createBudget(
        budget: BusinessFinancialBudget,
        actorId: String,
        actorRole: String
    ): DomainResult<BusinessFinancialBudget> {
        val validation = BusinessFinancialGovernanceValidator.validateBudgetCreation(budget)
        if (validation is DomainResult.Error) {
            return DomainResult.Error(message = validation.message)
        }

        // Verify period status
        val period = costControlRepository.findFinancialPeriodById(budget.periodId, budget.tenantId, budget.projectId)
        if (period != null && period.status.isClosed) {
            return DomainResult.Error(message = "Cannot create budget for a CLOSED financial period (${period.periodCode}).")
        }

        // Check for existing budget with same dimension and version
        val existing = governanceRepository.findBudgetByDimension(
            tenantId = budget.tenantId,
            projectId = budget.projectId,
            periodId = budget.periodId,
            dimensionType = budget.dimensionType,
            dimensionId = budget.dimensionId
        )
        if (existing is DomainResult.Success && existing.data != null && existing.data!!.status != BusinessFinancialBudgetStatus.REJECTED && existing.data!!.status != BusinessFinancialBudgetStatus.CLOSED) {
            return DomainResult.Error(message = "An active or pending budget already exists for period ${budget.periodId} and dimension ${budget.dimensionType}:${budget.dimensionId}.")
        }

        val budgetToSave = budget.copy(
            status = BusinessFinancialBudgetStatus.DRAFT,
            version = 1L,
            createdBy = actorId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val created = governanceRepository.createBudget(budgetToSave)
        if (created is DomainResult.Success) {
            recordAudit(
                tenantId = budget.tenantId,
                projectId = budget.projectId,
                actorId = actorId,
                actorRole = actorRole,
                eventType = "BUDGET_CREATED",
                targetId = budget.id,
                targetType = "BUDGET",
                details = "Budget ${budget.budgetName} created with amount ${budget.allocatedAmount} ${budget.currency}"
            )
        }
        return created
    }

    override suspend fun submitBudget(
        tenantId: String,
        projectId: String,
        budgetId: String,
        actorId: String,
        actorRole: String
    ): DomainResult<BusinessFinancialBudget> {
        val budgetRes = governanceRepository.getBudgetById(tenantId, projectId, budgetId)
        val budget = (budgetRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Budget not found with ID: $budgetId")

        val valRes = BusinessFinancialGovernanceValidator.validateBudgetStatusTransition(
            budget = budget,
            targetStatus = BusinessFinancialBudgetStatus.SUBMITTED,
            actorId = actorId,
            actorRole = actorRole
        )
        if (valRes is DomainResult.Error) {
            return DomainResult.Error(message = valRes.message)
        }

        val updatedBudget = budget.copy(
            status = BusinessFinancialBudgetStatus.SUBMITTED,
            updatedAt = System.currentTimeMillis()
        )

        val res = governanceRepository.updateBudget(updatedBudget)
        if (res is DomainResult.Success) {
            recordAudit(
                tenantId = tenantId,
                projectId = projectId,
                actorId = actorId,
                actorRole = actorRole,
                eventType = "BUDGET_SUBMITTED",
                targetId = budgetId,
                targetType = "BUDGET",
                details = "Budget submitted for management review"
            )
        }
        return res
    }

    override suspend fun reviewBudget(
        tenantId: String,
        projectId: String,
        budgetId: String,
        actorId: String,
        actorRole: String
    ): DomainResult<BusinessFinancialBudget> {
        val budgetRes = governanceRepository.getBudgetById(tenantId, projectId, budgetId)
        val budget = (budgetRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Budget not found with ID: $budgetId")

        val valRes = BusinessFinancialGovernanceValidator.validateBudgetStatusTransition(
            budget = budget,
            targetStatus = BusinessFinancialBudgetStatus.REVIEWED,
            actorId = actorId,
            actorRole = actorRole
        )
        if (valRes is DomainResult.Error) {
            return DomainResult.Error(message = valRes.message)
        }

        val updatedBudget = budget.copy(
            status = BusinessFinancialBudgetStatus.REVIEWED,
            reviewedBy = actorId,
            updatedAt = System.currentTimeMillis()
        )

        val res = governanceRepository.updateBudget(updatedBudget)
        if (res is DomainResult.Success) {
            recordAudit(
                tenantId = tenantId,
                projectId = projectId,
                actorId = actorId,
                actorRole = actorRole,
                eventType = "BUDGET_REVIEWED",
                targetId = budgetId,
                targetType = "BUDGET",
                details = "Budget reviewed by $actorId"
            )
        }
        return res
    }

    override suspend fun approveBudget(
        tenantId: String,
        projectId: String,
        budgetId: String,
        actorId: String,
        actorRole: String
    ): DomainResult<BusinessFinancialBudget> {
        val budgetRes = governanceRepository.getBudgetById(tenantId, projectId, budgetId)
        val budget = (budgetRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Budget not found with ID: $budgetId")

        val valRes = BusinessFinancialGovernanceValidator.validateBudgetStatusTransition(
            budget = budget,
            targetStatus = BusinessFinancialBudgetStatus.APPROVED,
            actorId = actorId,
            actorRole = actorRole
        )
        if (valRes is DomainResult.Error) {
            return DomainResult.Error(message = valRes.message)
        }

        val updatedBudget = budget.copy(
            status = BusinessFinancialBudgetStatus.APPROVED,
            approvedBy = actorId,
            approvedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val res = governanceRepository.updateBudget(updatedBudget)
        if (res is DomainResult.Success) {
            recordAudit(
                tenantId = tenantId,
                projectId = projectId,
                actorId = actorId,
                actorRole = actorRole,
                eventType = "BUDGET_APPROVED",
                targetId = budgetId,
                targetType = "BUDGET",
                details = "Budget approved by $actorId ($actorRole)"
            )
        }
        return res
    }

    override suspend fun activateBudget(
        tenantId: String,
        projectId: String,
        budgetId: String,
        actorId: String,
        actorRole: String
    ): DomainResult<BusinessFinancialBudget> {
        val budgetRes = governanceRepository.getBudgetById(tenantId, projectId, budgetId)
        val budget = (budgetRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Budget not found with ID: $budgetId")

        val valRes = BusinessFinancialGovernanceValidator.validateBudgetStatusTransition(
            budget = budget,
            targetStatus = BusinessFinancialBudgetStatus.ACTIVE,
            actorId = actorId,
            actorRole = actorRole
        )
        if (valRes is DomainResult.Error) {
            return DomainResult.Error(message = valRes.message)
        }

        val updatedBudget = budget.copy(
            status = BusinessFinancialBudgetStatus.ACTIVE,
            updatedAt = System.currentTimeMillis()
        )

        val res = governanceRepository.updateBudget(updatedBudget)
        if (res is DomainResult.Success) {
            recordAudit(
                tenantId = tenantId,
                projectId = projectId,
                actorId = actorId,
                actorRole = actorRole,
                eventType = "BUDGET_ACTIVATED",
                targetId = budgetId,
                targetType = "BUDGET",
                details = "Budget activated for real-time tracking"
            )
        }
        return res
    }

    override suspend fun rejectBudget(
        tenantId: String,
        projectId: String,
        budgetId: String,
        actorId: String,
        actorRole: String,
        rejectionReason: String
    ): DomainResult<BusinessFinancialBudget> {
        val budgetRes = governanceRepository.getBudgetById(tenantId, projectId, budgetId)
        val budget = (budgetRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Budget not found with ID: $budgetId")

        val valRes = BusinessFinancialGovernanceValidator.validateBudgetStatusTransition(
            budget = budget,
            targetStatus = BusinessFinancialBudgetStatus.REJECTED,
            actorId = actorId,
            actorRole = actorRole,
            rejectionReason = rejectionReason
        )
        if (valRes is DomainResult.Error) {
            return DomainResult.Error(message = valRes.message)
        }

        val updatedBudget = budget.copy(
            status = BusinessFinancialBudgetStatus.REJECTED,
            rejectionReason = rejectionReason,
            updatedAt = System.currentTimeMillis()
        )

        val res = governanceRepository.updateBudget(updatedBudget)
        if (res is DomainResult.Success) {
            recordAudit(
                tenantId = tenantId,
                projectId = projectId,
                actorId = actorId,
                actorRole = actorRole,
                eventType = "BUDGET_REJECTED",
                targetId = budgetId,
                targetType = "BUDGET",
                details = "Budget rejected by $actorId. Reason: $rejectionReason"
            )
        }
        return res
    }

    override suspend fun reviseBudget(
        tenantId: String,
        projectId: String,
        budgetId: String,
        newAllocatedAmount: BigDecimal,
        revisionReason: String,
        actorId: String,
        actorRole: String
    ): DomainResult<BusinessFinancialBudget> {
        val budgetRes = governanceRepository.getBudgetById(tenantId, projectId, budgetId)
        val budget = (budgetRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Budget not found with ID: $budgetId")

        val valRes = BusinessFinancialGovernanceValidator.validateBudgetRevision(
            budget = budget,
            newAmount = newAllocatedAmount,
            reason = revisionReason,
            revisedBy = actorId
        )
        if (valRes is DomainResult.Error) {
            return DomainResult.Error(message = valRes.message)
        }

        val previousAmount = budget.allocatedAmount
        val newVersion = budget.version + 1

        val revision = BusinessFinancialBudgetRevision(
            id = UUID.randomUUID().toString(),
            budgetId = budgetId,
            tenantId = tenantId,
            projectId = projectId,
            version = newVersion,
            previousAllocatedAmount = previousAmount,
            newAllocatedAmount = newAllocatedAmount,
            revisionReason = revisionReason,
            revisedBy = actorId,
            approvedBy = actorId,
            revisedAt = System.currentTimeMillis(),
            status = "APPLIED"
        )
        governanceRepository.recordBudgetRevision(revision)

        val updatedBudget = budget.copy(
            allocatedAmount = newAllocatedAmount,
            version = newVersion,
            updatedAt = System.currentTimeMillis()
        )

        val res = governanceRepository.updateBudget(updatedBudget)
        if (res is DomainResult.Success) {
            recordAudit(
                tenantId = tenantId,
                projectId = projectId,
                actorId = actorId,
                actorRole = actorRole,
                eventType = "BUDGET_REVISED",
                targetId = budgetId,
                targetType = "BUDGET",
                details = "Budget revised from $previousAmount to $newAllocatedAmount (v$newVersion). Reason: $revisionReason"
            )
        }
        return res
    }

    override suspend fun closeBudget(
        tenantId: String,
        projectId: String,
        budgetId: String,
        actorId: String,
        actorRole: String
    ): DomainResult<BusinessFinancialBudget> {
        val budgetRes = governanceRepository.getBudgetById(tenantId, projectId, budgetId)
        val budget = (budgetRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Budget not found with ID: $budgetId")

        val valRes = BusinessFinancialGovernanceValidator.validateBudgetStatusTransition(
            budget = budget,
            targetStatus = BusinessFinancialBudgetStatus.CLOSED,
            actorId = actorId,
            actorRole = actorRole
        )
        if (valRes is DomainResult.Error) {
            return DomainResult.Error(message = valRes.message)
        }

        val updatedBudget = budget.copy(
            status = BusinessFinancialBudgetStatus.CLOSED,
            updatedAt = System.currentTimeMillis()
        )

        val res = governanceRepository.updateBudget(updatedBudget)
        if (res is DomainResult.Success) {
            recordAudit(
                tenantId = tenantId,
                projectId = projectId,
                actorId = actorId,
                actorRole = actorRole,
                eventType = "BUDGET_CLOSED",
                targetId = budgetId,
                targetType = "BUDGET",
                details = "Budget closed by $actorId"
            )
        }
        return res
    }

    override suspend fun getBudgetById(
        tenantId: String,
        projectId: String,
        budgetId: String
    ): DomainResult<BusinessFinancialBudget?> {
        return governanceRepository.getBudgetById(tenantId, projectId, budgetId)
    }

    override suspend fun listBudgets(
        tenantId: String,
        projectId: String,
        filter: BusinessFinancialBudgetFilter
    ): DomainResult<List<BusinessFinancialBudget>> {
        return governanceRepository.listBudgets(tenantId, projectId, filter)
    }

    override suspend fun listBudgetRevisions(
        tenantId: String,
        projectId: String,
        budgetId: String
    ): DomainResult<List<BusinessFinancialBudgetRevision>> {
        return governanceRepository.listBudgetRevisions(tenantId, projectId, budgetId)
    }

    // =========================================================================
    // 2. BUDGET CONSUMPTION, VARIANCE & RECONCILIATION
    // =========================================================================

    override suspend fun calculateBudgetVsActual(
        tenantId: String,
        projectId: String,
        budgetId: String,
        currency: String
    ): DomainResult<BudgetVsActualComparison> {
        val budgetRes = governanceRepository.getBudgetById(tenantId, projectId, budgetId)
        val budget = (budgetRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Budget not found with ID: $budgetId")

        val comp = computeBudgetVsActual(tenantId, projectId, budget, currency)
        return DomainResult.Success(comp)
    }

    override suspend fun calculateAllBudgetVsActual(
        tenantId: String,
        projectId: String,
        periodId: String?,
        currency: String
    ): DomainResult<List<BudgetVsActualComparison>> {
        val filter = BusinessFinancialBudgetFilter(
            periodId = periodId,
            status = BusinessFinancialBudgetStatus.ACTIVE,
            currency = currency,
            limit = 500
        )
        val budgetsRes = governanceRepository.listBudgets(tenantId, projectId, filter)
        val budgets = (budgetsRes as? DomainResult.Success)?.data ?: emptyList()

        val comparisons = budgets.map { computeBudgetVsActual(tenantId, projectId, it, currency) }
        return DomainResult.Success(comparisons)
    }

    override suspend fun getExecutiveGovernanceOverview(
        tenantId: String,
        projectId: String,
        periodId: String?,
        currency: String
    ): DomainResult<ExecutiveGovernanceOverview> {
        val comparisonsRes = calculateAllBudgetVsActual(tenantId, projectId, periodId, currency)
        val comparisons = (comparisonsRes as? DomainResult.Success)?.data ?: emptyList()

        val alertsRes = governanceRepository.listAlerts(tenantId, projectId, GovernanceAlertFilter(periodId = periodId, limit = 100))
        val alerts = (alertsRes as? DomainResult.Success)?.data ?: emptyList()

        val forecastsRes = governanceRepository.listForecasts(tenantId, projectId, periodId)
        val forecasts = (forecastsRes as? DomainResult.Success)?.data ?: emptyList()

        val thresholdsRes = governanceRepository.listThresholds(tenantId, projectId)
        val thresholds = (thresholdsRes as? DomainResult.Success)?.data ?: emptyList()

        val totalAllocated = comparisons.fold(BigDecimal.ZERO) { acc, c -> acc + c.allocatedBudget }.setScale(4, RoundingMode.HALF_UP)
        val totalActual = comparisons.fold(BigDecimal.ZERO) { acc, c -> acc + c.actualSpend }.setScale(4, RoundingMode.HALF_UP)
        val totalCommitted = comparisons.fold(BigDecimal.ZERO) { acc, c -> acc + c.committedExposure }.setScale(4, RoundingMode.HALF_UP)
        val totalAccrued = comparisons.fold(BigDecimal.ZERO) { acc, c -> acc + c.accruedExposure }.setScale(4, RoundingMode.HALF_UP)
        val totalProjected = comparisons.fold(BigDecimal.ZERO) { acc, c -> acc + c.totalProjectedExposure }.setScale(4, RoundingMode.HALF_UP)
        val totalRemaining = (totalAllocated - totalActual).setScale(4, RoundingMode.HALF_UP)

        val overallUtilization = if (totalAllocated > BigDecimal.ZERO) {
            totalActual.multiply(BigDecimal("100.0000")).divide(totalAllocated, 4, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val openAlerts = alerts.filter { it.status == GovernanceAlertStatus.OPEN }
        val criticalAlerts = openAlerts.filter { it.severity == GovernanceAlertSeverity.CRITICAL }
        val warningAlerts = openAlerts.filter { it.severity == GovernanceAlertSeverity.WARNING }

        val overview = ExecutiveGovernanceOverview(
            tenantId = tenantId,
            projectId = projectId,
            periodId = periodId,
            currency = currency,
            totalActiveBudgetsCount = comparisons.size,
            totalAllocatedBudgetAmount = totalAllocated,
            totalActualSpendAmount = totalActual,
            totalCommittedExposureAmount = totalCommitted,
            totalAccruedExposureAmount = totalAccrued,
            totalProjectedExposureAmount = totalProjected,
            totalRemainingBudgetAmount = totalRemaining,
            overallUtilizationPercentage = overallUtilization,
            activeThresholdsCount = thresholds.count { it.isActive },
            openAlertsCount = openAlerts.size,
            criticalAlertsCount = criticalAlerts.size,
            warningAlertsCount = warningAlerts.size,
            comparisons = comparisons,
            alerts = alerts,
            forecasts = forecasts
        )

        return DomainResult.Success(overview)
    }

    private suspend fun computeBudgetVsActual(
        tenantId: String,
        projectId: String,
        budget: BusinessFinancialBudget,
        currency: String
    ): BudgetVsActualComparison {
        // 1. Actual Expenses from canonical BusinessExpenseRepository
        val rawExpenses = when (val res = expenseRepository.listExpenses(tenantId = tenantId, projectId = projectId, limit = 2000, offset = 0)) {
            is DomainResult.Success -> res.data.filter { e ->
                e.currency == currency &&
                (e.status == BusinessExpenseStatus.APPROVED || e.status == BusinessExpenseStatus.POSTABLE) &&
                (e.expenseDate in budget.effectiveStartDate..budget.effectiveEndDate) &&
                matchesDimension(budget.dimensionType, budget.dimensionId, null, e.expenseCategoryId, e.jobId, e.branchId, e.expenseCategoryId)
            }
            else -> emptyList()
        }
        val actualExpenseTotal = rawExpenses.fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }.setScale(4, RoundingMode.HALF_UP)

        // 2. Committed Exposure from canonical BusinessCostControlRepository
        val commitments = costControlRepository.listCommitments(tenantId, projectId).filter { c ->
            c.currency == currency &&
            c.status.canBeConsumed &&
            (c.commitmentDate in budget.effectiveStartDate..budget.effectiveEndDate) &&
            matchesDimension(budget.dimensionType, budget.dimensionId, c.costCenterId, c.costCategoryId, c.jobId, null, c.costCategoryId)
        }
        val committedExposure = commitments.fold(BigDecimal.ZERO) { acc, c -> acc + c.remainingAmount }.setScale(4, RoundingMode.HALF_UP)

        // 3. Accrual Exposure from canonical BusinessCostControlRepository
        val accruals = costControlRepository.listAccruals(tenantId, projectId).filter { a ->
            a.currency == currency &&
            a.status in setOf(BusinessCostAccrualStatus.REVIEWED, BusinessCostAccrualStatus.APPROVED, BusinessCostAccrualStatus.POSTED) &&
            (a.accrualDate in budget.effectiveStartDate..budget.effectiveEndDate) &&
            matchesDimension(budget.dimensionType, budget.dimensionId, a.costCenterId, a.costCategoryId, a.jobId, null, a.costCategoryId)
        }
        val accruedExposure = accruals.fold(BigDecimal.ZERO) { acc, a -> acc + a.netAccrualAmount }.setScale(4, RoundingMode.HALF_UP)

        // 4. Totals and Variances
        val totalExposure = (actualExpenseTotal + committedExposure + accruedExposure).setScale(4, RoundingMode.HALF_UP)
        val remainingBudget = (budget.allocatedAmount - actualExpenseTotal).setScale(4, RoundingMode.HALF_UP)
        val remainingProjectedBudget = (budget.allocatedAmount - totalExposure).setScale(4, RoundingMode.HALF_UP)

        val utilizationPct = if (budget.allocatedAmount > BigDecimal.ZERO) {
            actualExpenseTotal.multiply(BigDecimal("100.0000")).divide(budget.allocatedAmount, 4, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val projectedUtilizationPct = if (budget.allocatedAmount > BigDecimal.ZERO) {
            totalExposure.multiply(BigDecimal("100.0000")).divide(budget.allocatedAmount, 4, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val varianceAmount = (budget.allocatedAmount - totalExposure).setScale(4, RoundingMode.HALF_UP)

        val varianceStatus = when {
            actualExpenseTotal > budget.allocatedAmount -> BudgetVarianceStatus.OVER_BUDGET
            totalExposure > budget.allocatedAmount -> BudgetVarianceStatus.CRITICAL
            utilizationPct >= BigDecimal("90.0000") || projectedUtilizationPct >= BigDecimal("85.0000") -> BudgetVarianceStatus.WARNING
            else -> BudgetVarianceStatus.ON_TRACK
        }

        return BudgetVsActualComparison(
            budgetId = budget.id,
            budgetName = budget.budgetName,
            periodId = budget.periodId,
            dimensionType = budget.dimensionType,
            dimensionId = budget.dimensionId,
            currency = currency,
            allocatedBudget = budget.allocatedAmount,
            actualSpend = actualExpenseTotal,
            committedExposure = committedExposure,
            accruedExposure = accruedExposure,
            totalProjectedExposure = totalExposure,
            remainingBudget = remainingBudget,
            remainingProjectedBudget = remainingProjectedBudget,
            utilizationPercentage = utilizationPct,
            projectedUtilizationPercentage = projectedUtilizationPct,
            varianceAmount = varianceAmount,
            varianceStatus = varianceStatus
        )
    }

    // =========================================================================
    // 3. DETERMINISTIC FORECAST FOUNDATION
    // =========================================================================

    override suspend fun generateDeterministicForecast(
        tenantId: String,
        projectId: String,
        periodId: String,
        dimensionType: BusinessFinancialBudgetDimensionType,
        dimensionId: String,
        currency: String,
        actorId: String
    ): DomainResult<Pair<BusinessFinancialForecast, List<BusinessFinancialForecastScenario>>> {
        val period = costControlRepository.findFinancialPeriodById(periodId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Financial Period '$periodId' not found.")

        // 1. Actual YTD Expenses
        val rawExpenses = when (val res = expenseRepository.listExpenses(tenantId = tenantId, projectId = projectId, limit = 2000, offset = 0)) {
            is DomainResult.Success -> res.data.filter { e ->
                e.currency == currency &&
                (e.status == BusinessExpenseStatus.APPROVED || e.status == BusinessExpenseStatus.POSTABLE) &&
                (e.expenseDate in period.startDate..period.endDate) &&
                matchesDimension(dimensionType, dimensionId, null, e.expenseCategoryId, e.jobId, e.branchId, e.expenseCategoryId)
            }
            else -> emptyList()
        }
        val actualYtd = rawExpenses.fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }.setScale(4, RoundingMode.HALF_UP)

        // Time projection
        val now = System.currentTimeMillis()
        val totalDays = maxOf(1L, (period.endDate - period.startDate) / (86400 * 1000L))
        val elapsedDays = maxOf(1L, minOf(totalDays, (now - period.startDate) / (86400 * 1000L)))
        val remainingDays = maxOf(0L, totalDays - elapsedDays)

        val runRatePerDay = actualYtd.divide(BigDecimal(elapsedDays), 4, RoundingMode.HALF_UP)
        val projectedRemaining = runRatePerDay.multiply(BigDecimal(remainingDays)).setScale(4, RoundingMode.HALF_UP)
        val forecastTotal = (actualYtd + projectedRemaining).setScale(4, RoundingMode.HALF_UP)

        // Active budget for variance
        val budgetRes = governanceRepository.findBudgetByDimension(tenantId, projectId, periodId, dimensionType, dimensionId)
        val budgetAmount = (budgetRes as? DomainResult.Success)?.data?.allocatedAmount ?: BigDecimal.ZERO

        val forecastId = UUID.randomUUID().toString()
        val forecast = BusinessFinancialForecast(
            id = forecastId,
            tenantId = tenantId,
            projectId = projectId,
            forecastName = "Forecast: ${dimensionType.name}-$dimensionId ($periodId)",
            periodId = periodId,
            dimensionType = dimensionType,
            dimensionId = dimensionId,
            currency = currency,
            actualYtdAmount = actualYtd,
            projectedRemainingAmount = projectedRemaining,
            forecastTotalAmount = forecastTotal,
            runRatePerDay = runRatePerDay,
            generatedAt = System.currentTimeMillis(),
            createdBy = actorId
        )
        governanceRepository.saveForecast(forecast)

        // Generate Scenarios (BASELINE, OPTIMISTIC, CONSERVATIVE)
        val committedExposure = costControlRepository.listCommitments(tenantId, projectId).filter { c ->
            c.currency == currency &&
            c.status.canBeConsumed &&
            matchesDimension(dimensionType, dimensionId, c.costCenterId, c.costCategoryId, c.jobId, null, c.costCategoryId)
        }.fold(BigDecimal.ZERO) { acc, c -> acc + c.remainingAmount }

        val baselineScenario = BusinessFinancialForecastScenario(
            id = UUID.randomUUID().toString(),
            forecastId = forecastId,
            tenantId = tenantId,
            projectId = projectId,
            scenarioType = ForecastScenarioType.BASELINE,
            projectedAmount = forecastTotal,
            varianceVsBudget = (budgetAmount - forecastTotal).setScale(4, RoundingMode.HALF_UP),
            assumptionsJson = """{"basis":"Linear run-rate per day ($runRatePerDay) over $remainingDays remaining days."}""",
            createdAt = System.currentTimeMillis()
        )

        // Optimistic: 15% reduction on projected remaining
        val optimisticProjected = (actualYtd + projectedRemaining.multiply(BigDecimal("0.8500"))).setScale(4, RoundingMode.HALF_UP)
        val optimisticScenario = BusinessFinancialForecastScenario(
            id = UUID.randomUUID().toString(),
            forecastId = forecastId,
            tenantId = tenantId,
            projectId = projectId,
            scenarioType = ForecastScenarioType.OPTIMISTIC,
            projectedAmount = optimisticProjected,
            varianceVsBudget = (budgetAmount - optimisticProjected).setScale(4, RoundingMode.HALF_UP),
            assumptionsJson = """{"basis":"15% efficiency savings on remaining run-rate spend."}""",
            createdAt = System.currentTimeMillis()
        )

        // Conservative: 20% increase on projected remaining + active unfulfilled commitments
        val conservativeProjected = (actualYtd + projectedRemaining.multiply(BigDecimal("1.2000")) + committedExposure).setScale(4, RoundingMode.HALF_UP)
        val conservativeScenario = BusinessFinancialForecastScenario(
            id = UUID.randomUUID().toString(),
            forecastId = forecastId,
            tenantId = tenantId,
            projectId = projectId,
            scenarioType = ForecastScenarioType.CONSERVATIVE,
            projectedAmount = conservativeProjected,
            varianceVsBudget = (budgetAmount - conservativeProjected).setScale(4, RoundingMode.HALF_UP),
            assumptionsJson = """{"basis":"20% cost surge risk on run-rate plus committed exposure ($committedExposure)."}""",
            createdAt = System.currentTimeMillis()
        )

        val scenarios = listOf(baselineScenario, optimisticScenario, conservativeScenario)
        for (s in scenarios) {
            governanceRepository.saveForecastScenario(s)
        }

        recordAudit(
            tenantId = tenantId,
            projectId = projectId,
            actorId = actorId,
            actorRole = "SYSTEM",
            eventType = "FORECAST_GENERATED",
            targetId = forecastId,
            targetType = "FORECAST",
            details = "Deterministic forecast generated for $dimensionType:$dimensionId (Total: $forecastTotal $currency)"
        )

        return DomainResult.Success(forecast to scenarios)
    }

    override suspend fun listForecasts(
        tenantId: String,
        projectId: String,
        periodId: String?
    ): DomainResult<List<BusinessFinancialForecast>> {
        return governanceRepository.listForecasts(tenantId, projectId, periodId)
    }

    override suspend fun getForecastScenarios(
        tenantId: String,
        projectId: String,
        forecastId: String
    ): DomainResult<List<BusinessFinancialForecastScenario>> {
        return governanceRepository.listForecastScenarios(tenantId, projectId, forecastId)
    }

    // =========================================================================
    // 4. SPENDING THRESHOLDS & DECISION ALERTS
    // =========================================================================

    override suspend fun upsertThreshold(
        threshold: BusinessFinancialBudgetThreshold,
        actorId: String,
        actorRole: String
    ): DomainResult<BusinessFinancialBudgetThreshold> {
        val valRes = BusinessFinancialGovernanceValidator.validateThreshold(threshold)
        if (valRes is DomainResult.Error) {
            return DomainResult.Error(message = valRes.message)
        }

        val res = governanceRepository.upsertThreshold(threshold)
        if (res is DomainResult.Success) {
            recordAudit(
                tenantId = threshold.tenantId,
                projectId = threshold.projectId,
                actorId = actorId,
                actorRole = actorRole,
                eventType = "THRESHOLD_CONFIGURED",
                targetId = threshold.id,
                targetType = "THRESHOLD",
                details = "Configured threshold '${threshold.thresholdName}' for ${threshold.dimensionType}:${threshold.dimensionId}"
            )
        }
        return res
    }

    override suspend fun listThresholds(
        tenantId: String,
        projectId: String
    ): DomainResult<List<BusinessFinancialBudgetThreshold>> {
        return governanceRepository.listThresholds(tenantId, projectId)
    }

    override suspend fun evaluateGovernanceThresholdsAndGenerateAlerts(
        tenantId: String,
        projectId: String,
        periodId: String?,
        currency: String,
        actorId: String
    ): DomainResult<List<BusinessFinancialGovernanceAlert>> {
        val comparisonsRes = calculateAllBudgetVsActual(tenantId, projectId, periodId, currency)
        val comparisons = (comparisonsRes as? DomainResult.Success)?.data ?: emptyList()
        val generatedAlerts = mutableListOf<BusinessFinancialGovernanceAlert>()

        // 1. Budget & Exposure Threshold Evaluations
        for (comp in comparisons) {
            val thresholdRes = governanceRepository.getThreshold(tenantId, projectId, comp.dimensionType, comp.dimensionId)
            val threshold = (thresholdRes as? DomainResult.Success)?.data ?: BusinessFinancialBudgetThreshold(
                id = "DEFAULT",
                tenantId = tenantId,
                projectId = projectId,
                thresholdName = "Default Threshold"
            )

            // Critical Over-Budget Alert
            if (comp.utilizationPercentage >= threshold.criticalUtilizationPct) {
                maybeCreateAlert(
                    tenantId = tenantId,
                    projectId = projectId,
                    alertType = GovernanceAlertType.OVER_BUDGET,
                    severity = GovernanceAlertSeverity.CRITICAL,
                    dimensionType = comp.dimensionType,
                    dimensionId = comp.dimensionId,
                    message = "Budget '${comp.budgetName}' exceeded limit! Utilization is ${comp.utilizationPercentage}%. Actual spend: ${comp.actualSpend} ${comp.currency} vs Budget ${comp.allocatedBudget}.",
                    thresholdValue = threshold.criticalUtilizationPct,
                    currentValue = comp.utilizationPercentage,
                    periodId = comp.periodId,
                    sink = generatedAlerts
                )
            } else if (comp.utilizationPercentage >= threshold.warningUtilizationPct) {
                maybeCreateAlert(
                    tenantId = tenantId,
                    projectId = projectId,
                    alertType = GovernanceAlertType.BUDGET_WARNING,
                    severity = GovernanceAlertSeverity.WARNING,
                    dimensionType = comp.dimensionType,
                    dimensionId = comp.dimensionId,
                    message = "Budget '${comp.budgetName}' approaching limit: ${comp.utilizationPercentage}% utilized.",
                    thresholdValue = threshold.warningUtilizationPct,
                    currentValue = comp.utilizationPercentage,
                    periodId = comp.periodId,
                    sink = generatedAlerts
                )
            }

            // High Commitment Exposure Alert
            val commitmentLimit = comp.allocatedBudget.multiply(threshold.commitmentExposureThresholdPct).divide(BigDecimal("100.0000"), 4, RoundingMode.HALF_UP)
            if (comp.committedExposure > commitmentLimit && commitmentLimit > BigDecimal.ZERO) {
                maybeCreateAlert(
                    tenantId = tenantId,
                    projectId = projectId,
                    alertType = GovernanceAlertType.HIGH_COMMITMENT_EXPOSURE,
                    severity = GovernanceAlertSeverity.WARNING,
                    dimensionType = comp.dimensionType,
                    dimensionId = comp.dimensionId,
                    message = "High committed exposure: ${comp.committedExposure} ${comp.currency} exceeds threshold limit $commitmentLimit.",
                    thresholdValue = threshold.commitmentExposureThresholdPct,
                    currentValue = comp.committedExposure,
                    periodId = comp.periodId,
                    sink = generatedAlerts
                )
            }
        }

        // 2. Overdue Payables Evaluation
        val payablesRes = payableRepository.listPayables(tenantId = tenantId, projectId = projectId, limit = 1000)
        val now = System.currentTimeMillis()
        val overduePayables = (payablesRes as? DomainResult.Success)?.data?.filter { p ->
            p.currency == currency &&
            p.dueDate != null && p.dueDate < now &&
            p.status in setOf(VendorPayableStatus.SUBMITTED, VendorPayableStatus.APPROVED, VendorPayableStatus.PARTIALLY_PAID)
        } ?: emptyList()
        val overdueTotal = overduePayables.fold(BigDecimal.ZERO) { acc, p -> acc + p.outstandingAmount }.setScale(4, RoundingMode.HALF_UP)

        if (overdueTotal > BigDecimal("10000.0000")) {
            maybeCreateAlert(
                tenantId = tenantId,
                projectId = projectId,
                alertType = GovernanceAlertType.PAYABLE_PRESSURE,
                severity = GovernanceAlertSeverity.CRITICAL,
                dimensionType = BusinessFinancialBudgetDimensionType.OVERALL_BUSINESS,
                dimensionId = "ALL",
                message = "Critical Vendor Payable Pressure: ${overduePayables.size} overdue payables totaling $overdueTotal $currency.",
                thresholdValue = BigDecimal("10000.0000"),
                currentValue = overdueTotal,
                periodId = periodId,
                sink = generatedAlerts
            )
        }

        return DomainResult.Success(generatedAlerts)
    }

    private suspend fun maybeCreateAlert(
        tenantId: String,
        projectId: String,
        alertType: GovernanceAlertType,
        severity: GovernanceAlertSeverity,
        dimensionType: BusinessFinancialBudgetDimensionType,
        dimensionId: String,
        message: String,
        thresholdValue: BigDecimal,
        currentValue: BigDecimal,
        periodId: String?,
        sink: MutableList<BusinessFinancialGovernanceAlert>
    ) {
        val existing = governanceRepository.findOpenAlert(
            tenantId = tenantId,
            projectId = projectId,
            alertType = alertType,
            dimensionType = dimensionType,
            dimensionId = dimensionId,
            periodId = periodId
        )
        if (existing is DomainResult.Success && existing.data != null) {
            return
        }

        val alert = BusinessFinancialGovernanceAlert(
            id = UUID.randomUUID().toString(),
            tenantId = tenantId,
            projectId = projectId,
            alertType = alertType,
            severity = severity,
            sourceDimensionType = dimensionType,
            sourceDimensionId = dimensionId,
            message = message,
            thresholdValue = thresholdValue,
            currentValue = currentValue,
            status = GovernanceAlertStatus.OPEN,
            periodId = periodId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val created = governanceRepository.createAlert(alert)
        if (created is DomainResult.Success) {
            sink.add(created.data)
            recordAudit(
                tenantId = tenantId,
                projectId = projectId,
                actorId = "SYSTEM",
                actorRole = "SYSTEM",
                eventType = "GOVERNANCE_ALERT_TRIGGERED",
                targetId = alert.id,
                targetType = "ALERT",
                details = message
            )
        }
    }

    override suspend fun acknowledgeAlert(
        tenantId: String,
        projectId: String,
        alertId: String,
        notes: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<BusinessFinancialGovernanceAlert> {
        val alertRes = governanceRepository.getAlertById(tenantId, projectId, alertId)
        val alert = (alertRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Alert not found with ID: $alertId")

        val valRes = BusinessFinancialGovernanceValidator.validateAlertStatusTransition(
            alert = alert,
            targetStatus = GovernanceAlertStatus.ACKNOWLEDGED,
            notes = notes,
            actorId = actorId
        )
        if (valRes is DomainResult.Error) {
            return DomainResult.Error(message = valRes.message)
        }

        val updated = alert.copy(
            status = GovernanceAlertStatus.ACKNOWLEDGED,
            acknowledgedBy = actorId,
            acknowledgedAt = System.currentTimeMillis(),
            acknowledgementNotes = notes,
            updatedAt = System.currentTimeMillis()
        )

        val res = governanceRepository.updateAlert(updated)
        if (res is DomainResult.Success) {
            recordAudit(
                tenantId = tenantId,
                projectId = projectId,
                actorId = actorId,
                actorRole = actorRole,
                eventType = "ALERT_ACKNOWLEDGED",
                targetId = alertId,
                targetType = "ALERT",
                details = "Alert acknowledged by $actorId. Notes: $notes"
            )
        }
        return res
    }

    override suspend fun resolveAlert(
        tenantId: String,
        projectId: String,
        alertId: String,
        resolutionNotes: String?,
        actorId: String,
        actorRole: String
    ): DomainResult<BusinessFinancialGovernanceAlert> {
        val alertRes = governanceRepository.getAlertById(tenantId, projectId, alertId)
        val alert = (alertRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Alert not found with ID: $alertId")

        val valRes = BusinessFinancialGovernanceValidator.validateAlertStatusTransition(
            alert = alert,
            targetStatus = GovernanceAlertStatus.RESOLVED,
            notes = resolutionNotes,
            actorId = actorId
        )
        if (valRes is DomainResult.Error) {
            return DomainResult.Error(message = valRes.message)
        }

        val updated = alert.copy(
            status = GovernanceAlertStatus.RESOLVED,
            resolvedBy = actorId,
            resolvedAt = System.currentTimeMillis(),
            resolutionNotes = resolutionNotes,
            updatedAt = System.currentTimeMillis()
        )

        val res = governanceRepository.updateAlert(updated)
        if (res is DomainResult.Success) {
            recordAudit(
                tenantId = tenantId,
                projectId = projectId,
                actorId = actorId,
                actorRole = actorRole,
                eventType = "ALERT_RESOLVED",
                targetId = alertId,
                targetType = "ALERT",
                details = "Alert resolved by $actorId. Notes: $resolutionNotes"
            )
        }
        return res
    }

    override suspend fun dismissAlert(
        tenantId: String,
        projectId: String,
        alertId: String,
        dismissalReason: String,
        actorId: String,
        actorRole: String
    ): DomainResult<BusinessFinancialGovernanceAlert> {
        val alertRes = governanceRepository.getAlertById(tenantId, projectId, alertId)
        val alert = (alertRes as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Alert not found with ID: $alertId")

        val valRes = BusinessFinancialGovernanceValidator.validateAlertStatusTransition(
            alert = alert,
            targetStatus = GovernanceAlertStatus.DISMISSED,
            notes = dismissalReason,
            actorId = actorId
        )
        if (valRes is DomainResult.Error) {
            return DomainResult.Error(message = valRes.message)
        }

        val updated = alert.copy(
            status = GovernanceAlertStatus.DISMISSED,
            dismissalReason = dismissalReason,
            updatedAt = System.currentTimeMillis()
        )

        val res = governanceRepository.updateAlert(updated)
        if (res is DomainResult.Success) {
            recordAudit(
                tenantId = tenantId,
                projectId = projectId,
                actorId = actorId,
                actorRole = actorRole,
                eventType = "ALERT_DISMISSED",
                targetId = alertId,
                targetType = "ALERT",
                details = "Alert dismissed by $actorId. Reason: $dismissalReason"
            )
        }
        return res
    }

    override suspend fun listAlerts(
        tenantId: String,
        projectId: String,
        filter: GovernanceAlertFilter
    ): DomainResult<List<BusinessFinancialGovernanceAlert>> {
        return governanceRepository.listAlerts(tenantId, projectId, filter)
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        filter: GovernanceAuditFilter
    ): DomainResult<List<BusinessFinancialGovernanceAuditEvent>> {
        return governanceRepository.listAuditEvents(tenantId, projectId, filter)
    }

    // =========================================================================
    // DIMENSION MATCHING & AUDIT HELPERS
    // =========================================================================

    private fun matchesDimension(
        dimensionType: BusinessFinancialBudgetDimensionType,
        dimensionId: String,
        costCenterId: String? = null,
        costCategoryId: String? = null,
        jobId: String? = null,
        branchId: String? = null,
        expenseCategoryId: String? = null
    ): Boolean {
        return when (dimensionType) {
            BusinessFinancialBudgetDimensionType.OVERALL_BUSINESS -> true
            BusinessFinancialBudgetDimensionType.COST_CENTER -> costCenterId == dimensionId
            BusinessFinancialBudgetDimensionType.COST_CATEGORY -> costCategoryId == dimensionId
            BusinessFinancialBudgetDimensionType.JOB -> jobId == dimensionId
            BusinessFinancialBudgetDimensionType.BRANCH -> branchId == dimensionId
            BusinessFinancialBudgetDimensionType.EXPENSE_CATEGORY -> expenseCategoryId == dimensionId
        }
    }

    private suspend fun recordAudit(
        tenantId: String,
        projectId: String,
        actorId: String,
        actorRole: String,
        eventType: String,
        targetId: String? = null,
        targetType: String? = null,
        details: String? = null
    ) {
        val event = BusinessFinancialGovernanceAuditEvent(
            id = UUID.randomUUID().toString(),
            tenantId = tenantId,
            projectId = projectId,
            actorId = actorId,
            actorRole = actorRole,
            eventType = eventType,
            outcome = "SUCCESS",
            targetId = targetId,
            targetType = targetType,
            timestamp = System.currentTimeMillis(),
            detailsJson = details
        )
        governanceRepository.recordAuditEvent(event)
    }
}

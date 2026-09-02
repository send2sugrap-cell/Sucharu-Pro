package com.sucharu.sucharupro.data.api.model.businessfinancialgovernance

import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.*
import java.math.BigDecimal

/**
 * Request payload for creating a financial budget.
 */
data class CreateFinancialBudgetRequestDto(
    val budgetName: String,
    val periodId: String,
    val dimensionType: BusinessFinancialBudgetDimensionType,
    val dimensionId: String,
    val allocatedAmount: BigDecimal,
    val currency: String = "BDT",
    val effectiveStartDate: Long,
    val effectiveEndDate: Long,
    val description: String? = null
)

/**
 * Request payload for revising an existing budget.
 */
data class ReviseFinancialBudgetRequestDto(
    val newAllocatedAmount: BigDecimal,
    val revisionReason: String
)

/**
 * Request payload for rejecting a budget.
 */
data class RejectFinancialBudgetRequestDto(
    val rejectionReason: String
)

/**
 * Request payload for configuring a budget threshold.
 */
data class ConfigureBudgetThresholdRequestDto(
    val thresholdName: String,
    val dimensionType: BusinessFinancialBudgetDimensionType = BusinessFinancialBudgetDimensionType.OVERALL_BUSINESS,
    val dimensionId: String = "ALL",
    val warningUtilizationPct: BigDecimal = BigDecimal("80.0000"),
    val criticalUtilizationPct: BigDecimal = BigDecimal("100.0000"),
    val largeExpenseThresholdAmount: BigDecimal = BigDecimal("50000.0000"),
    val commitmentExposureThresholdPct: BigDecimal = BigDecimal("90.0000"),
    val isActive: Boolean = true
)

/**
 * Request payload for generating a deterministic forecast.
 */
data class GenerateForecastRequestDto(
    val periodId: String,
    val dimensionType: BusinessFinancialBudgetDimensionType = BusinessFinancialBudgetDimensionType.OVERALL_BUSINESS,
    val dimensionId: String = "ALL",
    val currency: String = "BDT"
)

/**
 * Request payload for acknowledging an alert.
 */
data class AcknowledgeAlertRequestDto(
    val notes: String? = null
)

/**
 * Request payload for resolving an alert.
 */
data class ResolveAlertRequestDto(
    val resolutionNotes: String? = null
)

/**
 * Request payload for dismissing an alert.
 */
data class DismissAlertRequestDto(
    val dismissalReason: String
)

/**
 * DTO for budget response.
 */
data class BusinessFinancialBudgetDto(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val budgetName: String,
    val periodId: String,
    val dimensionType: BusinessFinancialBudgetDimensionType,
    val dimensionId: String,
    val allocatedAmount: BigDecimal,
    val currency: String,
    val status: BusinessFinancialBudgetStatus,
    val version: Long,
    val effectiveStartDate: Long,
    val effectiveEndDate: Long,
    val description: String?,
    val createdBy: String,
    val reviewedBy: String?,
    val approvedBy: String?,
    val approvedAt: Long?,
    val rejectionReason: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object
}

/**
 * DTO for budget revision response.
 */
data class BusinessFinancialBudgetRevisionDto(
    val id: String,
    val budgetId: String,
    val tenantId: String,
    val projectId: String,
    val version: Long,
    val previousAllocatedAmount: BigDecimal,
    val newAllocatedAmount: BigDecimal,
    val revisionReason: String,
    val revisedBy: String,
    val approvedBy: String?,
    val revisedAt: Long,
    val status: String
) {
    companion object
}

/**
 * DTO for budget threshold response.
 */
data class BusinessFinancialBudgetThresholdDto(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val thresholdName: String,
    val dimensionType: BusinessFinancialBudgetDimensionType,
    val dimensionId: String,
    val warningUtilizationPct: BigDecimal,
    val criticalUtilizationPct: BigDecimal,
    val largeExpenseThresholdAmount: BigDecimal,
    val commitmentExposureThresholdPct: BigDecimal,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object
}

/**
 * DTO for forecast projection response.
 */
data class BusinessFinancialForecastDto(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val forecastName: String,
    val periodId: String,
    val dimensionType: BusinessFinancialBudgetDimensionType,
    val dimensionId: String,
    val currency: String,
    val actualYtdAmount: BigDecimal,
    val projectedRemainingAmount: BigDecimal,
    val forecastTotalAmount: BigDecimal,
    val runRatePerDay: BigDecimal,
    val generatedAt: Long,
    val createdBy: String,
    val scenarios: List<BusinessFinancialForecastScenarioDto> = emptyList()
) {
    companion object
}

/**
 * DTO for forecast scenario response.
 */
data class BusinessFinancialForecastScenarioDto(
    val id: String,
    val forecastId: String,
    val tenantId: String,
    val projectId: String,
    val scenarioType: ForecastScenarioType,
    val projectedAmount: BigDecimal,
    val varianceVsBudget: BigDecimal,
    val assumptionsJson: String?,
    val createdAt: Long
) {
    companion object
}

/**
 * DTO for governance alert response.
 */
data class BusinessFinancialGovernanceAlertDto(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val alertType: GovernanceAlertType,
    val severity: GovernanceAlertSeverity,
    val sourceDimensionType: BusinessFinancialBudgetDimensionType,
    val sourceDimensionId: String,
    val message: String,
    val thresholdValue: BigDecimal,
    val currentValue: BigDecimal,
    val status: GovernanceAlertStatus,
    val acknowledgedBy: String?,
    val acknowledgedAt: Long?,
    val acknowledgementNotes: String?,
    val resolvedBy: String?,
    val resolvedAt: Long?,
    val resolutionNotes: String?,
    val dismissalReason: String?,
    val periodId: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object
}

/**
 * DTO for budget vs actual comparison projection.
 */
data class BudgetVsActualComparisonDto(
    val budgetId: String,
    val budgetName: String,
    val periodId: String,
    val dimensionType: BusinessFinancialBudgetDimensionType,
    val dimensionId: String,
    val currency: String,
    val allocatedBudget: BigDecimal,
    val actualSpend: BigDecimal,
    val committedExposure: BigDecimal,
    val accruedExposure: BigDecimal,
    val totalProjectedExposure: BigDecimal,
    val remainingBudget: BigDecimal,
    val remainingProjectedBudget: BigDecimal,
    val utilizationPercentage: BigDecimal,
    val projectedUtilizationPercentage: BigDecimal,
    val varianceAmount: BigDecimal,
    val varianceStatus: BudgetVarianceStatus
) {
    companion object
}

/**
 * DTO for executive governance overview.
 */
data class ExecutiveGovernanceOverviewDto(
    val tenantId: String,
    val projectId: String,
    val periodId: String?,
    val currency: String,
    val totalActiveBudgetsCount: Int,
    val totalAllocatedBudgetAmount: BigDecimal,
    val totalActualSpendAmount: BigDecimal,
    val totalCommittedExposureAmount: BigDecimal,
    val totalAccruedExposureAmount: BigDecimal,
    val totalProjectedExposureAmount: BigDecimal,
    val totalRemainingBudgetAmount: BigDecimal,
    val overallUtilizationPercentage: BigDecimal,
    val activeThresholdsCount: Int,
    val openAlertsCount: Int,
    val criticalAlertsCount: Int,
    val warningAlertsCount: Int,
    val comparisons: List<BudgetVsActualComparisonDto>,
    val alerts: List<BusinessFinancialGovernanceAlertDto>,
    val forecasts: List<BusinessFinancialForecastDto>
) {
    companion object
}

/**
 * DTO for governance audit event.
 */
data class BusinessFinancialGovernanceAuditEventDto(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val actorId: String,
    val actorRole: String,
    val eventType: String,
    val outcome: String,
    val targetId: String?,
    val targetType: String?,
    val timestamp: Long,
    val detailsJson: String?
)

// =========================================================================
// MAPPINGS & FACTORIES
// =========================================================================

fun CreateFinancialBudgetRequestDto.toDomain(
    tenantId: String,
    projectId: String,
    createdBy: String,
    id: String = "BUD-${System.currentTimeMillis()}"
): BusinessFinancialBudget = BusinessFinancialBudget(
    id = id,
    tenantId = tenantId,
    projectId = projectId,
    budgetName = budgetName,
    periodId = periodId,
    dimensionType = dimensionType,
    dimensionId = dimensionId,
    allocatedAmount = allocatedAmount,
    currency = currency,
    status = BusinessFinancialBudgetStatus.DRAFT,
    version = 1L,
    effectiveStartDate = effectiveStartDate,
    effectiveEndDate = effectiveEndDate,
    description = description,
    createdBy = createdBy,
    createdAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis()
)

fun BusinessFinancialBudget.toDto(): BusinessFinancialBudgetDto = BusinessFinancialBudgetDto(
    id = id,
    tenantId = tenantId,
    projectId = projectId,
    budgetName = budgetName,
    periodId = periodId,
    dimensionType = dimensionType,
    dimensionId = dimensionId,
    allocatedAmount = allocatedAmount,
    currency = currency,
    status = status,
    version = version,
    effectiveStartDate = effectiveStartDate,
    effectiveEndDate = effectiveEndDate,
    description = description,
    createdBy = createdBy,
    reviewedBy = reviewedBy,
    approvedBy = approvedBy,
    approvedAt = approvedAt,
    rejectionReason = rejectionReason,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun BusinessFinancialBudgetRevision.toDto(): BusinessFinancialBudgetRevisionDto = BusinessFinancialBudgetRevisionDto(
    id = id,
    budgetId = budgetId,
    tenantId = tenantId,
    projectId = projectId,
    version = version,
    previousAllocatedAmount = previousAllocatedAmount,
    newAllocatedAmount = newAllocatedAmount,
    revisionReason = revisionReason,
    revisedBy = revisedBy,
    approvedBy = approvedBy,
    revisedAt = revisedAt,
    status = status
)

fun BusinessFinancialBudgetThreshold.toDto(): BusinessFinancialBudgetThresholdDto = BusinessFinancialBudgetThresholdDto(
    id = id,
    tenantId = tenantId,
    projectId = projectId,
    thresholdName = thresholdName,
    dimensionType = dimensionType,
    dimensionId = dimensionId,
    warningUtilizationPct = warningUtilizationPct,
    criticalUtilizationPct = criticalUtilizationPct,
    largeExpenseThresholdAmount = largeExpenseThresholdAmount,
    commitmentExposureThresholdPct = commitmentExposureThresholdPct,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun BusinessFinancialForecastScenario.toDto(): BusinessFinancialForecastScenarioDto = BusinessFinancialForecastScenarioDto(
    id = id,
    forecastId = forecastId,
    tenantId = tenantId,
    projectId = projectId,
    scenarioType = scenarioType,
    projectedAmount = projectedAmount,
    varianceVsBudget = varianceVsBudget,
    assumptionsJson = assumptionsJson,
    createdAt = createdAt
)

fun BusinessFinancialForecast.toDto(scenarios: List<BusinessFinancialForecastScenario> = emptyList()): BusinessFinancialForecastDto = BusinessFinancialForecastDto(
    id = id,
    tenantId = tenantId,
    projectId = projectId,
    forecastName = forecastName,
    periodId = periodId,
    dimensionType = dimensionType,
    dimensionId = dimensionId,
    currency = currency,
    actualYtdAmount = actualYtdAmount,
    projectedRemainingAmount = projectedRemainingAmount,
    forecastTotalAmount = forecastTotalAmount,
    runRatePerDay = runRatePerDay,
    generatedAt = generatedAt,
    createdBy = createdBy,
    scenarios = scenarios.map { it.toDto() }
)

fun BusinessFinancialGovernanceAlert.toDto(): BusinessFinancialGovernanceAlertDto = BusinessFinancialGovernanceAlertDto(
    id = id,
    tenantId = tenantId,
    projectId = projectId,
    alertType = alertType,
    severity = severity,
    sourceDimensionType = sourceDimensionType,
    sourceDimensionId = sourceDimensionId,
    message = message,
    thresholdValue = thresholdValue,
    currentValue = currentValue,
    status = status,
    acknowledgedBy = acknowledgedBy,
    acknowledgedAt = acknowledgedAt,
    acknowledgementNotes = acknowledgementNotes,
    resolvedBy = resolvedBy,
    resolvedAt = resolvedAt,
    resolutionNotes = resolutionNotes,
    dismissalReason = dismissalReason,
    periodId = periodId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun BudgetVsActualComparison.toDto(): BudgetVsActualComparisonDto = BudgetVsActualComparisonDto(
    budgetId = budgetId,
    budgetName = budgetName,
    periodId = periodId,
    dimensionType = dimensionType,
    dimensionId = dimensionId,
    currency = currency,
    allocatedBudget = allocatedBudget,
    actualSpend = actualSpend,
    committedExposure = committedExposure,
    accruedExposure = accruedExposure,
    totalProjectedExposure = totalProjectedExposure,
    remainingBudget = remainingBudget,
    remainingProjectedBudget = remainingProjectedBudget,
    utilizationPercentage = utilizationPercentage,
    projectedUtilizationPercentage = projectedUtilizationPercentage,
    varianceAmount = varianceAmount,
    varianceStatus = varianceStatus
)

fun ExecutiveGovernanceOverview.toDto(): ExecutiveGovernanceOverviewDto = ExecutiveGovernanceOverviewDto(
    tenantId = tenantId,
    projectId = projectId,
    periodId = periodId,
    currency = currency,
    totalActiveBudgetsCount = totalActiveBudgetsCount,
    totalAllocatedBudgetAmount = totalAllocatedBudgetAmount,
    totalActualSpendAmount = totalActualSpendAmount,
    totalCommittedExposureAmount = totalCommittedExposureAmount,
    totalAccruedExposureAmount = totalAccruedExposureAmount,
    totalProjectedExposureAmount = totalProjectedExposureAmount,
    totalRemainingBudgetAmount = totalRemainingBudgetAmount,
    overallUtilizationPercentage = overallUtilizationPercentage,
    activeThresholdsCount = activeThresholdsCount,
    openAlertsCount = openAlertsCount,
    criticalAlertsCount = criticalAlertsCount,
    warningAlertsCount = warningAlertsCount,
    comparisons = comparisons.map { it.toDto() },
    alerts = alerts.map { it.toDto() },
    forecasts = forecasts.map { it.toDto() }
)

fun BusinessFinancialGovernanceAuditEvent.toDto(): BusinessFinancialGovernanceAuditEventDto = BusinessFinancialGovernanceAuditEventDto(
    id = id,
    tenantId = tenantId,
    projectId = projectId,
    actorId = actorId,
    actorRole = actorRole,
    eventType = eventType,
    outcome = outcome,
    targetId = targetId,
    targetType = targetType,
    timestamp = timestamp,
    detailsJson = detailsJson
)

// Companion object extensions
fun BusinessFinancialBudgetDto.Companion.fromDomain(budget: BusinessFinancialBudget): BusinessFinancialBudgetDto = budget.toDto()
fun BudgetVsActualComparisonDto.Companion.fromDomain(comp: BudgetVsActualComparison): BudgetVsActualComparisonDto = comp.toDto()
fun ExecutiveGovernanceOverviewDto.Companion.fromDomain(overview: ExecutiveGovernanceOverview): ExecutiveGovernanceOverviewDto = overview.toDto()
fun BusinessFinancialGovernanceAlertDto.Companion.fromDomain(alert: BusinessFinancialGovernanceAlert): BusinessFinancialGovernanceAlertDto = alert.toDto()
fun BusinessFinancialForecastDto.Companion.fromDomain(forecast: BusinessFinancialForecast, scenarios: List<BusinessFinancialForecastScenario> = emptyList()): BusinessFinancialForecastDto = forecast.toDto(scenarios)


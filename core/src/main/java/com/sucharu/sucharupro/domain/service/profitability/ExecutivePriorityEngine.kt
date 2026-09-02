package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.math.RoundingMode

interface ExecutivePriorityEngine {
    fun computePriorities(
        alerts: List<ProfitabilityAlert>,
        actions: List<ProfitabilityManagementAction>,
        forecastSnapshot: ProfitabilityForecastSnapshot?,
        leakages: List<ProfitLeakageItem>,
        concentrationSummary: ExecutiveConcentrationSummary?
    ): List<ExecutivePriorityItem>
}

class ExecutivePriorityEngineImpl : ExecutivePriorityEngine {

    private val ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)

    override fun computePriorities(
        alerts: List<ProfitabilityAlert>,
        actions: List<ProfitabilityManagementAction>,
        forecastSnapshot: ProfitabilityForecastSnapshot?,
        leakages: List<ProfitLeakageItem>,
        concentrationSummary: ExecutiveConcentrationSummary?
    ): List<ExecutivePriorityItem> {
        val items = mutableListOf<ExecutivePriorityItem>()

        // 1. Critical and High Alerts
        alerts.filter { it.status !in setOf(ProfitabilityAlertStatus.RESOLVED, ProfitabilityAlertStatus.DISMISSED) }
            .sortedByDescending { it.financialImpact }
            .take(5)
            .forEach { alt ->
                val score = when (alt.severity) {
                    ProfitabilityAlertSeverity.CRITICAL -> BigDecimal("95.0000")
                    ProfitabilityAlertSeverity.HIGH -> BigDecimal("80.0000")
                    ProfitabilityAlertSeverity.MEDIUM -> BigDecimal("60.0000")
                    ProfitabilityAlertSeverity.LOW -> BigDecimal("40.0000")
                    ProfitabilityAlertSeverity.INFO -> BigDecimal("20.0000")
                }
                val urgency = when (alt.severity) {
                    ProfitabilityAlertSeverity.CRITICAL -> AlertEscalationLevel.CRITICAL
                    ProfitabilityAlertSeverity.HIGH -> AlertEscalationLevel.URGENT
                    ProfitabilityAlertSeverity.MEDIUM -> AlertEscalationLevel.ESCALATE
                    else -> AlertEscalationLevel.WATCH
                }
                items.add(
                    ExecutivePriorityItem(
                        priorityRank = 0, // Assigned after sorting
                        priorityId = "prio-alt-${alt.alertId}",
                        title = "Resolve Alert: ${alt.dimensionLabel} - ${alt.alertType}",
                        category = "Alert",
                        dimension = alt.dimensionType,
                        entityId = alt.dimensionId,
                        entityLabel = alt.dimensionLabel,
                        financialImpact = alt.financialImpact,
                        priorityScore = score,
                        severity = alt.severity,
                        urgencyLevel = urgency,
                        recommendedActionCode = alt.recommendedActionCode,
                        recommendedActionTitle = alt.recommendedActionCode?.name ?: "Review Alert Condition",
                        sourceModule = "Module 16",
                        sourceStep = "Step 09",
                        sourceReferenceId = alt.alertId,
                        currentStatus = alt.status.name
                    )
                )
            }

        // 2. Pending Management Actions
        actions.filter { it.status in setOf(ManagementActionStatus.PROPOSED, ManagementActionStatus.ASSIGNED, ManagementActionStatus.IN_PROGRESS, ManagementActionStatus.BLOCKED) }
            .sortedByDescending { it.priorityScore }
            .take(5)
            .forEach { act ->
                items.add(
                    ExecutivePriorityItem(
                        priorityRank = 0,
                        priorityId = "prio-act-${act.actionId}",
                        title = act.actionTitle,
                        category = "ManagementAction",
                        dimension = ProfitabilityAlertDimension.BUSINESS,
                        entityId = act.actionId,
                        entityLabel = act.actionTitle,
                        financialImpact = act.expectedFinancialImpact ?: ZERO,
                        priorityScore = act.priorityScore,
                        severity = ProfitabilityAlertSeverity.HIGH,
                        urgencyLevel = AlertEscalationLevel.URGENT,
                        recommendedActionCode = act.actionCode,
                        recommendedActionTitle = act.actionTitle,
                        sourceModule = "Module 16",
                        sourceStep = "Step 09",
                        sourceReferenceId = act.actionId,
                        currentStatus = act.status.name
                    )
                )
            }

        // 3. Top Profitability Leakages
        leakages.take(3).forEach { lk ->
            val dim = when (lk.dimensionType) {
                ProfitabilityDimensionType.JOB -> ProfitabilityAlertDimension.JOB
                ProfitabilityDimensionType.PRODUCT -> ProfitabilityAlertDimension.PRODUCT
                ProfitabilityDimensionType.CUSTOMER -> ProfitabilityAlertDimension.CUSTOMER
                ProfitabilityDimensionType.VENDOR -> ProfitabilityAlertDimension.VENDOR
                else -> ProfitabilityAlertDimension.BUSINESS
            }
            val sev = when (lk.severity) {
                ManagementPriorityLevel.CRITICAL -> ProfitabilityAlertSeverity.CRITICAL
                ManagementPriorityLevel.HIGH -> ProfitabilityAlertSeverity.HIGH
                ManagementPriorityLevel.MEDIUM -> ProfitabilityAlertSeverity.MEDIUM
                ManagementPriorityLevel.LOW -> ProfitabilityAlertSeverity.LOW
                ManagementPriorityLevel.INFORMATIONAL -> ProfitabilityAlertSeverity.INFO
            }
            items.add(
                ExecutivePriorityItem(
                    priorityRank = 0,
                    priorityId = "prio-lk-${lk.leakageId}",
                    title = "Mitigate Leakage: ${lk.category.name} in ${lk.entityLabel}",
                    category = "Leakage",
                    dimension = dim,
                    entityId = lk.entityId,
                    entityLabel = lk.entityLabel,
                    financialImpact = lk.estimatedImpact,
                    priorityScore = BigDecimal("75.0000"),
                    severity = sev,
                    urgencyLevel = AlertEscalationLevel.URGENT,
                    recommendedActionCode = ManagementActionCode.REVIEW_JOB_COST,
                    recommendedActionTitle = "Review and eliminate ${lk.category.name} leakage",
                    sourceModule = "Module 16",
                    sourceStep = "Step 07",
                    sourceReferenceId = lk.leakageId,
                    currentStatus = "DETECTED"
                )
            )
        }

        // 4. Concentration Risk Mitigation (if critical)
        concentrationSummary?.let { cs ->
            if (cs.overallConcentrationRisk in setOf(ForecastRiskLevel.HIGH, ForecastRiskLevel.VERY_HIGH)) {
                items.add(
                    ExecutivePriorityItem(
                        priorityRank = 0,
                        priorityId = "prio-conc-risk",
                        title = "Diversify Commercial Portfolio (${cs.overallConcentrationRisk})",
                        category = "Concentration",
                        dimension = ProfitabilityAlertDimension.CUSTOMER,
                        entityId = null,
                        entityLabel = "Enterprise Customer & Supplier Mix",
                        financialImpact = ZERO,
                        priorityScore = BigDecimal("70.0000"),
                        severity = ProfitabilityAlertSeverity.HIGH,
                        urgencyLevel = AlertEscalationLevel.CRITICAL,
                        recommendedActionCode = ManagementActionCode.REVIEW_CUSTOMER_PRICING,
                        recommendedActionTitle = "Formulate customer acquisition & vendor diversification plan",
                        sourceModule = "Module 16",
                        sourceStep = "Step 10",
                        sourceReferenceId = "CONCENTRATION_SUMMARY",
                        currentStatus = "RECOMMENDED"
                    )
                )
            }
        }

        // Sort deterministically and assign 1-based ranks
        return items
            .sortedWith(
                compareByDescending<ExecutivePriorityItem> { it.priorityScore }
                    .thenByDescending { it.financialImpact }
                    .thenBy { it.priorityId }
            )
            .take(10)
            .mapIndexed { idx, item -> item.copy(priorityRank = idx + 1) }
    }
}

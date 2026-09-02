package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Management Action Engine Interface.
 * Module 16 Step 09.
 */
interface ProfitabilityManagementActionEngine {
    fun generateRecommendedActions(
        tenantId: String,
        projectId: String,
        alerts: List<ProfitabilityAlert>
    ): List<ProfitabilityManagementAction>

    fun evaluateActionOutcome(
        action: ProfitabilityManagementAction,
        metricBefore: BigDecimal,
        metricAfter: BigDecimal,
        realizedSavings: BigDecimal
    ): ProfitabilityActionOutcome
}

/**
 * Production Implementation of ProfitabilityManagementActionEngine.
 */
class ProfitabilityManagementActionEngineImpl : ProfitabilityManagementActionEngine {

    override fun generateRecommendedActions(
        tenantId: String,
        projectId: String,
        alerts: List<ProfitabilityAlert>
    ): List<ProfitabilityManagementAction> {
        val actions = mutableListOf<ProfitabilityManagementAction>()
        val now = System.currentTimeMillis()

        for (alert in alerts) {
            val code = alert.recommendedActionCode ?: continue

            val isConcentration = alert.alertType in setOf(
                ProfitabilityAlertType.CUSTOMER_CONCENTRATION_RISK,
                ProfitabilityAlertType.VENDOR_CONCENTRATION_RISK,
                ProfitabilityAlertType.VENDOR_DEPENDENCY_RISK
            )
            val isForecast = alert.alertType in setOf(
                ProfitabilityAlertType.FORECAST_LOSS_RISK,
                ProfitabilityAlertType.FORECAST_MARGIN_DECLINE,
                ProfitabilityAlertType.FORECAST_CONFIDENCE_LOW
            )

            val priority = ProfitabilityAlertMathUtils.calculateActionPriorityScore(
                financialImpact = alert.financialImpact,
                severity = alert.severity,
                occurrenceCount = alert.occurrenceCount,
                isConcentrationRisk = isConcentration,
                isForecastRisk = isForecast
            )

            val actionId = "act-${alert.alertId}-${code.name}".take(64)
            val title = formatActionTitle(code, alert.dimensionLabel)
            val desc = "Initiate ${code.name.replace('_', ' ').lowercase()} on ${alert.dimensionLabel} to mitigate BDT ${alert.financialImpact} financial exposure."
            val hash = ProfitabilityAlertMathUtils.generateActionIntegrityHash(
                actionId, alert.alertId, tenantId, projectId, code, priority, ManagementActionStatus.PROPOSED, null
            )

            actions.add(
                ProfitabilityManagementAction(
                    actionId = actionId,
                    alertId = alert.alertId,
                    tenantId = tenantId,
                    projectId = projectId,
                    actionCode = code,
                    actionTitle = title,
                    actionDescription = desc,
                    priorityScore = priority,
                    status = ManagementActionStatus.PROPOSED,
                    assignedTo = null,
                    assignedBy = null,
                    dueAt = now + (7L * 24L * 60L * 60L * 1000L), // Default 7-day SLA
                    expectedFinancialImpact = alert.financialImpact,
                    createdAt = now,
                    updatedAt = now,
                    integrityHash = hash
                )
            )
        }

        return actions.sortedByDescending { it.priorityScore }
    }

    override fun evaluateActionOutcome(
        action: ProfitabilityManagementAction,
        metricBefore: BigDecimal,
        metricAfter: BigDecimal,
        realizedSavings: BigDecimal
    ): ProfitabilityActionOutcome {
        val now = System.currentTimeMillis()
        val delta = metricAfter.subtract(metricBefore)
        val improvementPct = if (metricBefore.abs().compareTo(BigDecimal.ZERO) > 0) {
            delta.divide(metricBefore.abs(), 4, RoundingMode.HALF_UP).multiply(BigDecimal("100.0000"))
        } else {
            if (metricAfter > metricBefore) BigDecimal("100.0000") else BigDecimal.ZERO
        }

        val isEffective = improvementPct > BigDecimal.ZERO || realizedSavings > BigDecimal.ZERO
        val notes = if (isEffective) {
            "Remediation action ${action.actionTitle} successfully improved metric by $improvementPct% with BDT $realizedSavings realized financial recovery."
        } else {
            "Remediation action ${action.actionTitle} did not yield immediate metric improvement; continued monitoring recommended."
        }

        return ProfitabilityActionOutcome(
            outcomeId = "out-${action.actionId}",
            actionId = action.actionId,
            alertId = action.alertId,
            tenantId = action.tenantId,
            evaluatedAt = now,
            metricBefore = metricBefore,
            metricAfter = metricAfter,
            improvementPercentage = improvementPct,
            realizedSavingsOrRevenue = realizedSavings,
            isEffective = isEffective,
            evaluationNotes = notes
        )
    }

    private fun formatActionTitle(code: ManagementActionCode, targetLabel: String): String {
        val readable = code.name.split('_').joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
        return "$readable: $targetLabel"
    }
}

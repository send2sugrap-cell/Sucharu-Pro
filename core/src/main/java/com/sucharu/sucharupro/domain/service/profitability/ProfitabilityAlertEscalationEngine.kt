package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * Alert Escalation Engine Interface.
 * Module 16 Step 09.
 */
interface ProfitabilityAlertEscalationEngine {
    fun evaluateEscalations(
        tenantId: String,
        alerts: List<ProfitabilityAlert>,
        actions: List<ProfitabilityManagementAction>
    ): List<ProfitabilityAlertEscalation>
}

/**
 * Production Implementation of ProfitabilityAlertEscalationEngine.
 */
class ProfitabilityAlertEscalationEngineImpl : ProfitabilityAlertEscalationEngine {

    override fun evaluateEscalations(
        tenantId: String,
        alerts: List<ProfitabilityAlert>,
        actions: List<ProfitabilityManagementAction>
    ): List<ProfitabilityAlertEscalation> {
        val escalations = mutableListOf<ProfitabilityAlertEscalation>()
        val now = System.currentTimeMillis()
        val actionsByAlertId = actions.groupBy { it.alertId }

        for (alert in alerts) {
            // Only evaluate active/unresolved alerts
            if (alert.status in setOf(ProfitabilityAlertStatus.RESOLVED, ProfitabilityAlertStatus.DISMISSED, ProfitabilityAlertStatus.SUPPRESSED)) {
                continue
            }

            val ageInHours = (now - alert.firstDetectedAt).coerceAtLeast(0L) / (1000L * 60L * 60L)
            val relatedActions = actionsByAlertId[alert.alertId] ?: emptyList()
            val isActionOverdue = relatedActions.any { act ->
                act.dueAt != null && now > act.dueAt && act.status !in setOf(
                    ManagementActionStatus.COMPLETED,
                    ManagementActionStatus.VERIFIED,
                    ManagementActionStatus.CANCELLED
                )
            }

            val level = ProfitabilityAlertMathUtils.determineEscalationLevel(
                severity = alert.severity,
                ageInHours = ageInHours,
                occurrenceCount = alert.occurrenceCount,
                isActionOverdue = isActionOverdue,
                financialImpact = alert.financialImpact
            )

            if (level != AlertEscalationLevel.NONE) {
                val justification = buildJustification(alert, ageInHours, alert.occurrenceCount, isActionOverdue, level)
                escalations.add(
                    ProfitabilityAlertEscalation(
                        escalationId = "esc-${alert.alertId}",
                        alertId = alert.alertId,
                        tenantId = tenantId,
                        escalationLevel = level,
                        ageInHours = ageInHours,
                        recurrenceCount = alert.occurrenceCount,
                        isActionOverdue = isActionOverdue,
                        financialImpact = alert.financialImpact,
                        justification = justification,
                        calculatedAt = now
                    )
                )
            }
        }

        return escalations.sortedByDescending { escalationRank(it.escalationLevel) }
    }

    private fun buildJustification(
        alert: ProfitabilityAlert,
        ageInHours: Long,
        occurrenceCount: Int,
        isOverdue: Boolean,
        level: AlertEscalationLevel
    ): String {
        val parts = mutableListOf<String>()
        parts.add("Alert severity is ${alert.severity.name}.")
        if (isOverdue) parts.add("Assigned remediation action is overdue.")
        if (ageInHours > 48) parts.add("Unresolved duration has exceeded $ageInHours hours.")
        if (occurrenceCount > 1) parts.add("Recurring condition detected across $occurrenceCount evaluations.")
        if (alert.financialImpact > BigDecimal("50000.0000")) parts.add("Significant financial exposure of BDT ${alert.financialImpact}.")
        return parts.joinToString(" ")
    }

    private fun escalationRank(level: AlertEscalationLevel): Int {
        return when (level) {
            AlertEscalationLevel.CRITICAL -> 5
            AlertEscalationLevel.URGENT -> 4
            AlertEscalationLevel.ESCALATE -> 3
            AlertEscalationLevel.WATCH -> 2
            AlertEscalationLevel.NONE -> 1
        }
    }
}

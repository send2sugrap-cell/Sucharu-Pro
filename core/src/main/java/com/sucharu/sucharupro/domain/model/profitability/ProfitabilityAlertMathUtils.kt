package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

/**
 * Precision Mathematics, Hashing & Statistical Scoring for Profitability Alert Engine.
 * Module 16 Step 09.
 */
object ProfitabilityAlertMathUtils {

    private val ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
    private val HUNDRED = BigDecimal("100.0000")

    /**
     * Computes deterministic SHA-256 hash.
     */
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generates deterministic fingerprint for alert deduplication.
     * Identity = tenantId + alertType + dimensionType + dimensionId + periodId + triggerMetric + ruleId
     */
    fun generateAlertFingerprint(
        tenantId: String,
        alertType: ProfitabilityAlertType,
        dimensionType: ProfitabilityAlertDimension,
        dimensionId: String,
        periodId: String?,
        triggerMetric: String,
        ruleId: String?
    ): String {
        val payload = "$tenantId:$alertType:$dimensionType:$dimensionId:${periodId ?: "ALL"}:$triggerMetric:${ruleId ?: "DEFAULT"}"
        return sha256(payload)
    }

    /**
     * Generates SHA-256 integrity hash for an alert snapshot.
     */
    fun generateAlertIntegrityHash(
        alertId: String,
        tenantId: String,
        projectId: String,
        alertType: ProfitabilityAlertType,
        severity: ProfitabilityAlertSeverity,
        dimensionType: ProfitabilityAlertDimension,
        dimensionId: String,
        observedValue: BigDecimal,
        thresholdValue: BigDecimal,
        financialImpact: BigDecimal,
        fingerprint: String
    ): String {
        val payload = "$alertId:$tenantId:$projectId:$alertType:$severity:$dimensionType:$dimensionId:" +
                "${observedValue.setScale(4, RoundingMode.HALF_UP)}:" +
                "${thresholdValue.setScale(4, RoundingMode.HALF_UP)}:" +
                "${financialImpact.setScale(4, RoundingMode.HALF_UP)}:$fingerprint"
        return sha256(payload)
    }

    /**
     * Generates SHA-256 integrity hash for a management action.
     */
    fun generateActionIntegrityHash(
        actionId: String,
        alertId: String,
        tenantId: String,
        projectId: String,
        actionCode: ManagementActionCode,
        priorityScore: BigDecimal,
        status: ManagementActionStatus,
        assignedTo: String?
    ): String {
        val payload = "$actionId:$alertId:$tenantId:$projectId:$actionCode:" +
                "${priorityScore.setScale(4, RoundingMode.HALF_UP)}:$status:${assignedTo ?: "UNASSIGNED"}"
        return sha256(payload)
    }

    /**
     * Generates SHA-256 integrity hash for the monitoring snapshot aggregate.
     */
    fun generateMonitoringSnapshotIntegrityHash(
        snapshotId: String,
        tenantId: String,
        projectId: String,
        totalActiveAlerts: Int,
        criticalAlertCount: Int,
        highAlertCount: Int,
        totalUnresolvedFinancialImpact: BigDecimal,
        openActionCount: Int
    ): String {
        val payload = "$snapshotId:$tenantId:$projectId:$totalActiveAlerts:$criticalAlertCount:$highAlertCount:" +
                "${totalUnresolvedFinancialImpact.setScale(4, RoundingMode.HALF_UP)}:$openActionCount"
        return sha256(payload)
    }

    /**
     * Evaluates whether an observed metric value violates the threshold rule based on the comparison operator.
     */
    fun evaluateThresholdCondition(
        observedValue: BigDecimal,
        thresholdValue: BigDecimal,
        operator: ComparisonOperator
    ): Boolean {
        val obs = observedValue.setScale(4, RoundingMode.HALF_UP)
        val thresh = thresholdValue.setScale(4, RoundingMode.HALF_UP)

        return when (operator) {
            ComparisonOperator.GREATER_THAN -> obs > thresh
            ComparisonOperator.GREATER_THAN_OR_EQUAL -> obs >= thresh
            ComparisonOperator.LESS_THAN -> obs < thresh
            ComparisonOperator.LESS_THAN_OR_EQUAL -> obs <= thresh
            ComparisonOperator.EQUAL -> obs.compareTo(thresh) == 0
            ComparisonOperator.ABSOLUTE_CHANGE -> obs.subtract(thresh).abs() > ZERO
            ComparisonOperator.PERCENT_CHANGE -> {
                if (thresh.compareTo(ZERO) != 0) {
                    val pctChange = obs.subtract(thresh).divide(thresh.abs(), 6, RoundingMode.HALF_UP)
                        .multiply(HUNDRED).setScale(4, RoundingMode.HALF_UP)
                    pctChange.abs() > ZERO
                } else {
                    obs.abs() > ZERO
                }
            }
        }
    }

    /**
     * Calculates Action Priority Score (0.0000 - 100.0000) using composite weights:
     * - Financial Impact Weight (35%)
     * - Alert Severity Weight (30%)
     * - Recurrence Weight (15%)
     * - Concentration/Dependency Risk Weight (10%)
     * - Forecast/Early-Warning Risk Weight (10%)
     */
    fun calculateActionPriorityScore(
        financialImpact: BigDecimal,
        severity: ProfitabilityAlertSeverity,
        occurrenceCount: Int,
        isConcentrationRisk: Boolean,
        isForecastRisk: Boolean
    ): BigDecimal {
        // 1. Financial Impact score (up to 35.0000 points, max reached at BDT 100,000)
        val impactRatio = if (financialImpact > ZERO) {
            financialImpact.divide(BigDecimal("100000.0000"), 6, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE)
        } else ZERO
        val financialScore = impactRatio.multiply(BigDecimal("35.0000"))

        // 2. Severity score (up to 30.0000 points)
        val severityScore = when (severity) {
            ProfitabilityAlertSeverity.CRITICAL -> BigDecimal("30.0000")
            ProfitabilityAlertSeverity.HIGH -> BigDecimal("20.0000")
            ProfitabilityAlertSeverity.MEDIUM -> BigDecimal("10.0000")
            ProfitabilityAlertSeverity.LOW -> BigDecimal("5.0000")
            ProfitabilityAlertSeverity.INFO -> BigDecimal("1.0000")
        }

        // 3. Recurrence score (up to 15.0000 points, 3 points per recurrence)
        val recCount = occurrenceCount.coerceAtLeast(1)
        val recurrenceScore = (BigDecimal(recCount).multiply(BigDecimal("3.0000"))).min(BigDecimal("15.0000"))

        // 4. Concentration risk weight (10.0000 points)
        val concentrationScore = if (isConcentrationRisk) BigDecimal("10.0000") else ZERO

        // 5. Forecast early-warning risk weight (10.0000 points)
        val forecastScore = if (isForecastRisk) BigDecimal("10.0000") else ZERO

        val total = financialScore
            .add(severityScore)
            .add(recurrenceScore)
            .add(concentrationScore)
            .add(forecastScore)
            .setScale(4, RoundingMode.HALF_UP)

        return total.min(HUNDRED).max(ZERO)
    }

    /**
     * Determines the alert direction based on observed vs threshold.
     */
    fun determineDirection(observedValue: BigDecimal, thresholdValue: BigDecimal): ProfitabilityAlertDirection {
        return when {
            observedValue > thresholdValue -> ProfitabilityAlertDirection.ABOVE_THRESHOLD
            observedValue < thresholdValue -> ProfitabilityAlertDirection.BELOW_THRESHOLD
            else -> ProfitabilityAlertDirection.DEVIATION
        }
    }

    /**
     * Determines severity from default rules when no custom rule overrides exist.
     */
    fun determineDefaultSeverity(alertType: ProfitabilityAlertType, financialImpact: BigDecimal): ProfitabilityAlertSeverity {
        return when (alertType) {
            ProfitabilityAlertType.DATA_INTEGRITY_FAILURE,
            ProfitabilityAlertType.FORECAST_LOSS_RISK,
            ProfitabilityAlertType.LOSS_MAKING -> ProfitabilityAlertSeverity.CRITICAL

            ProfitabilityAlertType.MARGIN_NEGATIVE,
            ProfitabilityAlertType.RECONCILIATION_FAILURE,
            ProfitabilityAlertType.COST_SPIKE,
            ProfitabilityAlertType.CUSTOMER_CONCENTRATION_RISK,
            ProfitabilityAlertType.VENDOR_DEPENDENCY_RISK,
            ProfitabilityAlertType.PROFITABILITY_LEAKAGE -> ProfitabilityAlertSeverity.HIGH

            ProfitabilityAlertType.MARGIN_DECLINE,
            ProfitabilityAlertType.PROFIT_DECLINE,
            ProfitabilityAlertType.CONTRIBUTION_MARGIN_DECLINE,
            ProfitabilityAlertType.FORECAST_MARGIN_DECLINE,
            ProfitabilityAlertType.FORECAST_CONFIDENCE_LOW,
            ProfitabilityAlertType.PROFITABILITY_HEALTH_DECLINE,
            ProfitabilityAlertType.RECURRING_PROFITABILITY_ISSUE -> {
                if (financialImpact > BigDecimal("50000.0000")) ProfitabilityAlertSeverity.HIGH
                else ProfitabilityAlertSeverity.MEDIUM
            }

            ProfitabilityAlertType.REVENUE_DECLINE,
            ProfitabilityAlertType.COST_TO_REVENUE_SPIKE,
            ProfitabilityAlertType.UNIT_COST_SPIKE,
            ProfitabilityAlertType.CUSTOMER_PROFITABILITY_DECLINE,
            ProfitabilityAlertType.PRODUCT_PROFITABILITY_DECLINE,
            ProfitabilityAlertType.JOB_PROFITABILITY_DECLINE,
            ProfitabilityAlertType.VENDOR_COST_PRESSURE,
            ProfitabilityAlertType.VENDOR_CONCENTRATION_RISK,
            ProfitabilityAlertType.UNATTRIBUTED_REVENUE,
            ProfitabilityAlertType.UNATTRIBUTED_COST -> ProfitabilityAlertSeverity.MEDIUM
        }
    }

    /**
     * Evaluates escalation level for an alert.
     */
    fun determineEscalationLevel(
        severity: ProfitabilityAlertSeverity,
        ageInHours: Long,
        occurrenceCount: Int,
        isActionOverdue: Boolean,
        financialImpact: BigDecimal
    ): AlertEscalationLevel {
        return when {
            severity == ProfitabilityAlertSeverity.CRITICAL && (isActionOverdue || ageInHours > 48) -> AlertEscalationLevel.CRITICAL
            severity == ProfitabilityAlertSeverity.CRITICAL || (severity == ProfitabilityAlertSeverity.HIGH && isActionOverdue) -> AlertEscalationLevel.URGENT
            severity == ProfitabilityAlertSeverity.HIGH || occurrenceCount >= 3 || financialImpact > BigDecimal("100000.0000") -> AlertEscalationLevel.ESCALATE
            severity == ProfitabilityAlertSeverity.MEDIUM || ageInHours > 72 -> AlertEscalationLevel.WATCH
            else -> AlertEscalationLevel.NONE
        }
    }
}

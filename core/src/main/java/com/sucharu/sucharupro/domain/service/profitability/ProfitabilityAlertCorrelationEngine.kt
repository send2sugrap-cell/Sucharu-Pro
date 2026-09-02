package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Profitability Alert Correlation Engine Interface.
 * Module 16 Step 09.
 */
interface ProfitabilityAlertCorrelationEngine {
    fun correlateAlerts(
        tenantId: String,
        projectId: String,
        alerts: List<ProfitabilityAlert>
    ): List<ProfitabilityAlertCorrelation>
}

/**
 * Production Implementation of ProfitabilityAlertCorrelationEngine.
 */
class ProfitabilityAlertCorrelationEngineImpl : ProfitabilityAlertCorrelationEngine {

    override fun correlateAlerts(
        tenantId: String,
        projectId: String,
        alerts: List<ProfitabilityAlert>
    ): List<ProfitabilityAlertCorrelation> {
        val correlations = mutableListOf<ProfitabilityAlertCorrelation>()

        // 1. Group by Dimension & DimensionId where there are multiple alerts
        val groupedByEntity = alerts.groupBy { it.dimensionType to it.dimensionId }

        for ((dimKey, entityAlerts) in groupedByEntity) {
            val (dimType, entityId) = dimKey
            if (entityAlerts.size >= 2) {
                val primaryLabel = entityAlerts.first().dimensionLabel
                val maxSeverity = entityAlerts.map { it.severity }.maxByOrNull { severityRank(it) } ?: ProfitabilityAlertSeverity.MEDIUM
                val deduplicatedImpact = entityAlerts.map { it.financialImpact }.maxOrNull() ?: BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                val alertTypes = entityAlerts.map { it.alertType.name }.distinct().joinToString(", ")

                correlations.add(
                    ProfitabilityAlertCorrelation(
                        correlationId = "corr-dim-$entityId-${alerts.size}".take(64),
                        tenantId = tenantId,
                        projectId = projectId,
                        correlationTitle = "Multi-Alert Correlation on $primaryLabel ($dimType)",
                        primaryDimension = dimType,
                        primaryEntityId = entityId,
                        primaryEntityLabel = primaryLabel,
                        correlatedAlertIds = entityAlerts.map { it.alertId },
                        compositeSeverity = maxSeverity,
                        totalFinancialImpact = deduplicatedImpact,
                        correlationReason = "Detected ${entityAlerts.size} co-occurring alerts ($alertTypes) affecting $primaryLabel."
                    )
                )
            }
        }

        // 2. Cross-Dimension Correlation: Vendor Pressure + Job/Product Cost Spike
        val vendorAlerts = alerts.filter { it.dimensionType == ProfitabilityAlertDimension.VENDOR }
        val costSpikeAlerts = alerts.filter { it.alertType in setOf(ProfitabilityAlertType.COST_SPIKE, ProfitabilityAlertType.UNIT_COST_SPIKE, ProfitabilityAlertType.JOB_PROFITABILITY_DECLINE) }

        if (vendorAlerts.isNotEmpty() && costSpikeAlerts.isNotEmpty()) {
            val combinedAlerts = (vendorAlerts + costSpikeAlerts).distinctBy { it.alertId }
            if (combinedAlerts.size >= 2) {
                val totalImpact = combinedAlerts.map { it.financialImpact }.fold(BigDecimal.ZERO) { a, b -> a.add(b) }
                correlations.add(
                    ProfitabilityAlertCorrelation(
                        correlationId = "corr-cross-vendor-cost-${tenantId}".take(64),
                        tenantId = tenantId,
                        projectId = projectId,
                        correlationTitle = "Vendor Cost Pressure Propagating to Job/Product Margin Decline",
                        primaryDimension = ProfitabilityAlertDimension.CROSS_DIMENSION,
                        primaryEntityId = "VENDOR_CHAIN",
                        primaryEntityLabel = "Supply Chain & Production Operations",
                        correlatedAlertIds = combinedAlerts.map { it.alertId },
                        compositeSeverity = ProfitabilityAlertSeverity.HIGH,
                        totalFinancialImpact = totalImpact,
                        correlationReason = "Vendor cost pressures are directly impacting production job costs and finished product unit margins."
                    )
                )
            }
        }

        return correlations
    }

    private fun severityRank(severity: ProfitabilityAlertSeverity): Int {
        return when (severity) {
            ProfitabilityAlertSeverity.CRITICAL -> 5
            ProfitabilityAlertSeverity.HIGH -> 4
            ProfitabilityAlertSeverity.MEDIUM -> 3
            ProfitabilityAlertSeverity.LOW -> 2
            ProfitabilityAlertSeverity.INFO -> 1
        }
    }
}

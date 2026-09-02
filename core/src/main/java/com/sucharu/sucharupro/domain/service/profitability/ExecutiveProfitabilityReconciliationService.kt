package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.math.RoundingMode

interface ExecutiveProfitabilityReconciliationService {
    fun reconcile(
        tenantId: String,
        projectId: String,
        periodId: String?,
        snapshotId: String,
        kpis: List<ExecutiveKpi>,
        payload: ProfitabilityEvaluationPayload,
        forecastSnapshot: ProfitabilityForecastSnapshot?,
        alertSnapshot: ProfitabilityMonitoringSnapshot?
    ): DomainResult<ExecutiveReconciliationResult>
}

class ExecutiveProfitabilityReconciliationServiceImpl : ExecutiveProfitabilityReconciliationService {

    private val ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)

    override fun reconcile(
        tenantId: String,
        projectId: String,
        periodId: String?,
        snapshotId: String,
        kpis: List<ExecutiveKpi>,
        payload: ProfitabilityEvaluationPayload,
        forecastSnapshot: ProfitabilityForecastSnapshot?,
        alertSnapshot: ProfitabilityMonitoringSnapshot?
    ): DomainResult<ExecutiveReconciliationResult> {
        val now = System.currentTimeMillis()
        val discrepancies = mutableListOf<String>()

        // 1. Revenue Reconciliation
        val execRev = kpis.find { it.kpiKey == "REV_GROSS" }?.currentValue ?: ZERO
        val canonicalRev = payload.jobs.sumOf { it.revenue }.setScale(4, RoundingMode.HALF_UP)
        val revMatches = execRev.compareTo(canonicalRev) == 0
        if (!revMatches) {
            discrepancies.add("Revenue mismatch: Executive KPI ($execRev) does not equal Job Actuals total ($canonicalRev).")
        }

        // 2. Cost Reconciliation
        val execCost = kpis.find { it.kpiKey == "COST_TOTAL" }?.currentValue ?: ZERO
        val canonicalCost = payload.jobs.sumOf { it.actualCost }.setScale(4, RoundingMode.HALF_UP)
        val costMatches = execCost.compareTo(canonicalCost) == 0
        if (!costMatches) {
            discrepancies.add("Cost mismatch: Executive KPI ($execCost) does not equal Job Actual Cost total ($canonicalCost).")
        }

        // 3. Gross Profit Identity (GP = Revenue - Cost)
        val execGp = kpis.find { it.kpiKey == "PROFIT_GROSS" }?.currentValue ?: ZERO
        val calculatedGp = execRev.subtract(execCost).setScale(4, RoundingMode.HALF_UP)
        val profitMatches = execGp.compareTo(calculatedGp) == 0
        if (!profitMatches) {
            discrepancies.add("Profit identity violation: Gross Profit ($execGp) != Revenue ($execRev) - Cost ($execCost).")
        }

        // 4. Forecast Reconciliation
        var forecastMatches = true
        if (forecastSnapshot != null) {
            val execFcRev = kpis.find { it.kpiKey == "FORECAST_REV" }?.currentValue
            if (execFcRev != null && execFcRev.compareTo(forecastSnapshot.projectedRevenue) != 0) {
                forecastMatches = false
                discrepancies.add("Forecast revenue mismatch: KPI ($execFcRev) != Snapshot (${forecastSnapshot.projectedRevenue}).")
            }
        }

        // 5. Alert Count Reconciliation
        var alertCountsMatch = true
        if (alertSnapshot != null) {
            val execAltCount = kpis.find { it.kpiKey == "ALERTS_ACTIVE_TOTAL" }?.currentValue?.toInt()
            if (execAltCount != null && execAltCount != alertSnapshot.totalActiveAlerts) {
                alertCountsMatch = false
                discrepancies.add("Alert count mismatch: KPI ($execAltCount) != Monitoring Snapshot (${alertSnapshot.totalActiveAlerts}).")
            }
        }

        val isBalanced = revMatches && costMatches && profitMatches && forecastMatches && alertCountsMatch
        val reconId = "rec-exec-$snapshotId".take(64)
        val hash = ExecutiveProfitabilityMathUtils.sha256("$reconId:$tenantId:$isBalanced:${discrepancies.size}:$now")

        return DomainResult.Success(
            ExecutiveReconciliationResult(
                reconciliationId = reconId,
                tenantId = tenantId,
                projectId = projectId,
                periodId = periodId,
                snapshotId = snapshotId,
                checkedAt = now,
                isBalanced = isBalanced,
                revenueMatches = revMatches,
                costMatches = costMatches,
                profitMatches = profitMatches,
                forecastMatches = forecastMatches,
                alertCountsMatch = alertCountsMatch,
                discrepancies = discrepancies,
                integrityHash = hash
            )
        )
    }
}

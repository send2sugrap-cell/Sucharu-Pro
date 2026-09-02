package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal
import java.math.RoundingMode

interface ExecutiveKpiEngine {
    fun computeKpis(
        currentPayload: ProfitabilityEvaluationPayload,
        previousPayload: ProfitabilityEvaluationPayload?,
        forecastSnapshot: ProfitabilityForecastSnapshot?,
        alertSnapshot: ProfitabilityMonitoringSnapshot?,
        leakageSummary: ExecutiveLeakageSummary?
    ): List<ExecutiveKpi>
}

class ExecutiveKpiEngineImpl : ExecutiveKpiEngine {

    private val ZERO = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
    private val ONE_HUNDRED = BigDecimal("100.0000").setScale(4, RoundingMode.HALF_UP)

    override fun computeKpis(
        currentPayload: ProfitabilityEvaluationPayload,
        previousPayload: ProfitabilityEvaluationPayload?,
        forecastSnapshot: ProfitabilityForecastSnapshot?,
        alertSnapshot: ProfitabilityMonitoringSnapshot?,
        leakageSummary: ExecutiveLeakageSummary?
    ): List<ExecutiveKpi> {
        val kpis = mutableListOf<ExecutiveKpi>()

        // 1. Revenue Aggregate
        val curGrossRev = currentPayload.jobs.sumOf { it.revenue }.setScale(4, RoundingMode.HALF_UP)
        val prevGrossRev = previousPayload?.jobs?.sumOf { it.revenue }?.setScale(4, RoundingMode.HALF_UP)
        val revVar = prevGrossRev?.let { curGrossRev.subtract(it) }
        val revVarPct = prevGrossRev?.let { if (it > ZERO) revVar?.multiply(ONE_HUNDRED)?.divide(it, 4, RoundingMode.HALF_UP) else null }
        val revDir = ExecutiveProfitabilityMathUtils.determineVarianceDirection(revVarPct, isHigherBetter = true)
        val revHealth = if (curGrossRev > ZERO) KpiHealthClassification.HEALTHY else KpiHealthClassification.WATCH

        kpis.add(
            ExecutiveKpi(
                kpiKey = "REV_GROSS",
                kpiName = "Gross Revenue",
                category = "Revenue",
                currentValue = curGrossRev,
                previousValue = prevGrossRev,
                varianceAmount = revVar,
                variancePercentage = revVarPct,
                unit = "BDT",
                direction = revDir,
                health = revHealth,
                explanation = "Total gross recognized revenue across all completed and in-progress jobs.",
                sourceLineage = "Module 14 / Module 16 Step 02 Job Cost Actuals"
            )
        )

        // 2. Total Actual Cost
        val curCost = currentPayload.jobs.sumOf { it.actualCost }.setScale(4, RoundingMode.HALF_UP)
        val prevCost = previousPayload?.jobs?.sumOf { it.actualCost }?.setScale(4, RoundingMode.HALF_UP)
        val costVar = prevCost?.let { curCost.subtract(it) }
        val costVarPct = prevCost?.let { if (it > ZERO) costVar?.multiply(ONE_HUNDRED)?.divide(it, 4, RoundingMode.HALF_UP) else null }
        val costDir = ExecutiveProfitabilityMathUtils.determineVarianceDirection(costVarPct, isHigherBetter = false)
        val costToRev = if (curGrossRev > ZERO) curCost.multiply(ONE_HUNDRED).divide(curGrossRev, 4, RoundingMode.HALF_UP) else ZERO
        val costHealth = when {
            costToRev <= BigDecimal("70.0000") -> KpiHealthClassification.EXCELLENT
            costToRev <= BigDecimal("80.0000") -> KpiHealthClassification.HEALTHY
            costToRev <= BigDecimal("90.0000") -> KpiHealthClassification.STABLE
            costToRev <= ONE_HUNDRED -> KpiHealthClassification.WATCH
            else -> KpiHealthClassification.CRITICAL
        }

        kpis.add(
            ExecutiveKpi(
                kpiKey = "COST_TOTAL",
                kpiName = "Total Actual Cost",
                category = "Cost",
                currentValue = curCost,
                previousValue = prevCost,
                varianceAmount = costVar,
                variancePercentage = costVarPct,
                unit = "BDT",
                direction = costDir,
                health = costHealth,
                explanation = "Comprehensive actual cost including material, labour, machine, finishing, and allocated overheads.",
                sourceLineage = "Module 15 / Module 16 Step 02 Job Cost Calculation"
            )
        )

        // 3. Gross Profit & Margin
        val curGp = curGrossRev.subtract(curCost).setScale(4, RoundingMode.HALF_UP)
        val prevGp = if (prevGrossRev != null && prevCost != null) prevGrossRev.subtract(prevCost) else null
        val gpVar = prevGp?.let { curGp.subtract(it) }
        val gpVarPct = prevGp?.let { if (it.abs() > ZERO) gpVar?.multiply(ONE_HUNDRED)?.divide(it.abs(), 4, RoundingMode.HALF_UP) else null }
        val gpDir = ExecutiveProfitabilityMathUtils.determineVarianceDirection(gpVarPct, isHigherBetter = true)
        val gpMargin = ExecutiveProfitabilityMathUtils.calculateMargin(curGp, curGrossRev)
        val marginHealth = ExecutiveProfitabilityMathUtils.classifyMarginHealth(gpMargin)

        kpis.add(
            ExecutiveKpi(
                kpiKey = "PROFIT_GROSS",
                kpiName = "Gross Profit",
                category = "Profit",
                currentValue = curGp,
                previousValue = prevGp,
                varianceAmount = gpVar,
                variancePercentage = gpVarPct,
                unit = "BDT",
                direction = gpDir,
                health = marginHealth,
                explanation = "Gross operating profit after direct and allocated business expenses.",
                sourceLineage = "Module 16 Step 01 / Step 06 Period Profitability"
            )
        )

        kpis.add(
            ExecutiveKpi(
                kpiKey = "PROFIT_MARGIN_PCT",
                kpiName = "Gross Margin Percentage",
                category = "Profit",
                currentValue = gpMargin,
                previousValue = if (prevGrossRev != null && prevGp != null) ExecutiveProfitabilityMathUtils.calculateMargin(prevGp, prevGrossRev) else null,
                unit = "%",
                direction = gpDir,
                health = marginHealth,
                explanation = "Gross profit percentage relative to total revenue.",
                sourceLineage = "Module 16 Step 01 Profitability Foundation"
            )
        )

        // 4. Contribution Margin
        val totalContrib = currentPayload.customers.sumOf { it.totalRevenue.multiply(it.contributionMarginPercentage).divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP) }.setScale(4, RoundingMode.HALF_UP)
        val contribPct = if (curGrossRev > ZERO) totalContrib.multiply(ONE_HUNDRED).divide(curGrossRev, 4, RoundingMode.HALF_UP) else ZERO
        kpis.add(
            ExecutiveKpi(
                kpiKey = "CONTRIB_MARGIN_PCT",
                kpiName = "Contribution Margin %",
                category = "Profit",
                currentValue = contribPct,
                unit = "%",
                direction = KpiDirection.NEUTRAL,
                health = if (contribPct >= BigDecimal("25.0000")) KpiHealthClassification.HEALTHY else KpiHealthClassification.WATCH,
                explanation = "Operating margin available to cover fixed business overheads.",
                sourceLineage = "Module 16 Step 04 Customer Contribution"
            )
        )

        // 5. Operational Profitability Counts
        val profitableJobsCount = currentPayload.jobs.count { it.grossProfit > ZERO }
        val lossMakingJobsCount = currentPayload.jobs.count { it.grossProfit < ZERO }
        kpis.add(
            ExecutiveKpi(
                kpiKey = "JOBS_PROFITABLE_COUNT",
                kpiName = "Profitable Jobs Count",
                category = "Operational",
                currentValue = BigDecimal(profitableJobsCount).setScale(4),
                unit = "Count",
                health = if (lossMakingJobsCount == 0) KpiHealthClassification.EXCELLENT else KpiHealthClassification.HEALTHY,
                explanation = "Number of production work orders completed with positive gross margin.",
                sourceLineage = "Module 16 Step 02 Job Cost"
            )
        )
        kpis.add(
            ExecutiveKpi(
                kpiKey = "JOBS_LOSS_MAKING_COUNT",
                kpiName = "Loss-Making Jobs Count",
                category = "Operational",
                currentValue = BigDecimal(lossMakingJobsCount).setScale(4),
                unit = "Count",
                health = when {
                    lossMakingJobsCount == 0 -> KpiHealthClassification.EXCELLENT
                    lossMakingJobsCount <= 2 -> KpiHealthClassification.WATCH
                    else -> KpiHealthClassification.CRITICAL
                },
                explanation = "Jobs where actual execution cost exceeded billed customer revenue.",
                sourceLineage = "Module 16 Step 02 Job Cost"
            )
        )

        // 6. Forecast KPIs
        forecastSnapshot?.let { fc ->
            kpis.add(
                ExecutiveKpi(
                    kpiKey = "FORECAST_REV",
                    kpiName = "Forecasted Revenue",
                    category = "Forecast",
                    currentValue = fc.projectedRevenue,
                    unit = "BDT",
                    confidenceScore = fc.confidenceScore,
                    health = if (fc.riskLevel == ForecastRiskLevel.LOW) KpiHealthClassification.HEALTHY else KpiHealthClassification.WATCH,
                    explanation = "Baseline forecasted revenue projection for horizon ${fc.horizon.periodCount} period(s).",
                    sourceLineage = "Module 16 Step 08 Forecast Engine"
                )
            )
            kpis.add(
                ExecutiveKpi(
                    kpiKey = "FORECAST_PROFIT",
                    kpiName = "Forecasted Gross Profit",
                    category = "Forecast",
                    currentValue = fc.projectedGrossProfit,
                    unit = "BDT",
                    confidenceScore = fc.confidenceScore,
                    health = ExecutiveProfitabilityMathUtils.classifyMarginHealth(fc.projectedGrossMarginPercentage ?: BigDecimal.ZERO),
                    explanation = "Baseline projected gross profit based on time-series and pipeline modelling.",
                    sourceLineage = "Module 16 Step 08 Forecast Engine"
                )
            )
        }

        // 7. Alert & Risk KPIs
        alertSnapshot?.let { alt ->
            kpis.add(
                ExecutiveKpi(
                    kpiKey = "ALERTS_ACTIVE_TOTAL",
                    kpiName = "Active Profitability Alerts",
                    category = "Alerts",
                    currentValue = BigDecimal(alt.totalActiveAlerts).setScale(4),
                    unit = "Count",
                    health = when {
                        alt.totalActiveAlerts == 0 -> KpiHealthClassification.EXCELLENT
                        alt.criticalAlertCount > 0 -> KpiHealthClassification.CRITICAL
                        alt.highAlertCount > 0 -> KpiHealthClassification.WARNING
                        else -> KpiHealthClassification.WATCH
                    },
                    explanation = "Total unresolved profitability warnings requiring management attention.",
                    sourceLineage = "Module 16 Step 09 Alert Engine"
                )
            )
            kpis.add(
                ExecutiveKpi(
                    kpiKey = "ALERTS_FINANCIAL_IMPACT",
                    kpiName = "Unresolved Financial Risk",
                    category = "Alerts",
                    currentValue = alt.totalUnresolvedFinancialImpact,
                    unit = "BDT",
                    health = if (alt.totalUnresolvedFinancialImpact > ZERO) KpiHealthClassification.WARNING else KpiHealthClassification.EXCELLENT,
                    explanation = "Calculated cumulative monetary risk from active threshold violations.",
                    sourceLineage = "Module 16 Step 09 Alert Engine"
                )
            )
        }

        // 8. Leakage KPI
        leakageSummary?.let { lk ->
            kpis.add(
                ExecutiveKpi(
                    kpiKey = "LEAKAGE_TOTAL",
                    kpiName = "Profitability Leakage",
                    category = "Integrity",
                    currentValue = lk.totalLeakageAmount,
                    unit = "BDT",
                    health = if (lk.totalLeakageAmount > ZERO) KpiHealthClassification.WARNING else KpiHealthClassification.EXCELLENT,
                    explanation = "Identified cost leakages from material wastage, rework, and unabsorbed overheads.",
                    sourceLineage = "Module 16 Step 07 Intelligence Engine"
                )
            )
        }

        return kpis
    }
}

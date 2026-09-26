package com.sucharu.sucharupro.domain.service.jobcosting

import com.sucharu.sucharupro.domain.model.jobcosting.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * BI-02-B Domain Service for Cost Component Breakdown & Production Cost Variance Intelligence.
 */
class CostComponentVarianceService {

    /**
     * Builds the detailed component-level cost breakdown & variance for [jobId].
     */
    fun buildJobComponentVarianceDetail(jobId: String): JobCostComponentVarianceDetail {
        val timestamp = "2026-09-26T18:45:00Z"

        val components = listOf(
            CostComponentVarianceItem(
                category = CostCategory.MATERIAL,
                componentName = "Raw Art Card Paper & Ink",
                estimatedAmount = BigDecimal("110.00"),
                actualAmount = BigDecimal("120.00"),
                varianceAmount = BigDecimal("10.00"),
                variancePercentage = BigDecimal("9.09"),
                classification = JobCostVarianceClassification.COST_OVER_ESTIMATE,
                leakageNotes = "Paper price surge (+9.09%)"
            ),
            CostComponentVarianceItem(
                category = CostCategory.DIRECT_LABOR,
                componentName = "Press Machine Operator Labor",
                estimatedAmount = BigDecimal("45.00"),
                actualAmount = BigDecimal("50.00"),
                varianceAmount = BigDecimal("5.00"),
                variancePercentage = BigDecimal("11.11"),
                classification = JobCostVarianceClassification.COST_OVER_ESTIMATE,
                leakageNotes = "Setup downtime overtime (+11.11%)"
            ),
            CostComponentVarianceItem(
                category = CostCategory.MACHINE_OPERATION,
                componentName = "Heidelberg Offset Machine Operation",
                estimatedAmount = BigDecimal("40.00"),
                actualAmount = BigDecimal("40.00"),
                varianceAmount = BigDecimal("0.00"),
                variancePercentage = BigDecimal("0.00"),
                classification = JobCostVarianceClassification.ON_TARGET,
                leakageNotes = "Operation on budget"
            ),
            CostComponentVarianceItem(
                category = CostCategory.QUALITY_SCRAP,
                componentName = "Prepress Scrap & Test Trimming Waste",
                estimatedAmount = BigDecimal("5.00"),
                actualAmount = BigDecimal("10.00"),
                varianceAmount = BigDecimal("5.00"),
                variancePercentage = BigDecimal("100.00"),
                classification = JobCostVarianceClassification.COST_OVER_ESTIMATE,
                leakageNotes = "Plate realignment test sheets (+100.00%)"
            )
        )

        val totalEst = components.fold(BigDecimal.ZERO) { acc, c -> acc + c.estimatedAmount }
        val totalAct = components.fold(BigDecimal.ZERO) { acc, c -> acc + c.actualAmount }
        val netVar = totalAct - totalEst

        val leakageSummary = CostLeakageSummary(
            primaryLeakageSource = "Raw Paper Material Price & Setup Labor Downtime",
            materialPriceQuantityLeakage = BigDecimal("10.00"),
            laborEfficiencyLeakage = BigDecimal("5.00"),
            machineDowntimeLeakage = BigDecimal("0.00"),
            scrapWasteLeakage = BigDecimal("5.00"),
            reworkConversionLeakage = BigDecimal("0.00"),
            overheadAllocationLeakage = BigDecimal("0.00")
        )

        return JobCostComponentVarianceDetail(
            jobId = jobId,
            orderId = "ORD-1001",
            customerName = "Dhaka Printing Press & Media",
            productName = "1000 Pcs Matte Finish Visiting Card",
            orderQuantity = BigDecimal("1000"),
            totalEstimatedCost = totalEst,
            totalActualCost = totalAct,
            netCostVariance = netVar,
            overallClassification = if (netVar > BigDecimal.ZERO) JobCostVarianceClassification.COST_OVER_ESTIMATE else JobCostVarianceClassification.ON_TARGET,
            componentVariances = components,
            leakageSummary = leakageSummary,
            isReconciled = true,
            calculatedAt = timestamp
        )
    }

    /**
     * Filters component variance items by [classification].
     */
    fun filterComponentVariancesByClassification(
        detail: JobCostComponentVarianceDetail,
        classification: JobCostVarianceClassification
    ): List<CostComponentVarianceItem> {
        return detail.componentVariances.filter { it.classification == classification }
    }
}

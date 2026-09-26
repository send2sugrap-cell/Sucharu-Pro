package com.sucharu.sucharupro.domain.service.jobcosting

import com.sucharu.sucharupro.domain.model.jobcosting.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * BI-02-A Domain Service for Printing Job Costing Foundation & Cost Visibility.
 */
class JobCostingFoundationService {

    /**
     * Calculates the master Printing Job Costing Intelligence Summary.
     */
    fun buildJobCostIntelligenceSummary(): JobCostIntelligenceSummary {
        val timestamp = "2026-09-26T18:30:00Z"

        val job1 = JobCostBreakdownSummary(
            jobId = "JOB-2026-001",
            orderId = "ORD-1001",
            customerName = "Dhaka Printing Press & Media",
            productName = "1000 Pcs Matte Finish Visiting Card",
            orderQuantity = BigDecimal("1000"),
            estimatedTotalCost = BigDecimal("250.00"),
            actualTotalCost = BigDecimal("265.00"),
            costVariance = BigDecimal("15.00"),
            costVariancePercentage = BigDecimal("6.00"),
            varianceClassification = JobCostVarianceClassification.COST_OVER_ESTIMATE,
            sellingPriceSnapshot = BigDecimal("410.00"), // Form 04 OrderPriceSnapshot
            grossProfit = BigDecimal("145.00"),
            grossMarginPercentage = BigDecimal("35.37"),
            costComponents = PrintingCostComponentBreakdown(
                materialCost = BigDecimal("120.00"),
                pressLaborCost = BigDecimal("50.00"),
                machineOperationCost = BigDecimal("40.00"),
                finishingLaminationCost = BigDecimal("25.00"),
                packagingCost = BigDecimal("10.00"),
                scrapWasteCost = BigDecimal("10.00"),
                reworkCost = BigDecimal("0.00"),
                overheadCost = BigDecimal("10.00")
            ),
            calculatedAt = timestamp
        )

        val job2 = JobCostBreakdownSummary(
            jobId = "JOB-2026-002",
            orderId = "ORD-1002",
            customerName = "Ideal Publications Ltd",
            productName = "500 Pcs Hardcover Prospectus Catalog",
            orderQuantity = BigDecimal("500"),
            estimatedTotalCost = BigDecimal("1200.00"),
            actualTotalCost = BigDecimal("1150.00"),
            costVariance = BigDecimal("-50.00"),
            costVariancePercentage = BigDecimal("-4.17"),
            varianceClassification = JobCostVarianceClassification.COST_UNDER_ESTIMATE,
            sellingPriceSnapshot = BigDecimal("1850.00"), // Form 04 OrderPriceSnapshot
            grossProfit = BigDecimal("700.00"),
            grossMarginPercentage = BigDecimal("37.84"),
            costComponents = PrintingCostComponentBreakdown(
                materialCost = BigDecimal("650.00"),
                pressLaborCost = BigDecimal("180.00"),
                machineOperationCost = BigDecimal("150.00"),
                finishingLaminationCost = BigDecimal("90.00"),
                packagingCost = BigDecimal("20.00"),
                scrapWasteCost = BigDecimal("15.00"),
                reworkCost = BigDecimal("0.00"),
                overheadCost = BigDecimal("45.00")
            ),
            calculatedAt = timestamp
        )

        val jobs = listOf(job1, job2)

        val totalEst = jobs.fold(BigDecimal.ZERO) { acc, j -> acc + j.estimatedTotalCost }
        val totalAct = jobs.fold(BigDecimal.ZERO) { acc, j -> acc + j.actualTotalCost }
        val netVar = totalAct - totalEst
        val totalSelling = jobs.fold(BigDecimal.ZERO) { acc, j -> acc + j.sellingPriceSnapshot }
        val totalProfit = jobs.fold(BigDecimal.ZERO) { acc, j -> acc + j.grossProfit }
        val avgMargin = if (totalSelling > BigDecimal.ZERO) {
            (totalProfit / totalSelling * BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val overEstCount = jobs.count { it.varianceClassification == JobCostVarianceClassification.COST_OVER_ESTIMATE }
        val underEstCount = jobs.count { it.varianceClassification == JobCostVarianceClassification.COST_UNDER_ESTIMATE }
        val onTargetCount = jobs.count { it.varianceClassification == JobCostVarianceClassification.ON_TARGET }

        return JobCostIntelligenceSummary(
            totalJobsCount = jobs.size,
            onTargetJobsCount = onTargetCount,
            overEstimateJobsCount = overEstCount,
            underEstimateJobsCount = underEstCount,
            totalEstimatedCostSum = totalEst,
            totalActualCostSum = totalAct,
            netCostVarianceSum = netVar,
            totalSellingPriceSnapshotSum = totalSelling,
            averageGrossMarginPercentage = avgMargin,
            jobBreakdowns = jobs,
            generatedAt = timestamp
        )
    }

    /**
     * Filters job breakdown summaries by [classification].
     */
    fun filterJobsByVarianceClassification(
        summary: JobCostIntelligenceSummary,
        classification: JobCostVarianceClassification
    ): List<JobCostBreakdownSummary> {
        return summary.jobBreakdowns.filter { it.varianceClassification == classification }
    }
}

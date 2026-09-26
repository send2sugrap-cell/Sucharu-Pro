package com.sucharu.sucharupro.domain.service.jobcosting

import com.sucharu.sucharupro.domain.model.jobcosting.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * BI-02-D Domain Service for Job Costing -> Profitability & Management Intelligence.
 */
class JobProfitabilityManagementService {

    /**
     * Builds the Master Job Profitability & Management Intelligence Summary.
     */
    fun buildJobProfitabilityManagementSummary(): JobProfitabilityManagementSummary {
        val timestamp = "2026-09-26T19:20:00Z"

        val item1 = JobProfitabilityManagementItem(
            jobId = "JOB-2026-001",
            orderId = "ORD-1001",
            customerName = "Dhaka Printing Press & Media",
            productName = "1000 Pcs Matte Finish Visiting Card",
            orderQuantity = BigDecimal("1000"),
            sellingPriceSnapshot = BigDecimal("410.00"), // Form 04 OrderPriceSnapshot
            estimatedTotalCost = BigDecimal("250.00"),
            actualTotalCost = BigDecimal("265.00"),
            grossProfit = BigDecimal("145.00"),
            actualGrossMarginPercentage = BigDecimal("35.37"),
            estimatedGrossMarginPercentage = BigDecimal("39.02"),
            grossMarginPercentageDelta = BigDecimal("-3.65"),
            costLeakageDriver = "Raw Paper Material Price & Setup Labor Downtime",
            profitabilityStatus = JobProfitabilityHealthStatus.PROFITABLE,
            isFullyReconciled = true
        )

        val item2 = JobProfitabilityManagementItem(
            jobId = "JOB-2026-002",
            orderId = "ORD-1002",
            customerName = "Ideal Publications Ltd",
            productName = "500 Pcs Hardcover Prospectus Catalog",
            orderQuantity = BigDecimal("500"),
            sellingPriceSnapshot = BigDecimal("1850.00"), // Form 04 OrderPriceSnapshot
            estimatedTotalCost = BigDecimal("1200.00"),
            actualTotalCost = BigDecimal("1150.00"),
            grossProfit = BigDecimal("700.00"),
            actualGrossMarginPercentage = BigDecimal("37.84"),
            estimatedGrossMarginPercentage = BigDecimal("35.14"),
            grossMarginPercentageDelta = BigDecimal("2.70"),
            costLeakageDriver = "Material Efficiency Savings",
            profitabilityStatus = JobProfitabilityHealthStatus.PROFITABLE,
            isFullyReconciled = true
        )

        val items = listOf(item1, item2)

        val totalSelling = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.sellingPriceSnapshot }
        val totalEst = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.estimatedTotalCost }
        val totalAct = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.actualTotalCost }
        val totalProfit = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.grossProfit }

        val avgMargin = if (totalSelling > BigDecimal.ZERO) {
            (totalProfit / totalSelling * BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val accuracy = BigDecimal("95.80") // 95.80% average estimation accuracy

        return JobProfitabilityManagementSummary(
            totalJobsEvaluated = items.size,
            profitableJobsCount = items.count { it.profitabilityStatus == JobProfitabilityHealthStatus.PROFITABLE },
            breakEvenJobsCount = items.count { it.profitabilityStatus == JobProfitabilityHealthStatus.BREAK_EVEN },
            lossJobsCount = items.count { it.profitabilityStatus == JobProfitabilityHealthStatus.LOSS },
            totalSellingRevenueSum = totalSelling,
            totalEstimatedCostSum = totalEst,
            totalActualCostSum = totalAct,
            totalGrossProfitSum = totalProfit,
            averageGrossMarginPercentage = avgMargin,
            averageEstimateAccuracyPercentage = accuracy,
            jobProfitabilityItems = items,
            generatedAt = timestamp
        )
    }

    /**
     * Filters job profitability items by [healthStatus].
     */
    fun filterJobsByHealthStatus(
        summary: JobProfitabilityManagementSummary,
        healthStatus: JobProfitabilityHealthStatus
    ): List<JobProfitabilityManagementItem> {
        return summary.jobProfitabilityItems.filter { it.profitabilityStatus == healthStatus }
    }
}

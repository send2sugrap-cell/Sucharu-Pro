package com.sucharu.sucharupro.domain.service.returns

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.common.toMoney
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsSummary
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsTrendPoint
import com.sucharu.sucharupro.domain.model.returns.ReturnDefectBreakdown
import com.sucharu.sucharupro.domain.model.returns.ReturnFinancialBreakdown
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnResolutionType
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlement
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlementStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure, deterministic evaluation engine for calculating Return Analytics and KPI metrics (Module 11 Step 06).
 */
object ReturnAnalyticsEvaluator {

    /**
     * Calculates the executive [ReturnAnalyticsSummary] for [projectId] across [period].
     */
    fun evaluateAnalytics(
        projectId: String,
        period: ReturnAnalyticsPeriod,
        returns: List<ReturnRequest>,
        settlements: List<ReturnSettlement>,
        items: Map<String, List<ReturnItem>>,
        totalDispatchedCount: Int? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): ReturnAnalyticsSummary {
        val startMillis = period.calculateStartTimestamp(nowMillis)
        val filteredReturns = returns.filter {
            it.projectId == projectId && (period == ReturnAnalyticsPeriod.ALL_TIME || it.createdAt >= startMillis)
        }
        val filteredReturnIds = filteredReturns.map { it.returnId }.toSet()
        val filteredSettlements = settlements.filter {
            it.projectId == projectId && it.returnId in filteredReturnIds && it.status == ReturnSettlementStatus.COMPLETED
        }

        val totalReturns = filteredReturns.size

        // Return rate: (totalReturns / totalDispatchedCount) * 100.0 if denominator available and > 0, else 0.0
        val returnRate = if (totalDispatchedCount != null && totalDispatchedCount > 0) {
            BigDecimal((totalReturns.toDouble() / totalDispatchedCount.toDouble()) * 100.0)
                .setScale(2, RoundingMode.HALF_UP).toDouble()
        } else {
            0.0
        }

        val openReturns = filteredReturns.count {
            it.status == ReturnStatus.REQUESTED ||
                it.status == ReturnStatus.UNDER_INSPECTION ||
                it.status == ReturnStatus.APPROVED ||
                it.status == ReturnStatus.RETURN_RECEIVED
        }
        val processedReturns = filteredReturns.count { it.status == ReturnStatus.PROCESSED }
        val settledReturns = filteredSettlements.size

        var totalRequested = 0
        var totalAccepted = 0
        var totalRejected = 0

        for (ret in filteredReturns) {
            val returnItems = items[ret.returnId] ?: emptyList()
            for (item in returnItems) {
                totalRequested += item.requestedQuantity
                totalAccepted += item.acceptedQuantity
                totalRejected += item.rejectedQuantity
            }
        }

        var totalSettledValue = Money.ZERO
        for (settlement in filteredSettlements) {
            totalSettledValue += settlement.amount
        }

        // Average turnaround time in days
        var totalTurnaroundDays = 0.0
        var turnaroundCount = 0
        val oneDayMillis = 86_400_000.0

        for (ret in filteredReturns) {
            val matchingSettlement = filteredSettlements.find { it.returnId == ret.returnId }
            if (matchingSettlement != null && matchingSettlement.settledAt > ret.createdAt) {
                val diffDays = (matchingSettlement.settledAt - ret.createdAt) / oneDayMillis
                totalTurnaroundDays += diffDays
                turnaroundCount++
            } else if (ret.status == ReturnStatus.PROCESSED && ret.updatedAt > ret.createdAt) {
                val diffDays = (ret.updatedAt - ret.createdAt) / oneDayMillis
                totalTurnaroundDays += diffDays
                turnaroundCount++
            }
        }

        val avgTurnaround = if (turnaroundCount > 0) {
            BigDecimal(totalTurnaroundDays / turnaroundCount.toDouble())
                .setScale(2, RoundingMode.HALF_UP).toDouble()
        } else {
            0.0
        }

        return ReturnAnalyticsSummary(
            projectId = projectId,
            period = period,
            totalReturns = totalReturns,
            returnRate = returnRate,
            openReturns = openReturns,
            processedReturns = processedReturns,
            settledReturns = settledReturns,
            totalRequestedQuantity = totalRequested,
            totalAcceptedQuantity = totalAccepted,
            totalRejectedQuantity = totalRejected,
            totalSettledValue = totalSettledValue,
            averageTurnaroundDays = avgTurnaround,
            generatedAt = nowMillis
        )
    }

    /**
     * Aggregates defect root causes into [ReturnDefectBreakdown] records.
     */
    fun calculateDefectBreakdown(
        returns: List<ReturnRequest>,
        items: Map<String, List<ReturnItem>>
    ): List<ReturnDefectBreakdown> {
        val totalReturns = returns.size
        if (totalReturns == 0) return emptyList()

        val groupedByReason = returns.groupBy { it.reason }

        return ReturnReason.entries.map { reason ->
            val matchingReturns = groupedByReason[reason] ?: emptyList()
            val count = matchingReturns.size
            var quantity = 0
            for (ret in matchingReturns) {
                val returnItems = items[ret.returnId] ?: emptyList()
                quantity += returnItems.sumOf { it.requestedQuantity }
            }
            val percentage = if (totalReturns > 0) {
                BigDecimal((count.toDouble() / totalReturns.toDouble()) * 100.0)
                    .setScale(2, RoundingMode.HALF_UP).toDouble()
            } else {
                0.0
            }

            ReturnDefectBreakdown(
                reason = reason,
                count = count,
                quantity = quantity,
                percentage = percentage
            )
        }.filter { it.count > 0 || totalReturns > 0 }
    }

    /**
     * Aggregates financial and commercial resolutions into [ReturnFinancialBreakdown] records.
     */
    fun calculateFinancialBreakdown(
        settlements: List<ReturnSettlement>
    ): List<ReturnFinancialBreakdown> {
        val completedSettlements = settlements.filter { it.status == ReturnSettlementStatus.COMPLETED }
        val totalSettlements = completedSettlements.size
        if (totalSettlements == 0) return emptyList()

        val totalAmount = completedSettlements.fold(Money.ZERO) { acc, s -> acc + s.amount }
        val groupedByType = completedSettlements.groupBy { it.resolutionType }

        return ReturnResolutionType.entries.map { type ->
            val matching = groupedByType[type] ?: emptyList()
            val count = matching.size
            val sumAmount = matching.fold(Money.ZERO) { acc, s -> acc + s.amount }
            val percentage = if (!totalAmount.isZero()) {
                val ratio = sumAmount.amount.divide(totalAmount.amount, 4, RoundingMode.HALF_UP).toDouble() * 100.0
                BigDecimal(ratio).setScale(2, RoundingMode.HALF_UP).toDouble()
            } else if (totalSettlements > 0) {
                BigDecimal((count.toDouble() / totalSettlements.toDouble()) * 100.0)
                    .setScale(2, RoundingMode.HALF_UP).toDouble()
            } else {
                0.0
            }

            ReturnFinancialBreakdown(
                resolutionType = type,
                count = count,
                totalAmount = sumAmount,
                percentage = percentage
            )
        }
    }

    /**
     * Calculates chronological [ReturnAnalyticsTrendPoint] series across [period].
     */
    fun calculateTrends(
        returns: List<ReturnRequest>,
        settlements: List<ReturnSettlement>,
        items: Map<String, List<ReturnItem>>,
        period: ReturnAnalyticsPeriod,
        nowMillis: Long = System.currentTimeMillis()
    ): List<ReturnAnalyticsTrendPoint> {
        val startMillis = period.calculateStartTimestamp(nowMillis)
        val filteredReturns = returns.filter {
            period == ReturnAnalyticsPeriod.ALL_TIME || it.createdAt >= startMillis
        }.sortedBy { it.createdAt }

        if (filteredReturns.isEmpty()) {
            return emptyList()
        }

        // Group returns by bucket (Day format "yyyy-MM-dd")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val groupedByDate = filteredReturns.groupBy { dateFormat.format(Date(it.createdAt)) }

        val completedSettlements = settlements.filter { it.status == ReturnSettlementStatus.COMPLETED }

        return groupedByDate.map { (dateStr, dateReturns) ->
            val dateReturnIds = dateReturns.map { it.returnId }.toSet()
            val firstTimestamp = dateReturns.minOf { it.createdAt }

            var acceptedQty = 0
            var rejectedQty = 0

            for (ret in dateReturns) {
                val returnItems = items[ret.returnId] ?: emptyList()
                acceptedQty += returnItems.sumOf { it.acceptedQuantity }
                rejectedQty += returnItems.sumOf { it.rejectedQuantity }
            }

            val matchingSettlements = completedSettlements.filter { it.returnId in dateReturnIds }
            val financialValue = matchingSettlements.fold(Money.ZERO) { acc, s -> acc + s.amount }

            ReturnAnalyticsTrendPoint(
                timestamp = firstTimestamp,
                periodLabel = dateStr,
                returnCount = dateReturns.size,
                acceptedQuantity = acceptedQty,
                rejectedQuantity = rejectedQty,
                financialValue = financialValue
            )
        }
    }
}

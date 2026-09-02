package com.sucharu.sucharupro.domain.service.businessfinancialreporting

import com.sucharu.sucharupro.data.datasource.businesscost.BusinessCostTrackingFilter
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.BusinessCostAccrualFilter
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.BusinessCostCommitmentFilter
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.AdjustmentFilter
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.RefundFilter
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.WriteOffFilter
import com.sucharu.sucharupro.data.datasource.businessledger.BusinessLedgerPostingFilter
import com.sucharu.sucharupro.data.datasource.businessreconciliation.DiscrepancyFilter
import com.sucharu.sucharupro.data.datasource.businessreconciliation.ReconciliationRunFilter
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepository
import com.sucharu.sucharupro.data.repository.businessfinancialadjustment.BusinessFinancialAdjustmentRepository
import com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepository
import com.sucharu.sucharupro.domain.model.businesscost.BusinessCostAllocationStatus
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessCostAccrualStatus
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessCostCommitmentStatus
import com.sucharu.sucharupro.domain.model.businessexpense.*
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.AdjustmentStatus
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.RefundStatus
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.WriteOffStatus
import com.sucharu.sucharupro.domain.model.businessfinancialreporting.*
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerPosting
import com.sucharu.sucharupro.domain.model.businessreconciliation.DiscrepancyStatus
import com.sucharu.sucharupro.domain.model.businessreconciliation.ReconciliationRunStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorpayable.*
import com.sucharu.sucharupro.domain.repository.OrderRepository
import com.sucharu.sucharupro.domain.repository.businesscost.BusinessCostManagementRepository
import com.sucharu.sucharupro.domain.repository.businessexpense.BusinessExpenseRepository
import com.sucharu.sucharupro.domain.repository.vendorpayable.VendorPayableRepository
import com.sucharu.sucharupro.domain.repository.businessfinancialreporting.BusinessFinancialReportingRepository
import com.sucharu.sucharupro.domain.repository.businessledger.BusinessLedgerRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

/**
 * Production implementation of BusinessFinancialReportingService.
 * Strictly read-only over canonical financial repositories (Steps 01–07).
 */
class BusinessFinancialReportingServiceImpl(
    private val reportingRepository: BusinessFinancialReportingRepository,
    private val expenseRepository: BusinessExpenseRepository,
    private val payableRepository: VendorPayableRepository,
    private val ledgerRepository: BusinessLedgerRepository,
    private val costManagementRepository: BusinessCostManagementRepository,
    private val costControlRepository: BusinessCostControlRepository,
    private val reconciliationRepository: BusinessFinancialReconciliationRepository,
    private val adjustmentRepository: BusinessFinancialAdjustmentRepository,
    private val orderRepository: OrderRepository? = null,
    private val defaultTenantId: String = "TENANT-001"
) : BusinessFinancialReportingService {

    // --- 1. Executive Summary ---

    override suspend fun generateExecutiveSummary(filter: BusinessFinancialReportFilter): BusinessExecutiveFinancialSummary {
        val tenantId = filter.tenantId
        val projectId = filter.projectId
        val currency = filter.currency

        // Expenses
        val expenses: List<BusinessExpense> = when (val res = expenseRepository.listExpenses(tenantId, projectId, limit = 1000, offset = 0)) {
            is DomainResult.Success -> res.data.filter { it.currency == currency }
            else -> emptyList()
        }
        val totalExpense = expenses.fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }.setScale(4, RoundingMode.HALF_UP)
        val approvedExpense = expenses.filter { it.status == BusinessExpenseStatus.APPROVED || it.status == BusinessExpenseStatus.POSTABLE }
            .fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }.setScale(4, RoundingMode.HALF_UP)
        val pendingExpense = expenses.filter { it.status == BusinessExpenseStatus.SUBMITTED || it.status == BusinessExpenseStatus.DRAFT }
            .fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }.setScale(4, RoundingMode.HALF_UP)

        // Payables
        val payables: List<VendorPayable> = when (val res = payableRepository.listPayables(tenantId, projectId, limit = 1000, offset = 0)) {
            is DomainResult.Success -> res.data.filter { it.currency == currency }
            else -> emptyList()
        }
        val now = System.currentTimeMillis()
        val totalPayable = payables.fold(BigDecimal.ZERO) { acc, p -> acc + p.originalAmount }.setScale(4, RoundingMode.HALF_UP)
        val outstandingPayable = payables.filter { it.status != VendorPayableStatus.PAID && it.status != VendorPayableStatus.CANCELLED && it.status != VendorPayableStatus.VOIDED }
            .fold(BigDecimal.ZERO) { acc, p -> acc + (p.originalAmount - p.paidAmount) }.setScale(4, RoundingMode.HALF_UP)
        val overduePayable = payables.filter { p -> (now > p.dueDate) && p.status != VendorPayableStatus.PAID && p.status != VendorPayableStatus.CANCELLED && p.status != VendorPayableStatus.VOIDED }
            .fold(BigDecimal.ZERO) { acc, p -> acc + (p.originalAmount - p.paidAmount) }.setScale(4, RoundingMode.HALF_UP)

        // Ledger
        val ledgerSummary = ledgerRepository.calculateBalanceSummary(tenantId, projectId)
        val totalDebit = ledgerSummary.totalDebit.setScale(4, RoundingMode.HALF_UP)
        val totalCredit = ledgerSummary.totalCredit.setScale(4, RoundingMode.HALF_UP)
        val netMovement = ledgerSummary.netMovement.setScale(4, RoundingMode.HALF_UP)
        val ledgerPostings = ledgerRepository.listPostings(tenantId, projectId, BusinessLedgerPostingFilter(limit = 1000))
        val postingCount = ledgerPostings.size

        // Cost Management
        val costSummary = costManagementRepository.calculateTrackingSummary(tenantId, projectId)
        val totalAllocatedCost = costSummary.totalAllocatedCost.setScale(4, RoundingMode.HALF_UP)
        val totalUnallocatedCost = costSummary.totalUnallocatedCost.setScale(4, RoundingMode.HALF_UP)
        val costCenterCount = costManagementRepository.listCostCenters(tenantId, projectId, activeOnly = true).size
        val costCategoryCount = costManagementRepository.listCostCategories(tenantId, projectId, activeOnly = true).size

        // Commitments & Accruals
        val commitments = costControlRepository.listCommitments(tenantId, projectId, BusinessCostCommitmentFilter())
            .filter { it.currency == currency }
        val activeCommitments = commitments.filter { it.status == BusinessCostCommitmentStatus.ACTIVE || it.status == BusinessCostCommitmentStatus.PARTIALLY_CONSUMED || it.status == BusinessCostCommitmentStatus.APPROVED }
        val remainingCommitmentAmount = activeCommitments.fold(BigDecimal.ZERO) { acc, c -> acc + c.remainingAmount }.setScale(4, RoundingMode.HALF_UP)

        val accruals = costControlRepository.listAccruals(tenantId, projectId, BusinessCostAccrualFilter())
            .filter { it.currency == currency }
        val activeAccruals = accruals.filter { it.status == BusinessCostAccrualStatus.POSTED || it.status == BusinessCostAccrualStatus.APPROVED }
        val outstandingAccrualAmount = activeAccruals.fold(BigDecimal.ZERO) { acc, a -> acc + (a.accrualAmount - a.reversedAmount) }.setScale(4, RoundingMode.HALF_UP)

        // Reconciliation
        val latestRuns = reconciliationRepository.listRuns(tenantId, projectId, ReconciliationRunFilter(limit = 1))
        val latestRun = latestRuns.firstOrNull()
        val allDiscrepancies = reconciliationRepository.listDiscrepancies(tenantId, projectId, DiscrepancyFilter())
        val openDiscrepancies = allDiscrepancies.filter { it.status == DiscrepancyStatus.OPEN || it.status == DiscrepancyStatus.INVESTIGATING }
        val reconciledAmount = (if (latestRun?.status == ReconciliationRunStatus.COMPLETED || latestRun?.status == ReconciliationRunStatus.APPROVED) totalDebit else BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)
        val unreconciledAmount = openDiscrepancies.fold(BigDecimal.ZERO) { acc, d -> acc + d.differenceAmount }.setScale(4, RoundingMode.HALF_UP)

        // Adjustments
        val adjustments = adjustmentRepository.listAdjustments(tenantId, projectId, AdjustmentFilter()).filter { it.currency == currency }
        val refunds = adjustmentRepository.listRefunds(tenantId, projectId, RefundFilter()).filter { it.currency == currency }
        val writeOffs = adjustmentRepository.listWriteOffs(tenantId, projectId, WriteOffFilter()).filter { it.currency == currency }

        val totalAdjAmount = adjustments.fold(BigDecimal.ZERO) { acc, a -> acc + a.effectiveAmount }.setScale(4, RoundingMode.HALF_UP)
        val totalRefundAmount = refunds.fold(BigDecimal.ZERO) { acc, r -> acc + r.requestedAmount }.setScale(4, RoundingMode.HALF_UP)
        val totalWriteOffAmount = writeOffs.fold(BigDecimal.ZERO) { acc, w -> acc + w.amount }.setScale(4, RoundingMode.HALF_UP)

        // Period-End Diagnostics
        val hasBlockers = openDiscrepancies.isNotEmpty() || pendingExpense > BigDecimal.ZERO || adjustments.any { it.status == AdjustmentStatus.SUBMITTED || it.status == AdjustmentStatus.UNDER_REVIEW }
        val readinessStatus = if (hasBlockers) PeriodReadinessStatus.NOT_READY else PeriodReadinessStatus.READY
        val blockerCount = (if (openDiscrepancies.isNotEmpty()) 1 else 0) + (if (pendingExpense > BigDecimal.ZERO) 1 else 0) + (if (adjustments.any { it.status == AdjustmentStatus.SUBMITTED || it.status == AdjustmentStatus.UNDER_REVIEW }) 1 else 0)

        return BusinessExecutiveFinancialSummary(
            tenantId = tenantId,
            projectId = projectId,
            periodId = filter.periodId,
            currency = currency,
            totalExpenseAmount = totalExpense,
            approvedExpenseAmount = approvedExpense,
            pendingExpenseAmount = pendingExpense,
            expenseCount = expenses.size,
            totalPayableAmount = totalPayable,
            outstandingPayableAmount = outstandingPayable,
            overduePayableAmount = overduePayable,
            payableCount = payables.size,
            totalLedgerDebit = totalDebit,
            totalLedgerCredit = totalCredit,
            netLedgerMovement = netMovement,
            ledgerPostingCount = postingCount,
            totalAllocatedCost = totalAllocatedCost,
            totalUnallocatedCost = totalUnallocatedCost,
            costCenterCount = costCenterCount,
            costCategoryCount = costCategoryCount,
            activeCommitmentCount = activeCommitments.size,
            remainingCommitmentAmount = remainingCommitmentAmount,
            activeAccrualCount = activeAccruals.size,
            outstandingAccrualAmount = outstandingAccrualAmount,
            reconciledAmount = reconciledAmount,
            unreconciledAmount = unreconciledAmount,
            openDiscrepancyCount = openDiscrepancies.size,
            adjustmentCount = adjustments.size,
            totalAdjustmentAmount = totalAdjAmount,
            totalRefundAmount = totalRefundAmount,
            totalWriteOffAmount = totalWriteOffAmount,
            periodReadinessStatus = readinessStatus,
            periodClosureBlockerCount = blockerCount,
            generatedAt = System.currentTimeMillis()
        )
    }

    // --- 2. Expense Analytics ---

    override suspend fun generateExpenseAnalytics(filter: BusinessFinancialReportFilter): BusinessExpenseAnalyticsReport {
        val tenantId = filter.tenantId
        val projectId = filter.projectId
        val currency = filter.currency

        val rawExpenses = when (val res = expenseRepository.listExpenses(
            tenantId = tenantId,
            projectId = projectId,
            fromDate = filter.fromDate,
            toDate = filter.toDate,
            limit = 1000
        )) {
            is DomainResult.Success -> res.data.filter { it.currency == currency }
            else -> emptyList()
        }

        val expenses = rawExpenses.filter { e ->
            (filter.costCategoryId == null || e.expenseCategoryId == filter.costCategoryId) &&
            (filter.jobId == null || e.jobId == filter.jobId) &&
            (filter.vendorId == null || e.vendorId == filter.vendorId) &&
            (filter.status == null || e.status.name == filter.status)
        }

        val total = expenses.fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }.setScale(4, RoundingMode.HALF_UP)
        val approved = expenses.filter { it.status == BusinessExpenseStatus.APPROVED || it.status == BusinessExpenseStatus.POSTABLE }
            .fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }.setScale(4, RoundingMode.HALF_UP)
        val pending = expenses.filter { it.status == BusinessExpenseStatus.SUBMITTED || it.status == BusinessExpenseStatus.DRAFT }
            .fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }.setScale(4, RoundingMode.HALF_UP)
        val rejected = expenses.filter { it.status == BusinessExpenseStatus.REJECTED }
            .fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }.setScale(4, RoundingMode.HALF_UP)
        val cancelled = expenses.filter { it.status == BusinessExpenseStatus.CANCELLED }
            .fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }.setScale(4, RoundingMode.HALF_UP)

        val average = if (expenses.isNotEmpty()) {
            total.divide(BigDecimal(expenses.size), 4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        }

        // Category breakdown
        val categoryBreakdown = expenses.groupBy { it.expenseCategoryId }.map { (cat, list) ->
            val catSum = list.fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }.setScale(4, RoundingMode.HALF_UP)
            val pct = if (total > BigDecimal.ZERO) {
                catSum.multiply(BigDecimal(100)).divide(total, 2, RoundingMode.HALF_UP)
            } else BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            CategoryExpenseSummary(category = cat, count = list.size, totalAmount = catSum, percentage = pct)
        }.sortedByDescending { it.totalAmount }

        // Cost Center breakdown
        val costCenters = costManagementRepository.listCostCenters(tenantId, projectId)
        val costCenterBreakdown = listOf(
            CostCenterExpenseSummary(
                costCenterId = "DEFAULT",
                code = "CC-GENERAL",
                name = "General Operational Center",
                count = expenses.size,
                totalAmount = total
            )
        )

        // Payment Method breakdown
        val paymentMethodBreakdown = expenses.groupBy { it.paymentMethod.name }.map { (pm, list) ->
            val pmSum = list.fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }.setScale(4, RoundingMode.HALF_UP)
            PaymentMethodExpenseSummary(paymentMethod = pm, count = list.size, totalAmount = pmSum)
        }.sortedByDescending { it.totalAmount }

        // Trend breakdown by date
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val trend = expenses.groupBy { sdf.format(Date(it.expenseDate)) }.map { (dateStr, list) ->
            val trendSum = list.fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }.setScale(4, RoundingMode.HALF_UP)
            ExpenseTrendPoint(dateLabel = dateStr, timestamp = list.first().expenseDate, amount = trendSum, count = list.size)
        }.sortedBy { it.dateLabel }

        return BusinessExpenseAnalyticsReport(
            tenantId = tenantId,
            projectId = projectId,
            periodId = filter.periodId,
            currency = currency,
            totalAmount = total,
            approvedAmount = approved,
            pendingAmount = pending,
            rejectedAmount = rejected,
            cancelledAmount = cancelled,
            averageAmount = average,
            totalCount = expenses.size,
            approvedCount = expenses.count { it.status == BusinessExpenseStatus.APPROVED || it.status == BusinessExpenseStatus.POSTABLE },
            pendingCount = expenses.count { it.status == BusinessExpenseStatus.SUBMITTED || it.status == BusinessExpenseStatus.DRAFT },
            categoryBreakdown = categoryBreakdown,
            costCenterBreakdown = costCenterBreakdown,
            paymentMethodBreakdown = paymentMethodBreakdown,
            trend = trend,
            generatedAt = System.currentTimeMillis()
        )
    }

    // --- 3. Vendor Payable Analytics ---

    override suspend fun generateVendorPayableAnalytics(filter: BusinessFinancialReportFilter): VendorPayableAnalyticsReport {
        val tenantId = filter.tenantId
        val projectId = filter.projectId
        val currency = filter.currency
        val now = System.currentTimeMillis()

        val rawPayables: List<VendorPayable> = when (val res = payableRepository.listPayables(
            tenantId = tenantId,
            projectId = projectId,
            vendorId = filter.vendorId,
            fromDate = filter.fromDate,
            toDate = filter.toDate,
            limit = 1000
        )) {
            is DomainResult.Success -> res.data.filter { it.currency == currency }
            else -> emptyList()
        }

        val payables: List<VendorPayable> = rawPayables.filter { p: VendorPayable ->
            (filter.jobId == null || p.jobId == filter.jobId) &&
            (filter.status == null || p.status.name == filter.status)
        }

        val totalPayable: BigDecimal = payables.fold(BigDecimal.ZERO) { acc, p -> acc + p.originalAmount }.setScale(4, RoundingMode.HALF_UP)
        val paidAmount: BigDecimal = payables.fold(BigDecimal.ZERO) { acc, p -> acc + p.paidAmount }.setScale(4, RoundingMode.HALF_UP)
        val outstandingAmount: BigDecimal = payables.filter { it.status != VendorPayableStatus.CANCELLED && it.status != VendorPayableStatus.VOIDED }
            .fold(BigDecimal.ZERO) { acc, p -> acc + (p.originalAmount - p.paidAmount) }.setScale(4, RoundingMode.HALF_UP)
        val overduePayables: List<VendorPayable> = payables.filter { p -> (now > p.dueDate) && p.status != VendorPayableStatus.PAID && p.status != VendorPayableStatus.CANCELLED && p.status != VendorPayableStatus.VOIDED }
        val overdueAmount: BigDecimal = overduePayables.fold(BigDecimal.ZERO) { acc, p -> acc + (p.originalAmount - p.paidAmount) }.setScale(4, RoundingMode.HALF_UP)
        val currentAmount: BigDecimal = (outstandingAmount - overdueAmount).coerceAtLeast(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)

        // Aging calculation
        var currentSum = BigDecimal.ZERO
        var currentCount = 0
        var d1To30Sum = BigDecimal.ZERO
        var d1To30Count = 0
        var d31To60Sum = BigDecimal.ZERO
        var d31To60Count = 0
        var d61To90Sum = BigDecimal.ZERO
        var d61To90Count = 0
        var dOver90Sum = BigDecimal.ZERO
        var dOver90Count = 0

        for (p in payables) {
            if (p.status == VendorPayableStatus.PAID || p.status == VendorPayableStatus.CANCELLED || p.status == VendorPayableStatus.VOIDED) continue
            val remaining = (p.originalAmount - p.paidAmount).setScale(4, RoundingMode.HALF_UP)
            if (remaining <= BigDecimal.ZERO) continue

            if (p.dueDate >= now) {
                currentSum += remaining
                currentCount++
            } else {
                val overdueDays = ((now - p.dueDate) / (1000L * 60 * 60 * 24)).toInt()
                when {
                    overdueDays in 1..30 -> {
                        d1To30Sum += remaining
                        d1To30Count++
                    }
                    overdueDays in 31..60 -> {
                        d31To60Sum += remaining
                        d31To60Count++
                    }
                    overdueDays in 61..90 -> {
                        d61To90Sum += remaining
                        d61To90Count++
                    }
                    else -> {
                        dOver90Sum += remaining
                        dOver90Count++
                    }
                }
            }
        }

        val agingBuckets: List<PayableAgingBucket> = listOf(
            PayableAgingBucket(PayableAgingBucketType.CURRENT, "Current (Not Due)", currentCount, currentSum.setScale(4, RoundingMode.HALF_UP), currency),
            PayableAgingBucket(PayableAgingBucketType.DAYS_1_TO_30, "1–30 Days Overdue", d1To30Count, d1To30Sum.setScale(4, RoundingMode.HALF_UP), currency),
            PayableAgingBucket(PayableAgingBucketType.DAYS_31_TO_60, "31–60 Days Overdue", d31To60Count, d31To60Sum.setScale(4, RoundingMode.HALF_UP), currency),
            PayableAgingBucket(PayableAgingBucketType.DAYS_61_TO_90, "61–90 Days Overdue", d61To90Count, d61To90Sum.setScale(4, RoundingMode.HALF_UP), currency),
            PayableAgingBucket(PayableAgingBucketType.DAYS_OVER_90, "90+ Days Overdue", dOver90Count, dOver90Sum.setScale(4, RoundingMode.HALF_UP), currency)
        )

        // Vendor Breakdown
        val vendorGroups: Map<String, List<VendorPayable>> = payables.groupBy { it.vendorId }
        val vendorBreakdowns: List<VendorPayableBreakdown> = vendorGroups.map { (vId: String, list: List<VendorPayable>) ->
            val vTotal = list.fold(BigDecimal.ZERO) { acc, p -> acc + p.originalAmount }.setScale(4, RoundingMode.HALF_UP)
            val vOut = list.filter { it.status != VendorPayableStatus.CANCELLED && it.status != VendorPayableStatus.VOIDED }
                .fold(BigDecimal.ZERO) { acc, p -> acc + (p.originalAmount - p.paidAmount) }.setScale(4, RoundingMode.HALF_UP)
            val vOverdue = list.filter { p -> (now > p.dueDate) && p.status != VendorPayableStatus.PAID && p.status != VendorPayableStatus.CANCELLED && p.status != VendorPayableStatus.VOIDED }
                .fold(BigDecimal.ZERO) { acc, p -> acc + (p.originalAmount - p.paidAmount) }.setScale(4, RoundingMode.HALF_UP)
            VendorPayableBreakdown(
                vendorId = vId,
                vendorName = list.firstOrNull()?.description ?: vId,
                totalPayable = vTotal,
                outstandingAmount = vOut,
                overdueAmount = vOverdue,
                billCount = list.size,
                currency = currency
            )
        }.sortedByDescending { it.outstandingAmount }

        return VendorPayableAnalyticsReport(
            tenantId = tenantId,
            projectId = projectId,
            currency = currency,
            totalPayable = totalPayable,
            paidAmount = paidAmount,
            outstandingAmount = outstandingAmount,
            overdueAmount = overdueAmount,
            currentAmount = currentAmount,
            totalBillsCount = payables.size,
            overdueBillsCount = overduePayables.size,
            agingBuckets = agingBuckets,
            vendorBreakdowns = vendorBreakdowns,
            generatedAt = System.currentTimeMillis()
        )
    }

    // --- 4. Business Ledger Report ---

    override suspend fun generateLedgerReport(filter: BusinessFinancialReportFilter): BusinessLedgerReport {
        val tenantId = filter.tenantId
        val projectId = filter.projectId
        val currency = filter.currency

        val postings = ledgerRepository.listPostings(
            tenantId = tenantId,
            projectId = projectId,
            filter = BusinessLedgerPostingFilter(
                fromDate = filter.fromDate,
                toDate = filter.toDate,
                limit = 1000
            )
        ).filter { it.currency == currency }

        var totalDebit = BigDecimal.ZERO
        var totalCredit = BigDecimal.ZERO
        var reversalCount = 0
        var reversalTotal = BigDecimal.ZERO
        var manualAdjustmentCount = 0
        var manualAdjustmentTotal = BigDecimal.ZERO

        val sourceGroups = mutableMapOf<String, MutableList<BusinessLedgerPosting>>()

        for (p in postings) {
            totalDebit += p.debitAmount
            totalCredit += p.creditAmount
            if (p.isReversed) {
                reversalCount++
                reversalTotal += (p.debitAmount + p.creditAmount)
            }
            if (p.sourceType.name.contains("MANUAL") || p.sourceType.name.contains("ADJUSTMENT")) {
                manualAdjustmentCount++
                manualAdjustmentTotal += (p.debitAmount + p.creditAmount)
            }
            sourceGroups.getOrPut(p.sourceType.name) { mutableListOf() }.add(p)
        }

        val netMovement = (totalDebit - totalCredit).setScale(4, RoundingMode.HALF_UP)

        val sourceBreakdowns = sourceGroups.map { (src, list) ->
            val dSum = list.fold(BigDecimal.ZERO) { acc, p -> acc + p.debitAmount }.setScale(4, RoundingMode.HALF_UP)
            val cSum = list.fold(BigDecimal.ZERO) { acc, p -> acc + p.creditAmount }.setScale(4, RoundingMode.HALF_UP)
            val nSum = (dSum - cSum).setScale(4, RoundingMode.HALF_UP)
            LedgerSourceBreakdown(sourceType = src, debitAmount = dSum, creditAmount = cSum, netAmount = nSum, entryCount = list.size)
        }.sortedByDescending { it.entryCount }

        return BusinessLedgerReport(
            tenantId = tenantId,
            projectId = projectId,
            currency = currency,
            totalDebit = totalDebit.setScale(4, RoundingMode.HALF_UP),
            totalCredit = totalCredit.setScale(4, RoundingMode.HALF_UP),
            netMovement = netMovement,
            postingCount = postings.size,
            reversalCount = reversalCount,
            reversalTotalAmount = reversalTotal.setScale(4, RoundingMode.HALF_UP),
            manualAdjustmentCount = manualAdjustmentCount,
            manualAdjustmentTotalAmount = manualAdjustmentTotal.setScale(4, RoundingMode.HALF_UP),
            sourceBreakdowns = sourceBreakdowns,
            generatedAt = System.currentTimeMillis()
        )
    }

    // --- 5. Cost Center Report ---

    override suspend fun generateCostCenterReport(filter: BusinessFinancialReportFilter): BusinessCostCenterReport {
        val tenantId = filter.tenantId
        val projectId = filter.projectId
        val currency = filter.currency

        val costCenters = costManagementRepository.listCostCenters(tenantId, projectId)
        val trackedCosts = costManagementRepository.listCostTracking(
            tenantId = tenantId,
            projectId = projectId,
            filter = BusinessCostTrackingFilter(
                fromDate = filter.fromDate,
                toDate = filter.toDate,
                limit = 1000
            )
        ).filter { it.currency == currency }

        var totalTracked = BigDecimal.ZERO
        var totalAllocated = BigDecimal.ZERO
        var totalUnallocated = BigDecimal.ZERO
        val categoryTotals = mutableMapOf<String, BigDecimal>()

        val centerDetailSummaries = costCenters.map { cc ->
            val ccTracked = trackedCosts.filter { it.costCenterId == cc.id }
            val allocated = ccTracked.filter { it.allocationStatus == BusinessCostAllocationStatus.FULLY_ALLOCATED || it.allocationStatus == BusinessCostAllocationStatus.PARTIALLY_ALLOCATED }
                .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }.setScale(4, RoundingMode.HALF_UP)
            val unallocated = ccTracked.filter { it.allocationStatus == BusinessCostAllocationStatus.UNALLOCATED }
                .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }.setScale(4, RoundingMode.HALF_UP)
            val total = (allocated + unallocated).setScale(4, RoundingMode.HALF_UP)

            totalTracked += total
            totalAllocated += allocated
            totalUnallocated += unallocated

            val catMap = mutableMapOf<String, BigDecimal>()
            ccTracked.forEach { t ->
                val current = catMap.getOrDefault(t.costCategoryId, BigDecimal.ZERO)
                catMap[t.costCategoryId] = (current + t.amount).setScale(4, RoundingMode.HALF_UP)
                val globalCatCurrent = categoryTotals.getOrDefault(t.costCategoryId, BigDecimal.ZERO)
                categoryTotals[t.costCategoryId] = (globalCatCurrent + t.amount).setScale(4, RoundingMode.HALF_UP)
            }

            val jobCount = ccTracked.mapNotNull { it.jobId }.distinct().size

            CostCenterDetailSummary(
                costCenterId = cc.id,
                code = cc.code,
                name = cc.name,
                allocatedAmount = allocated,
                unallocatedAmount = unallocated,
                totalTrackedAmount = total,
                categoryBreakdown = catMap,
                jobCount = jobCount
            )
        }.sortedByDescending { it.totalTrackedAmount }

        val activeCategoriesCount = costManagementRepository.listCostCategories(tenantId, projectId, activeOnly = true).size

        return BusinessCostCenterReport(
            tenantId = tenantId,
            projectId = projectId,
            currency = currency,
            totalTrackedCost = totalTracked.setScale(4, RoundingMode.HALF_UP),
            totalAllocatedCost = totalAllocated.setScale(4, RoundingMode.HALF_UP),
            totalUnallocatedCost = totalUnallocated.setScale(4, RoundingMode.HALF_UP),
            activeCostCentersCount = costCenters.size,
            activeCategoriesCount = activeCategoriesCount,
            costCenters = centerDetailSummaries,
            topCategories = categoryTotals,
            generatedAt = System.currentTimeMillis()
        )
    }

    // --- 6. Job / Project Cost Report ---

    override suspend fun generateProjectCostReport(filter: BusinessFinancialReportFilter): JobProjectCostReport {
        val tenantId = filter.tenantId
        val projectId = filter.projectId
        val currency = filter.currency

        val trackedCosts = costManagementRepository.listCostTracking(
            tenantId = tenantId,
            projectId = projectId,
            filter = BusinessCostTrackingFilter(jobId = filter.jobId, limit = 1000)
        ).filter { it.currency == currency }

        val commitments = costControlRepository.listCommitments(
            tenantId = tenantId,
            projectId = projectId,
            filter = BusinessCostCommitmentFilter(jobId = filter.jobId)
        ).filter { it.currency == currency }

        val accruals = costControlRepository.listAccruals(
            tenantId = tenantId,
            projectId = projectId,
            filter = BusinessCostAccrualFilter(jobId = filter.jobId)
        ).filter { it.currency == currency }

        val jobIds = (trackedCosts.mapNotNull { it.jobId } +
                      commitments.mapNotNull { it.jobId } +
                      accruals.mapNotNull { it.jobId }).distinct()

        var totalRecognized = BigDecimal.ZERO
        var totalAllocated = BigDecimal.ZERO
        var totalCommitment = BigDecimal.ZERO
        var totalAccrual = BigDecimal.ZERO
        var totalRevenue: BigDecimal? = null

        val jobCostItems = jobIds.map { jId ->
            val jCosts = trackedCosts.filter { it.jobId == jId }
            val jComm = commitments.filter { it.jobId == jId && (it.status == BusinessCostCommitmentStatus.ACTIVE || it.status == BusinessCostCommitmentStatus.PARTIALLY_CONSUMED || it.status == BusinessCostCommitmentStatus.APPROVED) }
            val jAcc = accruals.filter { it.jobId == jId && (it.status == BusinessCostAccrualStatus.POSTED || it.status == BusinessCostAccrualStatus.APPROVED) }

            val recognized = jCosts.fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }.setScale(4, RoundingMode.HALF_UP)
            val allocated = jCosts.filter { it.allocationStatus == BusinessCostAllocationStatus.FULLY_ALLOCATED || it.allocationStatus == BusinessCostAllocationStatus.PARTIALLY_ALLOCATED }
                .fold(BigDecimal.ZERO) { acc, t -> acc + t.amount }.setScale(4, RoundingMode.HALF_UP)
            val commAmount = jComm.fold(BigDecimal.ZERO) { acc, c -> acc + c.remainingAmount }.setScale(4, RoundingMode.HALF_UP)
            val accAmount = jAcc.fold(BigDecimal.ZERO) { acc, a -> acc + (a.accrualAmount - a.reversedAmount) }.setScale(4, RoundingMode.HALF_UP)
            val totalCost = (recognized + commAmount + accAmount).setScale(4, RoundingMode.HALF_UP)

            totalRecognized += recognized
            totalAllocated += allocated
            totalCommitment += commAmount
            totalAccrual += accAmount

            // Check canonical order revenue if repository available
            val order = if (orderRepository != null) {
                when (val ordRes = orderRepository.findOrderById(jId)) {
                    is DomainResult.Success -> ordRes.data
                    else -> null
                }
            } else null

            val canonRev = order?.totalAmount?.amount?.setScale(4, RoundingMode.HALF_UP)
            if (canonRev != null) {
                totalRevenue = (totalRevenue ?: BigDecimal.ZERO) + canonRev
            }

            val grossMargin = canonRev?.let { (it - totalCost).setScale(4, RoundingMode.HALF_UP) }
            val marginPct = if (canonRev != null && canonRev.compareTo(BigDecimal.ZERO) > 0 && grossMargin != null) {
                grossMargin.multiply(BigDecimal(100)).divide(canonRev, 2, RoundingMode.HALF_UP)
            } else null

            JobProjectCostItem(
                jobId = jId,
                costCenterId = jCosts.firstOrNull()?.costCenterId ?: "DEFAULT",
                actualRecognizedCost = recognized,
                allocatedCost = allocated,
                commitmentAmount = commAmount,
                accrualAmount = accAmount,
                totalCost = totalCost,
                canonicalRevenue = canonRev,
                grossMargin = grossMargin,
                marginPercentage = marginPct,
                currency = currency
            )
        }.sortedByDescending { it.totalCost }

        val overallMargin = totalRevenue?.let { (it - (totalRecognized + totalCommitment + totalAccrual)).setScale(4, RoundingMode.HALF_UP) }

        return JobProjectCostReport(
            tenantId = tenantId,
            projectId = projectId,
            currency = currency,
            totalRecognizedCost = totalRecognized.setScale(4, RoundingMode.HALF_UP),
            totalAllocatedCost = totalAllocated.setScale(4, RoundingMode.HALF_UP),
            totalCommitmentCost = totalCommitment.setScale(4, RoundingMode.HALF_UP),
            totalAccrualCost = totalAccrual.setScale(4, RoundingMode.HALF_UP),
            totalCanonicalRevenue = totalRevenue?.setScale(4, RoundingMode.HALF_UP),
            overallGrossMargin = overallMargin,
            jobsCount = jobIds.size,
            jobCosts = jobCostItems,
            generatedAt = System.currentTimeMillis()
        )
    }

    // --- 7. Commitment & Accrual Report ---

    override suspend fun generateCommitmentAccrualReport(filter: BusinessFinancialReportFilter): CommitmentAccrualReport {
        val tenantId = filter.tenantId
        val projectId = filter.projectId
        val currency = filter.currency

        val commitments = costControlRepository.listCommitments(
            tenantId = tenantId,
            projectId = projectId,
            filter = BusinessCostCommitmentFilter(periodId = filter.periodId)
        ).filter { it.currency == currency }

        val accruals = costControlRepository.listAccruals(
            tenantId = tenantId,
            projectId = projectId,
            filter = BusinessCostAccrualFilter(accountingPeriodId = filter.periodId)
        ).filter { it.currency == currency }

        val totalComm = commitments.fold(BigDecimal.ZERO) { acc, c -> acc + c.committedAmount }.setScale(4, RoundingMode.HALF_UP)
        val consumedComm = commitments.fold(BigDecimal.ZERO) { acc, c -> acc + c.consumedAmount }.setScale(4, RoundingMode.HALF_UP)
        val remainingComm = commitments.fold(BigDecimal.ZERO) { acc, c -> acc + c.remainingAmount }.setScale(4, RoundingMode.HALF_UP)
        val activeCommCount = commitments.count { it.status == BusinessCostCommitmentStatus.ACTIVE || it.status == BusinessCostCommitmentStatus.PARTIALLY_CONSUMED || it.status == BusinessCostCommitmentStatus.APPROVED }

        val totalAcc = accruals.fold(BigDecimal.ZERO) { acc, a -> acc + a.accrualAmount }.setScale(4, RoundingMode.HALF_UP)
        val reversedAcc = accruals.fold(BigDecimal.ZERO) { acc, a -> acc + a.reversedAmount }.setScale(4, RoundingMode.HALF_UP)
        val outstandingAcc = accruals.fold(BigDecimal.ZERO) { acc, a -> acc + (a.accrualAmount - a.reversedAmount) }.setScale(4, RoundingMode.HALF_UP)
        val activeAccCount = accruals.count { it.status == BusinessCostAccrualStatus.POSTED || it.status == BusinessCostAccrualStatus.APPROVED }

        val items = mutableListOf<CommitmentAccrualItem>()
        commitments.forEach { c ->
            items.add(
                CommitmentAccrualItem(
                    id = c.id,
                    type = "COMMITMENT",
                    title = c.description,
                    costCenterId = c.costCenterId ?: "UNASSIGNED",
                    originalAmount = c.committedAmount.setScale(4, RoundingMode.HALF_UP),
                    consumedOrReversedAmount = c.consumedAmount.setScale(4, RoundingMode.HALF_UP),
                    remainingOutstandingAmount = c.remainingAmount.setScale(4, RoundingMode.HALF_UP),
                    status = c.status.name,
                    expiryOrReversalDate = c.expectedDate,
                    currency = c.currency
                )
            )
        }
        accruals.forEach { a ->
            items.add(
                CommitmentAccrualItem(
                    id = a.id,
                    type = "ACCRUAL",
                    title = a.description,
                    costCenterId = a.costCenterId ?: "UNASSIGNED",
                    originalAmount = a.accrualAmount.setScale(4, RoundingMode.HALF_UP),
                    consumedOrReversedAmount = a.reversedAmount.setScale(4, RoundingMode.HALF_UP),
                    remainingOutstandingAmount = (a.accrualAmount - a.reversedAmount).setScale(4, RoundingMode.HALF_UP),
                    status = a.status.name,
                    expiryOrReversalDate = null,
                    currency = a.currency
                )
            )
        }

        return CommitmentAccrualReport(
            tenantId = tenantId,
            projectId = projectId,
            periodId = filter.periodId,
            currency = currency,
            totalCommitmentAmount = totalComm,
            consumedCommitmentAmount = consumedComm,
            remainingCommitmentAmount = remainingComm,
            activeCommitmentCount = activeCommCount,
            totalAccrualAmount = totalAcc,
            reversedAccrualAmount = reversedAcc,
            outstandingAccrualAmount = outstandingAcc,
            activeAccrualCount = activeAccCount,
            items = items,
            generatedAt = System.currentTimeMillis()
        )
    }

    // --- 8. Reconciliation Report ---

    override suspend fun generateReconciliationReport(filter: BusinessFinancialReportFilter): BusinessReconciliationReport {
        val tenantId = filter.tenantId
        val projectId = filter.projectId
        val currency = filter.currency

        val runs = reconciliationRepository.listRuns(
            tenantId = tenantId,
            projectId = projectId,
            filter = ReconciliationRunFilter(periodId = filter.periodId, limit = 5)
        )
        val latestRun = runs.firstOrNull()

        val discrepancies = reconciliationRepository.listDiscrepancies(
            tenantId = tenantId,
            projectId = projectId,
            filter = DiscrepancyFilter(limit = 100)
        )

        val openCount = discrepancies.count { it.status == DiscrepancyStatus.OPEN || it.status == DiscrepancyStatus.INVESTIGATING }
        val resolvedCount = discrepancies.count { it.status == DiscrepancyStatus.RESOLVED || it.status == DiscrepancyStatus.WAIVED }
        val isPassed = (latestRun?.status == ReconciliationRunStatus.COMPLETED || latestRun?.status == ReconciliationRunStatus.APPROVED) && openCount == 0

        val discrepancyItems = discrepancies.map { d ->
            ReconciliationDiscrepancySummaryItem(
                discrepancyId = d.id,
                sourceA = d.sourceType,
                sourceB = d.sourceId,
                amountA = d.expectedAmount.setScale(4, RoundingMode.HALF_UP),
                amountB = d.actualAmount.setScale(4, RoundingMode.HALF_UP),
                variance = d.differenceAmount.setScale(4, RoundingMode.HALF_UP),
                isResolved = d.status == DiscrepancyStatus.RESOLVED || d.status == DiscrepancyStatus.WAIVED,
                reason = d.description
            )
        }

        val totalDebit = ledgerRepository.calculateBalanceSummary(tenantId, projectId).totalDebit.setScale(4, RoundingMode.HALF_UP)

        return BusinessReconciliationReport(
            tenantId = tenantId,
            projectId = projectId,
            periodId = filter.periodId,
            currency = currency,
            lastRunId = latestRun?.id,
            lastRunStatus = latestRun?.status?.name,
            lastRunTimestamp = latestRun?.completedAt ?: latestRun?.startedAt,
            reconciledAmount = (if (isPassed) totalDebit else BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP),
            unreconciledAmount = discrepancies.filter { it.status == DiscrepancyStatus.OPEN }.fold(BigDecimal.ZERO) { acc, d -> acc + d.differenceAmount }.setScale(4, RoundingMode.HALF_UP),
            totalDiscrepanciesCount = discrepancies.size,
            openDiscrepanciesCount = openCount,
            resolvedDiscrepanciesCount = resolvedCount,
            isPeriodReconciliationPassed = isPassed,
            discrepancies = discrepancyItems,
            generatedAt = System.currentTimeMillis()
        )
    }

    // --- 9. Adjustment / Refund / Write-Off Report ---

    override suspend fun generateAdjustmentReport(filter: BusinessFinancialReportFilter): BusinessFinancialAdjustmentReport {
        val tenantId = filter.tenantId
        val projectId = filter.projectId
        val currency = filter.currency

        val adjustments = adjustmentRepository.listAdjustments(
            tenantId = tenantId,
            projectId = projectId,
            filter = AdjustmentFilter(periodId = filter.periodId)
        ).filter { it.currency == currency }

        val refunds = adjustmentRepository.listRefunds(
            tenantId = tenantId,
            projectId = projectId,
            filter = RefundFilter(periodId = filter.periodId)
        ).filter { it.currency == currency }

        val writeOffs = adjustmentRepository.listWriteOffs(
            tenantId = tenantId,
            projectId = projectId,
            filter = WriteOffFilter(periodId = filter.periodId)
        ).filter { it.currency == currency }

        val totalAdjAmount = adjustments.fold(BigDecimal.ZERO) { acc, a -> acc + a.effectiveAmount }.setScale(4, RoundingMode.HALF_UP)
        val totalRefundAmount = refunds.fold(BigDecimal.ZERO) { acc, r -> acc + r.requestedAmount }.setScale(4, RoundingMode.HALF_UP)
        val totalWriteOffAmount = writeOffs.fold(BigDecimal.ZERO) { acc, w -> acc + w.amount }.setScale(4, RoundingMode.HALF_UP)
        val totalReversalAmount = adjustments.filter { it.status == AdjustmentStatus.REVERSED }
            .fold(BigDecimal.ZERO) { acc, a -> acc + a.effectiveAmount }.setScale(4, RoundingMode.HALF_UP)

        val pendingList = adjustments.filter { it.status == AdjustmentStatus.SUBMITTED || it.status == AdjustmentStatus.UNDER_REVIEW }
        val pendingCount = pendingList.size
        val pendingAmount = pendingList.fold(BigDecimal.ZERO) { acc, a -> acc + a.effectiveAmount }.setScale(4, RoundingMode.HALF_UP)
        val postedCount = adjustments.count { it.status == AdjustmentStatus.POSTED }

        val typeSummaries = adjustments.groupBy { it.adjustmentType.name }.map { (type, list) ->
            val sum = list.fold(BigDecimal.ZERO) { acc, a -> acc + a.effectiveAmount }.setScale(4, RoundingMode.HALF_UP)
            AdjustmentTypeSummary(adjustmentType = type, count = list.size, totalAmount = sum, currency = currency)
        }

        return BusinessFinancialAdjustmentReport(
            tenantId = tenantId,
            projectId = projectId,
            periodId = filter.periodId,
            currency = currency,
            totalAdjustmentAmount = totalAdjAmount,
            totalRefundAmount = totalRefundAmount,
            totalWriteOffAmount = totalWriteOffAmount,
            totalReversalAmount = totalReversalAmount,
            pendingApprovalCount = pendingCount,
            pendingApprovalAmount = pendingAmount,
            postedCount = postedCount,
            typeSummaries = typeSummaries,
            generatedAt = System.currentTimeMillis()
        )
    }

    // --- 10. Financial Period-End Readiness Report ---

    override suspend fun generatePeriodEndReadinessReport(tenantId: String, projectId: String, periodId: String): BusinessPeriodEndReadinessReport {
        val period = costControlRepository.findFinancialPeriodById(periodId, tenantId, projectId)
            ?: throw IllegalArgumentException("Financial period '$periodId' not found for tenant: $tenantId")

        val blockers = mutableListOf<PeriodClosureBlocker>()

        // 1. Reconciliation check
        val recRuns = reconciliationRepository.listRuns(tenantId, projectId, ReconciliationRunFilter(periodId = periodId, limit = 1))
        val latestRec = recRuns.firstOrNull()
        val allDiscrepancies = reconciliationRepository.listDiscrepancies(tenantId, projectId, DiscrepancyFilter(periodId = periodId))
        val openDiscrepancies = allDiscrepancies.filter { it.status == DiscrepancyStatus.OPEN || it.status == DiscrepancyStatus.INVESTIGATING }
        val isRecPassed = latestRec != null && (latestRec.status == ReconciliationRunStatus.COMPLETED || latestRec.status == ReconciliationRunStatus.APPROVED) && openDiscrepancies.isEmpty()

        if (latestRec == null) {
            blockers.add(
                PeriodClosureBlocker(
                    code = "MISSING_RECONCILIATION",
                    category = "RECONCILIATION",
                    description = "No completed reconciliation run exists for period '${period.periodCode}'",
                    severity = "CRITICAL"
                )
            )
        } else if (!isRecPassed) {
            blockers.add(
                PeriodClosureBlocker(
                    code = "OPEN_RECONCILIATION_DISCREPANCIES",
                    category = "RECONCILIATION",
                    description = "${openDiscrepancies.size} open reconciliation discrepancies must be resolved or waived before closing.",
                    severity = "CRITICAL"
                )
            )
        }

        // 2. Pending Expenses check
        val expenses = when (val res = expenseRepository.listExpenses(tenantId, projectId, fromDate = period.startDate, toDate = period.endDate, limit = 500)) {
            is DomainResult.Success -> res.data
            else -> emptyList()
        }
        val pendingExpenses = expenses.filter { it.status == BusinessExpenseStatus.SUBMITTED || it.status == BusinessExpenseStatus.DRAFT }
        if (pendingExpenses.isNotEmpty()) {
            blockers.add(
                PeriodClosureBlocker(
                    code = "PENDING_EXPENSES",
                    category = "EXPENSES",
                    description = "${pendingExpenses.size} expenses are pending approval/posting for period '${period.periodCode}'.",
                    severity = "CRITICAL"
                )
            )
        }

        // 3. Adjustments check
        val adjustments = adjustmentRepository.listAdjustments(tenantId, projectId, AdjustmentFilter(periodId = periodId))
        val pendingAdjustments = adjustments.filter { it.status == AdjustmentStatus.SUBMITTED || it.status == AdjustmentStatus.UNDER_REVIEW }
        val unpostedAdjustments = adjustments.filter { it.status == AdjustmentStatus.APPROVED }

        if (pendingAdjustments.isNotEmpty()) {
            blockers.add(
                PeriodClosureBlocker(
                    code = "PENDING_ADJUSTMENTS",
                    category = "ADJUSTMENTS",
                    description = "${pendingAdjustments.size} financial adjustments are pending review/approval.",
                    severity = "CRITICAL"
                )
            )
        }
        if (unpostedAdjustments.isNotEmpty()) {
            blockers.add(
                PeriodClosureBlocker(
                    code = "UNPOSTED_ADJUSTMENTS",
                    category = "ADJUSTMENTS",
                    description = "${unpostedAdjustments.size} approved financial adjustments have not yet been posted to the ledger.",
                    severity = "CRITICAL"
                )
            )
        }

        // 4. Accruals check
        val accruals = costControlRepository.listAccruals(tenantId, projectId, BusinessCostAccrualFilter(accountingPeriodId = periodId))
        val unreversedAccruals = accruals.filter { it.status == BusinessCostAccrualStatus.POSTED }

        val readinessStatus = if (blockers.isEmpty()) PeriodReadinessStatus.READY else PeriodReadinessStatus.NOT_READY

        return BusinessPeriodEndReadinessReport(
            tenantId = tenantId,
            projectId = projectId,
            periodId = period.id,
            periodCode = period.periodCode,
            periodName = period.periodName,
            periodStatus = period.status.name,
            readinessStatus = readinessStatus,
            blockerCount = blockers.size,
            blockers = blockers,
            isReconciliationPassed = isRecPassed,
            pendingExpensesCount = pendingExpenses.size,
            pendingAdjustmentsCount = pendingAdjustments.size,
            unpostedAdjustmentsCount = unpostedAdjustments.size,
            outstandingAccrualsCount = unreversedAccruals.size,
            openDiscrepanciesCount = openDiscrepancies.size,
            generatedAt = System.currentTimeMillis()
        )
    }

    // --- 11. Snapshot Management ---

    override suspend fun createReportSnapshot(
        filter: BusinessFinancialReportFilter,
        metricsJson: String,
        generatedBy: String
    ): BusinessFinancialReportSnapshot {
        val snapshotId = "SNAP-" + UUID.randomUUID().toString().take(12).uppercase()
        val now = System.currentTimeMillis()
        val hash = BusinessFinancialReportSnapshot.calculateIntegrityHash(
            tenantId = filter.tenantId,
            projectId = filter.projectId,
            reportType = filter.reportType,
            metricsPayloadJson = metricsJson,
            generatedAt = now
        )

        val snapshot = BusinessFinancialReportSnapshot(
            snapshotId = snapshotId,
            tenantId = filter.tenantId,
            projectId = filter.projectId,
            periodId = filter.periodId,
            reportType = filter.reportType,
            filterSummary = "Type=${filter.reportType.name}, Period=${filter.periodId ?: "ALL"}, Currency=${filter.currency}",
            metricsPayloadJson = metricsJson,
            integrityHash = hash,
            isImmutable = true,
            generatedBy = generatedBy,
            generatedAt = now
        )

        val saved = reportingRepository.saveSnapshot(snapshot)

        // Record audit
        reportingRepository.recordAuditEvent(
            BusinessFinancialReportAuditEvent(
                auditId = "AUD-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = filter.tenantId,
                projectId = filter.projectId,
                reportType = filter.reportType,
                format = filter.format,
                requestedBy = generatedBy,
                generatedAt = now,
                isSuccess = true,
                correlationId = filter.correlationId
            )
        )

        return saved
    }

    override suspend fun getReportSnapshot(tenantId: String, snapshotId: String): BusinessFinancialReportSnapshot? {
        return reportingRepository.findSnapshotById(tenantId, snapshotId)
    }

    override suspend fun listReportSnapshots(
        tenantId: String,
        projectId: String,
        reportType: BusinessFinancialReportType?,
        periodId: String?,
        limit: Int
    ): List<BusinessFinancialReportSnapshot> {
        return reportingRepository.listSnapshots(tenantId, projectId, reportType, periodId, limit)
    }

    // --- 12. Export Engine ---

    override suspend fun exportReport(filter: BusinessFinancialReportFilter): BusinessFinancialExportDocument {
        val docId = "EXP-" + UUID.randomUUID().toString().take(12).uppercase()
        val now = System.currentTimeMillis()
        val fileNameBase = "FinancialReport_${filter.reportType.name}_${filter.projectId}_$now"

        val (contentStr, contentType, ext) = when (filter.format) {
            BusinessFinancialReportFormat.CSV -> {
                val csvData = generateCsvContent(filter)
                Triple(csvData, "text/csv", "csv")
            }
            BusinessFinancialReportFormat.PDF_TEXT -> {
                val textData = generateTextSummaryContent(filter)
                Triple(textData, "text/plain", "txt")
            }
            BusinessFinancialReportFormat.JSON -> {
                val jsonData = generateJsonSummaryContent(filter)
                Triple(jsonData, "application/json", "json")
            }
        }

        // Record audit event
        reportingRepository.recordAuditEvent(
            BusinessFinancialReportAuditEvent(
                auditId = "AUD-" + UUID.randomUUID().toString().take(12).uppercase(),
                tenantId = filter.tenantId,
                projectId = filter.projectId,
                reportType = filter.reportType,
                format = filter.format,
                requestedBy = filter.requestedBy,
                generatedAt = now,
                isSuccess = true,
                correlationId = filter.correlationId
            )
        )

        return BusinessFinancialExportDocument(
            documentId = docId,
            reportType = filter.reportType,
            format = filter.format,
            fileName = "$fileNameBase.$ext",
            contentType = contentType,
            contentString = contentStr,
            generatedAt = now,
            generatedBy = filter.requestedBy,
            correlationId = filter.correlationId
        )
    }

    private suspend fun generateCsvContent(filter: BusinessFinancialReportFilter): String {
        val sb = StringBuilder()
        when (filter.reportType) {
            BusinessFinancialReportType.EXECUTIVE_SUMMARY -> {
                val r = generateExecutiveSummary(filter)
                sb.appendLine("Metric,Value,Currency")
                sb.appendLine("Total Expenses,${r.totalExpenseAmount},${r.currency}")
                sb.appendLine("Approved Expenses,${r.approvedExpenseAmount},${r.currency}")
                sb.appendLine("Pending Expenses,${r.pendingExpenseAmount},${r.currency}")
                sb.appendLine("Total Payables,${r.totalPayableAmount},${r.currency}")
                sb.appendLine("Outstanding Payables,${r.outstandingPayableAmount},${r.currency}")
                sb.appendLine("Overdue Payables,${r.overduePayableAmount},${r.currency}")
                sb.appendLine("Total Ledger Debit,${r.totalLedgerDebit},${r.currency}")
                sb.appendLine("Total Ledger Credit,${r.totalLedgerCredit},${r.currency}")
                sb.appendLine("Net Movement,${r.netLedgerMovement},${r.currency}")
                sb.appendLine("Allocated Cost,${r.totalAllocatedCost},${r.currency}")
                sb.appendLine("Unallocated Cost,${r.totalUnallocatedCost},${r.currency}")
                sb.appendLine("Remaining Commitments,${r.remainingCommitmentAmount},${r.currency}")
                sb.appendLine("Outstanding Accruals,${r.outstandingAccrualAmount},${r.currency}")
                sb.appendLine("Period-End Readiness,${r.periodReadinessStatus.name},")
            }
            BusinessFinancialReportType.EXPENSE_ANALYTICS -> {
                val r = generateExpenseAnalytics(filter)
                sb.appendLine("Category,Count,Total Amount,Percentage")
                r.categoryBreakdown.forEach {
                    sb.appendLine("\"${it.category}\",${it.count},${it.totalAmount},${it.percentage}%")
                }
            }
            BusinessFinancialReportType.VENDOR_PAYABLES -> {
                val r = generateVendorPayableAnalytics(filter)
                sb.appendLine("Aging Bucket,Count,Amount,Currency")
                r.agingBuckets.forEach {
                    sb.appendLine("\"${it.label}\",${it.count},${it.amount},${it.currency}")
                }
            }
            BusinessFinancialReportType.BUSINESS_LEDGER -> {
                val r = generateLedgerReport(filter)
                sb.appendLine("Source Type,Debit,Credit,Net,Count")
                r.sourceBreakdowns.forEach {
                    sb.appendLine("\"${it.sourceType}\",${it.debitAmount},${it.creditAmount},${it.netAmount},${it.entryCount}")
                }
            }
            BusinessFinancialReportType.COST_CENTERS -> {
                val r = generateCostCenterReport(filter)
                sb.appendLine("Cost Center Code,Name,Allocated,Unallocated,Total,Jobs")
                r.costCenters.forEach {
                    sb.appendLine("\"${it.code}\",\"${it.name}\",${it.allocatedAmount},${it.unallocatedAmount},${it.totalTrackedAmount},${it.jobCount}")
                }
            }
            BusinessFinancialReportType.PROJECT_COSTS -> {
                val r = generateProjectCostReport(filter)
                sb.appendLine("Job ID,Cost Center,Recognized Cost,Allocated,Commitment,Accrual,Total Cost,Revenue,Gross Margin")
                r.jobCosts.forEach {
                    sb.appendLine("\"${it.jobId}\",\"${it.costCenterId}\",${it.actualRecognizedCost},${it.allocatedCost},${it.commitmentAmount},${it.accrualAmount},${it.totalCost},${it.canonicalRevenue ?: "N/A"},${it.grossMargin ?: "N/A"}")
                }
            }
            BusinessFinancialReportType.COMMITMENTS_ACCRUALS -> {
                val r = generateCommitmentAccrualReport(filter)
                sb.appendLine("ID,Type,Title,Cost Center,Original Amount,Remaining Amount,Status")
                r.items.forEach {
                    sb.appendLine("\"${it.id}\",\"${it.type}\",\"${it.title}\",\"${it.costCenterId}\",${it.originalAmount},${it.remainingOutstandingAmount},\"${it.status}\"")
                }
            }
            BusinessFinancialReportType.RECONCILIATION -> {
                val r = generateReconciliationReport(filter)
                sb.appendLine("Discrepancy ID,Source A,Source B,Amount A,Amount B,Variance,Resolved")
                r.discrepancies.forEach {
                    sb.appendLine("\"${it.discrepancyId}\",\"${it.sourceA}\",\"${it.sourceB}\",${it.amountA},${it.amountB},${it.variance},${it.isResolved}")
                }
            }
            BusinessFinancialReportType.ADJUSTMENTS -> {
                val r = generateAdjustmentReport(filter)
                sb.appendLine("Adjustment Type,Count,Total Amount,Currency")
                r.typeSummaries.forEach {
                    sb.appendLine("\"${it.adjustmentType}\",${it.count},${it.totalAmount},${it.currency}")
                }
            }
            BusinessFinancialReportType.PERIOD_END_READINESS -> {
                val r = generatePeriodEndReadinessReport(filter.tenantId, filter.projectId, filter.periodId ?: "CURRENT")
                sb.appendLine("Blocker Code,Category,Severity,Description")
                r.blockers.forEach {
                    sb.appendLine("\"${it.code}\",\"${it.category}\",\"${it.severity}\",\"${it.description}\"")
                }
            }
        }
        return sb.toString()
    }

    private suspend fun generateTextSummaryContent(filter: BusinessFinancialReportFilter): String {
        val summary = generateExecutiveSummary(filter)
        val sb = StringBuilder()
        sb.appendLine("================================================================")
        sb.appendLine("SUCHARU PRO ERP — BUSINESS FINANCIAL REPORT")
        sb.appendLine("Type: ${filter.reportType.name} | Tenant: ${filter.tenantId} | Project: ${filter.projectId}")
        sb.appendLine("Generated At: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        sb.appendLine("================================================================")
        sb.appendLine("Total Expenses:        ${summary.totalExpenseAmount} ${summary.currency}")
        sb.appendLine("Approved Expenses:     ${summary.approvedExpenseAmount} ${summary.currency}")
        sb.appendLine("Pending Expenses:      ${summary.pendingExpenseAmount} ${summary.currency}")
        sb.appendLine("Outstanding Payables:  ${summary.outstandingPayableAmount} ${summary.currency}")
        sb.appendLine("Overdue Payables:      ${summary.overduePayableAmount} ${summary.currency}")
        sb.appendLine("Ledger Net Movement:   ${summary.netLedgerMovement} ${summary.currency}")
        sb.appendLine("Allocated Cost:        ${summary.totalAllocatedCost} ${summary.currency}")
        sb.appendLine("Remaining Commitment:  ${summary.remainingCommitmentAmount} ${summary.currency}")
        sb.appendLine("Outstanding Accrual:   ${summary.outstandingAccrualAmount} ${summary.currency}")
        sb.appendLine("Period-End Readiness:  ${summary.periodReadinessStatus.name} (${summary.periodClosureBlockerCount} blockers)")
        sb.appendLine("================================================================")
        return sb.toString()
    }

    private suspend fun generateJsonSummaryContent(filter: BusinessFinancialReportFilter): String {
        val summary = generateExecutiveSummary(filter)
        return """
        {
            "tenantId": "${summary.tenantId}",
            "projectId": "${summary.projectId}",
            "reportType": "${filter.reportType.name}",
            "currency": "${summary.currency}",
            "totalExpenseAmount": ${summary.totalExpenseAmount},
            "approvedExpenseAmount": ${summary.approvedExpenseAmount},
            "pendingExpenseAmount": ${summary.pendingExpenseAmount},
            "totalPayableAmount": ${summary.totalPayableAmount},
            "outstandingPayableAmount": ${summary.outstandingPayableAmount},
            "overduePayableAmount": ${summary.overduePayableAmount},
            "totalLedgerDebit": ${summary.totalLedgerDebit},
            "totalLedgerCredit": ${summary.totalLedgerCredit},
            "netLedgerMovement": ${summary.netLedgerMovement},
            "totalAllocatedCost": ${summary.totalAllocatedCost},
            "totalUnallocatedCost": ${summary.totalUnallocatedCost},
            "remainingCommitmentAmount": ${summary.remainingCommitmentAmount},
            "outstandingAccrualAmount": ${summary.outstandingAccrualAmount},
            "reconciledAmount": ${summary.reconciledAmount},
            "unreconciledAmount": ${summary.unreconciledAmount},
            "openDiscrepancyCount": ${summary.openDiscrepancyCount},
            "totalAdjustmentAmount": ${summary.totalAdjustmentAmount},
            "totalRefundAmount": ${summary.totalRefundAmount},
            "totalWriteOffAmount": ${summary.totalWriteOffAmount},
            "periodReadinessStatus": "${summary.periodReadinessStatus.name}",
            "periodClosureBlockerCount": ${summary.periodClosureBlockerCount},
            "generatedAt": ${summary.generatedAt}
        }
        """.trimIndent()
    }
}

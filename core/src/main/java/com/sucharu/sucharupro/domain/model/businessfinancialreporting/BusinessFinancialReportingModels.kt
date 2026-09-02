package com.sucharu.sucharupro.domain.model.businessfinancialreporting

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

/**
 * Enumeration of supported financial report types.
 */
enum class BusinessFinancialReportType {
    EXECUTIVE_SUMMARY,
    EXPENSE_ANALYTICS,
    VENDOR_PAYABLES,
    BUSINESS_LEDGER,
    COST_CENTERS,
    PROJECT_COSTS,
    COMMITMENTS_ACCRUALS,
    RECONCILIATION,
    ADJUSTMENTS,
    PERIOD_END_READINESS
}

/**
 * Supported report output and export formats.
 */
enum class BusinessFinancialReportFormat {
    JSON,
    CSV,
    PDF_TEXT
}

/**
 * Standard aging buckets for payable analysis.
 */
enum class PayableAgingBucketType {
    CURRENT,
    DAYS_1_TO_30,
    DAYS_31_TO_60,
    DAYS_61_TO_90,
    DAYS_OVER_90
}

/**
 * Period-end closure readiness diagnostic status.
 */
enum class PeriodReadinessStatus {
    READY,
    NOT_READY
}

/**
 * Reusable report filter descriptor.
 */
data class BusinessFinancialReportFilter(
    val tenantId: String,
    val projectId: String,
    val reportType: BusinessFinancialReportType = BusinessFinancialReportType.EXECUTIVE_SUMMARY,
    val format: BusinessFinancialReportFormat = BusinessFinancialReportFormat.JSON,
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val periodId: String? = null,
    val branchId: String? = null,
    val currency: String = "BDT",
    val costCenterId: String? = null,
    val costCategoryId: String? = null,
    val jobId: String? = null,
    val vendorId: String? = null,
    val status: String? = null,
    val requestedBy: String = "system",
    val correlationId: String? = null
)

/**
 * 1. Executive Financial Summary Report.
 */
data class BusinessExecutiveFinancialSummary(
    val tenantId: String,
    val projectId: String,
    val periodId: String? = null,
    val currency: String = "BDT",
    // Expense Metrics
    val totalExpenseAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val approvedExpenseAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val pendingExpenseAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val expenseCount: Int = 0,
    // Vendor Payable Metrics
    val totalPayableAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val outstandingPayableAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val overduePayableAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val payableCount: Int = 0,
    // Ledger Metrics
    val totalLedgerDebit: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalLedgerCredit: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val netLedgerMovement: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val ledgerPostingCount: Int = 0,
    // Cost Tracking Metrics
    val totalAllocatedCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalUnallocatedCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val costCenterCount: Int = 0,
    val costCategoryCount: Int = 0,
    // Commitment & Accrual Metrics
    val activeCommitmentCount: Int = 0,
    val remainingCommitmentAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val activeAccrualCount: Int = 0,
    val outstandingAccrualAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    // Reconciliation Metrics
    val reconciledAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val unreconciledAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val openDiscrepancyCount: Int = 0,
    // Adjustment & Correction Metrics
    val adjustmentCount: Int = 0,
    val totalAdjustmentAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalRefundAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalWriteOffAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    // Period-End Readiness
    val periodReadinessStatus: PeriodReadinessStatus = PeriodReadinessStatus.READY,
    val periodClosureBlockerCount: Int = 0,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * 2. Expense Analytics Models.
 */
data class CategoryExpenseSummary(
    val category: String,
    val count: Int,
    val totalAmount: BigDecimal,
    val percentage: BigDecimal
)

data class CostCenterExpenseSummary(
    val costCenterId: String,
    val code: String,
    val name: String,
    val count: Int,
    val totalAmount: BigDecimal
)

data class PaymentMethodExpenseSummary(
    val paymentMethod: String,
    val count: Int,
    val totalAmount: BigDecimal
)

data class ExpenseTrendPoint(
    val dateLabel: String,
    val timestamp: Long,
    val amount: BigDecimal,
    val count: Int
)

data class BusinessExpenseAnalyticsReport(
    val tenantId: String,
    val projectId: String,
    val periodId: String? = null,
    val currency: String = "BDT",
    val totalAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val approvedAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val pendingAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val rejectedAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val cancelledAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val averageAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalCount: Int = 0,
    val approvedCount: Int = 0,
    val pendingCount: Int = 0,
    val categoryBreakdown: List<CategoryExpenseSummary> = emptyList(),
    val costCenterBreakdown: List<CostCenterExpenseSummary> = emptyList(),
    val paymentMethodBreakdown: List<PaymentMethodExpenseSummary> = emptyList(),
    val trend: List<ExpenseTrendPoint> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * 3. Vendor Payable Analytics Models.
 */
data class PayableAgingBucket(
    val bucketType: PayableAgingBucketType,
    val label: String,
    val count: Int,
    val amount: BigDecimal,
    val currency: String = "BDT"
)

data class VendorPayableBreakdown(
    val vendorId: String,
    val vendorName: String,
    val totalPayable: BigDecimal,
    val outstandingAmount: BigDecimal,
    val overdueAmount: BigDecimal,
    val billCount: Int,
    val currency: String = "BDT"
)

data class VendorPayableAnalyticsReport(
    val tenantId: String,
    val projectId: String,
    val currency: String = "BDT",
    val totalPayable: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val paidAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val outstandingAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val overdueAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val currentAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalBillsCount: Int = 0,
    val overdueBillsCount: Int = 0,
    val agingBuckets: List<PayableAgingBucket> = emptyList(),
    val vendorBreakdowns: List<VendorPayableBreakdown> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * 4. Business Ledger Report Models.
 */
data class LedgerSourceBreakdown(
    val sourceType: String,
    val debitAmount: BigDecimal,
    val creditAmount: BigDecimal,
    val netAmount: BigDecimal,
    val entryCount: Int
)

data class BusinessLedgerReport(
    val tenantId: String,
    val projectId: String,
    val currency: String = "BDT",
    val totalDebit: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalCredit: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val netMovement: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val postingCount: Int = 0,
    val reversalCount: Int = 0,
    val reversalTotalAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val manualAdjustmentCount: Int = 0,
    val manualAdjustmentTotalAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val sourceBreakdowns: List<LedgerSourceBreakdown> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * 5. Cost Center Report Models.
 */
data class CostCenterDetailSummary(
    val costCenterId: String,
    val code: String,
    val name: String,
    val allocatedAmount: BigDecimal,
    val unallocatedAmount: BigDecimal,
    val totalTrackedAmount: BigDecimal,
    val categoryBreakdown: Map<String, BigDecimal> = emptyMap(),
    val jobCount: Int = 0
)

data class BusinessCostCenterReport(
    val tenantId: String,
    val projectId: String,
    val currency: String = "BDT",
    val totalTrackedCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalAllocatedCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalUnallocatedCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val activeCostCentersCount: Int = 0,
    val activeCategoriesCount: Int = 0,
    val costCenters: List<CostCenterDetailSummary> = emptyList(),
    val topCategories: Map<String, BigDecimal> = emptyMap(),
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * 6. Job / Project Cost Report Models.
 */
data class JobProjectCostItem(
    val jobId: String,
    val costCenterId: String,
    val actualRecognizedCost: BigDecimal,
    val allocatedCost: BigDecimal,
    val commitmentAmount: BigDecimal,
    val accrualAmount: BigDecimal,
    val totalCost: BigDecimal,
    val canonicalRevenue: BigDecimal? = null,
    val grossMargin: BigDecimal? = null,
    val marginPercentage: BigDecimal? = null,
    val currency: String = "BDT"
)

data class JobProjectCostReport(
    val tenantId: String,
    val projectId: String,
    val currency: String = "BDT",
    val totalRecognizedCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalAllocatedCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalCommitmentCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalAccrualCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalCanonicalRevenue: BigDecimal? = null,
    val overallGrossMargin: BigDecimal? = null,
    val jobsCount: Int = 0,
    val jobCosts: List<JobProjectCostItem> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * 7. Commitment & Accrual Report Models.
 */
data class CommitmentAccrualItem(
    val id: String,
    val type: String, // COMMITMENT or ACCRUAL
    val title: String,
    val costCenterId: String,
    val originalAmount: BigDecimal,
    val consumedOrReversedAmount: BigDecimal,
    val remainingOutstandingAmount: BigDecimal,
    val status: String,
    val expiryOrReversalDate: Long?,
    val currency: String = "BDT"
)

data class CommitmentAccrualReport(
    val tenantId: String,
    val projectId: String,
    val periodId: String? = null,
    val currency: String = "BDT",
    // Commitment Totals
    val totalCommitmentAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val consumedCommitmentAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val remainingCommitmentAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val activeCommitmentCount: Int = 0,
    // Accrual Totals
    val totalAccrualAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val reversedAccrualAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val outstandingAccrualAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val activeAccrualCount: Int = 0,
    val items: List<CommitmentAccrualItem> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * 8. Reconciliation Report Models.
 */
data class ReconciliationDiscrepancySummaryItem(
    val discrepancyId: String,
    val sourceA: String,
    val sourceB: String,
    val amountA: BigDecimal,
    val amountB: BigDecimal,
    val variance: BigDecimal,
    val isResolved: Boolean,
    val reason: String?
)

data class BusinessReconciliationReport(
    val tenantId: String,
    val projectId: String,
    val periodId: String? = null,
    val currency: String = "BDT",
    val lastRunId: String? = null,
    val lastRunStatus: String? = null,
    val lastRunTimestamp: Long? = null,
    val reconciledAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val unreconciledAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalDiscrepanciesCount: Int = 0,
    val openDiscrepanciesCount: Int = 0,
    val resolvedDiscrepanciesCount: Int = 0,
    val isPeriodReconciliationPassed: Boolean = true,
    val discrepancies: List<ReconciliationDiscrepancySummaryItem> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * 9. Adjustment / Refund / Write-Off Report Models.
 */
data class AdjustmentTypeSummary(
    val adjustmentType: String,
    val count: Int,
    val totalAmount: BigDecimal,
    val currency: String = "BDT"
)

data class BusinessFinancialAdjustmentReport(
    val tenantId: String,
    val projectId: String,
    val periodId: String? = null,
    val currency: String = "BDT",
    val totalAdjustmentAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalRefundAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalWriteOffAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalReversalAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val pendingApprovalCount: Int = 0,
    val pendingApprovalAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val postedCount: Int = 0,
    val typeSummaries: List<AdjustmentTypeSummary> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * 10. Financial Period-End Readiness Report Models.
 */
data class PeriodClosureBlocker(
    val code: String,
    val category: String,
    val description: String,
    val severity: String = "CRITICAL",
    val entityId: String? = null
)

data class BusinessPeriodEndReadinessReport(
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val periodCode: String,
    val periodName: String,
    val periodStatus: String,
    val readinessStatus: PeriodReadinessStatus,
    val blockerCount: Int,
    val blockers: List<PeriodClosureBlocker> = emptyList(),
    val isReconciliationPassed: Boolean,
    val pendingExpensesCount: Int,
    val pendingAdjustmentsCount: Int,
    val unpostedAdjustmentsCount: Int,
    val outstandingAccrualsCount: Int,
    val openDiscrepanciesCount: Int,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Report Snapshot Container with Tamper-Evident Integrity Hash (SHA-256).
 */
data class BusinessFinancialReportSnapshot(
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String? = null,
    val reportType: BusinessFinancialReportType,
    val filterSummary: String,
    val metricsPayloadJson: String,
    val integrityHash: String,
    val isImmutable: Boolean = true,
    val generatedBy: String,
    val generatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun calculateIntegrityHash(
            tenantId: String,
            projectId: String,
            reportType: BusinessFinancialReportType,
            metricsPayloadJson: String,
            generatedAt: Long
        ): String {
            val raw = "$tenantId|$projectId|${reportType.name}|$metricsPayloadJson|$generatedAt"
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(raw.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }
    }
}

/**
 * Generated report document output container for downloads and exports.
 */
data class BusinessFinancialExportDocument(
    val documentId: String,
    val reportType: BusinessFinancialReportType,
    val format: BusinessFinancialReportFormat,
    val fileName: String,
    val contentType: String,
    val contentString: String,
    val contentBytes: ByteArray = contentString.toByteArray(Charsets.UTF_8),
    val generatedAt: Long = System.currentTimeMillis(),
    val generatedBy: String = "system",
    val correlationId: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Audit event for financial report generation.
 */
data class BusinessFinancialReportAuditEvent(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val reportType: BusinessFinancialReportType,
    val format: BusinessFinancialReportFormat,
    val requestedBy: String,
    val generatedAt: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true,
    val correlationId: String? = null,
    val errorMessage: String? = null
)

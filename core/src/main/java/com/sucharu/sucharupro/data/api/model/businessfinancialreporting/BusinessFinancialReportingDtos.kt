package com.sucharu.sucharupro.data.api.model.businessfinancialreporting

import java.math.BigDecimal

data class BusinessExecutiveFinancialSummaryDto(
    val tenantId: String,
    val projectId: String,
    val periodId: String?,
    val currency: String,
    val totalExpenseAmount: BigDecimal,
    val approvedExpenseAmount: BigDecimal,
    val pendingExpenseAmount: BigDecimal,
    val expenseCount: Int,
    val totalPayableAmount: BigDecimal,
    val outstandingPayableAmount: BigDecimal,
    val overduePayableAmount: BigDecimal,
    val payableCount: Int,
    val totalLedgerDebit: BigDecimal,
    val totalLedgerCredit: BigDecimal,
    val netLedgerMovement: BigDecimal,
    val ledgerPostingCount: Int,
    val totalAllocatedCost: BigDecimal,
    val totalUnallocatedCost: BigDecimal,
    val costCenterCount: Int,
    val costCategoryCount: Int,
    val activeCommitmentCount: Int,
    val remainingCommitmentAmount: BigDecimal,
    val activeAccrualCount: Int,
    val outstandingAccrualAmount: BigDecimal,
    val reconciledAmount: BigDecimal,
    val unreconciledAmount: BigDecimal,
    val openDiscrepancyCount: Int,
    val adjustmentCount: Int,
    val totalAdjustmentAmount: BigDecimal,
    val totalRefundAmount: BigDecimal,
    val totalWriteOffAmount: BigDecimal,
    val periodReadinessStatus: String,
    val periodClosureBlockerCount: Int,
    val generatedAt: Long
)

data class CategoryExpenseSummaryDto(
    val category: String,
    val count: Int,
    val totalAmount: BigDecimal,
    val percentage: BigDecimal
)

data class CostCenterExpenseSummaryDto(
    val costCenterId: String,
    val code: String,
    val name: String,
    val count: Int,
    val totalAmount: BigDecimal
)

data class PaymentMethodExpenseSummaryDto(
    val paymentMethod: String,
    val count: Int,
    val totalAmount: BigDecimal
)

data class ExpenseTrendPointDto(
    val dateLabel: String,
    val timestamp: Long,
    val amount: BigDecimal,
    val count: Int
)

data class BusinessExpenseAnalyticsReportDto(
    val tenantId: String,
    val projectId: String,
    val periodId: String?,
    val currency: String,
    val totalAmount: BigDecimal,
    val approvedAmount: BigDecimal,
    val pendingAmount: BigDecimal,
    val rejectedAmount: BigDecimal,
    val cancelledAmount: BigDecimal,
    val averageAmount: BigDecimal,
    val totalCount: Int,
    val approvedCount: Int,
    val pendingCount: Int,
    val categoryBreakdown: List<CategoryExpenseSummaryDto>,
    val costCenterBreakdown: List<CostCenterExpenseSummaryDto>,
    val paymentMethodBreakdown: List<PaymentMethodExpenseSummaryDto>,
    val trend: List<ExpenseTrendPointDto>,
    val generatedAt: Long
)

data class PayableAgingBucketDto(
    val bucketType: String,
    val label: String,
    val count: Int,
    val amount: BigDecimal,
    val currency: String
)

data class VendorPayableBreakdownDto(
    val vendorId: String,
    val vendorName: String,
    val totalPayable: BigDecimal,
    val outstandingAmount: BigDecimal,
    val overdueAmount: BigDecimal,
    val billCount: Int,
    val currency: String
)

data class VendorPayableAnalyticsReportDto(
    val tenantId: String,
    val projectId: String,
    val currency: String,
    val totalPayable: BigDecimal,
    val paidAmount: BigDecimal,
    val outstandingAmount: BigDecimal,
    val overdueAmount: BigDecimal,
    val currentAmount: BigDecimal,
    val totalBillsCount: Int,
    val overdueBillsCount: Int,
    val agingBuckets: List<PayableAgingBucketDto>,
    val vendorBreakdowns: List<VendorPayableBreakdownDto>,
    val generatedAt: Long
)

data class LedgerSourceBreakdownDto(
    val sourceType: String,
    val debitAmount: BigDecimal,
    val creditAmount: BigDecimal,
    val netAmount: BigDecimal,
    val entryCount: Int
)

data class BusinessLedgerReportDto(
    val tenantId: String,
    val projectId: String,
    val currency: String,
    val totalDebit: BigDecimal,
    val totalCredit: BigDecimal,
    val netMovement: BigDecimal,
    val postingCount: Int,
    val reversalCount: Int,
    val reversalTotalAmount: BigDecimal,
    val manualAdjustmentCount: Int,
    val manualAdjustmentTotalAmount: BigDecimal,
    val sourceBreakdowns: List<LedgerSourceBreakdownDto>,
    val generatedAt: Long
)

data class CostCenterDetailSummaryDto(
    val costCenterId: String,
    val code: String,
    val name: String,
    val allocatedAmount: BigDecimal,
    val unallocatedAmount: BigDecimal,
    val totalTrackedAmount: BigDecimal,
    val categoryBreakdown: Map<String, BigDecimal>,
    val jobCount: Int
)

data class BusinessCostCenterReportDto(
    val tenantId: String,
    val projectId: String,
    val currency: String,
    val totalTrackedCost: BigDecimal,
    val totalAllocatedCost: BigDecimal,
    val totalUnallocatedCost: BigDecimal,
    val activeCostCentersCount: Int,
    val activeCategoriesCount: Int,
    val costCenters: List<CostCenterDetailSummaryDto>,
    val topCategories: Map<String, BigDecimal>,
    val generatedAt: Long
)

data class JobProjectCostItemDto(
    val jobId: String,
    val costCenterId: String,
    val actualRecognizedCost: BigDecimal,
    val allocatedCost: BigDecimal,
    val commitmentAmount: BigDecimal,
    val accrualAmount: BigDecimal,
    val totalCost: BigDecimal,
    val canonicalRevenue: BigDecimal?,
    val grossMargin: BigDecimal?,
    val marginPercentage: BigDecimal?,
    val currency: String
)

data class JobProjectCostReportDto(
    val tenantId: String,
    val projectId: String,
    val currency: String,
    val totalRecognizedCost: BigDecimal,
    val totalAllocatedCost: BigDecimal,
    val totalCommitmentCost: BigDecimal,
    val totalAccrualCost: BigDecimal,
    val totalCanonicalRevenue: BigDecimal?,
    val overallGrossMargin: BigDecimal?,
    val jobsCount: Int,
    val jobCosts: List<JobProjectCostItemDto>,
    val generatedAt: Long
)

data class CommitmentAccrualItemDto(
    val id: String,
    val type: String,
    val title: String,
    val costCenterId: String,
    val originalAmount: BigDecimal,
    val consumedOrReversedAmount: BigDecimal,
    val remainingOutstandingAmount: BigDecimal,
    val status: String,
    val expiryOrReversalDate: Long?,
    val currency: String
)

data class CommitmentAccrualReportDto(
    val tenantId: String,
    val projectId: String,
    val periodId: String?,
    val currency: String,
    val totalCommitmentAmount: BigDecimal,
    val consumedCommitmentAmount: BigDecimal,
    val remainingCommitmentAmount: BigDecimal,
    val activeCommitmentCount: Int,
    val totalAccrualAmount: BigDecimal,
    val reversedAccrualAmount: BigDecimal,
    val outstandingAccrualAmount: BigDecimal,
    val activeAccrualCount: Int,
    val items: List<CommitmentAccrualItemDto>,
    val generatedAt: Long
)

data class ReconciliationDiscrepancySummaryItemDto(
    val discrepancyId: String,
    val sourceA: String,
    val sourceB: String,
    val amountA: BigDecimal,
    val amountB: BigDecimal,
    val variance: BigDecimal,
    val isResolved: Boolean,
    val reason: String?
)

data class BusinessReconciliationReportDto(
    val tenantId: String,
    val projectId: String,
    val periodId: String?,
    val currency: String,
    val lastRunId: String?,
    val lastRunStatus: String?,
    val lastRunTimestamp: Long?,
    val reconciledAmount: BigDecimal,
    val unreconciledAmount: BigDecimal,
    val totalDiscrepanciesCount: Int,
    val openDiscrepanciesCount: Int,
    val resolvedDiscrepanciesCount: Int,
    val isPeriodReconciliationPassed: Boolean,
    val discrepancies: List<ReconciliationDiscrepancySummaryItemDto>,
    val generatedAt: Long
)

data class AdjustmentTypeSummaryDto(
    val adjustmentType: String,
    val count: Int,
    val totalAmount: BigDecimal,
    val currency: String
)

data class BusinessFinancialAdjustmentReportDto(
    val tenantId: String,
    val projectId: String,
    val periodId: String?,
    val currency: String,
    val totalAdjustmentAmount: BigDecimal,
    val totalRefundAmount: BigDecimal,
    val totalWriteOffAmount: BigDecimal,
    val totalReversalAmount: BigDecimal,
    val pendingApprovalCount: Int,
    val pendingApprovalAmount: BigDecimal,
    val postedCount: Int,
    val typeSummaries: List<AdjustmentTypeSummaryDto>,
    val generatedAt: Long
)

data class PeriodClosureBlockerDto(
    val code: String,
    val category: String,
    val description: String,
    val severity: String,
    val entityId: String?
)

data class BusinessPeriodEndReadinessReportDto(
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val periodCode: String,
    val periodName: String,
    val periodStatus: String,
    val readinessStatus: String,
    val blockerCount: Int,
    val blockers: List<PeriodClosureBlockerDto>,
    val isReconciliationPassed: Boolean,
    val pendingExpensesCount: Int,
    val pendingAdjustmentsCount: Int,
    val unpostedAdjustmentsCount: Int,
    val outstandingAccrualsCount: Int,
    val openDiscrepanciesCount: Int,
    val generatedAt: Long
)

data class CreateReportSnapshotRequestDto(
    val reportType: String,
    val periodId: String? = null,
    val currency: String = "BDT",
    val metricsJson: String
)

data class BusinessFinancialReportSnapshotDto(
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String?,
    val reportType: String,
    val filterSummary: String,
    val metricsPayloadJson: String,
    val integrityHash: String,
    val isImmutable: Boolean,
    val generatedBy: String,
    val generatedAt: Long
)

data class ExportFinancialReportRequestDto(
    val reportType: String,
    val format: String = "CSV",
    val periodId: String? = null,
    val currency: String = "BDT",
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val costCenterId: String? = null,
    val costCategoryId: String? = null,
    val jobId: String? = null,
    val vendorId: String? = null,
    val status: String? = null
)

data class BusinessFinancialExportDocumentDto(
    val documentId: String,
    val reportType: String,
    val format: String,
    val fileName: String,
    val contentType: String,
    val contentString: String,
    val generatedAt: Long,
    val generatedBy: String
)

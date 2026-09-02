package com.sucharu.sucharupro.domain.model.vendor

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Vendor Settlement Allocation representing a specific allocation of funds
 * against an approved vendor payable obligation (Module 12 Step 10).
 */
data class VendorSettlementAllocation(
    val allocationId: String,
    val settlementId: String,
    val payableId: String,
    val invoiceId: String? = null,
    val allocatedAmount: Money = Money.ZERO,
    val currency: String = "BDT",
    val status: String = "ALLOCATED",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system"
)

/**
 * Master aggregate representing a Vendor Settlement orchestration (Module 12 Step 10).
 */
data class VendorSettlement(
    val settlementId: String,
    val projectId: String,
    val tenantId: String = "TENANT-001",
    val vendorId: String,
    val settlementNumber: String,
    val settlementDate: Long = System.currentTimeMillis(),
    val currency: String = "BDT",
    val totalAmount: Money = Money.ZERO,
    val status: VendorSettlementStatus = VendorSettlementStatus.DRAFT,
    val settlementMethod: SettlementMethod = SettlementMethod.BANK_TRANSFER,
    val referenceNumber: String? = null,
    val paymentId: String? = null,
    val notes: String? = null,
    val approvedBy: String? = null,
    val approvedAt: Long? = null,
    val settledAt: Long? = null,
    val allocations: List<VendorSettlementAllocation> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)

/**
 * Result of evaluating settlement eligibility for a vendor or payable obligation.
 */
data class SettlementEligibilityResult(
    val vendorId: String,
    val payableId: String? = null,
    val status: SettlementEligibility = SettlementEligibility.ELIGIBLE,
    val isEligible: Boolean = true,
    val reasons: List<String> = emptyList(),
    val payableReferences: List<String> = emptyList(),
    val grossPayable: Money = Money.ZERO,
    val approvedAmount: Money = Money.ZERO,
    val previouslySettledAmount: Money = Money.ZERO,
    val creditsAmount: Money = Money.ZERO,
    val outstandingAmount: Money = Money.ZERO,
    val currency: String = "BDT"
)

/**
 * Reconciliation result comparing Vendor Payable vs Settlement vs Payment vs Ledger.
 */
data class VendorReconciliationResult(
    val reconciliationId: String,
    val vendorId: String,
    val projectId: String,
    val tenantId: String = "TENANT-001",
    val settlementId: String? = null,
    val payableId: String? = null,
    val paymentId: String? = null,
    val status: ReconciliationStatus = ReconciliationStatus.MATCHED,
    val expectedAmount: Money = Money.ZERO,
    val settledAmount: Money = Money.ZERO,
    val paidAmount: Money = Money.ZERO,
    val ledgerAmount: Money = Money.ZERO,
    val variance: Money = Money.ZERO,
    val reasons: List<String> = emptyList(),
    val reconciledAt: Long = System.currentTimeMillis(),
    val reconciledBy: String = "system"
)

/**
 * Comprehensive vendor financial analytics summary.
 */
data class VendorFinancialSummary(
    val vendorId: String,
    val currency: String = "BDT",
    val totalPoValue: Money = Money.ZERO,
    val totalInvoicedValue: Money = Money.ZERO,
    val totalApprovedPayable: Money = Money.ZERO,
    val totalSettledAmount: Money = Money.ZERO,
    val totalOutstandingPayable: Money = Money.ZERO,
    val averageInvoiceValue: Money = Money.ZERO,
    val paymentCycleDays: Double = 0.0,
    val priceVarianceAmount: Money = Money.ZERO,
    val creditAdjustmentAmount: Money = Money.ZERO,
    val disputeExposureAmount: Money = Money.ZERO
)

/**
 * Vendor operational analytics summary.
 */
data class VendorOperationalSummary(
    val vendorId: String,
    val orderCount: Int = 0,
    val openOrders: Int = 0,
    val completedOrders: Int = 0,
    val deliveryCount: Int = 0,
    val acceptedQuantity: Double = 0.0,
    val rejectedQuantity: Double = 0.0,
    val onTimeDeliveryRate: Double = 100.0,
    val partialReceiptRate: Double = 0.0,
    val inspectedQuantity: Double = 0.0,
    val defectRate: Double = 0.0,
    val rejectionRate: Double = 0.0,
    val invoiceCount: Int = 0,
    val matchedInvoiceCount: Int = 0,
    val mismatchRate: Double = 0.0,
    val openDisputes: Int = 0,
    val resolvedDisputes: Int = 0,
    val assignedJobs: Int = 0,
    val completedJobs: Int = 0,
    val jobCompletionRate: Double = 100.0
)

/**
 * Quality analytics summary.
 */
data class VendorQualitySummary(
    val vendorId: String,
    val inspectedQuantity: Double = 0.0,
    val acceptedQuantity: Double = 0.0,
    val rejectedQuantity: Double = 0.0,
    val defectRate: Double = 0.0,
    val rejectionRate: Double = 0.0,
    val openDefectsCount: Int = 0,
    val criticalDefectsCount: Int = 0
)

/**
 * Delivery analytics summary.
 */
data class VendorDeliverySummary(
    val vendorId: String,
    val deliveryReceiptCount: Int = 0,
    val receivedQuantity: Double = 0.0,
    val acceptedQuantity: Double = 0.0,
    val rejectedQuantity: Double = 0.0,
    val onTimeDeliveryRate: Double = 100.0,
    val delayedDeliveryCount: Int = 0
)

/**
 * Invoice and matching analytics summary.
 */
data class VendorInvoiceSummary(
    val vendorId: String,
    val invoiceCount: Int = 0,
    val matchedCount: Int = 0,
    val unmatchedCount: Int = 0,
    val totalInvoiced: Money = Money.ZERO,
    val totalApproved: Money = Money.ZERO,
    val exceptionCount: Int = 0
)

/**
 * Performance scoring analytics summary.
 */
data class VendorPerformanceSummary(
    val vendorId: String,
    val latestScore: Double = 0.0,
    val rating: String = "SATISFACTORY",
    val riskLevel: String = "LOW",
    val scorecardCount: Int = 0,
    val evaluationCount: Int = 0,
    val openCapaCount: Int = 0,
    val resolvedCapaCount: Int = 0
)

/**
 * Compliance analytics summary.
 */
data class VendorComplianceSummary(
    val vendorId: String,
    val totalRequirements: Int = 0,
    val verifiedCount: Int = 0,
    val pendingCount: Int = 0,
    val expiringSoonCount: Int = 0,
    val expiredCount: Int = 0,
    val complianceScore: Double = 100.0,
    val criticalRisksCount: Int = 0
)

/**
 * Risk analytics summary.
 */
data class VendorRiskSummary(
    val vendorId: String,
    val overallRiskLevel: String = "LOW",
    val activeRiskIndicators: List<String> = emptyList(),
    val criticalIssuesCount: Int = 0,
    val unresolvedDisputesCount: Int = 0,
    val overdueCapaCount: Int = 0
)

/**
 * Unified Vendor 360 read model.
 */
data class Vendor360Summary(
    val vendorId: String,
    val vendorCode: String,
    val vendorName: String,
    val status: String,
    val financial: VendorFinancialSummary,
    val operational: VendorOperationalSummary,
    val quality: VendorQualitySummary,
    val delivery: VendorDeliverySummary,
    val invoice: VendorInvoiceSummary,
    val performance: VendorPerformanceSummary,
    val compliance: VendorComplianceSummary,
    val risk: VendorRiskSummary
)

/**
 * Immutable historical analytics snapshot.
 */
data class VendorAnalyticsSnapshot(
    val snapshotId: String,
    val vendorId: String,
    val projectId: String,
    val tenantId: String = "TENANT-001",
    val period: AnalyticsPeriod = AnalyticsPeriod.MONTHLY,
    val startDate: Long,
    val endDate: Long,
    val generatedAt: Long = System.currentTimeMillis(),
    val generatedBy: String = "system",
    val calculationVersion: String = "1.0.0",
    val metricsJson: String = "{}"
)

/**
 * Vendor Settlement audit trail record.
 */
data class VendorSettlementAuditEvent(
    val eventId: String,
    val settlementId: String,
    val vendorId: String,
    val projectId: String,
    val tenantId: String = "TENANT-001",
    val eventType: VendorSettlementAuditEventType,
    val details: String,
    val actor: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Trend point for vendor analytics charts.
 */
data class VendorAnalyticsTrendPoint(
    val periodKey: String,
    val timestamp: Long,
    val poValue: Double = 0.0,
    val invoicedValue: Double = 0.0,
    val settledValue: Double = 0.0,
    val qualityScore: Double = 100.0,
    val onTimeDeliveryRate: Double = 100.0,
    val performanceScore: Double = 100.0
)

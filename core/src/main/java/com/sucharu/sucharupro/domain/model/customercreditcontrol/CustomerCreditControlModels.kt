package com.sucharu.sucharupro.domain.model.customercreditcontrol

import java.math.BigDecimal

/**
 * Customer Payment Terms Types (Module 14 Step 07).
 */
enum class CustomerPaymentTermsType(val defaultDays: Int) {
    PREPAID(0),
    DUE_ON_RECEIPT(0),
    NET_7(7),
    NET_15(15),
    NET_30(30),
    NET_45(45),
    NET_60(60),
    CUSTOM(0);
}

/**
 * Customer Financial Risk Status enum with deterministic precedence:
 * FINANCIAL_HOLD > OVERDUE > OVER_LIMIT > LIMIT_REACHED > ADVANCE_REQUIRED > WATCH > NORMAL
 */
enum class CustomerCreditRiskStatus {
    FINANCIAL_HOLD,
    OVERDUE,
    OVER_LIMIT,
    LIMIT_REACHED,
    ADVANCE_REQUIRED,
    WATCH,
    NORMAL;
}

/**
 * Receivable Aging Buckets.
 */
enum class ReceivableAgingBucket(val label: String) {
    CURRENT("Current"),
    DAYS_1_7("1–7 Days Overdue"),
    DAYS_8_30("8–30 Days Overdue"),
    DAYS_31_60("31–60 Days Overdue"),
    DAYS_61_90("61–90 Days Overdue"),
    DAYS_90_PLUS("90+ Days Overdue");
}

/**
 * Customer Credit Profile Aggregate Entity.
 */
data class CustomerCreditProfileEntity(
    val profileId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val creditLimit: BigDecimal = BigDecimal.ZERO,
    val currency: String = "BDT",
    val paymentTermsType: CustomerPaymentTermsType = CustomerPaymentTermsType.DUE_ON_RECEIPT,
    val creditDays: Int = 0,
    val requiresAdvance: Boolean = false,
    val financialHold: Boolean = false,
    val holdReason: String? = null,
    val holdPlacedAt: Long? = null,
    val holdPlacedBy: String? = null,
    val effectiveFrom: Long? = null,
    val effectiveUntil: Long? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1
)

/**
 * Request for credit evaluation during order/quotation workflow.
 */
data class CustomerCreditCheckRequest(
    val customerId: String,
    val requestedExposure: BigDecimal,
    val orderId: String? = null,
    val quotationId: String? = null,
    val notes: String? = null
)

/**
 * Result of customer credit evaluation.
 */
data class CustomerCreditCheckResult(
    val customerId: String,
    val approved: Boolean,
    val creditLimit: BigDecimal,
    val currentExposure: BigDecimal,
    val availableCredit: BigDecimal,
    val requestedExposure: BigDecimal,
    val projectedExposure: BigDecimal,
    val riskStatus: CustomerCreditRiskStatus,
    val reason: String,
    val failureCode: String? = null
)

/**
 * Aging Bucket Summary breakdown item.
 */
data class AgingBucketSummary(
    val bucket: ReceivableAgingBucket,
    val invoiceCount: Int,
    val outstandingAmount: BigDecimal
)

/**
 * Comprehensive Receivable Aging Report for a customer.
 */
data class CustomerReceivableAgingReport(
    val customerId: String,
    val asOfDate: Long,
    val totalOutstanding: BigDecimal,
    val buckets: List<AgingBucketSummary>,
    val oldestOverdueDate: Long? = null,
    val maxDaysOverdue: Int = 0
)

/**
 * Comprehensive Customer Receivable Risk Summary.
 */
data class CustomerReceivableRiskSummary(
    val customerId: String,
    val creditLimit: BigDecimal,
    val totalInvoiced: BigDecimal,
    val totalPaid: BigDecimal,
    val currentOutstanding: BigDecimal,
    val totalUnallocatedPayment: BigDecimal,
    val totalAvailableCredit: BigDecimal,
    val netReceivableExposure: BigDecimal,
    val availableCreditLimit: BigDecimal,
    val overdueAmount: BigDecimal,
    val overdueInvoiceCount: Int,
    val oldestDueInvoiceDate: Long?,
    val paymentTermsType: CustomerPaymentTermsType,
    val creditDays: Int,
    val requiresAdvance: Boolean,
    val financialHold: Boolean,
    val holdReason: String?,
    val riskStatus: CustomerCreditRiskStatus
)

/**
 * Audit event for customer credit policy and financial hold actions.
 */
data class CustomerCreditControlAuditEvent(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val previousValueJson: String? = null,
    val newValueJson: String? = null,
    val reason: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val metadataJson: String? = null
)

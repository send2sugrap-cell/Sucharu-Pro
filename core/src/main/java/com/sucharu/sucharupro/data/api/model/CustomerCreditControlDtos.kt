package com.sucharu.sucharupro.data.api.model

import com.sucharu.sucharupro.domain.model.customercreditcontrol.*
import java.math.BigDecimal

/**
 * DTOs and Mappers for Customer Credit Limits, Payment Terms & Receivable Risk Control (Module 14 Step 07).
 */

data class CustomerCreditProfileDto(
    val profileId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val creditLimit: BigDecimal,
    val currency: String,
    val paymentTermsType: String,
    val creditDays: Int,
    val requiresAdvance: Boolean,
    val financialHold: Boolean,
    val holdReason: String?,
    val holdPlacedAt: Long?,
    val holdPlacedBy: String?,
    val effectiveFrom: Long?,
    val effectiveUntil: Long?,
    val notes: String?,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val updatedBy: String,
    val version: Long
)

data class UpdateCustomerCreditProfileRequest(
    val creditLimit: BigDecimal,
    val currency: String = "BDT",
    val paymentTermsType: String = "DUE_ON_RECEIPT",
    val creditDays: Int = 0,
    val requiresAdvance: Boolean = false,
    val notes: String? = null,
    val reason: String = "Credit profile update"
)

data class CustomerFinancialHoldRequest(
    val reason: String
)

data class CustomerCreditCheckApiRequest(
    val requestedExposure: BigDecimal,
    val orderId: String? = null,
    val quotationId: String? = null,
    val notes: String? = null
)

data class CustomerCreditCheckResultDto(
    val customerId: String,
    val approved: Boolean,
    val creditLimit: BigDecimal,
    val currentExposure: BigDecimal,
    val availableCredit: BigDecimal,
    val requestedExposure: BigDecimal,
    val projectedExposure: BigDecimal,
    val riskStatus: String,
    val reason: String,
    val failureCode: String?
)

data class AgingBucketSummaryDto(
    val bucket: String,
    val label: String,
    val invoiceCount: Int,
    val outstandingAmount: BigDecimal
)

data class CustomerReceivableAgingReportDto(
    val customerId: String,
    val asOfDate: Long,
    val totalOutstanding: BigDecimal,
    val buckets: List<AgingBucketSummaryDto>,
    val oldestOverdueDate: Long?,
    val maxDaysOverdue: Int
)

data class CustomerReceivableRiskSummaryDto(
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
    val paymentTermsType: String,
    val creditDays: Int,
    val requiresAdvance: Boolean,
    val financialHold: Boolean,
    val holdReason: String?,
    val riskStatus: String
)

data class CustomerCreditControlAuditEventDto(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val previousValueJson: String?,
    val newValueJson: String?,
    val reason: String?,
    val occurredAt: Long,
    val metadataJson: String?
)

fun CustomerCreditProfileEntity.toDto() = CustomerCreditProfileDto(
    profileId = profileId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    creditLimit = creditLimit,
    currency = currency,
    paymentTermsType = paymentTermsType.name,
    creditDays = creditDays,
    requiresAdvance = requiresAdvance,
    financialHold = financialHold,
    holdReason = holdReason,
    holdPlacedAt = holdPlacedAt,
    holdPlacedBy = holdPlacedBy,
    effectiveFrom = effectiveFrom,
    effectiveUntil = effectiveUntil,
    notes = notes,
    createdAt = createdAt,
    createdBy = createdBy,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

fun CustomerCreditCheckResult.toDto() = CustomerCreditCheckResultDto(
    customerId = customerId,
    approved = approved,
    creditLimit = creditLimit,
    currentExposure = currentExposure,
    availableCredit = availableCredit,
    requestedExposure = requestedExposure,
    projectedExposure = projectedExposure,
    riskStatus = riskStatus.name,
    reason = reason,
    failureCode = failureCode
)

fun CustomerReceivableAgingReport.toDto() = CustomerReceivableAgingReportDto(
    customerId = customerId,
    asOfDate = asOfDate,
    totalOutstanding = totalOutstanding,
    buckets = buckets.map {
        AgingBucketSummaryDto(
            bucket = it.bucket.name,
            label = it.bucket.label,
            invoiceCount = it.invoiceCount,
            outstandingAmount = it.outstandingAmount
        )
    },
    oldestOverdueDate = oldestOverdueDate,
    maxDaysOverdue = maxDaysOverdue
)

fun CustomerReceivableRiskSummary.toDto() = CustomerReceivableRiskSummaryDto(
    customerId = customerId,
    creditLimit = creditLimit,
    totalInvoiced = totalInvoiced,
    totalPaid = totalPaid,
    currentOutstanding = currentOutstanding,
    totalUnallocatedPayment = totalUnallocatedPayment,
    totalAvailableCredit = totalAvailableCredit,
    netReceivableExposure = netReceivableExposure,
    availableCreditLimit = availableCreditLimit,
    overdueAmount = overdueAmount,
    overdueInvoiceCount = overdueInvoiceCount,
    oldestDueInvoiceDate = oldestDueInvoiceDate,
    paymentTermsType = paymentTermsType.name,
    creditDays = creditDays,
    requiresAdvance = requiresAdvance,
    financialHold = financialHold,
    holdReason = holdReason,
    riskStatus = riskStatus.name
)

fun CustomerCreditControlAuditEvent.toDto() = CustomerCreditControlAuditEventDto(
    auditId = auditId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    actorId = actorId,
    actorRole = actorRole,
    action = action,
    previousValueJson = previousValueJson,
    newValueJson = newValueJson,
    reason = reason,
    occurredAt = occurredAt,
    metadataJson = metadataJson
)

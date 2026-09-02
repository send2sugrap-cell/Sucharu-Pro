package com.sucharu.sucharupro.data.api.model.commercialcommitment

import com.sucharu.sucharupro.domain.model.commercialcommitment.*
import java.math.BigDecimal

/**
 * Data Transfer Objects for Module 17 Step 03 Commercial Commitment & Conversion Engine.
 */

data class CommercialCommitmentDto(
    val commitmentId: String,
    val tenantId: String,
    val projectId: String,
    val quotationId: String,
    val quotationVersion: Int,
    val customerId: String,
    val orderId: String?,
    val orderNumber: String?,
    val status: String,
    val committedQuantity: Long,
    val approvedUnitPrice: BigDecimal,
    val approvedSubtotal: BigDecimal,
    val approvedDiscount: BigDecimal,
    val approvedTax: BigDecimal,
    val approvedGrandTotal: BigDecimal,
    val currency: String,
    val paymentTerms: String,
    val deliveryTerms: String?,
    val conversionNotes: String?,
    val idempotencyKey: String?,
    val integrityHash: String,
    val createdAt: Long,
    val createdBy: String,
    val convertedAt: Long?,
    val convertedBy: String?
)

data class CommercialCommitmentEventDto(
    val eventId: String,
    val commitmentId: String,
    val tenantId: String,
    val projectId: String,
    val eventType: String,
    val actor: String,
    val actorRole: String,
    val detailsJson: String?,
    val occurredAt: Long
)

data class ConversionEligibilityDto(
    val isEligible: Boolean,
    val reasons: List<String>,
    val quotationId: String,
    val quotationVersion: Int,
    val quotationStatus: String,
    val customerId: String,
    val orderedQuantity: Long,
    val approvedGrandTotal: BigDecimal,
    val currency: String,
    val existingCommitmentId: String?,
    val existingOrderId: String?,
    val evaluatedAt: Long
)

data class ConvertQuotationToOrderRequestDto(
    val targetVersionNumber: Int? = null,
    val requestedQuantity: Long? = null,
    val customOrderNumber: String? = null,
    val priority: String = "NORMAL",
    val paymentTerms: String? = null,
    val deliveryTerms: String? = null,
    val notes: String? = null,
    val idempotencyKey: String? = null
)

data class ConversionResultDto(
    val isSuccess: Boolean,
    val commitment: CommercialCommitmentDto,
    val orderId: String,
    val orderNumber: String,
    val customerId: String,
    val committedAmount: BigDecimal,
    val currency: String,
    val message: String
)

data class CommercialCommitmentReconciliationDto(
    val commitmentId: String,
    val quotationId: String,
    val orderId: String?,
    val isFullyReconciled: Boolean,
    val customerMatch: Boolean,
    val currencyMatch: Boolean,
    val quantityMatch: Boolean,
    val amountMatch: Boolean,
    val statusIntegrityMatch: Boolean,
    val discrepancies: List<String>,
    val reconciledAt: Long
)

data class CommercialCommitmentHandoffDto(
    val handoffId: String,
    val contractVersion: String,
    val commitmentId: String,
    val tenantId: String,
    val projectId: String,
    val quotationId: String,
    val quotationVersion: Int,
    val quotationNumber: String,
    val customerId: String,
    val orderId: String?,
    val orderNumber: String?,
    val commitmentStatus: String,
    val committedQuantity: Long,
    val approvedUnitPrice: String,
    val approvedSubtotal: String,
    val approvedDiscount: String,
    val approvedTax: String,
    val approvedGrandTotal: String,
    val currency: String,
    val paymentTerms: String,
    val deliveryTerms: String?,
    val conversionTimestamp: Long?,
    val reconciliationStatus: String,
    val isReadOnly: Boolean,
    val isMutable: Boolean,
    val integrityHash: String,
    val generatedAt: Long
)

// ============================================================
// DTO MAPPERS
// ============================================================

fun CommercialCommitment.toDto(): CommercialCommitmentDto = CommercialCommitmentDto(
    commitmentId = commitmentId,
    tenantId = tenantId,
    projectId = projectId,
    quotationId = quotationId,
    quotationVersion = quotationVersion,
    customerId = customerId,
    orderId = orderId,
    orderNumber = orderNumber,
    status = status.name,
    committedQuantity = committedQuantity,
    approvedUnitPrice = approvedUnitPrice,
    approvedSubtotal = approvedSubtotal,
    approvedDiscount = approvedDiscount,
    approvedTax = approvedTax,
    approvedGrandTotal = approvedGrandTotal,
    currency = currency,
    paymentTerms = paymentTerms,
    deliveryTerms = deliveryTerms,
    conversionNotes = conversionNotes,
    idempotencyKey = idempotencyKey,
    integrityHash = integrityHash,
    createdAt = createdAt,
    createdBy = createdBy,
    convertedAt = convertedAt,
    convertedBy = convertedBy
)

fun CommercialCommitmentEvent.toDto(): CommercialCommitmentEventDto = CommercialCommitmentEventDto(
    eventId = eventId,
    commitmentId = commitmentId,
    tenantId = tenantId,
    projectId = projectId,
    eventType = eventType.name,
    actor = actor,
    actorRole = actorRole,
    detailsJson = detailsJson,
    occurredAt = occurredAt
)

fun ConversionEligibility.toDto(): ConversionEligibilityDto = ConversionEligibilityDto(
    isEligible = isEligible,
    reasons = reasons,
    quotationId = quotationId,
    quotationVersion = quotationVersion,
    quotationStatus = quotationStatus,
    customerId = customerId,
    orderedQuantity = orderedQuantity,
    approvedGrandTotal = approvedGrandTotal,
    currency = currency,
    existingCommitmentId = existingCommitmentId,
    existingOrderId = existingOrderId,
    evaluatedAt = evaluatedAt
)

fun ConversionResult.toDto(): ConversionResultDto = ConversionResultDto(
    isSuccess = isSuccess,
    commitment = commitment.toDto(),
    orderId = orderId,
    orderNumber = orderNumber,
    customerId = customerId,
    committedAmount = committedAmount,
    currency = currency,
    message = message
)

fun CommercialCommitmentReconciliationResult.toDto(): CommercialCommitmentReconciliationDto = CommercialCommitmentReconciliationDto(
    commitmentId = commitmentId,
    quotationId = quotationId,
    orderId = orderId,
    isFullyReconciled = isFullyReconciled,
    customerMatch = customerMatch,
    currencyMatch = currencyMatch,
    quantityMatch = quantityMatch,
    amountMatch = amountMatch,
    statusIntegrityMatch = statusIntegrityMatch,
    discrepancies = discrepancies,
    reconciledAt = reconciledAt
)

fun Module17Step03CommercialCommitmentHandoffContract.toDto(): CommercialCommitmentHandoffDto = CommercialCommitmentHandoffDto(
    handoffId = handoffId,
    contractVersion = contractVersion,
    commitmentId = commitmentId,
    tenantId = tenantId,
    projectId = projectId,
    quotationId = quotationId,
    quotationVersion = quotationVersion,
    quotationNumber = quotationNumber,
    customerId = customerId,
    orderId = orderId,
    orderNumber = orderNumber,
    commitmentStatus = commitmentStatus,
    committedQuantity = committedQuantity,
    approvedUnitPrice = approvedUnitPrice,
    approvedSubtotal = approvedSubtotal,
    approvedDiscount = approvedDiscount,
    approvedTax = approvedTax,
    approvedGrandTotal = approvedGrandTotal,
    currency = currency,
    paymentTerms = paymentTerms,
    deliveryTerms = deliveryTerms,
    conversionTimestamp = conversionTimestamp,
    reconciliationStatus = reconciliationStatus,
    isReadOnly = isReadOnly,
    isMutable = isMutable,
    integrityHash = integrityHash,
    generatedAt = generatedAt
)

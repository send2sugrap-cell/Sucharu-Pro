package com.sucharu.sucharupro.data.api.model

import com.sucharu.sucharupro.domain.model.businessledger.*
import java.math.BigDecimal

data class PostApprovedExpenseRequest(
    val expenseId: String,
    val accountCategory: String? = null,
    val description: String? = null,
    val reference: String? = null,
    val jobId: String? = null,
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class PostApprovedPayableRequest(
    val payableId: String,
    val accountCategory: String? = null,
    val description: String? = null,
    val reference: String? = null,
    val jobId: String? = null,
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class PostVendorPaymentRequest(
    val payableId: String,
    val allocationId: String,
    val amount: String,
    val currency: String = "BDT",
    val paymentDate: Long? = null,
    val paymentMethod: String? = null,
    val paymentReference: String? = null,
    val accountCategory: String? = null,
    val description: String? = null,
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class PostBusinessAdjustmentRequest(
    val amount: String,
    val isDebit: Boolean,
    val accountCategory: String,
    val description: String,
    val reference: String? = null,
    val jobId: String? = null,
    val vendorId: String? = null,
    val currency: String = "BDT",
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class ReversePostingRequest(
    val reason: String,
    val correlationId: String? = null
)

data class AllocateBusinessCostRequest(
    val sourceType: String,
    val sourceId: String,
    val jobId: String,
    val allocatedAmount: String,
    val costCategory: String = "PRODUCTION_COST",
    val vendorId: String? = null,
    val ledgerPostingId: String? = null,
    val reason: String? = null,
    val currency: String = "BDT",
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class ReverseBusinessCostAllocationRequest(
    val reason: String,
    val correlationId: String? = null
)

data class BusinessLedgerPostingDto(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val postingNumber: String,
    val postingType: String,
    val sourceType: String,
    val sourceId: String,
    val accountCategory: String,
    val debitAmount: String,
    val creditAmount: String,
    val netMovement: String,
    val currency: String,
    val postingDate: Long,
    val effectiveDate: Long,
    val description: String,
    val reference: String?,
    val jobId: String?,
    val vendorId: String?,
    val expenseId: String?,
    val payableId: String?,
    val allocationId: String?,
    val reversalOfPostingId: String?,
    val isReversed: Boolean,
    val reversalReason: String?,
    val reversedBy: String?,
    val reversedAt: Long?,
    val correlationId: String?,
    val idempotencyKey: String?,
    val checksum: String?,
    val createdBy: String,
    val createdAt: Long,
    val version: Long
)

fun BusinessLedgerPosting.toDto(): BusinessLedgerPostingDto {
    return BusinessLedgerPostingDto(
        id = id,
        tenantId = tenantId,
        projectId = projectId,
        postingNumber = postingNumber,
        postingType = postingType.name,
        sourceType = sourceType.name,
        sourceId = sourceId,
        accountCategory = accountCategory.name,
        debitAmount = debitAmount.toPlainString(),
        creditAmount = creditAmount.toPlainString(),
        netMovement = netMovement.toPlainString(),
        currency = currency,
        postingDate = postingDate,
        effectiveDate = effectiveDate,
        description = description,
        reference = reference,
        jobId = jobId,
        vendorId = vendorId,
        expenseId = expenseId,
        payableId = payableId,
        allocationId = allocationId,
        reversalOfPostingId = reversalOfPostingId,
        isReversed = isReversed,
        reversalReason = reversalReason,
        reversedBy = reversedBy,
        reversedAt = reversedAt,
        correlationId = correlationId,
        idempotencyKey = idempotencyKey,
        checksum = checksum,
        createdBy = createdBy,
        createdAt = createdAt,
        version = version
    )
}

data class BusinessCostAllocationDto(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val allocationNumber: String,
    val sourceType: String,
    val sourceId: String,
    val ledgerPostingId: String?,
    val jobId: String,
    val vendorId: String?,
    val costCategory: String,
    val allocatedAmount: String,
    val currency: String,
    val allocationDate: Long,
    val reason: String?,
    val isReversed: Boolean,
    val reversalReason: String?,
    val reversedBy: String?,
    val reversedAt: Long?,
    val correlationId: String?,
    val idempotencyKey: String?,
    val createdBy: String,
    val createdAt: Long,
    val version: Long
)

fun BusinessCostAllocation.toDto(): BusinessCostAllocationDto {
    return BusinessCostAllocationDto(
        id = id,
        tenantId = tenantId,
        projectId = projectId,
        allocationNumber = allocationNumber,
        sourceType = sourceType.name,
        sourceId = sourceId,
        ledgerPostingId = ledgerPostingId,
        jobId = jobId,
        vendorId = vendorId,
        costCategory = costCategory.name,
        allocatedAmount = allocatedAmount.toPlainString(),
        currency = currency,
        allocationDate = allocationDate,
        reason = reason,
        isReversed = isReversed,
        reversalReason = reversalReason,
        reversedBy = reversedBy,
        reversedAt = reversedAt,
        correlationId = correlationId,
        idempotencyKey = idempotencyKey,
        createdBy = createdBy,
        createdAt = createdAt,
        version = version
    )
}

data class BusinessLedgerAuditEventDto(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val eventType: String,
    val actorId: String,
    val actorRole: String,
    val timestamp: Long,
    val sourceType: String?,
    val sourceId: String?,
    val postingId: String?,
    val allocationId: String?,
    val action: String,
    val previousState: String?,
    val newState: String?,
    val amount: String?,
    val reason: String?,
    val correlationId: String?,
    val idempotencyKey: String?,
    val checksum: String?,
    val metadataJson: String?
)

fun BusinessLedgerAuditEvent.toDto(): BusinessLedgerAuditEventDto {
    return BusinessLedgerAuditEventDto(
        eventId = eventId,
        tenantId = tenantId,
        projectId = projectId,
        eventType = eventType,
        actorId = actorId,
        actorRole = actorRole,
        timestamp = timestamp,
        sourceType = sourceType?.name,
        sourceId = sourceId,
        postingId = postingId,
        allocationId = allocationId,
        action = action,
        previousState = previousState,
        newState = newState,
        amount = amount?.toPlainString(),
        reason = reason,
        correlationId = correlationId,
        idempotencyKey = idempotencyKey,
        checksum = checksum,
        metadataJson = metadataJson
    )
}

data class BusinessLedgerBalanceSummaryDto(
    val tenantId: String,
    val projectId: String,
    val openingBalance: String,
    val totalDebit: String,
    val totalCredit: String,
    val netMovement: String,
    val closingBalance: String,
    val currency: String,
    val asOfTimestamp: Long
)

fun BusinessLedgerBalanceSummary.toDto(): BusinessLedgerBalanceSummaryDto {
    return BusinessLedgerBalanceSummaryDto(
        tenantId = tenantId,
        projectId = projectId,
        openingBalance = openingBalance.toPlainString(),
        totalDebit = totalDebit.toPlainString(),
        totalCredit = totalCredit.toPlainString(),
        netMovement = netMovement.toPlainString(),
        closingBalance = closingBalance.toPlainString(),
        currency = currency,
        asOfTimestamp = asOfTimestamp
    )
}

data class BusinessLedgerPeriodSummaryDto(
    val tenantId: String,
    val projectId: String,
    val fromDate: Long,
    val toDate: Long,
    val openingBalance: String,
    val totalDebit: String,
    val totalCredit: String,
    val netMovement: String,
    val closingBalance: String,
    val postingCount: Int,
    val currency: String
)

fun BusinessLedgerPeriodSummary.toDto(): BusinessLedgerPeriodSummaryDto {
    return BusinessLedgerPeriodSummaryDto(
        tenantId = tenantId,
        projectId = projectId,
        fromDate = fromDate,
        toDate = toDate,
        openingBalance = openingBalance.toPlainString(),
        totalDebit = totalDebit.toPlainString(),
        totalCredit = totalCredit.toPlainString(),
        netMovement = netMovement.toPlainString(),
        closingBalance = closingBalance.toPlainString(),
        postingCount = postingCount,
        currency = currency
    )
}

data class BusinessJobCostSummaryDto(
    val jobId: String,
    val totalAllocatedCost: String,
    val allocationCount: Int,
    val currency: String,
    val breakdownByCategory: Map<String, String>,
    val allocations: List<BusinessCostAllocationDto>
)

fun BusinessJobCostSummary.toDto(): BusinessJobCostSummaryDto {
    return BusinessJobCostSummaryDto(
        jobId = jobId,
        totalAllocatedCost = totalAllocatedCost.toPlainString(),
        allocationCount = allocationCount,
        currency = currency,
        breakdownByCategory = breakdownByCategory.mapValues { it.value.toPlainString() },
        allocations = allocations.map { it.toDto() }
    )
}

data class BusinessUnallocatedCostSummaryDto(
    val sourceType: String,
    val sourceId: String,
    val totalSourceAmount: String,
    val allocatedAmount: String,
    val unallocatedAmount: String,
    val allocationPercentage: String,
    val currency: String
)

fun BusinessUnallocatedCostSummary.toDto(): BusinessUnallocatedCostSummaryDto {
    return BusinessUnallocatedCostSummaryDto(
        sourceType = sourceType.name,
        sourceId = sourceId,
        totalSourceAmount = totalSourceAmount.toPlainString(),
        allocatedAmount = allocatedAmount.toPlainString(),
        unallocatedAmount = unallocatedAmount.toPlainString(),
        allocationPercentage = allocationPercentage.toPlainString(),
        currency = currency
    )
}

data class BusinessCostAllocationSummaryDto(
    val totalAllocated: String,
    val totalUnallocated: String,
    val jobCount: Int,
    val currency: String,
    val jobSummaries: List<BusinessJobCostSummaryDto>
)

fun BusinessCostAllocationSummary.toDto(): BusinessCostAllocationSummaryDto {
    return BusinessCostAllocationSummaryDto(
        totalAllocated = totalAllocated.toPlainString(),
        totalUnallocated = totalUnallocated.toPlainString(),
        jobCount = jobCount,
        currency = currency,
        jobSummaries = jobSummaries.map { it.toDto() }
    )
}

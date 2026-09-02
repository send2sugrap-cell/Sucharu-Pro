package com.sucharu.sucharupro.data.api.model

import com.sucharu.sucharupro.domain.model.vendorpayable.*
import java.math.BigDecimal

data class CreateVendorPayableRequest(
    val vendorId: String,
    val jobId: String? = null,
    val vendorJobId: String? = null,
    val billReference: String? = null,
    val originalAmount: String,
    val currency: String = "BDT",
    val issueDate: Long? = null,
    val paymentTerms: String = "NET_30",
    val customTermDays: Int? = null,
    val description: String,
    val notes: String? = null,
    val attachmentUrl: String? = null,
    val idempotencyKey: String? = null,
    val autoSubmit: Boolean = false
)

data class UpdateVendorPayableRequest(
    val vendorId: String? = null,
    val jobId: String? = null,
    val vendorJobId: String? = null,
    val billReference: String? = null,
    val originalAmount: String? = null,
    val currency: String? = null,
    val issueDate: Long? = null,
    val paymentTerms: String? = null,
    val customTermDays: Int? = null,
    val description: String? = null,
    val notes: String? = null,
    val attachmentUrl: String? = null
)

data class AllocateVendorPayablePaymentRequest(
    val amount: String,
    val paymentMethod: String = "BANK",
    val paymentReference: String? = null,
    val paymentDate: Long? = null,
    val notes: String? = null,
    val idempotencyKey: String? = null
)

data class ApproveVendorPayableRequest(
    val notes: String? = null
)

data class RejectVendorPayableRequest(
    val reason: String
)

data class CancelVendorPayableRequest(
    val reason: String
)

data class VoidVendorPayableRequest(
    val reason: String
)

data class VendorPayableDto(
    val payableId: String,
    val tenantId: String,
    val projectId: String,
    val payableNumber: String,
    val vendorId: String,
    val jobId: String?,
    val vendorJobId: String?,
    val billReference: String?,
    val description: String,
    val notes: String?,
    val originalAmount: String,
    val paidAmount: String,
    val outstandingAmount: String,
    val currency: String,
    val issueDate: Long,
    val paymentTerms: String,
    val customTermDays: Int?,
    val dueDate: Long,
    val status: String,
    val agingBucket: String,
    val isOverdue: Boolean,
    val attachmentUrl: String?,
    val idempotencyKey: String?,
    val createdBy: String,
    val createdAt: Long,
    val submittedBy: String?,
    val submittedAt: Long?,
    val approvedBy: String?,
    val approvedAt: Long?,
    val rejectedBy: String?,
    val rejectedAt: Long?,
    val cancelledBy: String?,
    val cancelledAt: Long?,
    val voidedBy: String?,
    val voidedAt: Long?,
    val rejectionReason: String?,
    val cancellationReason: String?,
    val voidReason: String?,
    val updatedAt: Long,
    val updatedBy: String?,
    val version: Long
)

data class VendorPayablePaymentAllocationDto(
    val allocationId: String,
    val tenantId: String,
    val projectId: String,
    val payableId: String,
    val vendorId: String,
    val amount: String,
    val currency: String,
    val paymentMethod: String,
    val paymentReference: String?,
    val paymentDate: Long,
    val notes: String?,
    val allocatedBy: String,
    val allocatedAt: Long,
    val idempotencyKey: String?,
    val version: Long
)

data class VendorPayableAuditEventDto(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val payableId: String,
    val vendorId: String,
    val eventType: String,
    val actorId: String,
    val actorRole: String,
    val timestamp: Long,
    val previousStatus: String?,
    val newStatus: String?,
    val amount: String?,
    val reason: String?,
    val correlationId: String?,
    val idempotencyKey: String?,
    val metadataJson: String?
)

data class VendorPayableSummaryDto(
    val vendorId: String,
    val totalApprovedLiability: String,
    val totalPaid: String,
    val totalOutstanding: String,
    val totalOverdue: String,
    val currentDue: String,
    val dueToday: String,
    val upcomingDue: String,
    val draftCount: Int,
    val submittedCount: Int,
    val approvedCount: Int,
    val partiallyPaidCount: Int,
    val paidCount: Int,
    val rejectedCount: Int,
    val cancelledCount: Int,
    val voidedCount: Int,
    val currency: String
)

data class VendorPayableAgingItemDto(
    val bucket: String,
    val label: String,
    val count: Int,
    val totalAmount: String,
    val outstandingAmount: String
)

data class VendorPayableAgingReportDto(
    val vendorId: String?,
    val asOfDate: Long,
    val buckets: List<VendorPayableAgingItemDto>,
    val totalOutstanding: String,
    val currency: String
)

fun VendorPayable.toDto(): VendorPayableDto = VendorPayableDto(
    payableId = payableId,
    tenantId = tenantId,
    projectId = projectId,
    payableNumber = payableNumber,
    vendorId = vendorId,
    jobId = jobId,
    vendorJobId = vendorJobId,
    billReference = billReference,
    description = description,
    notes = notes,
    originalAmount = originalAmount.toPlainString(),
    paidAmount = paidAmount.toPlainString(),
    outstandingAmount = outstandingAmount.toPlainString(),
    currency = currency,
    issueDate = issueDate,
    paymentTerms = paymentTerms.name,
    customTermDays = customTermDays,
    dueDate = dueDate,
    status = status.name,
    agingBucket = getAgingBucket().name,
    isOverdue = isOverdue,
    attachmentUrl = attachmentUrl,
    idempotencyKey = idempotencyKey,
    createdBy = createdBy,
    createdAt = createdAt,
    submittedBy = submittedBy,
    submittedAt = submittedAt,
    approvedBy = approvedBy,
    approvedAt = approvedAt,
    rejectedBy = rejectedBy,
    rejectedAt = rejectedAt,
    cancelledBy = cancelledBy,
    cancelledAt = cancelledAt,
    voidedBy = voidedBy,
    voidedAt = voidedAt,
    rejectionReason = rejectionReason,
    cancellationReason = cancellationReason,
    voidReason = voidReason,
    updatedAt = updatedAt,
    updatedBy = updatedBy,
    version = version
)

fun VendorPayablePaymentAllocation.toDto(): VendorPayablePaymentAllocationDto = VendorPayablePaymentAllocationDto(
    allocationId = allocationId,
    tenantId = tenantId,
    projectId = projectId,
    payableId = payableId,
    vendorId = vendorId,
    amount = amount.toPlainString(),
    currency = currency,
    paymentMethod = paymentMethod.name,
    paymentReference = paymentReference,
    paymentDate = paymentDate,
    notes = notes,
    allocatedBy = allocatedBy,
    allocatedAt = allocatedAt,
    idempotencyKey = idempotencyKey,
    version = version
)

fun VendorPayableAuditEvent.toDto(): VendorPayableAuditEventDto = VendorPayableAuditEventDto(
    eventId = eventId,
    tenantId = tenantId,
    projectId = projectId,
    payableId = payableId,
    vendorId = vendorId,
    eventType = eventType,
    actorId = actorId,
    actorRole = actorRole,
    timestamp = timestamp,
    previousStatus = previousStatus?.name,
    newStatus = newStatus?.name,
    amount = amount?.toPlainString(),
    reason = reason,
    correlationId = correlationId,
    idempotencyKey = idempotencyKey,
    metadataJson = metadataJson
)

fun VendorPayableSummary.toDto(): VendorPayableSummaryDto = VendorPayableSummaryDto(
    vendorId = vendorId,
    totalApprovedLiability = totalApprovedLiability.toPlainString(),
    totalPaid = totalPaid.toPlainString(),
    totalOutstanding = totalOutstanding.toPlainString(),
    totalOverdue = totalOverdue.toPlainString(),
    currentDue = currentDue.toPlainString(),
    dueToday = dueToday.toPlainString(),
    upcomingDue = upcomingDue.toPlainString(),
    draftCount = draftCount,
    submittedCount = submittedCount,
    approvedCount = approvedCount,
    partiallyPaidCount = partiallyPaidCount,
    paidCount = paidCount,
    rejectedCount = rejectedCount,
    cancelledCount = cancelledCount,
    voidedCount = voidedCount,
    currency = currency
)

fun VendorPayableAgingReport.toDto(): VendorPayableAgingReportDto = VendorPayableAgingReportDto(
    vendorId = vendorId,
    asOfDate = asOfDate,
    buckets = buckets.map { item ->
        VendorPayableAgingItemDto(
            bucket = item.bucket.name,
            label = item.bucket.label,
            count = item.count,
            totalAmount = item.totalAmount.toPlainString(),
            outstandingAmount = item.outstandingAmount.toPlainString()
        )
    },
    totalOutstanding = totalOutstanding.toPlainString(),
    currency = currency
)

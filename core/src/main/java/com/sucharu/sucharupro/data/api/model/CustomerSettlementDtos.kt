package com.sucharu.sucharupro.data.api.model

import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocation
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerSettlementAuditEvent
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerSettlementResult
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerSettlementSummary
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerUnallocatedPayment
import com.sucharu.sucharupro.domain.model.customersettlement.InvoiceAllocationRequestItem
import java.math.BigDecimal

/**
 * Request and Response DTOs for Customer Financial Settlement & Payment Allocation (Module 14 Step 06).
 */

data class AllocateCustomerPaymentRequest(
    val invoiceId: String,
    val amount: BigDecimal,
    val idempotencyKey: String? = null
)

data class AllocateCustomerPaymentMultiRequest(
    val allocations: List<InvoiceAllocationItemDto>,
    val idempotencyKey: String? = null
)

data class InvoiceAllocationItemDto(
    val invoiceId: String,
    val amount: BigDecimal
)

data class ReverseCustomerPaymentAllocationRequest(
    val reason: String,
    val expectedVersion: Long = 1L
)

data class CustomerPaymentAllocationDto(
    val allocationId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val paymentId: String,
    val invoiceId: String,
    val allocatedAmount: BigDecimal,
    val currency: String,
    val status: String,
    val reversalReason: String? = null,
    val idempotencyKey: String? = null,
    val allocatedAt: Long,
    val allocatedBy: String,
    val reversedAt: Long? = null,
    val reversedBy: String? = null,
    val version: Long
)

data class CustomerUnallocatedPaymentDto(
    val paymentId: String,
    val paymentNumber: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val totalAmount: BigDecimal,
    val allocatedAmount: BigDecimal,
    val unallocatedAmount: BigDecimal,
    val currency: String,
    val paymentDate: Long,
    val status: String
)

data class CustomerSettlementSummaryDto(
    val customerId: String,
    val projectId: String,
    val customerFinancialAccountId: String,
    val totalInvoiced: BigDecimal,
    val totalPaid: BigDecimal,
    val totalAllocated: BigDecimal,
    val totalUnallocated: BigDecimal,
    val totalAvailableCredit: BigDecimal,
    val totalOutstanding: BigDecimal,
    val invoiceCount: Int,
    val partiallyPaidInvoiceCount: Int,
    val paidInvoiceCount: Int,
    val overdueInvoiceCount: Int,
    val currency: String
)

data class CustomerSettlementResultDto(
    val paymentId: String,
    val totalAllocated: BigDecimal,
    val remainingUnallocated: BigDecimal,
    val allocations: List<CustomerPaymentAllocationDto>
)

data class CustomerSettlementAuditEventDto(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val allocationId: String? = null,
    val paymentId: String? = null,
    val invoiceId: String? = null,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val previousStatus: String? = null,
    val newStatus: String? = null,
    val amount: BigDecimal? = null,
    val reason: String? = null,
    val occurredAt: Long,
    val metadataJson: String? = null
)

fun CustomerPaymentAllocation.toDto(): CustomerPaymentAllocationDto = CustomerPaymentAllocationDto(
    allocationId = allocationId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    customerFinancialAccountId = customerFinancialAccountId,
    paymentId = paymentId,
    invoiceId = invoiceId,
    allocatedAmount = allocatedAmount,
    currency = currency,
    status = status.name,
    reversalReason = reversalReason,
    idempotencyKey = idempotencyKey,
    allocatedAt = allocatedAt,
    allocatedBy = allocatedBy,
    reversedAt = reversedAt,
    reversedBy = reversedBy,
    version = version
)

fun CustomerUnallocatedPayment.toDto(): CustomerUnallocatedPaymentDto = CustomerUnallocatedPaymentDto(
    paymentId = paymentId,
    paymentNumber = paymentNumber,
    customerId = customerId,
    customerFinancialAccountId = customerFinancialAccountId,
    totalAmount = totalAmount,
    allocatedAmount = allocatedAmount,
    unallocatedAmount = unallocatedAmount,
    currency = currency,
    paymentDate = paymentDate,
    status = status
)

fun CustomerSettlementSummary.toDto(): CustomerSettlementSummaryDto = CustomerSettlementSummaryDto(
    customerId = customerId,
    projectId = projectId,
    customerFinancialAccountId = customerFinancialAccountId,
    totalInvoiced = totalInvoiced,
    totalPaid = totalPaid,
    totalAllocated = totalAllocated,
    totalUnallocated = totalUnallocated,
    totalAvailableCredit = totalAvailableCredit,
    totalOutstanding = totalOutstanding,
    invoiceCount = invoiceCount,
    partiallyPaidInvoiceCount = partiallyPaidInvoiceCount,
    paidInvoiceCount = paidInvoiceCount,
    overdueInvoiceCount = overdueInvoiceCount,
    currency = currency
)

fun CustomerSettlementResult.toDto(): CustomerSettlementResultDto = CustomerSettlementResultDto(
    paymentId = paymentId,
    totalAllocated = totalAllocated,
    remainingUnallocated = remainingUnallocated,
    allocations = allocations.map { it.toDto() }
)

fun CustomerSettlementAuditEvent.toDto(): CustomerSettlementAuditEventDto = CustomerSettlementAuditEventDto(
    auditId = auditId,
    tenantId = tenantId,
    projectId = projectId,
    customerId = customerId,
    allocationId = allocationId,
    paymentId = paymentId,
    invoiceId = invoiceId,
    actorId = actorId,
    actorRole = actorRole,
    action = action,
    previousStatus = previousStatus,
    newStatus = newStatus,
    amount = amount,
    reason = reason,
    occurredAt = occurredAt,
    metadataJson = metadataJson
)

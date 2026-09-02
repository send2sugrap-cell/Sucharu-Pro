package com.sucharu.sucharupro.domain.service.businessledger

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.datasource.businessledger.BusinessCostAllocationFilter
import com.sucharu.sucharupro.data.datasource.businessledger.BusinessLedgerPostingFilter
import com.sucharu.sucharupro.domain.model.businessledger.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.math.BigDecimal

data class PostApprovedExpenseCommand(
    val expenseId: String,
    val accountCategory: BusinessLedgerAccountCategory? = null,
    val description: String? = null,
    val reference: String? = null,
    val jobId: String? = null,
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class PostApprovedPayableCommand(
    val payableId: String,
    val accountCategory: BusinessLedgerAccountCategory = BusinessLedgerAccountCategory.VENDOR_COST,
    val description: String? = null,
    val reference: String? = null,
    val jobId: String? = null,
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class PostVendorPaymentCommand(
    val payableId: String,
    val allocationId: String,
    val amount: BigDecimal,
    val currency: String = "BDT",
    val paymentDate: Long = System.currentTimeMillis(),
    val paymentMethod: String? = null,
    val paymentReference: String? = null,
    val accountCategory: BusinessLedgerAccountCategory = BusinessLedgerAccountCategory.CASH,
    val description: String? = null,
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class PostBusinessAdjustmentCommand(
    val amount: BigDecimal,
    val isDebit: Boolean,
    val accountCategory: BusinessLedgerAccountCategory,
    val description: String,
    val reference: String? = null,
    val jobId: String? = null,
    val vendorId: String? = null,
    val currency: String = "BDT",
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class ReversePostingCommand(
    val postingId: String,
    val reason: String,
    val correlationId: String? = null
)

data class AllocateCostCommand(
    val sourceType: BusinessLedgerSourceType,
    val sourceId: String,
    val jobId: String,
    val allocatedAmount: BigDecimal,
    val costCategory: BusinessLedgerAccountCategory = BusinessLedgerAccountCategory.PRODUCTION_COST,
    val vendorId: String? = null,
    val ledgerPostingId: String? = null,
    val reason: String? = null,
    val currency: String = "BDT",
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class ReverseCostAllocationCommand(
    val allocationId: String,
    val reason: String,
    val correlationId: String? = null
)

/**
 * Domain Service contract for the Business Ledger, Financial Posting & Cost Allocation Foundation (Module 15 Step 03).
 */
interface BusinessLedgerService {
    suspend fun postApprovedExpense(principal: AuthenticatedPrincipal, command: PostApprovedExpenseCommand): DomainResult<BusinessLedgerPosting>
    suspend fun postApprovedPayable(principal: AuthenticatedPrincipal, command: PostApprovedPayableCommand): DomainResult<BusinessLedgerPosting>
    suspend fun postVendorPayment(principal: AuthenticatedPrincipal, command: PostVendorPaymentCommand): DomainResult<BusinessLedgerPosting>
    suspend fun postBusinessAdjustment(principal: AuthenticatedPrincipal, command: PostBusinessAdjustmentCommand): DomainResult<BusinessLedgerPosting>
    suspend fun reversePosting(principal: AuthenticatedPrincipal, command: ReversePostingCommand): DomainResult<BusinessLedgerPosting>

    suspend fun allocateCost(principal: AuthenticatedPrincipal, command: AllocateCostCommand): DomainResult<BusinessCostAllocation>
    suspend fun reverseCostAllocation(principal: AuthenticatedPrincipal, command: ReverseCostAllocationCommand): DomainResult<BusinessCostAllocation>

    suspend fun getPostingById(principal: AuthenticatedPrincipal, postingId: String): DomainResult<BusinessLedgerPosting>
    suspend fun getPostingByNumber(principal: AuthenticatedPrincipal, postingNumber: String): DomainResult<BusinessLedgerPosting>
    suspend fun listPostings(principal: AuthenticatedPrincipal, filter: BusinessLedgerPostingFilter): DomainResult<List<BusinessLedgerPosting>>
    suspend fun getPostingsBySource(principal: AuthenticatedPrincipal, sourceType: BusinessLedgerSourceType, sourceId: String): DomainResult<List<BusinessLedgerPosting>>

    suspend fun getBalanceSummary(principal: AuthenticatedPrincipal, asOfTimestamp: Long = System.currentTimeMillis()): DomainResult<BusinessLedgerBalanceSummary>
    suspend fun getPeriodSummary(principal: AuthenticatedPrincipal, fromDate: Long, toDate: Long): DomainResult<BusinessLedgerPeriodSummary>

    suspend fun listCostAllocations(principal: AuthenticatedPrincipal, filter: BusinessCostAllocationFilter): DomainResult<List<BusinessCostAllocation>>
    suspend fun getJobCostSummary(principal: AuthenticatedPrincipal, jobId: String): DomainResult<BusinessJobCostSummary>
    suspend fun getUnallocatedCostSummary(principal: AuthenticatedPrincipal, sourceType: BusinessLedgerSourceType, sourceId: String): DomainResult<BusinessUnallocatedCostSummary>
    suspend fun getCostAllocationSummary(principal: AuthenticatedPrincipal): DomainResult<BusinessCostAllocationSummary>

    suspend fun getAuditTrail(principal: AuthenticatedPrincipal, sourceId: String? = null, postingId: String? = null, allocationId: String? = null): DomainResult<List<BusinessLedgerAuditEvent>>
}

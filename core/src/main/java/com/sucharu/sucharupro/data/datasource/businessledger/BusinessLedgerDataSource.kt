package com.sucharu.sucharupro.data.datasource.businessledger

import com.sucharu.sucharupro.domain.model.businessledger.*
import java.math.BigDecimal

data class BusinessLedgerPostingFilter(
    val sourceType: BusinessLedgerSourceType? = null,
    val sourceId: String? = null,
    val postingType: BusinessLedgerPostingType? = null,
    val accountCategory: BusinessLedgerAccountCategory? = null,
    val jobId: String? = null,
    val vendorId: String? = null,
    val expenseId: String? = null,
    val payableId: String? = null,
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val isReversed: Boolean? = null,
    val limit: Int = 50,
    val offset: Int = 0
)

data class BusinessCostAllocationFilter(
    val sourceType: BusinessLedgerSourceType? = null,
    val sourceId: String? = null,
    val jobId: String? = null,
    val vendorId: String? = null,
    val costCategory: BusinessLedgerAccountCategory? = null,
    val isReversed: Boolean? = null,
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val limit: Int = 50,
    val offset: Int = 0
)

/**
 * Data Source contract for Business Ledger and Cost Allocations (Module 15 Step 03).
 */
interface BusinessLedgerDataSource {
    suspend fun createPosting(posting: BusinessLedgerPosting): BusinessLedgerPosting
    suspend fun findPostingById(id: String, tenantId: String, projectId: String): BusinessLedgerPosting?
    suspend fun findPostingByNumber(postingNumber: String, tenantId: String, projectId: String): BusinessLedgerPosting?
    suspend fun findPostingByIdempotencyKey(key: String, tenantId: String, projectId: String): BusinessLedgerPosting?
    suspend fun findPostingsBySource(sourceType: BusinessLedgerSourceType, sourceId: String, tenantId: String, projectId: String): List<BusinessLedgerPosting>
    suspend fun findPostingBySourceAndType(sourceType: BusinessLedgerSourceType, sourceId: String, postingType: BusinessLedgerPostingType, tenantId: String, projectId: String): BusinessLedgerPosting?
    suspend fun listPostings(tenantId: String, projectId: String, filter: BusinessLedgerPostingFilter): List<BusinessLedgerPosting>
    suspend fun countPostings(tenantId: String, projectId: String, filter: BusinessLedgerPostingFilter): Long
    suspend fun markPostingReversed(id: String, reversalReason: String, reversedBy: String, reversedAt: Long, reversalPostingId: String): Boolean

    suspend fun createCostAllocation(allocation: BusinessCostAllocation): BusinessCostAllocation
    suspend fun findCostAllocationById(id: String, tenantId: String, projectId: String): BusinessCostAllocation?
    suspend fun findCostAllocationByIdempotencyKey(key: String, tenantId: String, projectId: String): BusinessCostAllocation?
    suspend fun listCostAllocations(tenantId: String, projectId: String, filter: BusinessCostAllocationFilter): List<BusinessCostAllocation>
    suspend fun markCostAllocationReversed(id: String, reversalReason: String, reversedBy: String, reversedAt: Long): Boolean

    suspend fun recordAuditEvent(event: BusinessLedgerAuditEvent)
    suspend fun listAuditEvents(tenantId: String, projectId: String, sourceId: String? = null, postingId: String? = null, allocationId: String? = null): List<BusinessLedgerAuditEvent>

    suspend fun calculateBalanceSummary(tenantId: String, projectId: String, asOfTimestamp: Long = System.currentTimeMillis()): BusinessLedgerBalanceSummary
    suspend fun calculatePeriodSummary(tenantId: String, projectId: String, fromDate: Long, toDate: Long): BusinessLedgerPeriodSummary
}

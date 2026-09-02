package com.sucharu.sucharupro.domain.repository.businessledger

import com.sucharu.sucharupro.data.datasource.businessledger.BusinessCostAllocationFilter
import com.sucharu.sucharupro.data.datasource.businessledger.BusinessLedgerPostingFilter
import com.sucharu.sucharupro.domain.model.businessledger.*

/**
 * Repository interface for Business Ledger postings, cost allocations, and audits (Module 15 Step 03).
 */
interface BusinessLedgerRepository {
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

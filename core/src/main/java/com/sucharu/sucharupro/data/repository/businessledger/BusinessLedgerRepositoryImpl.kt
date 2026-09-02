package com.sucharu.sucharupro.data.repository.businessledger

import com.sucharu.sucharupro.data.datasource.businessledger.BusinessCostAllocationFilter
import com.sucharu.sucharupro.data.datasource.businessledger.BusinessLedgerDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.BusinessLedgerPostingFilter
import com.sucharu.sucharupro.domain.model.businessledger.*
import com.sucharu.sucharupro.domain.repository.businessledger.BusinessLedgerRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Production implementation of [BusinessLedgerRepository] delegating to [BusinessLedgerDataSource].
 */
class BusinessLedgerRepositoryImpl(
    private val dataSource: BusinessLedgerDataSource
) : BusinessLedgerRepository {

    private val mutex = Mutex()

    override suspend fun createPosting(posting: BusinessLedgerPosting): BusinessLedgerPosting = mutex.withLock {
        dataSource.createPosting(posting)
    }

    override suspend fun findPostingById(id: String, tenantId: String, projectId: String): BusinessLedgerPosting? {
        return dataSource.findPostingById(id, tenantId, projectId)
    }

    override suspend fun findPostingByNumber(
        postingNumber: String,
        tenantId: String,
        projectId: String
    ): BusinessLedgerPosting? {
        return dataSource.findPostingByNumber(postingNumber, tenantId, projectId)
    }

    override suspend fun findPostingByIdempotencyKey(
        key: String,
        tenantId: String,
        projectId: String
    ): BusinessLedgerPosting? {
        return dataSource.findPostingByIdempotencyKey(key, tenantId, projectId)
    }

    override suspend fun findPostingsBySource(
        sourceType: BusinessLedgerSourceType,
        sourceId: String,
        tenantId: String,
        projectId: String
    ): List<BusinessLedgerPosting> {
        return dataSource.findPostingsBySource(sourceType, sourceId, tenantId, projectId)
    }

    override suspend fun findPostingBySourceAndType(
        sourceType: BusinessLedgerSourceType,
        sourceId: String,
        postingType: BusinessLedgerPostingType,
        tenantId: String,
        projectId: String
    ): BusinessLedgerPosting? {
        return dataSource.findPostingBySourceAndType(sourceType, sourceId, postingType, tenantId, projectId)
    }

    override suspend fun listPostings(
        tenantId: String,
        projectId: String,
        filter: BusinessLedgerPostingFilter
    ): List<BusinessLedgerPosting> {
        return dataSource.listPostings(tenantId, projectId, filter)
    }

    override suspend fun countPostings(
        tenantId: String,
        projectId: String,
        filter: BusinessLedgerPostingFilter
    ): Long {
        return dataSource.countPostings(tenantId, projectId, filter)
    }

    override suspend fun markPostingReversed(
        id: String,
        reversalReason: String,
        reversedBy: String,
        reversedAt: Long,
        reversalPostingId: String
    ): Boolean = mutex.withLock {
        dataSource.markPostingReversed(id, reversalReason, reversedBy, reversedAt, reversalPostingId)
    }

    override suspend fun createCostAllocation(allocation: BusinessCostAllocation): BusinessCostAllocation = mutex.withLock {
        dataSource.createCostAllocation(allocation)
    }

    override suspend fun findCostAllocationById(
        id: String,
        tenantId: String,
        projectId: String
    ): BusinessCostAllocation? {
        return dataSource.findCostAllocationById(id, tenantId, projectId)
    }

    override suspend fun findCostAllocationByIdempotencyKey(
        key: String,
        tenantId: String,
        projectId: String
    ): BusinessCostAllocation? {
        return dataSource.findCostAllocationByIdempotencyKey(key, tenantId, projectId)
    }

    override suspend fun listCostAllocations(
        tenantId: String,
        projectId: String,
        filter: BusinessCostAllocationFilter
    ): List<BusinessCostAllocation> {
        return dataSource.listCostAllocations(tenantId, projectId, filter)
    }

    override suspend fun markCostAllocationReversed(
        id: String,
        reversalReason: String,
        reversedBy: String,
        reversedAt: Long
    ): Boolean = mutex.withLock {
        dataSource.markCostAllocationReversed(id, reversalReason, reversedBy, reversedAt)
    }

    override suspend fun recordAuditEvent(event: BusinessLedgerAuditEvent) {
        dataSource.recordAuditEvent(event)
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        sourceId: String?,
        postingId: String?,
        allocationId: String?
    ): List<BusinessLedgerAuditEvent> {
        return dataSource.listAuditEvents(tenantId, projectId, sourceId, postingId, allocationId)
    }

    override suspend fun calculateBalanceSummary(
        tenantId: String,
        projectId: String,
        asOfTimestamp: Long
    ): BusinessLedgerBalanceSummary {
        return dataSource.calculateBalanceSummary(tenantId, projectId, asOfTimestamp)
    }

    override suspend fun calculatePeriodSummary(
        tenantId: String,
        projectId: String,
        fromDate: Long,
        toDate: Long
    ): BusinessLedgerPeriodSummary {
        return dataSource.calculatePeriodSummary(tenantId, projectId, fromDate, toDate)
    }
}

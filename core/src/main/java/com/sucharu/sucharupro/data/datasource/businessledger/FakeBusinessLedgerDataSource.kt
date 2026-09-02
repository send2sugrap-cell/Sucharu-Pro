package com.sucharu.sucharupro.data.datasource.businessledger

import com.sucharu.sucharupro.domain.model.businessledger.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Thread-safe in-memory implementation of [BusinessLedgerDataSource] for fast unit testing.
 */
class FakeBusinessLedgerDataSource : BusinessLedgerDataSource {

    private val mutex = Mutex()
    private val postings = mutableListOf<BusinessLedgerPosting>()
    private val allocations = mutableListOf<BusinessCostAllocation>()
    private val auditEvents = mutableListOf<BusinessLedgerAuditEvent>()

    override suspend fun createPosting(posting: BusinessLedgerPosting): BusinessLedgerPosting = mutex.withLock {
        // Enforce uniqueness constraints
        if (postings.any { it.id == posting.id }) {
            throw IllegalStateException("Duplicate posting ID: '${posting.id}'")
        }
        if (postings.any { it.tenantId == posting.tenantId && it.projectId == posting.projectId && it.postingNumber == posting.postingNumber }) {
            throw IllegalStateException("Duplicate posting number '${posting.postingNumber}' for tenant '${posting.tenantId}'")
        }
        if (posting.idempotencyKey != null && postings.any { it.tenantId == posting.tenantId && it.projectId == posting.projectId && it.idempotencyKey == posting.idempotencyKey }) {
            throw IllegalStateException("Duplicate idempotency key '${posting.idempotencyKey}'")
        }
        if (posting.reversalOfPostingId == null && postings.any {
            it.tenantId == posting.tenantId && it.projectId == posting.projectId &&
            it.sourceType == posting.sourceType && it.sourceId == posting.sourceId &&
            it.postingType == posting.postingType && it.reversalOfPostingId == null
        }) {
            throw IllegalStateException("Duplicate posting for source ${posting.sourceType}:${posting.sourceId} and type ${posting.postingType}")
        }

        postings.add(posting)
        posting
    }

    override suspend fun findPostingById(id: String, tenantId: String, projectId: String): BusinessLedgerPosting? = mutex.withLock {
        postings.find { it.id == id && it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun findPostingByNumber(postingNumber: String, tenantId: String, projectId: String): BusinessLedgerPosting? = mutex.withLock {
        postings.find { it.postingNumber == postingNumber && it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun findPostingByIdempotencyKey(key: String, tenantId: String, projectId: String): BusinessLedgerPosting? = mutex.withLock {
        postings.find { it.idempotencyKey == key && it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun findPostingsBySource(
        sourceType: BusinessLedgerSourceType,
        sourceId: String,
        tenantId: String,
        projectId: String
    ): List<BusinessLedgerPosting> = mutex.withLock {
        postings.filter {
            it.sourceType == sourceType && it.sourceId == sourceId &&
            it.tenantId == tenantId && it.projectId == projectId
        }.sortedBy { it.postingDate }
    }

    override suspend fun findPostingBySourceAndType(
        sourceType: BusinessLedgerSourceType,
        sourceId: String,
        postingType: BusinessLedgerPostingType,
        tenantId: String,
        projectId: String
    ): BusinessLedgerPosting? = mutex.withLock {
        postings.find {
            it.sourceType == sourceType && it.sourceId == sourceId &&
            it.postingType == postingType && it.reversalOfPostingId == null &&
            it.tenantId == tenantId && it.projectId == projectId
        }
    }

    override suspend fun listPostings(
        tenantId: String,
        projectId: String,
        filter: BusinessLedgerPostingFilter
    ): List<BusinessLedgerPosting> = mutex.withLock {
        filterPostingsInternal(tenantId, projectId, filter)
            .drop(filter.offset)
            .take(filter.limit)
    }

    override suspend fun countPostings(
        tenantId: String,
        projectId: String,
        filter: BusinessLedgerPostingFilter
    ): Long = mutex.withLock {
        filterPostingsInternal(tenantId, projectId, filter).size.toLong()
    }

    private fun filterPostingsInternal(
        tenantId: String,
        projectId: String,
        filter: BusinessLedgerPostingFilter
    ): List<BusinessLedgerPosting> {
        return postings.filter { p ->
            p.tenantId == tenantId && p.projectId == projectId &&
            (filter.sourceType == null || p.sourceType == filter.sourceType) &&
            (filter.sourceId == null || p.sourceId == filter.sourceId) &&
            (filter.postingType == null || p.postingType == filter.postingType) &&
            (filter.accountCategory == null || p.accountCategory == filter.accountCategory) &&
            (filter.jobId == null || p.jobId == filter.jobId) &&
            (filter.vendorId == null || p.vendorId == filter.vendorId) &&
            (filter.expenseId == null || p.expenseId == filter.expenseId) &&
            (filter.payableId == null || p.payableId == filter.payableId) &&
            (filter.fromDate == null || p.postingDate >= filter.fromDate) &&
            (filter.toDate == null || p.postingDate <= filter.toDate) &&
            (filter.isReversed == null || p.isReversed == filter.isReversed)
        }.sortedByDescending { it.postingDate }
    }

    override suspend fun markPostingReversed(
        id: String,
        reversalReason: String,
        reversedBy: String,
        reversedAt: Long,
        reversalPostingId: String
    ): Boolean = mutex.withLock {
        val index = postings.indexOfFirst { it.id == id }
        if (index == -1) return@withLock false
        val existing = postings[index]
        if (existing.isReversed) return@withLock false

        postings[index] = existing.copy(
            isReversed = true,
            reversalReason = reversalReason,
            reversedBy = reversedBy,
            reversedAt = reversedAt,
            version = existing.version + 1
        )
        true
    }

    override suspend fun createCostAllocation(allocation: BusinessCostAllocation): BusinessCostAllocation = mutex.withLock {
        if (allocations.any { it.id == allocation.id }) {
            throw IllegalStateException("Duplicate cost allocation ID '${allocation.id}'")
        }
        if (allocations.any { it.tenantId == allocation.tenantId && it.projectId == allocation.projectId && it.allocationNumber == allocation.allocationNumber }) {
            throw IllegalStateException("Duplicate allocation number '${allocation.allocationNumber}'")
        }
        if (allocation.idempotencyKey != null && allocations.any { it.tenantId == allocation.tenantId && it.idempotencyKey == allocation.idempotencyKey }) {
            throw IllegalStateException("Duplicate allocation idempotency key '${allocation.idempotencyKey}'")
        }

        allocations.add(allocation)
        allocation
    }

    override suspend fun findCostAllocationById(
        id: String,
        tenantId: String,
        projectId: String
    ): BusinessCostAllocation? = mutex.withLock {
        allocations.find { it.id == id && it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun findCostAllocationByIdempotencyKey(
        key: String,
        tenantId: String,
        projectId: String
    ): BusinessCostAllocation? = mutex.withLock {
        allocations.find { it.idempotencyKey == key && it.tenantId == tenantId && it.projectId == projectId }
    }

    override suspend fun listCostAllocations(
        tenantId: String,
        projectId: String,
        filter: BusinessCostAllocationFilter
    ): List<BusinessCostAllocation> = mutex.withLock {
        allocations.filter { a ->
            a.tenantId == tenantId && a.projectId == projectId &&
            (filter.sourceType == null || a.sourceType == filter.sourceType) &&
            (filter.sourceId == null || a.sourceId == filter.sourceId) &&
            (filter.jobId == null || a.jobId == filter.jobId) &&
            (filter.vendorId == null || a.vendorId == filter.vendorId) &&
            (filter.costCategory == null || a.costCategory == filter.costCategory) &&
            (filter.isReversed == null || a.isReversed == filter.isReversed) &&
            (filter.fromDate == null || a.allocationDate >= filter.fromDate) &&
            (filter.toDate == null || a.allocationDate <= filter.toDate)
        }.sortedByDescending { it.allocationDate }
            .drop(filter.offset)
            .take(filter.limit)
    }

    override suspend fun markCostAllocationReversed(
        id: String,
        reversalReason: String,
        reversedBy: String,
        reversedAt: Long
    ): Boolean = mutex.withLock {
        val index = allocations.indexOfFirst { it.id == id }
        if (index == -1) return@withLock false
        val existing = allocations[index]
        if (existing.isReversed) return@withLock false

        allocations[index] = existing.copy(
            isReversed = true,
            reversalReason = reversalReason,
            reversedBy = reversedBy,
            reversedAt = reversedAt,
            version = existing.version + 1
        )
        true
    }

    override suspend fun recordAuditEvent(event: BusinessLedgerAuditEvent) {
        mutex.withLock {
            auditEvents.add(event)
        }
    }

    override suspend fun listAuditEvents(
        tenantId: String,
        projectId: String,
        sourceId: String?,
        postingId: String?,
        allocationId: String?
    ): List<BusinessLedgerAuditEvent> = mutex.withLock {
        auditEvents.filter { a ->
            a.tenantId == tenantId && a.projectId == projectId &&
            (sourceId == null || a.sourceId == sourceId) &&
            (postingId == null || a.postingId == postingId) &&
            (allocationId == null || a.allocationId == allocationId)
        }.sortedByDescending { it.timestamp }
    }

    override suspend fun calculateBalanceSummary(
        tenantId: String,
        projectId: String,
        asOfTimestamp: Long
    ): BusinessLedgerBalanceSummary = mutex.withLock {
        val eligiblePostings = postings.filter {
            it.tenantId == tenantId && it.projectId == projectId && it.postingDate <= asOfTimestamp
        }

        var totalDebit = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var totalCredit = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)

        for (p in eligiblePostings) {
            totalDebit = totalDebit.add(p.debitAmount)
            totalCredit = totalCredit.add(p.creditAmount)
        }

        val netMovement = totalDebit.subtract(totalCredit).setScale(4, RoundingMode.HALF_UP)
        val openingBalance = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        val closingBalance = openingBalance.add(netMovement).setScale(4, RoundingMode.HALF_UP)

        BusinessLedgerBalanceSummary(
            tenantId = tenantId,
            projectId = projectId,
            openingBalance = openingBalance,
            totalDebit = totalDebit,
            totalCredit = totalCredit,
            netMovement = netMovement,
            closingBalance = closingBalance,
            currency = "BDT",
            asOfTimestamp = asOfTimestamp
        )
    }

    override suspend fun calculatePeriodSummary(
        tenantId: String,
        projectId: String,
        fromDate: Long,
        toDate: Long
    ): BusinessLedgerPeriodSummary = mutex.withLock {
        val priorPostings = postings.filter {
            it.tenantId == tenantId && it.projectId == projectId && it.postingDate < fromDate
        }
        var priorDebit = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var priorCredit = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        for (p in priorPostings) {
            priorDebit = priorDebit.add(p.debitAmount)
            priorCredit = priorCredit.add(p.creditAmount)
        }
        val openingBalance = priorDebit.subtract(priorCredit).setScale(4, RoundingMode.HALF_UP)

        val periodPostings = postings.filter {
            it.tenantId == tenantId && it.projectId == projectId && it.postingDate in fromDate..toDate
        }
        var periodDebit = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        var periodCredit = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        for (p in periodPostings) {
            periodDebit = periodDebit.add(p.debitAmount)
            periodCredit = periodCredit.add(p.creditAmount)
        }
        val netMovement = periodDebit.subtract(periodCredit).setScale(4, RoundingMode.HALF_UP)
        val closingBalance = openingBalance.add(netMovement).setScale(4, RoundingMode.HALF_UP)

        BusinessLedgerPeriodSummary(
            tenantId = tenantId,
            projectId = projectId,
            fromDate = fromDate,
            toDate = toDate,
            openingBalance = openingBalance,
            totalDebit = periodDebit,
            totalCredit = periodCredit,
            netMovement = netMovement,
            closingBalance = closingBalance,
            postingCount = periodPostings.size,
            currency = "BDT"
        )
    }
}

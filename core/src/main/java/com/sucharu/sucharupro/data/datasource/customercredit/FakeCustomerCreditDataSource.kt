package com.sucharu.sucharupro.data.datasource.customercredit

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustment
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustmentType
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvance
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvanceStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAllocationStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditAllocation
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditAuditEvent
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditSummary
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefund
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefundStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Thread-safe in-memory data source for Customer Credit operations (Module 14 Step 04).
 */
class FakeCustomerCreditDataSource : CustomerCreditDataSource {

    private val mutex = Mutex()
    private val advances = mutableMapOf<String, CustomerAdvance>()
    private val allocations = mutableMapOf<String, CustomerCreditAllocation>()
    private val adjustments = mutableMapOf<String, CustomerAdjustment>()
    private val refunds = mutableMapOf<String, CustomerRefund>()
    private val auditEvents = mutableListOf<CustomerCreditAuditEvent>()

    override suspend fun insertAdvance(advance: CustomerAdvance): DomainResult<CustomerAdvance> = mutex.withLock {
        if (advances.values.any { it.tenantId == advance.tenantId && it.advanceNumber == advance.advanceNumber }) {
            return DomainResult.Error(IllegalStateException("Advance number '${advance.advanceNumber}' already exists in tenant '${advance.tenantId}'"))
        }
        advances[advance.advanceId] = advance
        DomainResult.Success(advance)
    }

    override suspend fun findAdvanceById(
        tenantId: String,
        projectId: String,
        advanceId: String
    ): DomainResult<CustomerAdvance> = mutex.withLock {
        val adv = advances[advanceId]
        if (adv != null && adv.tenantId == tenantId && adv.projectId == projectId) {
            DomainResult.Success(adv)
        } else {
            DomainResult.Error(IllegalArgumentException("Advance '$advanceId' not found in project/tenant scope"))
        }
    }

    override suspend fun findAdvanceByNumber(
        tenantId: String,
        advanceNumber: String
    ): DomainResult<CustomerAdvance> = mutex.withLock {
        val adv = advances.values.find { it.tenantId == tenantId && it.advanceNumber == advanceNumber }
        if (adv != null) {
            DomainResult.Success(adv)
        } else {
            DomainResult.Error(IllegalArgumentException("Advance number '$advanceNumber' not found in tenant '$tenantId'"))
        }
    }

    override suspend fun findAdvanceByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerAdvance?> = mutex.withLock {
        val adv = advances.values.find {
            it.tenantId == tenantId && it.projectId == projectId && it.idempotencyKey == idempotencyKey
        }
        DomainResult.Success(adv)
    }

    override suspend fun listAdvances(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerAdvanceStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerAdvance>> = mutex.withLock {
        val filtered = advances.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { customerId == null || it.customerId == customerId }
            .filter { status == null || it.status == status }
            .sortedByDescending { it.receiptDate }
            .drop(offset)
            .take(limit)
        DomainResult.Success(filtered)
    }

    override suspend fun updateAdvanceAllocation(
        tenantId: String,
        projectId: String,
        advanceId: String,
        newAllocatedAmount: BigDecimal,
        newAvailableAmount: BigDecimal,
        newStatus: CustomerAdvanceStatus,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerAdvance> = mutex.withLock {
        val existing = advances[advanceId]
            ?: return DomainResult.Error(IllegalArgumentException("Advance '$advanceId' not found"))
        if (existing.tenantId != tenantId || existing.projectId != projectId) {
            return DomainResult.Error(IllegalStateException("Tenant or project boundary violation"))
        }
        if (existing.version != expectedVersion) {
            return DomainResult.Error(IllegalStateException("Optimistic locking failure: expected version $expectedVersion, found ${existing.version}"))
        }
        val updated = existing.copy(
            allocatedAmount = newAllocatedAmount,
            availableAmount = newAvailableAmount,
            status = newStatus,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId,
            version = existing.version + 1
        )
        advances[advanceId] = updated
        DomainResult.Success(updated)
    }

    override suspend fun cancelAdvance(
        tenantId: String,
        projectId: String,
        advanceId: String,
        reason: String,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerAdvance> = mutex.withLock {
        val existing = advances[advanceId]
            ?: return DomainResult.Error(IllegalArgumentException("Advance '$advanceId' not found"))
        if (existing.tenantId != tenantId || existing.projectId != projectId) {
            return DomainResult.Error(IllegalStateException("Tenant or project boundary violation"))
        }
        if (existing.version != expectedVersion) {
            return DomainResult.Error(IllegalStateException("Optimistic locking failure: expected version $expectedVersion, found ${existing.version}"))
        }
        val updated = existing.copy(
            status = CustomerAdvanceStatus.CANCELLED,
            cancellationReason = reason,
            availableAmount = BigDecimal.ZERO,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorId,
            version = existing.version + 1
        )
        advances[advanceId] = updated
        DomainResult.Success(updated)
    }

    override suspend fun insertAllocation(allocation: CustomerCreditAllocation): DomainResult<CustomerCreditAllocation> = mutex.withLock {
        allocations[allocation.allocationId] = allocation
        DomainResult.Success(allocation)
    }

    override suspend fun findAllocationById(
        tenantId: String,
        projectId: String,
        allocationId: String
    ): DomainResult<CustomerCreditAllocation> = mutex.withLock {
        val alloc = allocations[allocationId]
        if (alloc != null && alloc.tenantId == tenantId && alloc.projectId == projectId) {
            DomainResult.Success(alloc)
        } else {
            DomainResult.Error(IllegalArgumentException("Allocation '$allocationId' not found"))
        }
    }

    override suspend fun findAllocationByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerCreditAllocation?> = mutex.withLock {
        val alloc = allocations.values.find {
            it.tenantId == tenantId && it.projectId == projectId && it.idempotencyKey == idempotencyKey
        }
        DomainResult.Success(alloc)
    }

    override suspend fun listAllocations(
        tenantId: String,
        projectId: String,
        customerId: String?,
        invoiceId: String?,
        advanceId: String?,
        status: CustomerAllocationStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerCreditAllocation>> = mutex.withLock {
        val filtered = allocations.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { customerId == null || it.customerId == customerId }
            .filter { invoiceId == null || it.invoiceId == invoiceId }
            .filter { advanceId == null || it.advanceId == advanceId }
            .filter { status == null || it.status == status }
            .sortedByDescending { it.allocatedAt }
            .drop(offset)
            .take(limit)
        DomainResult.Success(filtered)
    }

    override suspend fun updateAllocationStatus(
        tenantId: String,
        projectId: String,
        allocationId: String,
        newStatus: CustomerAllocationStatus,
        reason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerCreditAllocation> = mutex.withLock {
        val existing = allocations[allocationId]
            ?: return DomainResult.Error(IllegalArgumentException("Allocation '$allocationId' not found"))
        if (existing.tenantId != tenantId || existing.projectId != projectId) {
            return DomainResult.Error(IllegalStateException("Tenant or project boundary violation"))
        }
        if (existing.version != expectedVersion) {
            return DomainResult.Error(IllegalStateException("Optimistic locking failure: expected version $expectedVersion, found ${existing.version}"))
        }
        val updated = existing.copy(
            status = newStatus,
            reversalReason = reason,
            reversedAt = if (newStatus == CustomerAllocationStatus.REVERSED) System.currentTimeMillis() else existing.reversedAt,
            reversedBy = if (newStatus == CustomerAllocationStatus.REVERSED) actorId else existing.reversedBy,
            version = existing.version + 1
        )
        allocations[allocationId] = updated
        DomainResult.Success(updated)
    }

    override suspend fun insertAdjustment(adjustment: CustomerAdjustment): DomainResult<CustomerAdjustment> = mutex.withLock {
        if (adjustments.values.any { it.tenantId == adjustment.tenantId && it.adjustmentNumber == adjustment.adjustmentNumber }) {
            return DomainResult.Error(IllegalStateException("Adjustment number '${adjustment.adjustmentNumber}' already exists"))
        }
        adjustments[adjustment.adjustmentId] = adjustment
        DomainResult.Success(adjustment)
    }

    override suspend fun findAdjustmentById(
        tenantId: String,
        projectId: String,
        adjustmentId: String
    ): DomainResult<CustomerAdjustment> = mutex.withLock {
        val adj = adjustments[adjustmentId]
        if (adj != null && adj.tenantId == tenantId && adj.projectId == projectId) {
            DomainResult.Success(adj)
        } else {
            DomainResult.Error(IllegalArgumentException("Adjustment '$adjustmentId' not found"))
        }
    }

    override suspend fun findAdjustmentByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerAdjustment?> = mutex.withLock {
        val adj = adjustments.values.find {
            it.tenantId == tenantId && it.projectId == projectId && it.idempotencyKey == idempotencyKey
        }
        DomainResult.Success(adj)
    }

    override suspend fun listAdjustments(
        tenantId: String,
        projectId: String,
        customerId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerAdjustment>> = mutex.withLock {
        val filtered = adjustments.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { customerId == null || it.customerId == customerId }
            .sortedByDescending { it.createdAt }
            .drop(offset)
            .take(limit)
        DomainResult.Success(filtered)
    }

    override suspend fun insertRefund(refund: CustomerRefund): DomainResult<CustomerRefund> = mutex.withLock {
        if (refunds.values.any { it.tenantId == refund.tenantId && it.refundNumber == refund.refundNumber }) {
            return DomainResult.Error(IllegalStateException("Refund number '${refund.refundNumber}' already exists"))
        }
        refunds[refund.refundId] = refund
        DomainResult.Success(refund)
    }

    override suspend fun findRefundById(
        tenantId: String,
        projectId: String,
        refundId: String
    ): DomainResult<CustomerRefund> = mutex.withLock {
        val ref = refunds[refundId]
        if (ref != null && ref.tenantId == tenantId && ref.projectId == projectId) {
            DomainResult.Success(ref)
        } else {
            DomainResult.Error(IllegalArgumentException("Refund '$refundId' not found"))
        }
    }

    override suspend fun findRefundByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerRefund?> = mutex.withLock {
        val ref = refunds.values.find {
            it.tenantId == tenantId && it.projectId == projectId && it.idempotencyKey == idempotencyKey
        }
        DomainResult.Success(ref)
    }

    override suspend fun listRefunds(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerRefundStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerRefund>> = mutex.withLock {
        val filtered = refunds.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { customerId == null || it.customerId == customerId }
            .filter { status == null || it.status == status }
            .sortedByDescending { it.createdAt }
            .drop(offset)
            .take(limit)
        DomainResult.Success(filtered)
    }

    override suspend fun updateRefundStatus(
        tenantId: String,
        projectId: String,
        refundId: String,
        newStatus: CustomerRefundStatus,
        reason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerRefund> = mutex.withLock {
        val existing = refunds[refundId]
            ?: return DomainResult.Error(IllegalArgumentException("Refund '$refundId' not found"))
        if (existing.tenantId != tenantId || existing.projectId != projectId) {
            return DomainResult.Error(IllegalStateException("Tenant or project boundary violation"))
        }
        if (existing.version != expectedVersion) {
            return DomainResult.Error(IllegalStateException("Optimistic locking failure: expected version $expectedVersion, found ${existing.version}"))
        }
        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = newStatus,
            rejectionReason = if (newStatus in setOf(CustomerRefundStatus.REJECTED, CustomerRefundStatus.CANCELLED)) reason else existing.rejectionReason,
            approvedAt = if (newStatus == CustomerRefundStatus.APPROVED) now else existing.approvedAt,
            approvedBy = if (newStatus == CustomerRefundStatus.APPROVED) actorId else existing.approvedBy,
            processedAt = if (newStatus == CustomerRefundStatus.PROCESSED) now else existing.processedAt,
            processedBy = if (newStatus == CustomerRefundStatus.PROCESSED) actorId else existing.processedBy,
            completedAt = if (newStatus == CustomerRefundStatus.COMPLETED) now else existing.completedAt,
            completedBy = if (newStatus == CustomerRefundStatus.COMPLETED) actorId else existing.completedBy,
            updatedAt = now,
            updatedBy = actorId,
            version = existing.version + 1
        )
        refunds[refundId] = updated
        DomainResult.Success(updated)
    }

    override suspend fun getCustomerCreditSummary(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerCreditSummary> = mutex.withLock {
        val custAdvances = advances.values.filter {
            it.tenantId == tenantId && it.projectId == projectId && it.customerId == customerId && it.status != CustomerAdvanceStatus.CANCELLED
        }
        val totalAdv = custAdvances.map { it.amount }.fold(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP)
        val totalAvailAdv = custAdvances.map { it.availableAmount }.fold(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP)
        val totalAllocated = custAdvances.map { it.allocatedAmount }.fold(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP)

        val custAdjustments = adjustments.values.filter {
            it.tenantId == tenantId && it.projectId == projectId && it.customerId == customerId
        }
        val totalCreditAdj = custAdjustments
            .filter { it.adjustmentType == CustomerAdjustmentType.CREDIT }
            .map { it.amount }
            .fold(BigDecimal.ZERO, BigDecimal::add)
            .setScale(4, RoundingMode.HALF_UP)
        val totalDebitAdj = custAdjustments
            .filter { it.adjustmentType == CustomerAdjustmentType.DEBIT }
            .map { it.amount }
            .fold(BigDecimal.ZERO, BigDecimal::add)
            .setScale(4, RoundingMode.HALF_UP)

        val custRefunds = refunds.values.filter {
            it.tenantId == tenantId && it.projectId == projectId && it.customerId == customerId &&
                    it.status in setOf(CustomerRefundStatus.APPROVED, CustomerRefundStatus.PROCESSED, CustomerRefundStatus.COMPLETED)
        }
        val totalRef = custRefunds.map { it.amount }.fold(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP)

        val netAvailableCredit = totalAvailAdv.add(totalCreditAdj).subtract(totalDebitAdj).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)

        val accountId = custAdvances.firstOrNull()?.customerFinancialAccountId
            ?: custAdjustments.firstOrNull()?.customerFinancialAccountId
            ?: ""

        DomainResult.Success(
            CustomerCreditSummary(
                customerId = customerId,
                customerFinancialAccountId = accountId,
                totalAdvances = totalAdv,
                totalAllocated = totalAllocated,
                totalAvailableCredit = netAvailableCredit,
                totalAdjustmentsCredit = totalCreditAdj,
                totalAdjustmentsDebit = totalDebitAdj,
                totalRefunds = totalRef,
                currency = "BDT"
            )
        )
    }

    override suspend fun insertAuditEvent(event: CustomerCreditAuditEvent): DomainResult<CustomerCreditAuditEvent> = mutex.withLock {
        auditEvents.add(event)
        DomainResult.Success(event)
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        entityId: String
    ): DomainResult<List<CustomerCreditAuditEvent>> = mutex.withLock {
        val list = auditEvents.filter {
            it.tenantId == tenantId && it.projectId == projectId && it.entityId == entityId
        }.sortedByDescending { it.occurredAt }
        DomainResult.Success(list)
    }
}

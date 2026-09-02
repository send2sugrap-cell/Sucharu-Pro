package com.sucharu.sucharupro.data.repository.customercredit

import com.sucharu.sucharupro.data.datasource.customercredit.CustomerCreditDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustment
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvance
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvanceStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAllocationStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditAllocation
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditAuditEvent
import com.sucharu.sucharupro.domain.model.customercredit.CustomerCreditSummary
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefund
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefundStatus
import com.sucharu.sucharupro.domain.repository.customercredit.CustomerCreditRepository
import java.math.BigDecimal

/**
 * Production repository implementation for Customer Credit, Advance, Adjustment, and Refund (Module 14 Step 04).
 */
class CustomerCreditRepositoryImpl(
    private val dataSource: CustomerCreditDataSource
) : CustomerCreditRepository {

    override suspend fun createAdvance(advance: CustomerAdvance): DomainResult<CustomerAdvance> {
        return dataSource.insertAdvance(advance)
    }

    override suspend fun getAdvanceById(
        tenantId: String,
        projectId: String,
        advanceId: String
    ): DomainResult<CustomerAdvance> {
        return dataSource.findAdvanceById(tenantId, projectId, advanceId)
    }

    override suspend fun getAdvanceByNumber(
        tenantId: String,
        advanceNumber: String
    ): DomainResult<CustomerAdvance> {
        return dataSource.findAdvanceByNumber(tenantId, advanceNumber)
    }

    override suspend fun findAdvanceByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerAdvance?> {
        return dataSource.findAdvanceByIdempotencyKey(tenantId, projectId, idempotencyKey)
    }

    override suspend fun listAdvances(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerAdvanceStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerAdvance>> {
        return dataSource.listAdvances(tenantId, projectId, customerId, status, limit, offset)
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
    ): DomainResult<CustomerAdvance> {
        return dataSource.updateAdvanceAllocation(
            tenantId, projectId, advanceId,
            newAllocatedAmount, newAvailableAmount, newStatus,
            actorId, expectedVersion
        )
    }

    override suspend fun cancelAdvance(
        tenantId: String,
        projectId: String,
        advanceId: String,
        reason: String,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerAdvance> {
        return dataSource.cancelAdvance(tenantId, projectId, advanceId, reason, actorId, expectedVersion)
    }

    override suspend fun createAllocation(allocation: CustomerCreditAllocation): DomainResult<CustomerCreditAllocation> {
        return dataSource.insertAllocation(allocation)
    }

    override suspend fun getAllocationById(
        tenantId: String,
        projectId: String,
        allocationId: String
    ): DomainResult<CustomerCreditAllocation> {
        return dataSource.findAllocationById(tenantId, projectId, allocationId)
    }

    override suspend fun findAllocationByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerCreditAllocation?> {
        return dataSource.findAllocationByIdempotencyKey(tenantId, projectId, idempotencyKey)
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
    ): DomainResult<List<CustomerCreditAllocation>> {
        return dataSource.listAllocations(tenantId, projectId, customerId, invoiceId, advanceId, status, limit, offset)
    }

    override suspend fun updateAllocationStatus(
        tenantId: String,
        projectId: String,
        allocationId: String,
        newStatus: CustomerAllocationStatus,
        reason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerCreditAllocation> {
        return dataSource.updateAllocationStatus(tenantId, projectId, allocationId, newStatus, reason, actorId, expectedVersion)
    }

    override suspend fun createAdjustment(adjustment: CustomerAdjustment): DomainResult<CustomerAdjustment> {
        return dataSource.insertAdjustment(adjustment)
    }

    override suspend fun getAdjustmentById(
        tenantId: String,
        projectId: String,
        adjustmentId: String
    ): DomainResult<CustomerAdjustment> {
        return dataSource.findAdjustmentById(tenantId, projectId, adjustmentId)
    }

    override suspend fun findAdjustmentByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerAdjustment?> {
        return dataSource.findAdjustmentByIdempotencyKey(tenantId, projectId, idempotencyKey)
    }

    override suspend fun listAdjustments(
        tenantId: String,
        projectId: String,
        customerId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerAdjustment>> {
        return dataSource.listAdjustments(tenantId, projectId, customerId, limit, offset)
    }

    override suspend fun createRefund(refund: CustomerRefund): DomainResult<CustomerRefund> {
        return dataSource.insertRefund(refund)
    }

    override suspend fun getRefundById(
        tenantId: String,
        projectId: String,
        refundId: String
    ): DomainResult<CustomerRefund> {
        return dataSource.findRefundById(tenantId, projectId, refundId)
    }

    override suspend fun findRefundByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerRefund?> {
        return dataSource.findRefundByIdempotencyKey(tenantId, projectId, idempotencyKey)
    }

    override suspend fun listRefunds(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CustomerRefundStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerRefund>> {
        return dataSource.listRefunds(tenantId, projectId, customerId, status, limit, offset)
    }

    override suspend fun updateRefundStatus(
        tenantId: String,
        projectId: String,
        refundId: String,
        newStatus: CustomerRefundStatus,
        reason: String?,
        actorId: String,
        expectedVersion: Long
    ): DomainResult<CustomerRefund> {
        return dataSource.updateRefundStatus(tenantId, projectId, refundId, newStatus, reason, actorId, expectedVersion)
    }

    override suspend fun getCustomerCreditSummary(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerCreditSummary> {
        return dataSource.getCustomerCreditSummary(tenantId, projectId, customerId)
    }

    override suspend fun recordAuditEvent(event: CustomerCreditAuditEvent): DomainResult<CustomerCreditAuditEvent> {
        return dataSource.insertAuditEvent(event)
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        entityId: String
    ): DomainResult<List<CustomerCreditAuditEvent>> {
        return dataSource.getAuditEvents(tenantId, projectId, entityId)
    }
}

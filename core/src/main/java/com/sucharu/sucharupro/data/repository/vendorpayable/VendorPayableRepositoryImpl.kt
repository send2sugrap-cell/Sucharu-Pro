package com.sucharu.sucharupro.data.repository.vendorpayable

import com.sucharu.sucharupro.data.datasource.vendorpayable.VendorPayableDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorpayable.*
import com.sucharu.sucharupro.domain.repository.vendorpayable.VendorPayableRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe implementation of VendorPayableRepository with Mutex concurrency locking (Module 15 Step 02).
 */
class VendorPayableRepositoryImpl(
    private val dataSource: VendorPayableDataSource
) : VendorPayableRepository {

    private val mutex = Mutex()

    override suspend fun createPayable(payable: VendorPayable): DomainResult<VendorPayable> = mutex.withLock {
        try {
            if (!payable.idempotencyKey.isNullOrBlank()) {
                val existing = dataSource.getPayableByIdempotencyKey(
                    payable.tenantId,
                    payable.projectId,
                    payable.idempotencyKey
                )
                if (existing != null) {
                    return DomainResult.Success(existing)
                }
            }

            val inserted = dataSource.insertPayable(payable)
            if (inserted) {
                DomainResult.Success(payable)
            } else {
                DomainResult.Error(message = "Failed to insert vendor payable.")
            }
        } catch (e: Exception) {
            DomainResult.Error(message = "Exception creating vendor payable: ${e.message}", exception = e)
        }
    }

    override suspend fun updatePayable(payable: VendorPayable): DomainResult<VendorPayable> = mutex.withLock {
        try {
            val updated = dataSource.updatePayable(payable)
            if (updated) {
                DomainResult.Success(payable)
            } else {
                DomainResult.Error(message = "Failed to update vendor payable ${payable.payableId}.")
            }
        } catch (e: Exception) {
            DomainResult.Error(message = "Exception updating vendor payable: ${e.message}", exception = e)
        }
    }

    override suspend fun getPayableById(
        tenantId: String,
        projectId: String,
        payableId: String
    ): DomainResult<VendorPayable?> {
        return try {
            val result = dataSource.getPayableById(tenantId, projectId, payableId)
            DomainResult.Success(result)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to retrieve vendor payable $payableId: ${e.message}", exception = e)
        }
    }

    override suspend fun getPayableByNumber(
        tenantId: String,
        projectId: String,
        payableNumber: String
    ): DomainResult<VendorPayable?> {
        return try {
            val result = dataSource.getPayableByNumber(tenantId, projectId, payableNumber)
            DomainResult.Success(result)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to retrieve vendor payable number $payableNumber: ${e.message}", exception = e)
        }
    }

    override suspend fun getPayableByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<VendorPayable?> {
        return try {
            val result = dataSource.getPayableByIdempotencyKey(tenantId, projectId, idempotencyKey)
            DomainResult.Success(result)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to retrieve vendor payable by idempotency key: ${e.message}", exception = e)
        }
    }

    override suspend fun listPayables(
        tenantId: String,
        projectId: String,
        vendorId: String?,
        status: VendorPayableStatus?,
        jobId: String?,
        isOverdueOnly: Boolean,
        fromDate: Long?,
        toDate: Long?,
        limit: Int,
        offset: Int
    ): DomainResult<List<VendorPayable>> {
        return try {
            val list = dataSource.listPayables(
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                status = status,
                jobId = jobId,
                isOverdueOnly = isOverdueOnly,
                fromDate = fromDate,
                toDate = toDate,
                limit = limit,
                offset = offset
            )
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to list vendor payables: ${e.message}", exception = e)
        }
    }

    override suspend fun countPayables(
        tenantId: String,
        projectId: String,
        vendorId: String?,
        status: VendorPayableStatus?,
        jobId: String?,
        isOverdueOnly: Boolean,
        fromDate: Long?,
        toDate: Long?
    ): DomainResult<Long> {
        return try {
            val count = dataSource.countPayables(
                tenantId = tenantId,
                projectId = projectId,
                vendorId = vendorId,
                status = status,
                jobId = jobId,
                isOverdueOnly = isOverdueOnly,
                fromDate = fromDate,
                toDate = toDate
            )
            DomainResult.Success(count)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to count vendor payables: ${e.message}", exception = e)
        }
    }

    override suspend fun generateNextPayableNumber(tenantId: String, projectId: String): String {
        return dataSource.generateNextPayableNumber(tenantId, projectId)
    }

    override suspend fun recordPaymentAllocation(allocation: VendorPayablePaymentAllocation): DomainResult<VendorPayablePaymentAllocation> = mutex.withLock {
        try {
            if (!allocation.idempotencyKey.isNullOrBlank()) {
                val existing = dataSource.getAllocationByIdempotencyKey(allocation.tenantId, allocation.idempotencyKey)
                if (existing != null) {
                    return DomainResult.Success(existing)
                }
            }

            val inserted = dataSource.insertPaymentAllocation(allocation)
            if (inserted) {
                DomainResult.Success(allocation)
            } else {
                DomainResult.Error(message = "Failed to insert payment allocation.")
            }
        } catch (e: Exception) {
            DomainResult.Error(message = "Exception recording payment allocation: ${e.message}", exception = e)
        }
    }

    override suspend fun getPaymentAllocations(
        tenantId: String,
        projectId: String,
        payableId: String
    ): DomainResult<List<VendorPayablePaymentAllocation>> {
        return try {
            val list = dataSource.getAllocationsForPayable(tenantId, projectId, payableId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to get payment allocations: ${e.message}", exception = e)
        }
    }

    override suspend fun recordAuditEvent(event: VendorPayableAuditEvent): DomainResult<Unit> {
        return try {
            dataSource.insertAuditEvent(event)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to record audit event: ${e.message}", exception = e)
        }
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        payableId: String
    ): DomainResult<List<VendorPayableAuditEvent>> {
        return try {
            val list = dataSource.getAuditEvents(tenantId, projectId, payableId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = "Failed to get audit events: ${e.message}", exception = e)
        }
    }
}

package com.sucharu.sucharupro.data.datasource.vendorpayable

import com.sucharu.sucharupro.domain.model.vendorpayable.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * In-memory thread-safe mock implementation of VendorPayableDataSource for testing (Module 15 Step 02).
 */
class FakeVendorPayableDataSource : VendorPayableDataSource {

    private val payables = ConcurrentHashMap<String, VendorPayable>()
    private val allocations = ConcurrentHashMap<String, MutableList<VendorPayablePaymentAllocation>>()
    private val auditEvents = ConcurrentHashMap<String, MutableList<VendorPayableAuditEvent>>()
    private val sequenceCounter = AtomicLong(1001L)

    override suspend fun insertPayable(payable: VendorPayable): Boolean {
        payables[payable.payableId] = payable
        return true
    }

    override suspend fun updatePayable(payable: VendorPayable): Boolean {
        payables[payable.payableId] = payable
        return true
    }

    override suspend fun getPayableById(tenantId: String, projectId: String, payableId: String): VendorPayable? {
        val p = payables[payableId] ?: return null
        return if (p.tenantId == tenantId && p.projectId == projectId) p else null
    }

    override suspend fun getPayableByNumber(tenantId: String, projectId: String, payableNumber: String): VendorPayable? {
        return payables.values.firstOrNull {
            it.tenantId == tenantId && it.projectId == projectId && it.payableNumber == payableNumber
        }
    }

    override suspend fun getPayableByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): VendorPayable? {
        return payables.values.firstOrNull {
            it.tenantId == tenantId && it.projectId == projectId && it.idempotencyKey == idempotencyKey
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
    ): List<VendorPayable> {
        val now = System.currentTimeMillis()
        return payables.values
            .asSequence()
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { vendorId == null || it.vendorId == vendorId }
            .filter { status == null || it.status == status }
            .filter { jobId == null || it.jobId == jobId }
            .filter { !isOverdueOnly || (it.dueDate < now && it.status in setOf(VendorPayableStatus.APPROVED, VendorPayableStatus.PARTIALLY_PAID)) }
            .filter { fromDate == null || it.issueDate >= fromDate }
            .filter { toDate == null || it.issueDate <= toDate }
            .sortedByDescending { it.issueDate }
            .drop(offset)
            .take(limit)
            .toList()
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
    ): Long {
        val now = System.currentTimeMillis()
        return payables.values
            .asSequence()
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { vendorId == null || it.vendorId == vendorId }
            .filter { status == null || it.status == status }
            .filter { jobId == null || it.jobId == jobId }
            .filter { !isOverdueOnly || (it.dueDate < now && it.status in setOf(VendorPayableStatus.APPROVED, VendorPayableStatus.PARTIALLY_PAID)) }
            .filter { fromDate == null || it.issueDate >= fromDate }
            .filter { toDate == null || it.issueDate <= toDate }
            .count()
            .toLong()
    }

    override suspend fun generateNextPayableNumber(tenantId: String, projectId: String): String {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val seq = sequenceCounter.getAndIncrement()
        return "PAYABLE-$dateStr-${seq.toString().padStart(4, '0')}"
    }

    override suspend fun insertPaymentAllocation(allocation: VendorPayablePaymentAllocation): Boolean {
        val list = allocations.computeIfAbsent(allocation.payableId) { Collections.synchronizedList(mutableListOf()) }
        list.add(allocation)
        return true
    }

    override suspend fun getAllocationsForPayable(
        tenantId: String,
        projectId: String,
        payableId: String
    ): List<VendorPayablePaymentAllocation> {
        val list = allocations[payableId] ?: emptyList()
        return synchronized(list) {
            list.filter { it.tenantId == tenantId && it.projectId == projectId }
                .sortedBy { it.allocatedAt }
        }
    }

    override suspend fun getAllocationByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): VendorPayablePaymentAllocation? {
        return allocations.values
            .asSequence()
            .flatten()
            .firstOrNull { it.tenantId == tenantId && it.idempotencyKey == idempotencyKey }
    }

    override suspend fun insertAuditEvent(event: VendorPayableAuditEvent): Boolean {
        val list = auditEvents.computeIfAbsent(event.payableId) { Collections.synchronizedList(mutableListOf()) }
        list.add(event)
        return true
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        payableId: String
    ): List<VendorPayableAuditEvent> {
        val list = auditEvents[payableId] ?: emptyList()
        return synchronized(list) {
            list.filter { it.tenantId == tenantId && it.projectId == projectId }
                .sortedBy { it.timestamp }
        }
    }
}

package com.sucharu.sucharupro.domain.repository.vendorpayable

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorpayable.*

/**
 * Domain repository contract for Vendor Payables and Supplier Liabilities (Module 15 Step 02).
 */
interface VendorPayableRepository {

    suspend fun createPayable(payable: VendorPayable): DomainResult<VendorPayable>

    suspend fun updatePayable(payable: VendorPayable): DomainResult<VendorPayable>

    suspend fun getPayableById(tenantId: String, projectId: String, payableId: String): DomainResult<VendorPayable?>

    suspend fun getPayableByNumber(tenantId: String, projectId: String, payableNumber: String): DomainResult<VendorPayable?>

    suspend fun getPayableByIdempotencyKey(tenantId: String, projectId: String, idempotencyKey: String): DomainResult<VendorPayable?>

    suspend fun listPayables(
        tenantId: String,
        projectId: String,
        vendorId: String? = null,
        status: VendorPayableStatus? = null,
        jobId: String? = null,
        isOverdueOnly: Boolean = false,
        fromDate: Long? = null,
        toDate: Long? = null,
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<VendorPayable>>

    suspend fun countPayables(
        tenantId: String,
        projectId: String,
        vendorId: String? = null,
        status: VendorPayableStatus? = null,
        jobId: String? = null,
        isOverdueOnly: Boolean = false,
        fromDate: Long? = null,
        toDate: Long? = null
    ): DomainResult<Long>

    suspend fun generateNextPayableNumber(tenantId: String, projectId: String): String

    suspend fun recordPaymentAllocation(allocation: VendorPayablePaymentAllocation): DomainResult<VendorPayablePaymentAllocation>

    suspend fun getPaymentAllocations(tenantId: String, projectId: String, payableId: String): DomainResult<List<VendorPayablePaymentAllocation>>

    suspend fun recordAuditEvent(event: VendorPayableAuditEvent): DomainResult<Unit>

    suspend fun getAuditEvents(tenantId: String, projectId: String, payableId: String): DomainResult<List<VendorPayableAuditEvent>>
}

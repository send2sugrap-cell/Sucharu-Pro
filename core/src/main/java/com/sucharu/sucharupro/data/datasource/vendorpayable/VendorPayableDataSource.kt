package com.sucharu.sucharupro.data.datasource.vendorpayable

import com.sucharu.sucharupro.domain.model.vendorpayable.*

/**
 * Data source abstraction for Vendor Payables (Module 15 Step 02).
 */
interface VendorPayableDataSource {

    suspend fun insertPayable(payable: VendorPayable): Boolean

    suspend fun updatePayable(payable: VendorPayable): Boolean

    suspend fun getPayableById(tenantId: String, projectId: String, payableId: String): VendorPayable?

    suspend fun getPayableByNumber(tenantId: String, projectId: String, payableNumber: String): VendorPayable?

    suspend fun getPayableByIdempotencyKey(tenantId: String, projectId: String, idempotencyKey: String): VendorPayable?

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
    ): List<VendorPayable>

    suspend fun countPayables(
        tenantId: String,
        projectId: String,
        vendorId: String? = null,
        status: VendorPayableStatus? = null,
        jobId: String? = null,
        isOverdueOnly: Boolean = false,
        fromDate: Long? = null,
        toDate: Long? = null
    ): Long

    suspend fun generateNextPayableNumber(tenantId: String, projectId: String): String

    suspend fun insertPaymentAllocation(allocation: VendorPayablePaymentAllocation): Boolean

    suspend fun getAllocationsForPayable(tenantId: String, projectId: String, payableId: String): List<VendorPayablePaymentAllocation>

    suspend fun getAllocationByIdempotencyKey(tenantId: String, idempotencyKey: String): VendorPayablePaymentAllocation?

    suspend fun insertAuditEvent(event: VendorPayableAuditEvent): Boolean

    suspend fun getAuditEvents(tenantId: String, projectId: String, payableId: String): List<VendorPayableAuditEvent>
}

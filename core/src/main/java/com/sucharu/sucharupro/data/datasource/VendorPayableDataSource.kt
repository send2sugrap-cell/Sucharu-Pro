package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.VendorPayable
import com.sucharu.sucharupro.domain.model.finance.VendorPayableActivityEvent
import kotlinx.coroutines.flow.Flow

/**
 * Data Source abstraction for Vendor Payable liabilities (Module 09 Step 04).
 */
interface VendorPayableDataSource {

    suspend fun insertPayable(payable: VendorPayable): Boolean

    suspend fun updatePayable(payable: VendorPayable): Boolean

    suspend fun getPayableById(payableId: String): VendorPayable?

    suspend fun getPayableByNumber(projectId: String, payableNo: String): VendorPayable?

    suspend fun getPayableByReference(projectId: String, vendorId: String, referenceId: String): VendorPayable?

    suspend fun getPayableByInvoice(projectId: String, vendorId: String, supplierInvoiceNo: String): VendorPayable?

    fun observePayables(projectId: String): Flow<List<VendorPayable>>

    fun observeVendorPayables(projectId: String, vendorId: String): Flow<List<VendorPayable>>

    suspend fun insertActivityEvent(event: VendorPayableActivityEvent): Boolean

    suspend fun getActivityEvents(payableId: String): List<VendorPayableActivityEvent>

    suspend fun generateNextPayableNo(projectId: String): String
}

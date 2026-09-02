package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.SupplierPayment
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentActivityEvent
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentSettlement
import kotlinx.coroutines.flow.Flow

/**
 * Data Source contract for Supplier Payments and Settlements (Module 09 Step 05).
 */
interface SupplierPaymentDataSource {

    suspend fun insertPayment(payment: SupplierPayment): Boolean

    suspend fun updatePayment(payment: SupplierPayment): Boolean

    suspend fun getPaymentById(paymentId: String): SupplierPayment?

    suspend fun getPaymentByNumber(projectId: String, paymentNo: String): SupplierPayment?

    suspend fun getPaymentByIdempotencyKey(projectId: String, idempotencyKey: String): SupplierPayment?

    suspend fun getActivePaymentByReference(
        projectId: String,
        vendorId: String,
        paymentMethod: SupplierPaymentMethod,
        paymentReference: String
    ): SupplierPayment?

    fun observePayments(projectId: String): Flow<List<SupplierPayment>>

    fun observeVendorPayments(projectId: String, vendorId: String): Flow<List<SupplierPayment>>

    fun observePayablePayments(projectId: String, payableId: String): Flow<List<SupplierPayment>>

    suspend fun insertSettlement(settlement: SupplierPaymentSettlement): Boolean

    suspend fun getSettlementsByPayable(payableId: String): List<SupplierPaymentSettlement>

    suspend fun getSettlementsByPayment(paymentId: String): List<SupplierPaymentSettlement>

    fun observeSettlements(projectId: String): Flow<List<SupplierPaymentSettlement>>

    suspend fun insertActivityEvent(event: SupplierPaymentActivityEvent): Boolean

    suspend fun getActivityEvents(paymentId: String): List<SupplierPaymentActivityEvent>

    suspend fun generateNextPaymentNo(projectId: String): String
}

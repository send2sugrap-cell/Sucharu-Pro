package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.CustomerPayment
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentActivityEvent
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentReceipt
import kotlinx.coroutines.flow.Flow

/**
 * Data Source contract for Customer Payments and Receipts (Module 09 Step 03).
 */
interface CustomerPaymentDataSource {

    suspend fun insertPayment(payment: CustomerPayment): Boolean

    suspend fun updatePayment(payment: CustomerPayment): Boolean

    suspend fun getPaymentById(paymentId: String): CustomerPayment?

    suspend fun getPaymentByNumber(projectId: String, paymentNo: String): CustomerPayment?

    suspend fun getPaymentByIdempotencyKey(projectId: String, idempotencyKey: String): CustomerPayment?

    suspend fun getActivePaymentByReference(
        projectId: String,
        customerId: String,
        paymentMethod: CustomerPaymentMethod,
        paymentReference: String
    ): CustomerPayment?

    fun observePayments(projectId: String): Flow<List<CustomerPayment>>

    fun observeCustomerPayments(projectId: String, customerId: String): Flow<List<CustomerPayment>>

    suspend fun insertReceipt(receipt: CustomerPaymentReceipt): Boolean

    suspend fun getReceiptById(receiptId: String): CustomerPaymentReceipt?

    suspend fun getReceiptByPaymentId(paymentId: String): CustomerPaymentReceipt?

    fun observeCustomerReceipts(projectId: String, customerId: String): Flow<List<CustomerPaymentReceipt>>

    suspend fun insertActivityEvent(event: CustomerPaymentActivityEvent): Boolean

    suspend fun getActivityEvents(paymentId: String): List<CustomerPaymentActivityEvent>

    suspend fun generateNextPaymentNo(projectId: String): String

    suspend fun generateNextReceiptNo(projectId: String): String
}

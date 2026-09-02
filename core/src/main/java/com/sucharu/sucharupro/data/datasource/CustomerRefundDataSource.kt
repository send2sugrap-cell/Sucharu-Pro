package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.CustomerRefund
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentActivityEvent
import kotlinx.coroutines.flow.Flow

/**
 * Data Source contract for Customer Refunds (Module 09 Step 07).
 */
interface CustomerRefundDataSource {

    suspend fun insertRefund(refund: CustomerRefund): Boolean

    suspend fun updateRefund(refund: CustomerRefund): Boolean

    suspend fun getRefundById(refundId: String): CustomerRefund?

    suspend fun getRefundByNumber(projectId: String, refundNo: String): CustomerRefund?

    suspend fun getRefundByIdempotencyKey(projectId: String, idempotencyKey: String): CustomerRefund?

    suspend fun getRefundsByPayment(projectId: String, paymentId: String): List<CustomerRefund>

    suspend fun getRefundsByReceivable(projectId: String, receivableId: String): List<CustomerRefund>

    fun observeRefunds(projectId: String): Flow<List<CustomerRefund>>

    fun observeCustomerRefunds(projectId: String, customerId: String): Flow<List<CustomerRefund>>

    suspend fun insertActivityEvent(event: FinancialAdjustmentActivityEvent): Boolean

    suspend fun getActivityEvents(refundId: String): List<FinancialAdjustmentActivityEvent>

    suspend fun generateNextRefundNo(projectId: String): String
}

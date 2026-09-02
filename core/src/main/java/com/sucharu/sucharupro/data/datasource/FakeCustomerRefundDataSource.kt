package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.CustomerRefund
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentActivityEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safe in-memory implementation of CustomerRefundDataSource (Module 09 Step 07).
 */
class FakeCustomerRefundDataSource : CustomerRefundDataSource {

    private val mutex = Mutex()
    private val refunds = LinkedHashMap<String, CustomerRefund>()
    private val activityEvents = mutableListOf<FinancialAdjustmentActivityEvent>()
    private val refundsFlow = MutableStateFlow<List<CustomerRefund>>(emptyList())
    private val refundCounter = AtomicInteger(1)

    override suspend fun insertRefund(refund: CustomerRefund): Boolean = mutex.withLock {
        if (refunds.containsKey(refund.refundId)) return@withLock false
        refunds[refund.refundId] = refund
        refundsFlow.value = refunds.values.toList()
        true
    }

    override suspend fun updateRefund(refund: CustomerRefund): Boolean = mutex.withLock {
        if (!refunds.containsKey(refund.refundId)) return@withLock false
        refunds[refund.refundId] = refund
        refundsFlow.value = refunds.values.toList()
        true
    }

    override suspend fun getRefundById(refundId: String): CustomerRefund? = mutex.withLock {
        refunds[refundId]
    }

    override suspend fun getRefundByNumber(
        projectId: String,
        refundNo: String
    ): CustomerRefund? = mutex.withLock {
        refunds.values.firstOrNull { it.projectId == projectId && it.refundNo.equals(refundNo, ignoreCase = true) }
    }

    override suspend fun getRefundByIdempotencyKey(
        projectId: String,
        idempotencyKey: String
    ): CustomerRefund? = mutex.withLock {
        refunds.values.firstOrNull {
            it.projectId == projectId &&
                    it.idempotencyKey != null &&
                    it.idempotencyKey.equals(idempotencyKey, ignoreCase = true)
        }
    }

    override suspend fun getRefundsByPayment(
        projectId: String,
        paymentId: String
    ): List<CustomerRefund> = mutex.withLock {
        refunds.values.filter { it.projectId == projectId && it.sourcePaymentId == paymentId }
    }

    override suspend fun getRefundsByReceivable(
        projectId: String,
        receivableId: String
    ): List<CustomerRefund> = mutex.withLock {
        refunds.values.filter { it.projectId == projectId && it.receivableId == receivableId }
    }

    override fun observeRefunds(projectId: String): Flow<List<CustomerRefund>> {
        return refundsFlow.map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override fun observeCustomerRefunds(
        projectId: String,
        customerId: String
    ): Flow<List<CustomerRefund>> {
        return refundsFlow.map { list ->
            list.filter { it.projectId == projectId && it.customerId == customerId }
        }
    }

    override suspend fun insertActivityEvent(event: FinancialAdjustmentActivityEvent): Boolean = mutex.withLock {
        activityEvents.add(event)
        true
    }

    override suspend fun getActivityEvents(refundId: String): List<FinancialAdjustmentActivityEvent> = mutex.withLock {
        activityEvents.filter { it.entityId == refundId }.toList()
    }

    override suspend fun generateNextRefundNo(projectId: String): String = mutex.withLock {
        String.format("REF-%05d", refundCounter.getAndIncrement())
    }
}

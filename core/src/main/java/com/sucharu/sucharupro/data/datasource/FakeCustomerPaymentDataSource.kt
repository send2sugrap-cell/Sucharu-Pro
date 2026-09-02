package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.CustomerPayment
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentActivityEvent
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentReceipt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory thread-safe fake data source for Module 09 Customer Payments and Receipts.
 */
class FakeCustomerPaymentDataSource : CustomerPaymentDataSource {

    private val mutex = Mutex()
    private val paymentsFlow = MutableStateFlow<Map<String, CustomerPayment>>(emptyMap())
    private val receiptsFlow = MutableStateFlow<Map<String, CustomerPaymentReceipt>>(emptyMap())
    private val activityEvents = mutableListOf<CustomerPaymentActivityEvent>()
    private var paymentSequence = 1000
    private var receiptSequence = 1000

    override suspend fun insertPayment(payment: CustomerPayment): Boolean = mutex.withLock {
        val current = paymentsFlow.value.toMutableMap()
        if (current.containsKey(payment.paymentId)) return@withLock false
        current[payment.paymentId] = payment
        paymentsFlow.value = current
        true
    }

    override suspend fun updatePayment(payment: CustomerPayment): Boolean = mutex.withLock {
        val current = paymentsFlow.value.toMutableMap()
        if (!current.containsKey(payment.paymentId)) return@withLock false
        current[payment.paymentId] = payment
        paymentsFlow.value = current
        true
    }

    override suspend fun getPaymentById(paymentId: String): CustomerPayment? = mutex.withLock {
        paymentsFlow.value[paymentId]
    }

    override suspend fun getPaymentByNumber(projectId: String, paymentNo: String): CustomerPayment? = mutex.withLock {
        paymentsFlow.value.values.find { it.projectId == projectId && it.paymentNo == paymentNo }
    }

    override suspend fun getPaymentByIdempotencyKey(projectId: String, idempotencyKey: String): CustomerPayment? = mutex.withLock {
        paymentsFlow.value.values.find { it.projectId == projectId && it.idempotencyKey == idempotencyKey }
    }

    override suspend fun getActivePaymentByReference(
        projectId: String,
        customerId: String,
        paymentMethod: CustomerPaymentMethod,
        paymentReference: String
    ): CustomerPayment? = mutex.withLock {
        paymentsFlow.value.values.find {
            it.projectId == projectId &&
            it.customerId == customerId &&
            it.paymentMethod == paymentMethod &&
            it.paymentReference == paymentReference
        }
    }

    override fun observePayments(projectId: String): Flow<List<CustomerPayment>> {
        return paymentsFlow.map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override fun observeCustomerPayments(projectId: String, customerId: String): Flow<List<CustomerPayment>> {
        return paymentsFlow.map { map ->
            map.values.filter { it.projectId == projectId && it.customerId == customerId }
                .sortedByDescending { it.createdAt }
        }
    }

    override suspend fun insertReceipt(receipt: CustomerPaymentReceipt): Boolean = mutex.withLock {
        val current = receiptsFlow.value.toMutableMap()
        if (current.containsKey(receipt.receiptId)) return@withLock false
        current[receipt.receiptId] = receipt
        receiptsFlow.value = current
        true
    }

    override suspend fun getReceiptById(receiptId: String): CustomerPaymentReceipt? = mutex.withLock {
        receiptsFlow.value[receiptId]
    }

    override suspend fun getReceiptByPaymentId(paymentId: String): CustomerPaymentReceipt? = mutex.withLock {
        receiptsFlow.value.values.find { it.paymentId == paymentId }
    }

    override fun observeCustomerReceipts(projectId: String, customerId: String): Flow<List<CustomerPaymentReceipt>> {
        return receiptsFlow.map { map ->
            map.values.filter { it.projectId == projectId && it.customerId == customerId }
                .sortedByDescending { it.issuedAt }
        }
    }

    override suspend fun insertActivityEvent(event: CustomerPaymentActivityEvent): Boolean = mutex.withLock {
        activityEvents.add(event)
        true
    }

    override suspend fun getActivityEvents(paymentId: String): List<CustomerPaymentActivityEvent> = mutex.withLock {
        activityEvents.filter { it.paymentId == paymentId }.sortedBy { it.timestamp }
    }

    override suspend fun generateNextPaymentNo(projectId: String): String = mutex.withLock {
        paymentSequence++
        "PAY-$paymentSequence"
    }

    override suspend fun generateNextReceiptNo(projectId: String): String = mutex.withLock {
        receiptSequence++
        "RCT-$receiptSequence"
    }
}

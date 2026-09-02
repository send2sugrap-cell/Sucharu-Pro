package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.SupplierPayment
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentActivityEvent
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.SupplierPaymentSettlement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safe, reactive in-memory implementation of SupplierPaymentDataSource (Module 09 Step 05).
 */
class FakeSupplierPaymentDataSource : SupplierPaymentDataSource {

    private val mutex = Mutex()
    private val payments = LinkedHashMap<String, SupplierPayment>()
    private val settlements = mutableListOf<SupplierPaymentSettlement>()
    private val activityEvents = mutableListOf<SupplierPaymentActivityEvent>()

    private val paymentsFlow = MutableStateFlow<List<SupplierPayment>>(emptyList())
    private val settlementsFlow = MutableStateFlow<List<SupplierPaymentSettlement>>(emptyList())
    private val sequenceCounter = AtomicInteger(1)

    override suspend fun insertPayment(payment: SupplierPayment): Boolean = mutex.withLock {
        if (payments.containsKey(payment.paymentId)) return@withLock false
        payments[payment.paymentId] = payment
        paymentsFlow.value = payments.values.toList()
        true
    }

    override suspend fun updatePayment(payment: SupplierPayment): Boolean = mutex.withLock {
        if (!payments.containsKey(payment.paymentId)) return@withLock false
        payments[payment.paymentId] = payment
        paymentsFlow.value = payments.values.toList()
        true
    }

    override suspend fun getPaymentById(paymentId: String): SupplierPayment? = mutex.withLock {
        payments[paymentId]
    }

    override suspend fun getPaymentByNumber(
        projectId: String,
        paymentNo: String
    ): SupplierPayment? = mutex.withLock {
        payments.values.firstOrNull { it.projectId == projectId && it.paymentNo.equals(paymentNo, ignoreCase = true) }
    }

    override suspend fun getPaymentByIdempotencyKey(
        projectId: String,
        idempotencyKey: String
    ): SupplierPayment? = mutex.withLock {
        payments.values.firstOrNull {
            it.projectId == projectId &&
                    it.idempotencyKey != null &&
                    it.idempotencyKey.equals(idempotencyKey, ignoreCase = true)
        }
    }

    override suspend fun getActivePaymentByReference(
        projectId: String,
        vendorId: String,
        paymentMethod: SupplierPaymentMethod,
        paymentReference: String
    ): SupplierPayment? = mutex.withLock {
        payments.values.firstOrNull {
            it.projectId == projectId &&
                    it.vendorId == vendorId &&
                    it.paymentMethod == paymentMethod &&
                    it.paymentReference != null &&
                    it.paymentReference.equals(paymentReference.trim(), ignoreCase = true) &&
                    !it.status.isTerminal
        }
    }

    override fun observePayments(projectId: String): Flow<List<SupplierPayment>> {
        return paymentsFlow.map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override fun observeVendorPayments(
        projectId: String,
        vendorId: String
    ): Flow<List<SupplierPayment>> {
        return paymentsFlow.map { list ->
            list.filter { it.projectId == projectId && it.vendorId == vendorId }
        }
    }

    override fun observePayablePayments(
        projectId: String,
        payableId: String
    ): Flow<List<SupplierPayment>> {
        return paymentsFlow.map { list ->
            list.filter { it.projectId == projectId && it.payableId == payableId }
        }
    }

    override suspend fun insertSettlement(settlement: SupplierPaymentSettlement): Boolean = mutex.withLock {
        settlements.add(settlement)
        settlementsFlow.value = settlements.toList()
        true
    }

    override suspend fun getSettlementsByPayable(payableId: String): List<SupplierPaymentSettlement> = mutex.withLock {
        settlements.filter { it.payableId == payableId }.toList()
    }

    override suspend fun getSettlementsByPayment(paymentId: String): List<SupplierPaymentSettlement> = mutex.withLock {
        settlements.filter { it.paymentId == paymentId }.toList()
    }

    override fun observeSettlements(projectId: String): Flow<List<SupplierPaymentSettlement>> {
        return settlementsFlow.map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override suspend fun insertActivityEvent(event: SupplierPaymentActivityEvent): Boolean = mutex.withLock {
        activityEvents.add(event)
        true
    }

    override suspend fun getActivityEvents(paymentId: String): List<SupplierPaymentActivityEvent> = mutex.withLock {
        activityEvents.filter { it.paymentId == paymentId }.toList()
    }

    override suspend fun generateNextPaymentNo(projectId: String): String = mutex.withLock {
        val seq = sequenceCounter.getAndIncrement()
        String.format("SPAY-%05d", seq)
    }
}

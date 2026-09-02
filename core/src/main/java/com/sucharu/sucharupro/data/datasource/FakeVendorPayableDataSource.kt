package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.VendorPayable
import com.sucharu.sucharupro.domain.model.finance.VendorPayableActivityEvent
import com.sucharu.sucharupro.domain.service.finance.VendorPayableAgingCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safe, reactive in-memory data source for Vendor Payables (Module 09 Step 04).
 */
class FakeVendorPayableDataSource : VendorPayableDataSource {

    private val mutex = Mutex()
    private val payables = LinkedHashMap<String, VendorPayable>()
    private val activityEvents = mutableListOf<VendorPayableActivityEvent>()
    private val payablesFlow = MutableStateFlow<List<VendorPayable>>(emptyList())
    private val sequenceCounter = AtomicInteger(1)

    override suspend fun insertPayable(payable: VendorPayable): Boolean = mutex.withLock {
        if (payables.containsKey(payable.payableId)) return@withLock false
        payables[payable.payableId] = payable
        payablesFlow.value = payables.values.toList()
        true
    }

    override suspend fun updatePayable(payable: VendorPayable): Boolean = mutex.withLock {
        if (!payables.containsKey(payable.payableId)) return@withLock false
        payables[payable.payableId] = payable
        payablesFlow.value = payables.values.toList()
        true
    }

    override suspend fun getPayableById(payableId: String): VendorPayable? = mutex.withLock {
        val raw = payables[payableId] ?: return@withLock null
        val effStatus = VendorPayableAgingCalculator.evaluateEffectiveStatus(raw)
        val effAging = VendorPayableAgingCalculator.calculateAgingBucket(raw.dueDate)
        raw.copy(status = effStatus, agingBucket = effAging)
    }

    override suspend fun getPayableByNumber(projectId: String, payableNo: String): VendorPayable? = mutex.withLock {
        val raw = payables.values.firstOrNull { it.projectId == projectId && it.payableNo.equals(payableNo, ignoreCase = true) }
            ?: return@withLock null
        val effStatus = VendorPayableAgingCalculator.evaluateEffectiveStatus(raw)
        val effAging = VendorPayableAgingCalculator.calculateAgingBucket(raw.dueDate)
        raw.copy(status = effStatus, agingBucket = effAging)
    }

    override suspend fun getPayableByReference(
        projectId: String,
        vendorId: String,
        referenceId: String
    ): VendorPayable? = mutex.withLock {
        val raw = payables.values.firstOrNull {
            it.projectId == projectId &&
                    it.vendorId == vendorId &&
                    it.referenceId == referenceId &&
                    !it.status.isTerminal
        } ?: return@withLock null
        val effStatus = VendorPayableAgingCalculator.evaluateEffectiveStatus(raw)
        val effAging = VendorPayableAgingCalculator.calculateAgingBucket(raw.dueDate)
        raw.copy(status = effStatus, agingBucket = effAging)
    }

    override suspend fun getPayableByInvoice(
        projectId: String,
        vendorId: String,
        supplierInvoiceNo: String
    ): VendorPayable? = mutex.withLock {
        val raw = payables.values.firstOrNull {
            it.projectId == projectId &&
                    it.vendorId == vendorId &&
                    it.supplierInvoiceNo != null &&
                    it.supplierInvoiceNo.equals(supplierInvoiceNo.trim(), ignoreCase = true) &&
                    !it.status.isTerminal
        } ?: return@withLock null
        val effStatus = VendorPayableAgingCalculator.evaluateEffectiveStatus(raw)
        val effAging = VendorPayableAgingCalculator.calculateAgingBucket(raw.dueDate)
        raw.copy(status = effStatus, agingBucket = effAging)
    }

    override fun observePayables(projectId: String): Flow<List<VendorPayable>> {
        return payablesFlow.map { list ->
            list.filter { it.projectId == projectId }.map { p ->
                val effStatus = VendorPayableAgingCalculator.evaluateEffectiveStatus(p)
                val effAging = VendorPayableAgingCalculator.calculateAgingBucket(p.dueDate)
                p.copy(status = effStatus, agingBucket = effAging)
            }
        }
    }

    override fun observeVendorPayables(
        projectId: String,
        vendorId: String
    ): Flow<List<VendorPayable>> {
        return payablesFlow.map { list ->
            list.filter { it.projectId == projectId && it.vendorId == vendorId }.map { p ->
                val effStatus = VendorPayableAgingCalculator.evaluateEffectiveStatus(p)
                val effAging = VendorPayableAgingCalculator.calculateAgingBucket(p.dueDate)
                p.copy(status = effStatus, agingBucket = effAging)
            }
        }
    }

    override suspend fun insertActivityEvent(event: VendorPayableActivityEvent): Boolean = mutex.withLock {
        activityEvents.add(event)
        true
    }

    override suspend fun getActivityEvents(payableId: String): List<VendorPayableActivityEvent> = mutex.withLock {
        activityEvents.filter { it.payableId == payableId }.toList()
    }

    override suspend fun generateNextPayableNo(projectId: String): String = mutex.withLock {
        val seq = sequenceCounter.getAndIncrement()
        String.format("PAY-%05d", seq)
    }
}

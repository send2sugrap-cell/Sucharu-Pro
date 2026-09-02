package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.finance.CustomerCreditNote
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustment
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentActivityEvent
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.VendorDebitNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safe in-memory implementation of FinancialAdjustmentDataSource (Module 09 Step 07).
 */
class FakeFinancialAdjustmentDataSource : FinancialAdjustmentDataSource {

    private val mutex = Mutex()
    private val adjustments = LinkedHashMap<String, FinancialAdjustment>()
    private val creditNotes = LinkedHashMap<String, CustomerCreditNote>()
    private val debitNotes = LinkedHashMap<String, VendorDebitNote>()
    private val activityEvents = mutableListOf<FinancialAdjustmentActivityEvent>()

    private val adjustmentsFlow = MutableStateFlow<List<FinancialAdjustment>>(emptyList())
    private val creditNotesFlow = MutableStateFlow<List<CustomerCreditNote>>(emptyList())
    private val debitNotesFlow = MutableStateFlow<List<VendorDebitNote>>(emptyList())

    private val adjCounter = AtomicInteger(1)
    private val cnCounter = AtomicInteger(1)
    private val dnCounter = AtomicInteger(1)

    override suspend fun insertAdjustment(adjustment: FinancialAdjustment): Boolean = mutex.withLock {
        if (adjustments.containsKey(adjustment.adjustmentId)) return@withLock false
        adjustments[adjustment.adjustmentId] = adjustment
        adjustmentsFlow.value = adjustments.values.toList()
        true
    }

    override suspend fun updateAdjustment(adjustment: FinancialAdjustment): Boolean = mutex.withLock {
        if (!adjustments.containsKey(adjustment.adjustmentId)) return@withLock false
        adjustments[adjustment.adjustmentId] = adjustment
        adjustmentsFlow.value = adjustments.values.toList()
        true
    }

    override suspend fun getAdjustmentById(adjustmentId: String): FinancialAdjustment? = mutex.withLock {
        adjustments[adjustmentId]
    }

    override suspend fun getAdjustmentByNumber(
        projectId: String,
        adjustmentNo: String
    ): FinancialAdjustment? = mutex.withLock {
        adjustments.values.firstOrNull { it.projectId == projectId && it.adjustmentNo.equals(adjustmentNo, ignoreCase = true) }
    }

    override suspend fun getAdjustmentByIdempotencyKey(
        projectId: String,
        idempotencyKey: String
    ): FinancialAdjustment? = mutex.withLock {
        adjustments.values.firstOrNull {
            it.projectId == projectId &&
                    it.idempotencyKey != null &&
                    it.idempotencyKey.equals(idempotencyKey, ignoreCase = true)
        }
    }

    override suspend fun getAdjustmentsByReference(
        projectId: String,
        referenceType: FinancialReferenceType,
        referenceId: String
    ): List<FinancialAdjustment> = mutex.withLock {
        adjustments.values.filter {
            it.projectId == projectId &&
                    it.referenceType == referenceType &&
                    it.referenceId == referenceId
        }
    }

    override fun observeAdjustments(projectId: String): Flow<List<FinancialAdjustment>> {
        return adjustmentsFlow.map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override fun observeCustomerAdjustments(
        projectId: String,
        customerId: String
    ): Flow<List<FinancialAdjustment>> {
        return adjustmentsFlow.map { list ->
            list.filter { it.projectId == projectId && it.customerId == customerId }
        }
    }

    override fun observeVendorAdjustments(
        projectId: String,
        vendorId: String
    ): Flow<List<FinancialAdjustment>> {
        return adjustmentsFlow.map { list ->
            list.filter { it.projectId == projectId && it.vendorId == vendorId }
        }
    }

    override suspend fun insertCreditNote(creditNote: CustomerCreditNote): Boolean = mutex.withLock {
        if (creditNotes.containsKey(creditNote.creditNoteId)) return@withLock false
        creditNotes[creditNote.creditNoteId] = creditNote
        creditNotesFlow.value = creditNotes.values.toList()
        true
    }

    override suspend fun getCreditNoteById(creditNoteId: String): CustomerCreditNote? = mutex.withLock {
        creditNotes[creditNoteId]
    }

    override suspend fun getCreditNoteByAdjustmentId(adjustmentId: String): CustomerCreditNote? = mutex.withLock {
        creditNotes.values.firstOrNull { it.adjustmentId == adjustmentId }
    }

    override fun observeCreditNotes(projectId: String): Flow<List<CustomerCreditNote>> {
        return creditNotesFlow.map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override suspend fun insertDebitNote(debitNote: VendorDebitNote): Boolean = mutex.withLock {
        if (debitNotes.containsKey(debitNote.debitNoteId)) return@withLock false
        debitNotes[debitNote.debitNoteId] = debitNote
        debitNotesFlow.value = debitNotes.values.toList()
        true
    }

    override suspend fun getDebitNoteById(debitNoteId: String): VendorDebitNote? = mutex.withLock {
        debitNotes[debitNoteId]
    }

    override suspend fun getDebitNoteByAdjustmentId(adjustmentId: String): VendorDebitNote? = mutex.withLock {
        debitNotes.values.firstOrNull { it.adjustmentId == adjustmentId }
    }

    override fun observeDebitNotes(projectId: String): Flow<List<VendorDebitNote>> {
        return debitNotesFlow.map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override suspend fun insertActivityEvent(event: FinancialAdjustmentActivityEvent): Boolean = mutex.withLock {
        activityEvents.add(event)
        true
    }

    override suspend fun getActivityEvents(entityId: String): List<FinancialAdjustmentActivityEvent> = mutex.withLock {
        activityEvents.filter { it.entityId == entityId }.toList()
    }

    override suspend fun generateNextAdjustmentNo(projectId: String): String = mutex.withLock {
        String.format("ADJ-%05d", adjCounter.getAndIncrement())
    }

    override suspend fun generateNextCreditNoteNo(projectId: String): String = mutex.withLock {
        String.format("CN-%05d", cnCounter.getAndIncrement())
    }

    override suspend fun generateNextDebitNoteNo(projectId: String): String = mutex.withLock {
        String.format("DN-%05d", dnCounter.getAndIncrement())
    }
}

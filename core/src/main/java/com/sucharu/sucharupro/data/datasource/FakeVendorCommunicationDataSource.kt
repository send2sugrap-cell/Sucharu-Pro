package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.communication.vendor.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

/**
 * Concurrency-safe in-memory Fake implementation of [VendorCommunicationDataSource] (Module 10 Step 05).
 *
 * Uses [Mutex] for all state-mutating operations.
 * Private unlocked helpers are used to avoid recursive Mutex locking.
 * Number format: VCM-YYYY-XXXXX (e.g. VCM-2026-00001)
 */
class FakeVendorCommunicationDataSource : VendorCommunicationDataSource {

    private val mutex = Mutex()
    private val communicationsState = MutableStateFlow<Map<String, VendorCommunication>>(emptyMap())
    private val historyState = MutableStateFlow<List<VendorCommunicationHistory>>(emptyList())
    private val readReceiptsState = MutableStateFlow<Map<String, VendorCommunicationReadReceipt>>(emptyMap())
    private val acknowledgementsState = MutableStateFlow<Map<String, VendorCommunicationAcknowledgement>>(emptyMap())
    private val engagementEventsState = MutableStateFlow<List<VendorEngagementEvent>>(emptyList())
    private val activityEventsState = MutableStateFlow<List<VendorCommunicationActivityEvent>>(emptyList())

    private var sequenceCounter = 0

    // =========================================================================
    // Private unlocked helpers (called inside withLock blocks only)
    // =========================================================================

    private fun getCommunicationByIdUnlocked(projectId: String, communicationId: String): VendorCommunication? =
        communicationsState.value[communicationId]?.takeIf { it.projectId == projectId }

    private fun generateNumberUnlocked(projectId: String): String {
        sequenceCounter++
        val year = Calendar.getInstance().get(Calendar.YEAR)
        return "VCM-$year-%05d".format(sequenceCounter)
    }

    // =========================================================================
    // Core communications
    // =========================================================================

    override suspend fun saveCommunication(communication: VendorCommunication): Unit = mutex.withLock {
        communicationsState.update { current ->
            current + (communication.communicationId to communication)
        }
    }

    override suspend fun getCommunicationById(projectId: String, communicationId: String): VendorCommunication? =
        mutex.withLock {
            getCommunicationByIdUnlocked(projectId, communicationId)
        }

    override suspend fun getCommunicationByNo(projectId: String, communicationNo: String): VendorCommunication? =
        mutex.withLock {
            communicationsState.value.values.firstOrNull {
                it.projectId == projectId && it.communicationNo == communicationNo
            }
        }

    override suspend fun getByIdempotencyKey(projectId: String, idempotencyKey: String): VendorCommunication? =
        mutex.withLock {
            communicationsState.value.values.firstOrNull {
                it.projectId == projectId && it.idempotencyKey == idempotencyKey
            }
        }

    override suspend fun getByDuplicateCriteria(
        projectId: String,
        vendorId: String,
        communicationType: VendorCommunicationType,
        referenceType: String?,
        referenceId: String?
    ): VendorCommunication? = mutex.withLock {
        communicationsState.value.values.firstOrNull { c ->
            c.projectId == projectId &&
                    c.vendorId == vendorId &&
                    c.communicationType == communicationType &&
                    c.referenceType == referenceType &&
                    c.referenceId == referenceId &&
                    (c.status == VendorCommunicationStatus.QUEUED ||
                            c.status == VendorCommunicationStatus.SENT ||
                            c.status == VendorCommunicationStatus.DELIVERED ||
                            c.status == VendorCommunicationStatus.SCHEDULED)
        }
    }

    override fun observeCommunicationsByVendor(projectId: String, vendorId: String): Flow<List<VendorCommunication>> {
        return communicationsState.map { map ->
            map.values
                .filter { it.projectId == projectId && it.vendorId == vendorId }
                .sortedByDescending { it.createdAt }
        }
    }

    override fun observeCommunicationsByProject(projectId: String): Flow<List<VendorCommunication>> {
        return communicationsState.map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override suspend fun getCommunicationsByReference(
        projectId: String,
        referenceType: String,
        referenceId: String
    ): List<VendorCommunication> = mutex.withLock {
        communicationsState.value.values
            .filter { it.projectId == projectId && it.referenceType == referenceType && it.referenceId == referenceId }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun generateCommunicationNumber(projectId: String): String = mutex.withLock {
        generateNumberUnlocked(projectId)
    }

    // =========================================================================
    // History (append-only)
    // =========================================================================

    override suspend fun recordHistory(history: VendorCommunicationHistory): Unit = mutex.withLock {
        historyState.update { current -> current + history }
    }

    override suspend fun getHistory(projectId: String, communicationId: String): List<VendorCommunicationHistory> =
        mutex.withLock {
            historyState.value
                .filter { it.projectId == projectId && it.communicationId == communicationId }
                .sortedBy { it.performedAt }
        }

    // =========================================================================
    // Read receipts
    // =========================================================================

    override suspend fun saveReadReceipt(receipt: VendorCommunicationReadReceipt): Unit = mutex.withLock {
        val key = "${receipt.projectId}:${receipt.communicationId}:${receipt.vendorId}"
        readReceiptsState.update { current -> current + (key to receipt) }
    }

    override suspend fun getReadReceipt(
        projectId: String,
        communicationId: String,
        vendorId: String
    ): VendorCommunicationReadReceipt? = mutex.withLock {
        val key = "$projectId:$communicationId:$vendorId"
        readReceiptsState.value[key]
    }

    // =========================================================================
    // Acknowledgements (immutable after creation)
    // =========================================================================

    override suspend fun saveAcknowledgement(acknowledgement: VendorCommunicationAcknowledgement): Unit =
        mutex.withLock {
            // Only save if not already present (immutability guarantee)
            val existing = acknowledgementsState.value[acknowledgement.communicationId]
            if (existing == null) {
                acknowledgementsState.update { current ->
                    current + (acknowledgement.communicationId to acknowledgement)
                }
            }
        }

    override suspend fun getAcknowledgement(
        projectId: String,
        communicationId: String
    ): VendorCommunicationAcknowledgement? = mutex.withLock {
        acknowledgementsState.value[communicationId]?.takeIf { it.projectId == projectId }
    }

    // =========================================================================
    // Engagement events
    // =========================================================================

    override suspend fun recordEngagementEvent(event: VendorEngagementEvent): Unit = mutex.withLock {
        engagementEventsState.update { current -> current + event }
    }

    override suspend fun getEngagementEventsByVendor(
        projectId: String,
        vendorId: String
    ): List<VendorEngagementEvent> = mutex.withLock {
        engagementEventsState.value
            .filter { it.projectId == projectId && it.vendorId == vendorId }
            .sortedByDescending { it.timestamp }
    }

    // =========================================================================
    // Audit activity events (append-only, immutable)
    // =========================================================================

    override suspend fun recordActivityEvent(event: VendorCommunicationActivityEvent): Unit = mutex.withLock {
        activityEventsState.update { current -> current + event }
    }

    override suspend fun getActivityEvents(
        projectId: String,
        communicationId: String
    ): List<VendorCommunicationActivityEvent> = mutex.withLock {
        activityEventsState.value
            .filter { it.projectId == projectId && it.communicationId == communicationId }
            .sortedBy { it.timestamp }
    }
}

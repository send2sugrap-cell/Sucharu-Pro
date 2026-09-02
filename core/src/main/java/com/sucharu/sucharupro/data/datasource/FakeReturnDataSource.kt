package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.returns.ReturnActivityEvent
import com.sucharu.sucharupro.domain.model.returns.ReturnInspection
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReceivingInfo
import com.sucharu.sucharupro.domain.model.returns.ReturnReconciliationResult
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe, in-memory implementation of [ReturnDataSource] (Module 11 Step 01, 02, 03, 04, 05).
 *
 * Follows the Fake DataSource pattern established by [FakeDeliveryReturnDataSource]
 * and [FakeDeliveryChallanDataSource]:
 *   - [Mutex] for all write and read-under-lock operations.
 *   - [MutableStateFlow] for reactive observation.
 *   - Deterministic and project-scoped — suitable for unit tests and development.
 *   - No inventory mutation — inventory boundary is preserved.
 */
class FakeReturnDataSource : ReturnDataSource {

    private val mutex = Mutex()

    // Keyed by returnId
    private val returnsFlow = MutableStateFlow<Map<String, ReturnRequest>>(emptyMap())

    // Keyed by returnItemId
    private val itemsFlow = MutableStateFlow<Map<String, ReturnItem>>(emptyMap())

    // List of activity events
    private val eventsFlow = MutableStateFlow<List<ReturnActivityEvent>>(emptyList())

    // Keyed by returnId
    private val inspectionsFlow = MutableStateFlow<Map<String, ReturnInspection>>(emptyMap())

    // Keyed by returnId
    private val receivingsFlow = MutableStateFlow<Map<String, ReturnReceivingInfo>>(emptyMap())

    // Keyed by returnId
    private val reconciliationFlow = MutableStateFlow<Map<String, ReturnReconciliationResult>>(emptyMap())

    // Keyed by returnId
    private val settlementsFlow = MutableStateFlow<Map<String, ReturnSettlement>>(emptyMap())

    // =========================================================================
    // Observation
    // =========================================================================

    override fun observeReturns(projectId: String): Flow<List<ReturnRequest>> =
        returnsFlow.map { map ->
            map.values
                .filter { it.projectId == projectId }
                .sortedByDescending { it.createdAt }
        }

    override fun observeReturn(returnId: String): Flow<ReturnRequest?> =
        returnsFlow.map { it[returnId] }

    override fun observeActivityEvents(returnId: String): Flow<List<ReturnActivityEvent>> =
        eventsFlow.map { list ->
            list.filter { it.returnId == returnId }.sortedBy { it.timestamp }
        }

    override fun observeInspection(returnId: String): Flow<ReturnInspection?> =
        inspectionsFlow.map { it[returnId] }

    override fun observeReceiving(returnId: String): Flow<ReturnReceivingInfo?> =
        receivingsFlow.map { it[returnId] }

    override fun observeReconciliationResult(returnId: String): Flow<ReturnReconciliationResult?> =
        reconciliationFlow.map { it[returnId] }

    // =========================================================================
    // Reads
    // =========================================================================

    override suspend fun getReturn(returnId: String): ReturnRequest? =
        mutex.withLock { returnsFlow.value[returnId] }

    override suspend fun getReturnsByProject(
        projectId: String,
        customerId: String?
    ): List<ReturnRequest> = mutex.withLock {
        returnsFlow.value.values
            .filter { it.projectId == projectId }
            .let { list ->
                if (customerId != null) list.filter { it.customerId == customerId } else list
            }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun getReturnItems(returnId: String): List<ReturnItem> =
        mutex.withLock {
            itemsFlow.value.values.filter { it.returnId == returnId }
        }

    override suspend fun getActivityEvents(returnId: String): List<ReturnActivityEvent> =
        mutex.withLock {
            eventsFlow.value.filter { it.returnId == returnId }.sortedBy { it.timestamp }
        }

    override suspend fun getInspection(returnId: String): ReturnInspection? =
        mutex.withLock {
            inspectionsFlow.value[returnId]
        }

    override suspend fun getReceiving(returnId: String): ReturnReceivingInfo? =
        mutex.withLock {
            receivingsFlow.value[returnId]
        }

    override suspend fun getReceivingByIdempotencyKey(idempotencyKey: String): ReturnReceivingInfo? =
        mutex.withLock {
            receivingsFlow.value.values.firstOrNull { it.idempotencyKey == idempotencyKey }
        }

    override suspend fun getReconciliationResult(returnId: String): ReturnReconciliationResult? =
        mutex.withLock {
            reconciliationFlow.value[returnId]
        }

    // =========================================================================
    // Writes
    // =========================================================================

    override suspend fun insertReturn(
        request: ReturnRequest,
        items: List<ReturnItem>
    ) = mutex.withLock {
        returnsFlow.update { current -> current + (request.returnId to request) }
        itemsFlow.update { current ->
            current + items.associateBy { it.returnItemId }
        }
    }

    override suspend fun updateReturn(request: ReturnRequest) = mutex.withLock {
        returnsFlow.update { current -> current + (request.returnId to request) }
    }

    override suspend fun updateReturnItem(item: ReturnItem) = mutex.withLock {
        itemsFlow.update { current -> current + (item.returnItemId to item) }
    }

    override suspend fun insertActivityEvent(event: ReturnActivityEvent) = mutex.withLock {
        eventsFlow.update { current -> current + event }
    }

    override suspend fun insertOrUpdateInspection(inspection: ReturnInspection) = mutex.withLock {
        inspectionsFlow.update { current -> current + (inspection.returnId to inspection) }
    }

    override suspend fun insertOrUpdateReceiving(receivingInfo: ReturnReceivingInfo) = mutex.withLock {
        receivingsFlow.update { current -> current + (receivingInfo.returnId to receivingInfo) }
    }

    override suspend fun insertOrUpdateReconciliationResult(result: ReturnReconciliationResult) = mutex.withLock {
        reconciliationFlow.update { current -> current + (result.returnId to result) }
    }

    // =========================================================================
    // Return Settlement (Module 11 Step 05)
    // =========================================================================

    override suspend fun getSettlement(returnId: String): ReturnSettlement? =
        mutex.withLock {
            settlementsFlow.value[returnId]
        }

    override suspend fun getSettlementByIdempotencyKey(idempotencyKey: String): ReturnSettlement? =
        mutex.withLock {
            settlementsFlow.value.values.firstOrNull { it.idempotencyKey == idempotencyKey }
        }

    override suspend fun insertOrUpdateSettlement(settlement: ReturnSettlement) = mutex.withLock {
        settlementsFlow.update { current -> current + (settlement.returnId to settlement) }
    }

    override fun observeSettlement(returnId: String): Flow<ReturnSettlement?> =
        settlementsFlow.map { it[returnId] }

    // =========================================================================
    // Test helpers (not part of the interface — visible only in tests/fakes)
    // =========================================================================

    /** Returns the total number of persisted ReturnRequests. */
    fun countReturns(): Int = returnsFlow.value.size

    /** Returns the total number of persisted ReturnItems. */
    fun countItems(): Int = itemsFlow.value.size

    /** Returns the total number of recorded activity events. */
    fun countActivityEvents(): Int = eventsFlow.value.size

    /** Returns the total number of persisted ReturnInspections. */
    fun countInspections(): Int = inspectionsFlow.value.size

    /** Returns the total number of persisted ReturnReceivings. */
    fun countReceivings(): Int = receivingsFlow.value.size

    /** Returns the total number of persisted ReturnReconciliations. */
    fun countReconciliations(): Int = reconciliationFlow.value.size

    /** Returns the total number of persisted ReturnSettlements. */
    fun countSettlements(): Int = settlementsFlow.value.size

    /** Clears all in-memory state — useful between tests. */
    fun reset() {
        returnsFlow.value = emptyMap()
        itemsFlow.value = emptyMap()
        eventsFlow.value = emptyList()
        inspectionsFlow.value = emptyMap()
        receivingsFlow.value = emptyMap()
        reconciliationFlow.value = emptyMap()
        settlementsFlow.value = emptyMap()
    }
}

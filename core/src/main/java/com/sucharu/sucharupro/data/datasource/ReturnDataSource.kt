package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.returns.ReturnActivityEvent
import com.sucharu.sucharupro.domain.model.returns.ReturnInspection
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReceivingInfo
import com.sucharu.sucharupro.domain.model.returns.ReturnReconciliationResult
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlement
import kotlinx.coroutines.flow.Flow

/**
 * Data Source contract for Customer Return Requests (Module 11 Step 01, 02, 03, 04).
 *
 * Raw persistence operations — no RBAC, no lifecycle validation.
 * All business rules are enforced by [ReturnRepository].
 *
 * Naming convention follows the existing DataSource pattern in this project
 * (e.g. DeliveryReturnDataSource, DeliveryChallanDataSource).
 */
interface ReturnDataSource {

    // =========================================================================
    // Observation
    // =========================================================================

    /** Emits a live list of all ReturnRequests for the given project. */
    fun observeReturns(projectId: String): Flow<List<ReturnRequest>>

    /** Emits a live view of a specific ReturnRequest, or null. */
    fun observeReturn(returnId: String): Flow<ReturnRequest?>

    // =========================================================================
    // Reads
    // =========================================================================

    /** Returns the ReturnRequest with [returnId], or null if not found. */
    suspend fun getReturn(returnId: String): ReturnRequest?

    /** Returns all ReturnRequests for [projectId], optionally filtered by [customerId]. */
    suspend fun getReturnsByProject(projectId: String, customerId: String? = null): List<ReturnRequest>

    /** Returns all ReturnItems for the given [returnId]. */
    suspend fun getReturnItems(returnId: String): List<ReturnItem>

    // =========================================================================
    // Writes
    // =========================================================================

    /** Persists a new ReturnRequest and its initial items atomically. */
    suspend fun insertReturn(request: ReturnRequest, items: List<ReturnItem>)

    /** Replaces an existing ReturnRequest record (used for status transitions). */
    suspend fun updateReturn(request: ReturnRequest)

    /** Replaces an existing ReturnItem record. */
    suspend fun updateReturnItem(item: ReturnItem)

    // =========================================================================
    // Audit Activity Events (Module 11 Step 02)
    // =========================================================================

    /** Persists an audit activity event. */
    suspend fun insertActivityEvent(event: ReturnActivityEvent)

    /** Returns all audit activity events for a given returnId in chronological order. */
    suspend fun getActivityEvents(returnId: String): List<ReturnActivityEvent>

    /** Emits a live view of activity events for a return request. */
    fun observeActivityEvents(returnId: String): Flow<List<ReturnActivityEvent>>

    // =========================================================================
    // Return Inspection (Module 11 Step 03)
    // =========================================================================

    /** Returns the ReturnInspection for the given [returnId], or null if not created yet. */
    suspend fun getInspection(returnId: String): ReturnInspection?

    /** Inserts or updates the ReturnInspection record. */
    suspend fun insertOrUpdateInspection(inspection: ReturnInspection)

    /** Emits a live view of the ReturnInspection for a given [returnId]. */
    fun observeInspection(returnId: String): Flow<ReturnInspection?>

    // =========================================================================
    // Return Receiving (Module 11 Step 04)
    // =========================================================================

    /** Returns the ReturnReceivingInfo for the given [returnId], or null if not received yet. */
    suspend fun getReceiving(returnId: String): ReturnReceivingInfo?

    /** Returns the ReturnReceivingInfo with matching [idempotencyKey], or null if not found. */
    suspend fun getReceivingByIdempotencyKey(idempotencyKey: String): ReturnReceivingInfo?

    /** Inserts or updates the ReturnReceivingInfo record. */
    suspend fun insertOrUpdateReceiving(receivingInfo: ReturnReceivingInfo)

    /** Emits a live view of the ReturnReceivingInfo for a given [returnId]. */
    fun observeReceiving(returnId: String): Flow<ReturnReceivingInfo?>

    // =========================================================================
    // Return Inventory Reconciliation (Module 11 Step 04 Chunk 04)
    // =========================================================================

    /** Returns the ReturnReconciliationResult for the given [returnId], or null if not reconciled yet. */
    suspend fun getReconciliationResult(returnId: String): ReturnReconciliationResult?

    /** Inserts or updates the ReturnReconciliationResult record. */
    suspend fun insertOrUpdateReconciliationResult(result: ReturnReconciliationResult)

    /** Emits a live view of the ReturnReconciliationResult for a given [returnId]. */
    fun observeReconciliationResult(returnId: String): Flow<ReturnReconciliationResult?>

    // =========================================================================
    // Return Settlement (Module 11 Step 05)
    // =========================================================================

    /** Returns the ReturnSettlement for the given [returnId], or null if not settled yet. */
    suspend fun getSettlement(returnId: String): ReturnSettlement?

    /** Returns the ReturnSettlement with matching [idempotencyKey], or null if not found. */
    suspend fun getSettlementByIdempotencyKey(idempotencyKey: String): ReturnSettlement?

    /** Inserts or updates the ReturnSettlement record. */
    suspend fun insertOrUpdateSettlement(settlement: ReturnSettlement)

    /** Emits a live view of the ReturnSettlement for a given [returnId]. */
    fun observeSettlement(returnId: String): Flow<ReturnSettlement?>
}

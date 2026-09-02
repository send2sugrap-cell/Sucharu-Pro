package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceiptVerification
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceiving
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLine
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Stock Receiving and Stock-In management (Module 07 Step 03).
 *
 * All mutation operations:
 *   1. Validate RBAC
 *   2. Validate project isolation
 *   3. Validate product, warehouse, and location existence and eligibility
 *   4. Validate quantity rules
 *   5. Validate lifecycle transitions
 *   6. Prevent duplicates (receiving reference, stock-in per line)
 *   7. Persist mutation atomically
 *   8. Emit immutable audit event
 *
 * Completing a receiving line and creating its StockInRecord is atomic:
 * there is no state where a line is COMPLETED without a StockInRecord (when acceptedQty > 0),
 * and no state where a StockInRecord exists for an incomplete line.
 */
interface InventoryReceivingRepository {

    // ──────────────────────────────────────────────────────────────
    // Receiving Queries
    // ──────────────────────────────────────────────────────────────

    /** Observe all receivings for a project, reactively. */
    fun observeReceivings(projectId: String): Flow<List<InventoryReceiving>>

    /** Observe a single receiving by ID, reactively. */
    fun observeReceiving(receivingId: String): Flow<InventoryReceiving?>

    /** Retrieve a receiving by ID (one-shot). */
    suspend fun getReceiving(
        receivingId: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryReceiving>

    // ──────────────────────────────────────────────────────────────
    // Receiving Line Queries
    // ──────────────────────────────────────────────────────────────

    /** Observe all lines for a receiving, reactively. */
    fun observeReceivingLines(receivingId: String): Flow<List<InventoryReceivingLine>>

    /** Observe a single line by ID, reactively. */
    fun observeReceivingLine(receivingLineId: String): Flow<InventoryReceivingLine?>

    /** Retrieve a receiving line by ID (one-shot). */
    suspend fun getReceivingLine(
        receivingLineId: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryReceivingLine>

    // ──────────────────────────────────────────────────────────────
    // Receiving Mutations
    // ──────────────────────────────────────────────────────────────

    /**
     * Creates a new receiving record in DRAFT status.
     * The receivingReference must be unique within the project.
     */
    suspend fun createReceiving(
        receiving: InventoryReceiving,
        callerRole: UserRole? = null
    ): DomainResult<InventoryReceiving>

    /**
     * Updates metadata of a receiving that is not yet in RECEIVING or terminal status.
     */
    suspend fun updateReceiving(
        receivingId: String,
        receivingReference: String,
        warehouseId: String,
        receivingDate: String,
        sourceReference: String?,
        sourceType: String?,
        expectedTotalQuantity: Int,
        notes: String?,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryReceiving>

    /**
     * Transitions a receiving from DRAFT to PENDING.
     */
    suspend fun submitReceiving(
        receivingId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryReceiving>

    /**
     * Transitions a receiving from PENDING to RECEIVING (active reception in progress).
     */
    suspend fun startReceiving(
        receivingId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryReceiving>

    /**
     * Cancels a receiving that is in DRAFT or PENDING status.
     * Terminal and active receivings cannot be cancelled.
     */
    suspend fun cancelReceiving(
        receivingId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryReceiving>

    /**
     * Completes a receiving operation.
     *
     * Completion rules:
     * - All lines must be finalized (accepted + rejected == received).
     * - For each finalized line with acceptedQuantity > 0: exactly one StockInRecord is created.
     * - This operation is atomic: either all StockInRecords are created and the receiving
     *   is marked COMPLETED, or neither mutation occurs.
     * - Idempotent: repeated calls return the existing COMPLETED result.
     */
    suspend fun completeReceiving(
        receivingId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryReceiving>

    // ──────────────────────────────────────────────────────────────
    // Receiving Line Mutations
    // ──────────────────────────────────────────────────────────────

    /**
     * Adds a new receiving line to an existing receiving.
     *
     * Validates:
     * - Receiving exists and is in DRAFT, PENDING, or RECEIVING status.
     * - Product exists and is active (stock-tracked).
     * - Warehouse matches receiving.warehouseId.
     * - Location exists, is active, and belongs to the receiving's warehouse and project.
     * - No duplicate line for same product + location in this receiving.
     */
    suspend fun addReceivingLine(
        line: InventoryReceivingLine,
        callerRole: UserRole? = null
    ): DomainResult<InventoryReceivingLine>

    /**
     * Updates the expected quantity and notes of a PENDING receiving line.
     * The line must not be in a terminal status.
     */
    suspend fun updateReceivingLine(
        receivingLineId: String,
        expectedQuantity: Int,
        notes: String?,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryReceivingLine>

    /**
     * Records the physically received quantity for a line.
     * The line must be in PENDING status.
     * Transitions the receiving to RECEIVING status if it is still in PENDING.
     */
    suspend fun recordReceivedQuantity(
        receivingLineId: String,
        receivedQuantity: Int,
        actorId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryReceivingLine>

    // ──────────────────────────────────────────────────────────────
    // Verification & Acceptance
    // ──────────────────────────────────────────────────────────────

    /**
     * Verifies a receiving line's received quantity and records the accepted/rejected split.
     *
     * Creates an [InventoryReceiptVerification] record and transitions the line to VERIFIED.
     * The verifier identity is preserved for audit and separation of duties.
     *
     * acceptedQuantity + rejectedQuantity must equal receivedQuantity.
     */
    suspend fun verifyReceivingLine(
        receivingLineId: String,
        verifiedBy: String,
        acceptedQuantity: Int,
        rejectedQuantity: Int,
        verificationNotes: String?,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryReceiptVerification>

    /**
     * Accepts the full verified quantity of a line, transitioning it to ACCEPTED.
     * The line must be in VERIFIED status.
     */
    suspend fun acceptLine(
        receivingLineId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryReceivingLine>

    /**
     * Rejects the full verified quantity of a line, transitioning it to REJECTED.
     * The line must be in VERIFIED status.
     * A rejectionReason is recommended but not mandatory.
     */
    suspend fun rejectLine(
        receivingLineId: String,
        rejectionReason: String?,
        actorId: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryReceivingLine>

    // ──────────────────────────────────────────────────────────────
    // Stock-In Queries
    // ──────────────────────────────────────────────────────────────

    /** Observe all stock-in records for a project, reactively. */
    fun observeStockInRecords(projectId: String): Flow<List<InventoryStockInRecord>>

    /** Observe stock-in records for a specific receiving, reactively. */
    fun observeStockInRecordsByReceiving(receivingId: String): Flow<List<InventoryStockInRecord>>

    /** Retrieve all stock-in records for a specific receiving (one-shot). */
    suspend fun getStockInRecords(
        receivingId: String,
        callerRole: UserRole? = null
    ): DomainResult<List<InventoryStockInRecord>>

    // ──────────────────────────────────────────────────────────────
    // Verification Queries
    // ──────────────────────────────────────────────────────────────

    /** Retrieve all verifications for a receiving (one-shot). */
    suspend fun getVerifications(
        receivingId: String,
        callerRole: UserRole? = null
    ): DomainResult<List<InventoryReceiptVerification>>

    // ──────────────────────────────────────────────────────────────
    // Audit Trail
    // ──────────────────────────────────────────────────────────────

    /** Observe all audit events for a project, reactively. */
    fun observeAuditEvents(projectId: String): Flow<List<InventoryReceivingActivityEvent>>

    /** Retrieve all audit events for a specific receiving (one-shot). */
    suspend fun getAuditHistory(
        receivingId: String,
        callerRole: UserRole? = null
    ): DomainResult<List<InventoryReceivingActivityEvent>>
}

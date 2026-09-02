package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnActivityEvent
import com.sucharu.sucharupro.domain.model.returns.ReturnInspection
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReceivingInfo
import com.sucharu.sucharupro.domain.model.returns.ReturnReconciliationResult
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlement
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Customer Return Requests (Module 11 Step 01, 02, 03, 04).
 *
 * Enforces at every boundary:
 *   - Project isolation: callerProjectId must match the target Return's projectId.
 *   - RBAC: callerRole must be authorized for the requested operation.
 *   - Lifecycle: status transitions are validated before persistence.
 *   - Validation: domain-level invariants are checked before write.
 *
 * DO NOT add inventory mutation here — that belongs to a future Step.
 */
interface ReturnRepository {

    // =========================================================================
    // Observation (reactive)
    // =========================================================================

    /** Emits the live list of Return Requests for [projectId], ordered by creation time desc. */
    fun observeReturns(projectId: String): Flow<List<ReturnRequest>>

    /** Emits the live state of a specific Return Request, or null if not found. */
    fun observeReturn(returnId: String): Flow<ReturnRequest?>

    // =========================================================================
    // Reads
    // =========================================================================

    /**
     * Retrieves a Return Request by ID, enforcing project isolation and RBAC.
     *
     * Returns [DomainResult.Error] if:
     *   - the Return does not exist
     *   - caller's project does not match the Return's project
     *   - caller's role is not authorized to view
     */
    suspend fun getReturn(
        returnId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnRequest>

    /**
     * Retrieves all items belonging to a Return Request.
     * Project isolation and RBAC apply via [callerProjectId] and [callerRole].
     */
    suspend fun getReturnItems(
        returnId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<ReturnItem>>

    /**
     * Lists all Return Requests for the given project, optionally filtering by [customerId].
     */
    suspend fun listReturns(
        projectId: String,
        customerId: String? = null,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<ReturnRequest>>

    // =========================================================================
    // Writes
    // =========================================================================

    /**
     * Creates a new Return Request with its items.
     *
     * Validates:
     *   - domain invariants on [request] and each item in [items]
     *   - RBAC: caller must have CREATE_RETURN permission
     *   - Project isolation: caller project must match request project
     *
     * Does NOT mutate inventory — that is reserved for a later Step.
     */
    suspend fun createReturn(
        request: ReturnRequest,
        items: List<ReturnItem>,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnRequest>

    /**
     * Transitions an existing Return Request to [targetStatus].
     *
     * Validates:
     *   - Return exists
     *   - Lifecycle transition is valid
     *   - RBAC: caller is authorized to perform the transition
     *   - Project isolation
     *   - Optimistic concurrency: [expectedVersion] must match the stored version
     *
     * Returns the updated [ReturnRequest] on success.
     */
    suspend fun transitionReturnStatus(
        returnId: String,
        targetStatus: ReturnStatus,
        actorId: String,
        expectedVersion: Long,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnRequest>

    // =========================================================================
    // Module 11 Step 02 Operations
    // =========================================================================

    /**
     * Updates permitted fields of an existing Return Request and its items while in REQUESTED status.
     * Enforces:
     *   - Return exists and is in REQUESTED status
     *   - Project isolation & RBAC (STAFF, ADMIN, MANAGER)
     *   - Optimistic concurrency (version check)
     *   - Field validation
     */
    suspend fun updateReturnRequest(
        request: ReturnRequest,
        items: List<ReturnItem>,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnRequest>

    /**
     * Cancels a Return Request in REQUESTED status.
     * Semantic convenience wrapper delegating to transitionReturnStatus(CANCELLED).
     */
    suspend fun cancelReturnRequest(
        returnId: String,
        actorId: String,
        expectedVersion: Long,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnRequest>

    /**
     * Submits a Return Request for inspection (transitions REQUESTED -> UNDER_INSPECTION).
     * Semantic convenience wrapper delegating to transitionReturnStatus(UNDER_INSPECTION).
     */
    suspend fun submitForInspection(
        returnId: String,
        actorId: String,
        expectedVersion: Long,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnRequest>

    /**
     * Lists returns scoped to a specific customer within a project.
     * Enforces project isolation and RBAC.
     */
    suspend fun listReturnsByCustomer(
        projectId: String,
        customerId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<ReturnRequest>>

    /**
     * Retrieves the audit activity trail for a Return Request.
     * Enforces project isolation and RBAC (VIEW_RETURN).
     */
    suspend fun getAuditHistory(
        returnId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<ReturnActivityEvent>>

    /**
     * Observes the live audit activity trail for a Return Request.
     */
    fun observeAuditHistory(
        returnId: String
    ): Flow<List<ReturnActivityEvent>>

    // =========================================================================
    // Module 11 Step 03 Operations – Return Inspection & Decision Management
    // =========================================================================

    /**
     * Retrieves the inspection performed on a Return Request.
     * Enforces project isolation and RBAC (VIEW_RETURN or INSPECT_RETURN).
     */
    suspend fun getInspection(
        returnId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnInspection?>

    /**
     * Observes the live state of a Return Inspection.
     */
    fun observeInspection(
        returnId: String
    ): Flow<ReturnInspection?>

    /**
     * Records or updates in-progress inspection details and findings on a Return in UNDER_INSPECTION.
     * Enforces:
     *   - Return exists and is in UNDER_INSPECTION status
     *   - Project isolation & RBAC (INSPECT_RETURN: QC_INSPECTOR, WAREHOUSE, ADMIN, MANAGER)
     *   - Domain validation on ReturnInspection
     */
    suspend fun recordInspection(
        inspection: ReturnInspection,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnInspection>

    /**
     * Approves a Return Request following inspection (transitions UNDER_INSPECTION -> APPROVED).
     * Enforces:
     *   - Return exists and is in UNDER_INSPECTION status
     *   - Optimistic concurrency (expectedVersion check)
     *   - RBAC (APPROVE_RETURN: ADMIN, MANAGER)
     *   - Project isolation
     *   - Optional inspection record association
     *   - Updates item accepted/rejected quantities if provided
     */
    suspend fun approveReturn(
        returnId: String,
        actorId: String,
        expectedVersion: Long,
        inspection: ReturnInspection? = null,
        items: List<ReturnItem>? = null,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnRequest>

    /**
     * Rejects a Return Request following inspection (transitions UNDER_INSPECTION -> REJECTED).
     * Enforces:
     *   - Return exists and is in UNDER_INSPECTION status
     *   - Rejection reason is non-blank
     *   - Optimistic concurrency (expectedVersion check)
     *   - RBAC (REJECT_RETURN: ADMIN, MANAGER)
     *   - Project isolation
     *   - Optional inspection record association
     *   - Updates item accepted/rejected quantities if provided
     */
    suspend fun rejectReturn(
        returnId: String,
        actorId: String,
        expectedVersion: Long,
        rejectionReason: String,
        inspection: ReturnInspection? = null,
        items: List<ReturnItem>? = null,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnRequest>

    /**
     * Updates accepted and rejected quantities for return items during/after inspection.
     * Enforces:
     *   - Return exists and is in UNDER_INSPECTION status
     *   - RBAC (INSPECT_RETURN or APPROVE_RETURN)
     *   - Quantity validation
     */
    suspend fun updateReturnItemQuantities(
        returnId: String,
        items: List<ReturnItem>,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<ReturnItem>>

    // =========================================================================
    // Module 11 Step 04 Operations – Return Receiving
    // =========================================================================

    /**
     * Retrieves the physical receiving record for a Return Request.
     * Enforces project isolation and RBAC (VIEW_RETURN or RECEIVE_RETURN).
     */
    suspend fun getReceiving(
        returnId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnReceivingInfo?>

    /**
     * Observes the live state of a Return Receiving record.
     */
    fun observeReceiving(
        returnId: String
    ): Flow<ReturnReceivingInfo?>

    /**
     * Records the physical receiving of an APPROVED Return Request.
     *
     * Transitions Return status: APPROVED -> RETURN_RECEIVED.
     *
     * Enforces:
     *   - Return exists and is in APPROVED status
     *   - Project isolation & RBAC (RECEIVE_RETURN: WAREHOUSE, ADMIN, MANAGER)
     *   - Customer ownership verification (if callerCustomerId provided)
     *   - Optimistic concurrency (expectedVersion check)
     *   - Full domain validation on [receivingInfo] via ReturnReceivingValidator
     *   - Idempotency guard via idempotencyKey
     *   - Append-only audit trail logging
     *
     * Does NOT mutate inventory — that belongs to Step 04 Chunk 04.
     */
    suspend fun receiveReturn(
        receivingInfo: ReturnReceivingInfo,
        actorId: String,
        expectedVersion: Long,
        callerCustomerId: String? = null,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnRequest>

    // =========================================================================
    // Module 11 Step 04 Chunk 04 – Inventory Reconciliation & Closeout
    // =========================================================================

    /**
     * Reconciles the accepted returned quantity into the canonical inventory system
     * and completes the return lifecycle transition: RETURN_RECEIVED -> PROCESSED.
     *
     * Validates:
     *   - Return exists and is in RETURN_RECEIVED status
     *   - ReturnReceivingInfo exists for this return
     *   - Project isolation (callerProjectId matches Return projectId)
     *   - Customer ownership (if callerCustomerId provided)
     *   - RBAC (PROCESS_RETURN: WAREHOUSE, ADMIN, MANAGER)
     *   - Optimistic concurrency (expectedVersion check)
     *   - warehouseId and locationId are non-blank if acceptedQty > 0
     *
     * Inventory & Ledger Mutation:
     *   - If acceptedQty > 0: creates and persists canonical [InventoryStockInRecord] with quantity = acceptedQty
     *     and builds/persists canonical [InventoryMovementLedgerEntry] via [InventoryMovementLedgerBuilder].
     *   - If acceptedQty == 0: no inventory mutation occurs.
     *
     * Lifecycle & Audit:
     *   - ReturnRequest is transitioned to PROCESSED status with incremented version.
     *   - Records an immutable audit activity event.
     *
     * Idempotency:
     *   - Safe replay if already reconciled and processed.
     */
    suspend fun reconcileInventoryAndProcess(
        returnId: String,
        warehouseId: String,
        locationId: String,
        actorId: String,
        expectedVersion: Long,
        callerCustomerId: String? = null,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnReconciliationResult>

    /**
     * Retrieves the reconciliation result for a Return Request.
     */
    suspend fun getReconciliationResult(
        returnId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnReconciliationResult?>

    /**
     * Observes the live state of a Return Reconciliation result.
     */
    fun observeReconciliationResult(
        returnId: String
    ): Flow<ReturnReconciliationResult?>

    // =========================================================================
    // Module 11 Step 05 Operations (Customer Return Settlement)
    // =========================================================================

    /**
     * Finalizes the commercial/financial resolution for a PROCESSED Return Request.
     *
     * Validates:
     *   - Return exists and is in PROCESSED status
     *   - Project isolation (callerProjectId matches return and settlement)
     *   - Customer ownership (if callerCustomerId provided, matches return.customerId)
     *   - RBAC: caller must be ADMIN, MANAGER, or ACCOUNTS
     *   - Optimistic concurrency: expectedVersion matches return's stored version
     *   - Idempotency: repeated execution with matching key returns existing settlement
     *
     * Invariants:
     *   - No duplicate inventory or financial mutations.
     *   - Version increments on successful settlement.
     *   - Appends audit activity event.
     */
    suspend fun settleReturn(
        settlement: ReturnSettlement,
        actorId: String,
        expectedVersion: Long,
        callerCustomerId: String? = null,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnSettlement>

    /**
     * Retrieves the settlement record for a Return Request.
     */
    suspend fun getSettlement(
        returnId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<ReturnSettlement?>

    /**
     * Observes the live state of a Return Settlement.
     */
    fun observeSettlement(
        returnId: String
    ): Flow<ReturnSettlement?>
}

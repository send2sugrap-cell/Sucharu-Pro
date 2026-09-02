package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecution
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionLine
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Dispatch Execution & Stock-Out Integration (Module 08 Step 03).
 */
interface DispatchExecutionRepository {

    // ──────────────────────────────────────────────────────────────
    // Dispatch Queries
    // ──────────────────────────────────────────────────────────────

    fun observeDispatches(projectId: String): Flow<List<DispatchExecution>>

    fun observeDispatchesForChallan(deliveryChallanId: String): Flow<List<DispatchExecution>>

    fun observeDispatch(dispatchExecutionId: String): Flow<DispatchExecution?>

    suspend fun getDispatch(
        dispatchExecutionId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DispatchExecution>

    suspend fun getDispatchesForChallan(
        deliveryChallanId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DispatchExecution>>

    // ──────────────────────────────────────────────────────────────
    // Line Queries
    // ──────────────────────────────────────────────────────────────

    fun observeDispatchLines(dispatchExecutionId: String): Flow<List<DispatchExecutionLine>>

    suspend fun getDispatchLines(
        dispatchExecutionId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DispatchExecutionLine>>

    // ──────────────────────────────────────────────────────────────
    // Activity Queries
    // ──────────────────────────────────────────────────────────────

    fun observeActivityEvents(dispatchExecutionId: String): Flow<List<DispatchExecutionActivityEvent>>

    suspend fun getActivityEvents(
        dispatchExecutionId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DispatchExecutionActivityEvent>>

    // ──────────────────────────────────────────────────────────────
    // Mutations
    // ──────────────────────────────────────────────────────────────

    suspend fun createDispatch(
        dispatch: DispatchExecution,
        lines: List<DispatchExecutionLine>,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DispatchExecution>

    suspend fun updateDraftDispatch(
        dispatchExecutionId: String,
        dispatchType: DispatchExecutionType,
        sourceWarehouseId: String,
        sourceLocationId: String,
        dispatchDate: Long,
        notes: String?,
        lines: List<DispatchExecutionLine>,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DispatchExecution>

    suspend fun submitDispatch(
        dispatchExecutionId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DispatchExecution>

    suspend fun approveDispatch(
        dispatchExecutionId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DispatchExecution>

    suspend fun markReadyForExecution(
        dispatchExecutionId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DispatchExecution>

    /**
     * Executes the physical dispatch atomically:
     * 1. Validates stock availability for each line at source location via [InventoryStockOutRepository].
     * 2. Creates and completes an [InventoryStockOut] movement.
     * 3. Links batch/lot traceability if specified.
     * 4. Updates [DeliveryChallan] status to DISPATCHED.
     * 5. Updates [DispatchExecution] status to DISPATCHED with linked stockOutId.
     * 6. Emits audit events.
     */
    suspend fun executeDispatch(
        dispatchExecutionId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DispatchExecution>

    suspend fun cancelDispatch(
        dispatchExecutionId: String,
        actorId: String,
        reason: String? = null,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DispatchExecution>
}

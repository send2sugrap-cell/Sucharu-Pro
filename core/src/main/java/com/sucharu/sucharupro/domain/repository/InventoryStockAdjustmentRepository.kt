package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustment
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentLine
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentRecord
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Stock Adjustment Management (Module 07 Step 06).
 */
interface InventoryStockAdjustmentRepository {

    // ──────────────────────────────────────────────────────────────
    // Stock Adjustment Queries
    // ──────────────────────────────────────────────────────────────

    fun observeStockAdjustments(projectId: String): Flow<List<InventoryStockAdjustment>>
    fun observeStockAdjustment(adjustmentId: String): Flow<InventoryStockAdjustment?>
    suspend fun getStockAdjustment(adjustmentId: String, callerRole: UserRole? = null): DomainResult<InventoryStockAdjustment>

    // ──────────────────────────────────────────────────────────────
    // Stock Adjustment Line Queries
    // ──────────────────────────────────────────────────────────────

    fun observeStockAdjustmentLines(adjustmentId: String): Flow<List<InventoryStockAdjustmentLine>>
    fun observeStockAdjustmentLine(adjustmentLineId: String): Flow<InventoryStockAdjustmentLine?>
    suspend fun getStockAdjustmentLine(adjustmentLineId: String, callerRole: UserRole? = null): DomainResult<InventoryStockAdjustmentLine>

    // ──────────────────────────────────────────────────────────────
    // Stock Calculation
    // ──────────────────────────────────────────────────────────────

    /**
     * Calculates available quantity for a product at a specific location.
     * Logic: StockIn - StockOut - TransfersOut + TransfersIn + AdjustmentsIn - AdjustmentsOut.
     */
    suspend fun getAvailableQuantity(
        projectId: String,
        warehouseId: String,
        locationId: String,
        productId: String
    ): Int

    // ──────────────────────────────────────────────────────────────
    // Stock Adjustment Mutations
    // ──────────────────────────────────────────────────────────────

    suspend fun createStockAdjustment(adjustment: InventoryStockAdjustment, callerRole: UserRole? = null): DomainResult<InventoryStockAdjustment>

    suspend fun updateStockAdjustment(
        adjustmentId: String,
        adjustmentReference: String,
        warehouseId: String,
        adjustmentDate: String,
        notes: String?,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryStockAdjustment>

    suspend fun submitStockAdjustment(adjustmentId: String, actorId: String, timestamp: String, callerRole: UserRole? = null): DomainResult<InventoryStockAdjustment>

    suspend fun approveStockAdjustment(adjustmentId: String, actorId: String, timestamp: String, callerRole: UserRole? = null): DomainResult<InventoryStockAdjustment>

    suspend fun startStockAdjustment(adjustmentId: String, actorId: String, timestamp: String, callerRole: UserRole? = null): DomainResult<InventoryStockAdjustment>

    suspend fun rejectStockAdjustment(adjustmentId: String, actorId: String, timestamp: String, callerRole: UserRole? = null): DomainResult<InventoryStockAdjustment>

    suspend fun cancelStockAdjustment(adjustmentId: String, actorId: String, timestamp: String, callerRole: UserRole? = null): DomainResult<InventoryStockAdjustment>

    /**
     * Completes a stock adjustment operation atomically.
     * Within one mutex lock:
     * 1. Validates sufficient stock for all DECREASE lines.
     * 2. Creates InventoryStockAdjustmentRecords (INCREASE -> ADJUSTMENT_IN, DECREASE -> ADJUSTMENT_OUT).
     * 3. Marks as COMPLETED.
     * 4. Emits audit events.
     */
    suspend fun completeStockAdjustment(adjustmentId: String, actorId: String, timestamp: String, callerRole: UserRole? = null): DomainResult<InventoryStockAdjustment>

    // ──────────────────────────────────────────────────────────────
    // Stock Adjustment Line Mutations
    // ──────────────────────────────────────────────────────────────

    suspend fun addStockAdjustmentLine(line: InventoryStockAdjustmentLine, callerRole: UserRole? = null): DomainResult<InventoryStockAdjustmentLine>

    suspend fun updateStockAdjustmentLine(
        adjustmentLineId: String,
        adjustedQuantity: Int,
        locationId: String,
        notes: String?,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryStockAdjustmentLine>

    suspend fun removeStockAdjustmentLine(adjustmentLineId: String, callerRole: UserRole? = null): DomainResult<Unit>

    // ──────────────────────────────────────────────────────────────
    // Stock Adjustment Records & Audit
    // ──────────────────────────────────────────────────────────────

    fun observeStockAdjustmentRecords(projectId: String): Flow<List<InventoryStockAdjustmentRecord>>
    fun observeAuditEvents(projectId: String): Flow<List<InventoryStockAdjustmentActivityEvent>>
    suspend fun getAuditHistory(adjustmentId: String, callerRole: UserRole? = null): DomainResult<List<InventoryStockAdjustmentActivityEvent>>
}

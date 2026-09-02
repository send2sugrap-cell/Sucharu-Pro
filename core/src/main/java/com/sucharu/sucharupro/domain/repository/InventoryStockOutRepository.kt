package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryIssueType
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOut
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutLine
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutRecord
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Stock Out & Issue Management (Module 07 Step 04).
 */
interface InventoryStockOutRepository {

    // ──────────────────────────────────────────────────────────────
    // Stock Out Queries
    // ──────────────────────────────────────────────────────────────

    fun observeStockOuts(projectId: String): Flow<List<InventoryStockOut>>
    fun observeStockOut(stockOutId: String): Flow<InventoryStockOut?>
    suspend fun getStockOut(stockOutId: String, callerRole: UserRole? = null): DomainResult<InventoryStockOut>

    // ──────────────────────────────────────────────────────────────
    // Stock Out Line Queries
    // ──────────────────────────────────────────────────────────────

    fun observeStockOutLines(stockOutId: String): Flow<List<InventoryStockOutLine>>
    fun observeStockOutLine(stockOutLineId: String): Flow<InventoryStockOutLine?>
    suspend fun getStockOutLine(stockOutLineId: String, callerRole: UserRole? = null): DomainResult<InventoryStockOutLine>

    // ──────────────────────────────────────────────────────────────
    // Stock Calculation
    // ──────────────────────────────────────────────────────────────

    /**
     * Calculates available quantity for a product at a specific location.
     * Logic: Sum(StockInRecord.quantity) - Sum(StockOutRecord.quantity).
     */
    suspend fun getAvailableQuantity(
        projectId: String,
        warehouseId: String,
        locationId: String,
        productId: String
    ): Int

    // ──────────────────────────────────────────────────────────────
    // Stock Out Mutations
    // ──────────────────────────────────────────────────────────────

    suspend fun createStockOut(stockOut: InventoryStockOut, callerRole: UserRole? = null): DomainResult<InventoryStockOut>

    suspend fun updateStockOut(
        stockOutId: String,
        stockOutReference: String,
        warehouseId: String,
        stockOutDate: String,
        issueType: InventoryIssueType,
        sourceReference: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryStockOut>

    suspend fun submitStockOut(stockOutId: String, actorId: String, timestamp: String, callerRole: UserRole? = null): DomainResult<InventoryStockOut>

    suspend fun approveStockOut(stockOutId: String, actorId: String, timestamp: String, callerRole: UserRole? = null): DomainResult<InventoryStockOut>

    suspend fun rejectStockOut(stockOutId: String, actorId: String, timestamp: String, callerRole: UserRole? = null): DomainResult<InventoryStockOut>

    suspend fun cancelStockOut(stockOutId: String, actorId: String, timestamp: String, callerRole: UserRole? = null): DomainResult<InventoryStockOut>

    /**
     * Completes a stock out operation atomically.
     * Within one mutex lock:
     * 1. Validates all lines have sufficient stock.
     * 2. Creates StockOutRecords.
     * 3. Marks as COMPLETED.
     * 4. Emits audit events.
     */
    suspend fun completeStockOut(stockOutId: String, actorId: String, timestamp: String, callerRole: UserRole? = null): DomainResult<InventoryStockOut>

    // ──────────────────────────────────────────────────────────────
    // Stock Out Line Mutations
    // ──────────────────────────────────────────────────────────────

    suspend fun addStockOutLine(line: InventoryStockOutLine, callerRole: UserRole? = null): DomainResult<InventoryStockOutLine>

    suspend fun updateStockOutLine(
        stockOutLineId: String,
        requestedQuantity: Int,
        notes: String?,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryStockOutLine>

    suspend fun removeStockOutLine(stockOutLineId: String, callerRole: UserRole? = null): DomainResult<Unit>

    // ──────────────────────────────────────────────────────────────
    // Stock Out Records & Audit
    // ──────────────────────────────────────────────────────────────

    fun observeStockOutRecords(projectId: String): Flow<List<InventoryStockOutRecord>>
    fun observeAuditEvents(projectId: String): Flow<List<InventoryStockOutActivityEvent>>
    suspend fun getAuditHistory(stockOutId: String, callerRole: UserRole? = null): DomainResult<List<InventoryStockOutActivityEvent>>
}

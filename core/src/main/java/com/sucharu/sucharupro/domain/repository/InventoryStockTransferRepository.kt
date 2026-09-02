package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransfer
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferLine
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferRecord
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Stock Transfer Management (Module 07 Step 05).
 */
interface InventoryStockTransferRepository {

    // ──────────────────────────────────────────────────────────────
    // Stock Transfer Queries
    // ──────────────────────────────────────────────────────────────

    fun observeStockTransfers(projectId: String): Flow<List<InventoryStockTransfer>>
    fun observeStockTransfer(transferId: String): Flow<InventoryStockTransfer?>
    suspend fun getStockTransfer(transferId: String, callerRole: UserRole? = null): DomainResult<InventoryStockTransfer>

    // ──────────────────────────────────────────────────────────────
    // Stock Transfer Line Queries
    // ──────────────────────────────────────────────────────────────

    fun observeStockTransferLines(transferId: String): Flow<List<InventoryStockTransferLine>>
    fun observeStockTransferLine(transferLineId: String): Flow<InventoryStockTransferLine?>
    suspend fun getStockTransferLine(transferLineId: String, callerRole: UserRole? = null): DomainResult<InventoryStockTransferLine>

    // ──────────────────────────────────────────────────────────────
    // Stock Calculation
    // ──────────────────────────────────────────────────────────────

    /**
     * Calculates available quantity for a product at a specific location.
     * Logic: Sum(StockInRecord) - Sum(StockOutRecord) - Sum(TransferRecord where source) + Sum(TransferRecord where destination).
     */
    suspend fun getAvailableQuantity(
        projectId: String,
        warehouseId: String,
        locationId: String,
        productId: String
    ): Int

    // ──────────────────────────────────────────────────────────────
    // Stock Transfer Mutations
    // ──────────────────────────────────────────────────────────────

    suspend fun createStockTransfer(transfer: InventoryStockTransfer, callerRole: UserRole? = null): DomainResult<InventoryStockTransfer>

    suspend fun updateStockTransfer(
        transferId: String,
        transferReference: String,
        fromWarehouseId: String,
        toWarehouseId: String,
        transferDate: String,
        notes: String?,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryStockTransfer>

    suspend fun submitStockTransfer(transferId: String, actorId: String, timestamp: String, callerRole: UserRole? = null): DomainResult<InventoryStockTransfer>

    suspend fun approveStockTransfer(transferId: String, actorId: String, timestamp: String, callerRole: UserRole? = null): DomainResult<InventoryStockTransfer>

    suspend fun startStockTransfer(transferId: String, actorId: String, timestamp: String, callerRole: UserRole? = null): DomainResult<InventoryStockTransfer>

    suspend fun rejectStockTransfer(transferId: String, actorId: String, timestamp: String, callerRole: UserRole? = null): DomainResult<InventoryStockTransfer>

    suspend fun cancelStockTransfer(transferId: String, actorId: String, timestamp: String, callerRole: UserRole? = null): DomainResult<InventoryStockTransfer>

    /**
     * Completes a stock transfer operation atomically.
     * Within one mutex lock:
     * 1. Validates all lines have sufficient stock at source.
     * 2. Creates StockTransferRecords.
     * 3. Marks as COMPLETED.
     * 4. Emits audit events.
     */
    suspend fun completeStockTransfer(transferId: String, actorId: String, timestamp: String, callerRole: UserRole? = null): DomainResult<InventoryStockTransfer>

    // ──────────────────────────────────────────────────────────────
    // Stock Transfer Line Mutations
    // ──────────────────────────────────────────────────────────────

    suspend fun addStockTransferLine(line: InventoryStockTransferLine, callerRole: UserRole? = null): DomainResult<InventoryStockTransferLine>

    suspend fun updateStockTransferLine(
        transferLineId: String,
        expectedQuantity: Int,
        fromLocationId: String,
        toLocationId: String,
        notes: String?,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<InventoryStockTransferLine>

    suspend fun removeStockTransferLine(transferLineId: String, callerRole: UserRole? = null): DomainResult<Unit>

    // ──────────────────────────────────────────────────────────────
    // Stock Transfer Records & Audit
    // ──────────────────────────────────────────────────────────────

    fun observeStockTransferRecords(projectId: String): Flow<List<InventoryStockTransferRecord>>
    fun observeAuditEvents(projectId: String): Flow<List<InventoryStockTransferActivityEvent>>
    suspend fun getAuditHistory(transferId: String, callerRole: UserRole? = null): DomainResult<List<InventoryStockTransferActivityEvent>>
}

package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.InventoryLocationDataSource
import com.sucharu.sucharupro.data.datasource.InventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.InventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.InventoryStockOutDataSource
import com.sucharu.sucharupro.data.datasource.InventoryWarehouseDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryIssueType
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOut
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutActivityType
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutLine
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutRecord
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryStockOutRepository
import com.sucharu.sucharupro.domain.validation.InventoryStockOutAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.InventoryStockOutLifecycleValidator
import com.sucharu.sucharupro.domain.validation.InventoryStockOutLineValidator
import com.sucharu.sucharupro.domain.validation.InventoryStockOutValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Thread-safe production-grade repository implementation for Stock Out & Issue Management
 * (Module 07 Step 04).
 */
class InventoryStockOutRepositoryImpl(
    private val stockOutDataSource: InventoryStockOutDataSource,
    private val receivingDataSource: InventoryReceivingDataSource,
    private val productDataSource: InventoryProductDataSource,
    private val warehouseDataSource: InventoryWarehouseDataSource,
    private val locationDataSource: InventoryLocationDataSource
) : InventoryStockOutRepository {

    private val repositoryMutex = Mutex()

    // ──────────────────────────────────────────────────────────────
    // Stock Out Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeStockOuts(projectId: String): Flow<List<InventoryStockOut>> {
        return stockOutDataSource.observeStockOuts().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override fun observeStockOut(stockOutId: String): Flow<InventoryStockOut?> {
        return stockOutDataSource.observeStockOuts().map { list ->
            list.find { it.stockOutId == stockOutId }
        }
    }

    override suspend fun getStockOut(
        stockOutId: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockOut> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryStockOutAuthorizationValidator.validateViewPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        val stockOut = stockOutDataSource.observeStockOuts().first()
            .find { it.stockOutId == stockOutId }
            ?: return DomainResult.Error(message = "Stock Out with ID '$stockOutId' not found.")
        DomainResult.Success(stockOut)
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Out Line Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeStockOutLines(stockOutId: String): Flow<List<InventoryStockOutLine>> {
        return stockOutDataSource.observeStockOutLines().map { list ->
            list.filter { it.stockOutId == stockOutId }
        }
    }

    override fun observeStockOutLine(stockOutLineId: String): Flow<InventoryStockOutLine?> {
        return stockOutDataSource.observeStockOutLines().map { list ->
            list.find { it.stockOutLineId == stockOutLineId }
        }
    }

    override suspend fun getStockOutLine(
        stockOutLineId: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockOutLine> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryStockOutAuthorizationValidator.validateViewPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        val line = stockOutDataSource.observeStockOutLines().first()
            .find { it.stockOutLineId == stockOutLineId }
            ?: return DomainResult.Error(message = "Stock Out Line with ID '$stockOutLineId' not found.")
        DomainResult.Success(line)
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Calculation
    // ──────────────────────────────────────────────────────────────

    override suspend fun getAvailableQuantity(
        projectId: String,
        warehouseId: String,
        locationId: String,
        productId: String
    ): Int {
        val stockInRecords = receivingDataSource.observeStockInRecords().first()
            .filter {
                it.projectId == projectId &&
                    it.warehouseId == warehouseId &&
                    it.locationId == locationId &&
                    it.inventoryProductId == productId
            }
        val stockOutRecords = stockOutDataSource.observeStockOutRecords().first()
            .filter {
                it.projectId == projectId &&
                    it.warehouseId == warehouseId &&
                    it.locationId == locationId &&
                    it.inventoryProductId == productId
            }

        val totalIn = stockInRecords.sumOf { it.quantity }
        val totalOut = stockOutRecords.sumOf { it.quantity }

        return (totalIn - totalOut).coerceAtLeast(0)
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Out Mutations
    // ──────────────────────────────────────────────────────────────

    override suspend fun createStockOut(
        stockOut: InventoryStockOut,
        callerRole: UserRole?
    ): DomainResult<InventoryStockOut> = repositoryMutex.withLock {
        val rbac = InventoryStockOutAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val structural = InventoryStockOutValidator.validateStockOut(stockOut)
        if (structural is DomainResult.Error) return structural

        val warehouses = warehouseDataSource.observeWarehouses().first()
        val warehouseResult = InventoryStockOutValidator.validateWarehouse(
            warehouseId = stockOut.warehouseId,
            projectId = stockOut.projectId,
            allWarehouses = warehouses
        )
        if (warehouseResult is DomainResult.Error) return warehouseResult

        val existingStockOuts = stockOutDataSource.observeStockOuts().first()
        val uniquenessResult = InventoryStockOutValidator.validateReferenceUniqueness(
            reference = stockOut.stockOutReference,
            stockOutId = stockOut.stockOutId,
            projectId = stockOut.projectId,
            existingStockOuts = existingStockOuts
        )
        if (uniquenessResult is DomainResult.Error) return uniquenessResult

        val insertResult = stockOutDataSource.insertStockOut(stockOut)
        if (insertResult is DomainResult.Success) {
            emitAudit(
                projectId = stockOut.projectId,
                stockOutId = stockOut.stockOutId,
                eventType = InventoryStockOutActivityType.STOCK_OUT_CREATED,
                actorId = stockOut.createdBy,
                description = "Created stock-out '${stockOut.stockOutReference}' for warehouse '${stockOut.warehouseId}'.",
                timestamp = stockOut.createdAt
            )
        }
        insertResult
    }

    override suspend fun updateStockOut(
        stockOutId: String,
        stockOutReference: String,
        warehouseId: String,
        stockOutDate: String,
        issueType: InventoryIssueType,
        sourceReference: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockOut> = repositoryMutex.withLock {
        val rbac = InventoryStockOutAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = stockOutDataSource.observeStockOuts().first()
            .find { it.stockOutId == stockOutId }
            ?: return DomainResult.Error(message = "Stock Out with ID '$stockOutId' not found.")

        if (current.isTerminal) {
            return DomainResult.Error(message = "Stock-out '$stockOutId' is in terminal state and cannot be updated.")
        }

        val warehouses = warehouseDataSource.observeWarehouses().first()
        val warehouseResult = InventoryStockOutValidator.validateWarehouse(
            warehouseId = warehouseId,
            projectId = current.projectId,
            allWarehouses = warehouses
        )
        if (warehouseResult is DomainResult.Error) return warehouseResult

        val existingStockOuts = stockOutDataSource.observeStockOuts().first()
        val uniquenessResult = InventoryStockOutValidator.validateReferenceUniqueness(
            reference = stockOutReference,
            stockOutId = stockOutId,
            projectId = current.projectId,
            existingStockOuts = existingStockOuts
        )
        if (uniquenessResult is DomainResult.Error) return uniquenessResult

        val updated = current.copy(
            stockOutReference = stockOutReference,
            warehouseId = warehouseId,
            stockOutDate = stockOutDate,
            issueType = issueType,
            sourceReference = sourceReference,
            notes = notes,
            updatedAt = timestamp
        )
        val updateResult = stockOutDataSource.updateStockOut(updated)
        if (updateResult is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                stockOutId = stockOutId,
                eventType = InventoryStockOutActivityType.STOCK_OUT_UPDATED,
                actorId = "SYSTEM",
                description = "Updated stock-out '${stockOutReference}' header.",
                timestamp = timestamp
            )
        }
        updateResult
    }

    override suspend fun submitStockOut(
        stockOutId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockOut> = repositoryMutex.withLock {
        val rbac = InventoryStockOutAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = stockOutDataSource.observeStockOuts().first()
            .find { it.stockOutId == stockOutId }
            ?: return DomainResult.Error(message = "Stock Out with ID '$stockOutId' not found.")

        val transition = InventoryStockOutLifecycleValidator.validateTransition(
            current = current.status,
            target = InventoryStockOutStatus.PENDING
        )
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(status = InventoryStockOutStatus.PENDING, updatedAt = timestamp)
        val result = stockOutDataSource.updateStockOut(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                stockOutId = stockOutId,
                eventType = InventoryStockOutActivityType.STOCK_OUT_UPDATED,
                actorId = actorId,
                description = "Stock-out '${current.stockOutReference}' submitted to PENDING.",
                timestamp = timestamp
            )
        }
        result
    }

    override suspend fun approveStockOut(
        stockOutId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockOut> = repositoryMutex.withLock {
        val rbac = InventoryStockOutAuthorizationValidator.validateApprovePermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = stockOutDataSource.observeStockOuts().first()
            .find { it.stockOutId == stockOutId }
            ?: return DomainResult.Error(message = "Stock Out with ID '$stockOutId' not found.")

        val transition = InventoryStockOutLifecycleValidator.validateTransition(
            current = current.status,
            target = InventoryStockOutStatus.ISSUING
        )
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(status = InventoryStockOutStatus.ISSUING, updatedAt = timestamp)
        val result = stockOutDataSource.updateStockOut(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                stockOutId = stockOutId,
                eventType = InventoryStockOutActivityType.ISSUING_STARTED,
                actorId = actorId,
                description = "Stock-out '${current.stockOutReference}' approved and moved to ISSUING.",
                timestamp = timestamp
            )
        }
        result
    }

    override suspend fun rejectStockOut(
        stockOutId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockOut> = repositoryMutex.withLock {
        // Rejecting often uses same permission as cancelling or approving depending on business rules.
        // Using cancel permission as it leads to a terminal state.
        val rbac = InventoryStockOutAuthorizationValidator.validateCancelPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = stockOutDataSource.observeStockOuts().first()
            .find { it.stockOutId == stockOutId }
            ?: return DomainResult.Error(message = "Stock Out with ID '$stockOutId' not found.")

        val transition = InventoryStockOutLifecycleValidator.validateTransition(
            current = current.status,
            target = InventoryStockOutStatus.CANCELLED // Using CANCELLED as terminal reject state for now
        )
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            status = InventoryStockOutStatus.CANCELLED,
            cancelledAt = timestamp,
            cancelledBy = actorId,
            updatedAt = timestamp
        )
        val result = stockOutDataSource.updateStockOut(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                stockOutId = stockOutId,
                eventType = InventoryStockOutActivityType.STOCK_OUT_CANCELLED,
                actorId = actorId,
                description = "Stock-out '${current.stockOutReference}' rejected/cancelled by '$actorId'.",
                timestamp = timestamp
            )
        }
        result
    }

    override suspend fun cancelStockOut(
        stockOutId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockOut> = repositoryMutex.withLock {
        val rbac = InventoryStockOutAuthorizationValidator.validateCancelPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = stockOutDataSource.observeStockOuts().first()
            .find { it.stockOutId == stockOutId }
            ?: return DomainResult.Error(message = "Stock Out with ID '$stockOutId' not found.")

        val transition = InventoryStockOutLifecycleValidator.validateTransition(
            current = current.status,
            target = InventoryStockOutStatus.CANCELLED
        )
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            status = InventoryStockOutStatus.CANCELLED,
            cancelledAt = timestamp,
            cancelledBy = actorId,
            updatedAt = timestamp
        )
        val result = stockOutDataSource.updateStockOut(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                stockOutId = stockOutId,
                eventType = InventoryStockOutActivityType.STOCK_OUT_CANCELLED,
                actorId = actorId,
                description = "Stock-out '${current.stockOutReference}' cancelled by '$actorId'.",
                timestamp = timestamp
            )
        }
        result
    }

    override suspend fun completeStockOut(
        stockOutId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockOut> = repositoryMutex.withLock {
        val rbac = InventoryStockOutAuthorizationValidator.validateCompletePermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = stockOutDataSource.observeStockOuts().first()
            .find { it.stockOutId == stockOutId }
            ?: return DomainResult.Error(message = "Stock Out with ID '$stockOutId' not found.")

        if (current.status == InventoryStockOutStatus.COMPLETED) return DomainResult.Success(current)

        val transition = InventoryStockOutLifecycleValidator.validateTransition(
            current = current.status,
            target = InventoryStockOutStatus.COMPLETED
        )
        if (transition is DomainResult.Error) return transition

        val lines = stockOutDataSource.observeStockOutLines().first()
            .filter { it.stockOutId == stockOutId }

        if (lines.isEmpty()) {
            return DomainResult.Error(message = "Stock Out '$stockOutId' has no lines.")
        }

        // Validate stock availability for all lines
        val products = productDataSource.observeProducts().first()
        for (line in lines) {
            val available = getAvailableQuantity(
                projectId = current.projectId,
                warehouseId = current.warehouseId,
                locationId = line.locationId,
                productId = line.inventoryProductId
            )
            val productName = products.find { it.id == line.inventoryProductId }?.name ?: "Unknown Product"
            val qtyCheck = InventoryStockOutLineValidator.validateQuantityAgainstAvailability(
                issuedQuantity = line.expectedQuantity, // Assuming we issue what was expected for completion
                availableQuantity = available,
                productName = productName
            )
            if (qtyCheck is DomainResult.Error) return qtyCheck
        }

        // Create StockOutRecords
        for (line in lines) {
            val record = InventoryStockOutRecord(
                stockOutRecordId = UUID.randomUUID().toString(),
                stockOutId = stockOutId,
                stockOutLineId = line.stockOutLineId,
                projectId = current.projectId,
                inventoryProductId = line.inventoryProductId,
                warehouseId = current.warehouseId,
                locationId = line.locationId,
                quantity = line.expectedQuantity,
                unit = line.unit,
                createdBy = actorId,
                createdAt = timestamp,
                sourceReference = current.stockOutReference
            )
            val recordResult = stockOutDataSource.insertStockOutRecord(record)
            if (recordResult is DomainResult.Error) {
                return DomainResult.Error(message = "Failed to create StockOutRecord for line '${line.stockOutLineId}': ${recordResult.message}")
            }
            emitAudit(
                projectId = current.projectId,
                stockOutId = stockOutId,
                stockOutLineId = line.stockOutLineId,
                eventType = InventoryStockOutActivityType.STOCK_OUT_RECORD_CREATED,
                actorId = actorId,
                description = "Stock-out record created for product '${line.inventoryProductId}': ${line.expectedQuantity} ${line.unit.defaultLabel}.",
                timestamp = timestamp
            )
        }

        val completed = current.copy(
            status = InventoryStockOutStatus.COMPLETED,
            completedAt = timestamp,
            completedBy = actorId,
            updatedAt = timestamp,
            issuedTotalQuantity = lines.sumOf { it.expectedQuantity }
        )
        val updateResult = stockOutDataSource.updateStockOut(completed)
        if (updateResult is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                stockOutId = stockOutId,
                eventType = InventoryStockOutActivityType.STOCK_OUT_COMPLETED,
                actorId = actorId,
                description = "Stock-out '${current.stockOutReference}' completed.",
                timestamp = timestamp
            )
        }
        updateResult
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Out Line Mutations
    // ──────────────────────────────────────────────────────────────

    override suspend fun addStockOutLine(
        line: InventoryStockOutLine,
        callerRole: UserRole?
    ): DomainResult<InventoryStockOutLine> = repositoryMutex.withLock {
        val rbac = InventoryStockOutAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val structural = InventoryStockOutLineValidator.validateLine(line)
        if (structural is DomainResult.Error) return structural

        val current = stockOutDataSource.observeStockOuts().first()
            .find { it.stockOutId == line.stockOutId }
            ?: return DomainResult.Error(message = "Stock Out with ID '${line.stockOutId}' not found.")

        if (current.isTerminal) {
            return DomainResult.Error(message = "Cannot add line to terminal stock-out.")
        }

        val products = productDataSource.observeProducts().first()
        val productResult = InventoryStockOutLineValidator.validateProduct(line.inventoryProductId, products)
        if (productResult is DomainResult.Error) return productResult

        val warehouses = warehouseDataSource.observeWarehouses().first()
        val warehouseResult = InventoryStockOutValidator.validateWarehouse(
            warehouseId = line.warehouseId,
            projectId = line.projectId,
            allWarehouses = warehouses
        )
        if (warehouseResult is DomainResult.Error) return warehouseResult

        val locations = locationDataSource.observeLocations().first()
        val locationResult = InventoryStockOutValidator.validateLocation(
            locationId = line.locationId,
            warehouseId = line.warehouseId,
            projectId = line.projectId,
            allLocations = locations
        )
        if (locationResult is DomainResult.Error) return locationResult

        val isolationResult = InventoryStockOutValidator.validateProjectIsolation(
            stockOutProjectId = current.projectId,
            warehouse = (warehouseResult as DomainResult.Success).data,
            location = (locationResult as DomainResult.Success).data
        )
        if (isolationResult is DomainResult.Error) return isolationResult

        val insertResult = stockOutDataSource.insertStockOutLine(line)
        if (insertResult is DomainResult.Success) {
            emitAudit(
                projectId = line.projectId,
                stockOutId = line.stockOutId,
                stockOutLineId = line.stockOutLineId,
                eventType = InventoryStockOutActivityType.LINE_ADDED,
                actorId = "SYSTEM",
                description = "Line added: product '${line.inventoryProductId}', location '${line.locationId}', expected: ${line.expectedQuantity} ${line.unit.defaultLabel}.",
                timestamp = line.createdAt
            )
        }
        insertResult
    }

    override suspend fun updateStockOutLine(
        stockOutLineId: String,
        requestedQuantity: Int,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockOutLine> = repositoryMutex.withLock {
        val rbac = InventoryStockOutAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = stockOutDataSource.observeStockOutLines().first()
            .find { it.stockOutLineId == stockOutLineId }
            ?: return DomainResult.Error(message = "Stock Out Line with ID '$stockOutLineId' not found.")

        val parent = stockOutDataSource.observeStockOuts().first()
            .find { it.stockOutId == current.stockOutId }
            ?: return DomainResult.Error(message = "Parent Stock Out not found.")

        if (parent.isTerminal) {
            return DomainResult.Error(message = "Cannot update line in terminal stock-out.")
        }

        val updated = current.copy(
            expectedQuantity = requestedQuantity,
            notes = notes,
            updatedAt = timestamp
        )
        val updateResult = stockOutDataSource.updateStockOutLine(updated)
        if (updateResult is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                stockOutId = current.stockOutId,
                stockOutLineId = stockOutLineId,
                eventType = InventoryStockOutActivityType.LINE_UPDATED,
                actorId = "SYSTEM",
                description = "Line updated: expectedQuantity=$requestedQuantity.",
                timestamp = timestamp
            )
        }
        updateResult
    }

    override suspend fun removeStockOutLine(
        stockOutLineId: String,
        callerRole: UserRole?
    ): DomainResult<Unit> = repositoryMutex.withLock {
        val rbac = InventoryStockOutAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = stockOutDataSource.observeStockOutLines().first()
            .find { it.stockOutLineId == stockOutLineId }
            ?: return DomainResult.Error(message = "Stock Out Line with ID '$stockOutLineId' not found.")

        val parent = stockOutDataSource.observeStockOuts().first()
            .find { it.stockOutId == current.stockOutId }
            ?: return DomainResult.Error(message = "Parent Stock Out not found.")

        if (parent.isTerminal) {
            return DomainResult.Error(message = "Cannot remove line from terminal stock-out.")
        }

        val deleteResult = stockOutDataSource.deleteStockOutLine(stockOutLineId)
        if (deleteResult is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                stockOutId = current.stockOutId,
                stockOutLineId = stockOutLineId,
                eventType = InventoryStockOutActivityType.STOCK_OUT_UPDATED,
                actorId = "SYSTEM",
                description = "Line removed: $stockOutLineId.",
                timestamp = "SYSTEM"
            )
        }
        deleteResult
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Out Records & Audit
    // ──────────────────────────────────────────────────────────────

    override fun observeStockOutRecords(projectId: String): Flow<List<InventoryStockOutRecord>> {
        return stockOutDataSource.observeStockOutRecords().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override fun observeAuditEvents(projectId: String): Flow<List<InventoryStockOutActivityEvent>> {
        return stockOutDataSource.observeAuditEvents().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override suspend fun getAuditHistory(
        stockOutId: String,
        callerRole: UserRole?
    ): DomainResult<List<InventoryStockOutActivityEvent>> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryStockOutAuthorizationValidator.validateViewPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        val events = stockOutDataSource.observeAuditEvents().first()
            .filter { it.stockOutId == stockOutId }
        DomainResult.Success(events)
    }

    // ──────────────────────────────────────────────────────────────
    // Internal Helpers
    // ──────────────────────────────────────────────────────────────

    private suspend fun emitAudit(
        projectId: String,
        stockOutId: String,
        stockOutLineId: String? = null,
        eventType: InventoryStockOutActivityType,
        actorId: String,
        description: String,
        timestamp: String,
        metadata: String? = null
    ) {
        val event = InventoryStockOutActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = projectId,
            stockOutId = stockOutId,
            stockOutLineId = stockOutLineId,
            eventType = eventType,
            actorId = actorId,
            description = description,
            timestamp = timestamp,
            metadata = metadata
        )
        stockOutDataSource.recordAuditEvent(event)
    }
}

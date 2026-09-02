package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.InventoryLocationDataSource
import com.sucharu.sucharupro.data.datasource.InventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.InventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.InventoryStockOutDataSource
import com.sucharu.sucharupro.data.datasource.InventoryStockTransferDataSource
import com.sucharu.sucharupro.data.datasource.InventoryWarehouseDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransfer
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferActivityType
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferLine
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferRecord
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryStockTransferRepository
import com.sucharu.sucharupro.domain.validation.InventoryStockTransferAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.InventoryStockTransferLifecycleValidator
import com.sucharu.sucharupro.domain.validation.InventoryStockTransferLineValidator
import com.sucharu.sucharupro.domain.validation.InventoryStockTransferValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Thread-safe production-grade repository implementation for Stock Transfer Management
 * (Module 07 Step 05).
 */
class InventoryStockTransferRepositoryImpl(
    private val transferDataSource: InventoryStockTransferDataSource,
    private val stockOutDataSource: InventoryStockOutDataSource,
    private val receivingDataSource: InventoryReceivingDataSource,
    private val productDataSource: InventoryProductDataSource,
    private val warehouseDataSource: InventoryWarehouseDataSource,
    private val locationDataSource: InventoryLocationDataSource
) : InventoryStockTransferRepository {

    private val repositoryMutex = Mutex()

    // ──────────────────────────────────────────────────────────────
    // Stock Transfer Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeStockTransfers(projectId: String): Flow<List<InventoryStockTransfer>> {
        return transferDataSource.observeStockTransfers().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override fun observeStockTransfer(transferId: String): Flow<InventoryStockTransfer?> {
        return transferDataSource.observeStockTransfers().map { list ->
            list.find { it.transferId == transferId }
        }
    }

    override suspend fun getStockTransfer(
        transferId: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockTransfer> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryStockTransferAuthorizationValidator.validateViewPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        val transfer = transferDataSource.observeStockTransfers().first()
            .find { it.transferId == transferId }
            ?: return DomainResult.Error(message = "Stock Transfer with ID '$transferId' not found.")
        DomainResult.Success(transfer)
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Transfer Line Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeStockTransferLines(transferId: String): Flow<List<InventoryStockTransferLine>> {
        return transferDataSource.observeStockTransferLines().map { list ->
            list.filter { it.transferId == transferId }
        }
    }

    override fun observeStockTransferLine(transferLineId: String): Flow<InventoryStockTransferLine?> {
        return transferDataSource.observeStockTransferLines().map { list ->
            list.find { it.transferLineId == transferLineId }
        }
    }

    override suspend fun getStockTransferLine(
        transferLineId: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockTransferLine> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryStockTransferAuthorizationValidator.validateViewPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        val line = transferDataSource.observeStockTransferLines().first()
            .find { it.transferLineId == transferLineId }
            ?: return DomainResult.Error(message = "Stock Transfer Line with ID '$transferLineId' not found.")
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
        val transferRecords = transferDataSource.observeStockTransferRecords().first()
            .filter { it.projectId == projectId && it.inventoryProductId == productId }

        val totalIn = stockInRecords.sumOf { it.quantity }
        val totalOut = stockOutRecords.sumOf { it.quantity }
        
        val transfersOut = transferRecords.filter { it.fromWarehouseId == warehouseId && it.fromLocationId == locationId }
            .sumOf { it.quantity }
        val transfersIn = transferRecords.filter { it.toWarehouseId == warehouseId && it.toLocationId == locationId }
            .sumOf { it.quantity }

        return (totalIn - totalOut - transfersOut + transfersIn).coerceAtLeast(0)
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Transfer Mutations
    // ──────────────────────────────────────────────────────────────

    override suspend fun createStockTransfer(
        transfer: InventoryStockTransfer,
        callerRole: UserRole?
    ): DomainResult<InventoryStockTransfer> = repositoryMutex.withLock {
        val rbac = InventoryStockTransferAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val structural = InventoryStockTransferValidator.validateTransfer(transfer)
        if (structural is DomainResult.Error) return structural

        val warehouses = warehouseDataSource.observeWarehouses().first()
        val fromWarehouseResult = InventoryStockTransferValidator.validateWarehouse(
            warehouseId = transfer.fromWarehouseId,
            projectId = transfer.projectId,
            allWarehouses = warehouses,
            role = "source"
        )
        if (fromWarehouseResult is DomainResult.Error) return fromWarehouseResult

        val toWarehouseResult = InventoryStockTransferValidator.validateWarehouse(
            warehouseId = transfer.toWarehouseId,
            projectId = transfer.projectId,
            allWarehouses = warehouses,
            role = "destination"
        )
        if (toWarehouseResult is DomainResult.Error) return toWarehouseResult

        val existingTransfers = transferDataSource.observeStockTransfers().first()
        val uniquenessResult = InventoryStockTransferValidator.validateReferenceUniqueness(
            reference = transfer.transferReference,
            transferId = transfer.transferId,
            projectId = transfer.projectId,
            existingTransfers = existingTransfers
        )
        if (uniquenessResult is DomainResult.Error) return uniquenessResult

        val insertResult = transferDataSource.insertStockTransfer(transfer)
        if (insertResult is DomainResult.Success) {
            emitAudit(
                projectId = transfer.projectId,
                transferId = transfer.transferId,
                eventType = InventoryStockTransferActivityType.CREATED,
                actorId = transfer.createdBy,
                description = "Created stock transfer '${transfer.transferReference}' from '${transfer.fromWarehouseId}' to '${transfer.toWarehouseId}'.",
                timestamp = transfer.createdAt
            )
        }
        insertResult
    }

    override suspend fun updateStockTransfer(
        transferId: String,
        transferReference: String,
        fromWarehouseId: String,
        toWarehouseId: String,
        transferDate: String,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockTransfer> = repositoryMutex.withLock {
        val rbac = InventoryStockTransferAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = transferDataSource.observeStockTransfers().first()
            .find { it.transferId == transferId }
            ?: return DomainResult.Error(message = "Stock Transfer with ID '$transferId' not found.")

        if (current.isTerminal) {
            return DomainResult.Error(message = "Stock transfer '$transferId' is in terminal state and cannot be updated.")
        }

        val warehouses = warehouseDataSource.observeWarehouses().first()
        val fromWarehouseResult = InventoryStockTransferValidator.validateWarehouse(
            warehouseId = fromWarehouseId,
            projectId = current.projectId,
            allWarehouses = warehouses,
            role = "source"
        )
        if (fromWarehouseResult is DomainResult.Error) return fromWarehouseResult

        val toWarehouseResult = InventoryStockTransferValidator.validateWarehouse(
            warehouseId = toWarehouseId,
            projectId = current.projectId,
            allWarehouses = warehouses,
            role = "destination"
        )
        if (toWarehouseResult is DomainResult.Error) return toWarehouseResult

        val existingTransfers = transferDataSource.observeStockTransfers().first()
        val uniquenessResult = InventoryStockTransferValidator.validateReferenceUniqueness(
            reference = transferReference,
            transferId = transferId,
            projectId = current.projectId,
            existingTransfers = existingTransfers
        )
        if (uniquenessResult is DomainResult.Error) return uniquenessResult

        val updated = current.copy(
            transferReference = transferReference,
            fromWarehouseId = fromWarehouseId,
            toWarehouseId = toWarehouseId,
            transferDate = transferDate,
            notes = notes,
            updatedAt = timestamp
        )
        val updateResult = transferDataSource.updateStockTransfer(updated)
        if (updateResult is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                transferId = transferId,
                eventType = InventoryStockTransferActivityType.CREATED, // Using CREATED or should we have UPDATED? Existing enum doesn't have UPDATED.
                actorId = "SYSTEM",
                description = "Updated stock transfer '${transferReference}' header.",
                timestamp = timestamp
            )
        }
        updateResult
    }

    override suspend fun submitStockTransfer(
        transferId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockTransfer> = repositoryMutex.withLock {
        val rbac = InventoryStockTransferAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = transferDataSource.observeStockTransfers().first()
            .find { it.transferId == transferId }
            ?: return DomainResult.Error(message = "Stock Transfer with ID '$transferId' not found.")

        val transition = InventoryStockTransferLifecycleValidator.validateTransition(
            current = current.status,
            target = InventoryStockTransferStatus.PENDING
        )
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(status = InventoryStockTransferStatus.PENDING, updatedAt = timestamp)
        val result = transferDataSource.updateStockTransfer(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                transferId = transferId,
                eventType = InventoryStockTransferActivityType.SUBMITTED,
                actorId = actorId,
                description = "Stock transfer '${current.transferReference}' submitted to PENDING.",
                timestamp = timestamp
            )
        }
        result
    }

    override suspend fun approveStockTransfer(
        transferId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockTransfer> = repositoryMutex.withLock {
        val rbac = InventoryStockTransferAuthorizationValidator.validateApprovePermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = transferDataSource.observeStockTransfers().first()
            .find { it.transferId == transferId }
            ?: return DomainResult.Error(message = "Stock Transfer with ID '$transferId' not found.")

        val transition = InventoryStockTransferLifecycleValidator.validateTransition(
            current = current.status,
            target = InventoryStockTransferStatus.APPROVED
        )
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            status = InventoryStockTransferStatus.APPROVED,
            approvedAt = timestamp,
            approvedBy = actorId,
            updatedAt = timestamp
        )
        val result = transferDataSource.updateStockTransfer(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                transferId = transferId,
                eventType = InventoryStockTransferActivityType.APPROVED,
                actorId = actorId,
                description = "Stock transfer '${current.transferReference}' approved and moved to APPROVED.",
                timestamp = timestamp
            )
        }
        result
    }

    override suspend fun startStockTransfer(
        transferId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockTransfer> = repositoryMutex.withLock {
        val rbac = InventoryStockTransferAuthorizationValidator.validateCreateEditPermission(callerRole) // Usually same as edit or a dedicated 'execute' role.
        if (rbac is DomainResult.Error) return rbac

        val current = transferDataSource.observeStockTransfers().first()
            .find { it.transferId == transferId }
            ?: return DomainResult.Error(message = "Stock Transfer with ID '$transferId' not found.")

        val transition = InventoryStockTransferLifecycleValidator.validateTransition(
            current = current.status,
            target = InventoryStockTransferStatus.TRANSFERRING
        )
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(status = InventoryStockTransferStatus.TRANSFERRING, updatedAt = timestamp)
        val result = transferDataSource.updateStockTransfer(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                transferId = transferId,
                eventType = InventoryStockTransferActivityType.STARTED,
                actorId = actorId,
                description = "Stock transfer '${current.transferReference}' started — now TRANSFERRING.",
                timestamp = timestamp
            )
        }
        result
    }

    override suspend fun rejectStockTransfer(
        transferId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockTransfer> = repositoryMutex.withLock {
        val rbac = InventoryStockTransferAuthorizationValidator.validateCancelPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = transferDataSource.observeStockTransfers().first()
            .find { it.transferId == transferId }
            ?: return DomainResult.Error(message = "Stock Transfer with ID '$transferId' not found.")

        val transition = InventoryStockTransferLifecycleValidator.validateTransition(
            current = current.status,
            target = InventoryStockTransferStatus.CANCELLED
        )
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            status = InventoryStockTransferStatus.CANCELLED,
            cancelledAt = timestamp,
            cancelledBy = actorId,
            updatedAt = timestamp
        )
        val result = transferDataSource.updateStockTransfer(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                transferId = transferId,
                eventType = InventoryStockTransferActivityType.CANCELLED,
                actorId = actorId,
                description = "Stock transfer '${current.transferReference}' rejected/cancelled by '$actorId'.",
                timestamp = timestamp
            )
        }
        result
    }

    override suspend fun cancelStockTransfer(
        transferId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockTransfer> = repositoryMutex.withLock {
        val rbac = InventoryStockTransferAuthorizationValidator.validateCancelPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = transferDataSource.observeStockTransfers().first()
            .find { it.transferId == transferId }
            ?: return DomainResult.Error(message = "Stock Transfer with ID '$transferId' not found.")

        val transition = InventoryStockTransferLifecycleValidator.validateTransition(
            current = current.status,
            target = InventoryStockTransferStatus.CANCELLED
        )
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            status = InventoryStockTransferStatus.CANCELLED,
            cancelledAt = timestamp,
            cancelledBy = actorId,
            updatedAt = timestamp
        )
        val result = transferDataSource.updateStockTransfer(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                transferId = transferId,
                eventType = InventoryStockTransferActivityType.CANCELLED,
                actorId = actorId,
                description = "Stock transfer '${current.transferReference}' cancelled by '$actorId'.",
                timestamp = timestamp
            )
        }
        result
    }

    override suspend fun completeStockTransfer(
        transferId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockTransfer> = repositoryMutex.withLock {
        val rbac = InventoryStockTransferAuthorizationValidator.validateCompletePermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = transferDataSource.observeStockTransfers().first()
            .find { it.transferId == transferId }
            ?: return DomainResult.Error(message = "Stock Transfer with ID '$transferId' not found.")

        if (current.status == InventoryStockTransferStatus.COMPLETED) return DomainResult.Success(current)

        val transition = InventoryStockTransferLifecycleValidator.validateTransition(
            current = current.status,
            target = InventoryStockTransferStatus.COMPLETED
        )
        if (transition is DomainResult.Error) return transition

        val lines = transferDataSource.observeStockTransferLines().first()
            .filter { it.transferId == transferId }

        if (lines.isEmpty()) {
            return DomainResult.Error(message = "Stock Transfer '$transferId' has no lines.")
        }

        // Validate stock availability for all lines at source location
        val products = productDataSource.observeProducts().first()
        for (line in lines) {
            val available = getAvailableQuantity(
                projectId = current.projectId,
                warehouseId = current.fromWarehouseId,
                locationId = line.fromLocationId,
                productId = line.inventoryProductId
            )
            val productName = products.find { it.id == line.inventoryProductId }?.name ?: "Unknown Product"
            val qtyCheck = InventoryStockTransferLineValidator.validateQuantityAgainstAvailability(
                requestedQuantity = line.expectedQuantity,
                availableQuantity = available,
                productName = productName
            )
            if (qtyCheck is DomainResult.Error) return qtyCheck
        }

        // Create StockTransferRecords and update lines
        for (line in lines) {
            val record = InventoryStockTransferRecord(
                transferRecordId = UUID.randomUUID().toString(),
                transferId = transferId,
                transferLineId = line.transferLineId,
                projectId = current.projectId,
                inventoryProductId = line.inventoryProductId,
                fromWarehouseId = current.fromWarehouseId,
                fromLocationId = line.fromLocationId,
                toWarehouseId = current.toWarehouseId,
                toLocationId = line.toLocationId,
                quantity = line.expectedQuantity,
                unit = line.unit,
                createdBy = actorId,
                createdAt = timestamp,
                sourceReference = current.transferReference
            )
            val recordResult = transferDataSource.insertStockTransferRecord(record)
            if (recordResult is DomainResult.Error) {
                return DomainResult.Error(message = "Failed to create StockTransferRecord for line '${line.transferLineId}': ${recordResult.message}")
            }

            // Update line with transferred quantity
            val updatedLine = line.copy(
                transferredQuantity = line.expectedQuantity,
                updatedAt = timestamp
            )
            transferDataSource.updateStockTransferLine(updatedLine)

            emitAudit(
                projectId = current.projectId,
                transferId = transferId,
                transferLineId = line.transferLineId,
                eventType = InventoryStockTransferActivityType.STARTED, // Using STARTED for record creation or maybe just COMPLETED later
                actorId = actorId,
                description = "Stock transfer record created for product '${line.inventoryProductId}': ${line.expectedQuantity} ${line.unit.defaultLabel}.",
                timestamp = timestamp
            )
        }

        val completed = current.copy(
            status = InventoryStockTransferStatus.COMPLETED,
            completedAt = timestamp,
            completedBy = actorId,
            updatedAt = timestamp,
            transferredTotalQuantity = lines.sumOf { it.expectedQuantity }
        )
        val updateResult = transferDataSource.updateStockTransfer(completed)
        if (updateResult is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                transferId = transferId,
                eventType = InventoryStockTransferActivityType.COMPLETED,
                actorId = actorId,
                description = "Stock transfer '${current.transferReference}' completed.",
                timestamp = timestamp
            )
        }
        updateResult
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Transfer Line Mutations
    // ──────────────────────────────────────────────────────────────

    override suspend fun addStockTransferLine(
        line: InventoryStockTransferLine,
        callerRole: UserRole?
    ): DomainResult<InventoryStockTransferLine> = repositoryMutex.withLock {
        val rbac = InventoryStockTransferAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val structural = InventoryStockTransferLineValidator.validateLine(line)
        if (structural is DomainResult.Error) return structural

        val current = transferDataSource.observeStockTransfers().first()
            .find { it.transferId == line.transferId }
            ?: return DomainResult.Error(message = "Stock Transfer with ID '${line.transferId}' not found.")

        if (current.isTerminal) {
            return DomainResult.Error(message = "Cannot add line to terminal stock transfer.")
        }

        val products = productDataSource.observeProducts().first()
        val productResult = InventoryStockTransferLineValidator.validateProduct(line.inventoryProductId, products)
        if (productResult is DomainResult.Error) return productResult

        val warehouses = warehouseDataSource.observeWarehouses().first()
        val fromWarehouseResult = InventoryStockTransferValidator.validateWarehouse(
            warehouseId = line.fromWarehouseId,
            projectId = line.projectId,
            allWarehouses = warehouses,
            role = "source"
        )
        if (fromWarehouseResult is DomainResult.Error) return fromWarehouseResult

        val toWarehouseResult = InventoryStockTransferValidator.validateWarehouse(
            warehouseId = line.toWarehouseId,
            projectId = line.projectId,
            allWarehouses = warehouses,
            role = "destination"
        )
        if (toWarehouseResult is DomainResult.Error) return toWarehouseResult

        val locations = locationDataSource.observeLocations().first()
        val fromLocationResult = InventoryStockTransferValidator.validateLocation(
            locationId = line.fromLocationId,
            warehouseId = line.fromWarehouseId,
            projectId = line.projectId,
            allLocations = locations,
            role = "source"
        )
        if (fromLocationResult is DomainResult.Error) return fromLocationResult

        val toLocationResult = InventoryStockTransferValidator.validateLocation(
            locationId = line.toLocationId,
            warehouseId = line.toWarehouseId,
            projectId = line.projectId,
            allLocations = locations,
            role = "destination"
        )
        if (toLocationResult is DomainResult.Error) return toLocationResult

        val isolationResult = InventoryStockTransferValidator.validateProjectIsolation(
            transferProjectId = current.projectId,
            fromWarehouse = (fromWarehouseResult as DomainResult.Success).data,
            fromLocation = (fromLocationResult as DomainResult.Success).data,
            toWarehouse = (toWarehouseResult as DomainResult.Success).data,
            toLocation = (toLocationResult as DomainResult.Success).data
        )
        if (isolationResult is DomainResult.Error) return isolationResult

        val insertResult = transferDataSource.insertStockTransferLine(line)
        if (insertResult is DomainResult.Success) {
            emitAudit(
                projectId = line.projectId,
                transferId = line.transferId,
                transferLineId = line.transferLineId,
                eventType = InventoryStockTransferActivityType.STARTED,
                actorId = "SYSTEM",
                description = "Line added: product '${line.inventoryProductId}', from '${line.fromLocationId}' to '${line.toLocationId}', expected: ${line.expectedQuantity} ${line.unit.defaultLabel}.",
                timestamp = line.createdAt
            )
        }
        insertResult
    }

    override suspend fun updateStockTransferLine(
        transferLineId: String,
        expectedQuantity: Int,
        fromLocationId: String,
        toLocationId: String,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockTransferLine> = repositoryMutex.withLock {
        val rbac = InventoryStockTransferAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = transferDataSource.observeStockTransferLines().first()
            .find { it.transferLineId == transferLineId }
            ?: return DomainResult.Error(message = "Stock Transfer Line with ID '$transferLineId' not found.")

        val parent = transferDataSource.observeStockTransfers().first()
            .find { it.transferId == current.transferId }
            ?: return DomainResult.Error(message = "Parent Stock Transfer not found.")

        if (parent.isTerminal) {
            return DomainResult.Error(message = "Cannot update line in terminal stock transfer.")
        }

        // Validate new locations if changed
        val locations = locationDataSource.observeLocations().first()
        if (fromLocationId != current.fromLocationId) {
            val fromLocResult = InventoryStockTransferValidator.validateLocation(
                locationId = fromLocationId,
                warehouseId = current.fromWarehouseId,
                projectId = current.projectId,
                allLocations = locations,
                role = "source"
            )
            if (fromLocResult is DomainResult.Error) return fromLocResult
        }
        if (toLocationId != current.toLocationId) {
            val toLocResult = InventoryStockTransferValidator.validateLocation(
                locationId = toLocationId,
                warehouseId = current.toWarehouseId,
                projectId = current.projectId,
                allLocations = locations,
                role = "destination"
            )
            if (toLocResult is DomainResult.Error) return toLocResult
        }

        val updated = current.copy(
            expectedQuantity = expectedQuantity,
            fromLocationId = fromLocationId,
            toLocationId = toLocationId,
            notes = notes,
            updatedAt = timestamp
        )
        val updateResult = transferDataSource.updateStockTransferLine(updated)
        if (updateResult is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                transferId = current.transferId,
                transferLineId = transferLineId,
                eventType = InventoryStockTransferActivityType.STARTED,
                actorId = "SYSTEM",
                description = "Line updated: expectedQuantity=$expectedQuantity, from=$fromLocationId, to=$toLocationId.",
                timestamp = timestamp
            )
        }
        updateResult
    }

    override suspend fun removeStockTransferLine(
        transferLineId: String,
        callerRole: UserRole?
    ): DomainResult<Unit> = repositoryMutex.withLock {
        val rbac = InventoryStockTransferAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = transferDataSource.observeStockTransferLines().first()
            .find { it.transferLineId == transferLineId }
            ?: return DomainResult.Error(message = "Stock Transfer Line with ID '$transferLineId' not found.")

        val parent = transferDataSource.observeStockTransfers().first()
            .find { it.transferId == current.transferId }
            ?: return DomainResult.Error(message = "Parent Stock Transfer not found.")

        if (parent.isTerminal) {
            return DomainResult.Error(message = "Cannot remove line from terminal stock transfer.")
        }

        val deleteResult = transferDataSource.deleteStockTransferLine(transferLineId)
        if (deleteResult is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                transferId = current.transferId,
                transferLineId = transferLineId,
                eventType = InventoryStockTransferActivityType.STARTED,
                actorId = "SYSTEM",
                description = "Line removed: $transferLineId.",
                timestamp = "SYSTEM"
            )
        }
        deleteResult
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Transfer Records & Audit
    // ──────────────────────────────────────────────────────────────

    override fun observeStockTransferRecords(projectId: String): Flow<List<InventoryStockTransferRecord>> {
        return transferDataSource.observeStockTransferRecords().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override fun observeAuditEvents(projectId: String): Flow<List<InventoryStockTransferActivityEvent>> {
        return transferDataSource.observeAuditEvents().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override suspend fun getAuditHistory(
        transferId: String,
        callerRole: UserRole?
    ): DomainResult<List<InventoryStockTransferActivityEvent>> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryStockTransferAuthorizationValidator.validateViewPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        val events = transferDataSource.observeAuditEvents().first()
            .filter { it.transferId == transferId }
        DomainResult.Success(events)
    }

    // ──────────────────────────────────────────────────────────────
    // Internal Helpers
    // ──────────────────────────────────────────────────────────────

    private suspend fun emitAudit(
        projectId: String,
        transferId: String,
        transferLineId: String? = null,
        eventType: InventoryStockTransferActivityType,
        actorId: String,
        description: String,
        timestamp: String,
        metadata: String? = null
    ) {
        val event = InventoryStockTransferActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = projectId,
            transferId = transferId,
            transferLineId = transferLineId,
            eventType = eventType,
            actorId = actorId,
            description = description,
            timestamp = timestamp,
            metadata = metadata
        )
        transferDataSource.recordAuditEvent(event)
    }
}

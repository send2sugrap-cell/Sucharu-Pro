package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.InventoryLocationDataSource
import com.sucharu.sucharupro.data.datasource.InventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.InventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.InventoryWarehouseDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceiptVerification
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceiving
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingActivityType
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLine
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLineStatus
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingStatus
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryReceivingRepository
import com.sucharu.sucharupro.domain.validation.InventoryReceivingAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.InventoryReceivingLifecycleValidator
import com.sucharu.sucharupro.domain.validation.InventoryReceivingLineValidator
import com.sucharu.sucharupro.domain.validation.InventoryReceivingValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Thread-safe production-grade repository implementation for Stock Receiving
 * and Stock-In Management (Module 07 Step 03).
 *
 * Concurrency model:
 * - A single [repositoryMutex] serializes all mutation operations.
 * - This ensures atomicity: completing a receiving and creating all StockInRecords
 *   either fully succeeds or fully fails within one lock acquisition.
 * - CASE A prevented: receiving cannot be COMPLETED without corresponding StockInRecords.
 * - CASE B prevented: StockInRecord cannot exist for an incomplete line.
 * - CASE C prevented: exactly one StockInRecord per finalized line.
 *
 * Cross-module integration (read-only):
 * - [InventoryProductDataSource] provides product existence/status checks.
 * - [InventoryWarehouseDataSource] provides warehouse existence/status/project checks.
 * - [InventoryLocationDataSource] provides location existence/status/warehouse/project checks.
 * - These data sources are NOT modified — zero changes to Step 01/02 modules.
 */
class InventoryReceivingRepositoryImpl(
    private val receivingDataSource: InventoryReceivingDataSource,
    private val productDataSource: InventoryProductDataSource,
    private val warehouseDataSource: InventoryWarehouseDataSource,
    private val locationDataSource: InventoryLocationDataSource
) : InventoryReceivingRepository {

    private val repositoryMutex = Mutex()

    // ──────────────────────────────────────────────────────────────
    // Receiving Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeReceivings(projectId: String): Flow<List<InventoryReceiving>> {
        return receivingDataSource.observeReceivings().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override fun observeReceiving(receivingId: String): Flow<InventoryReceiving?> {
        return receivingDataSource.observeReceivings().map { list ->
            list.find { it.receivingId == receivingId }
        }
    }

    override suspend fun getReceiving(
        receivingId: String,
        callerRole: UserRole?
    ): DomainResult<InventoryReceiving> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryReceivingAuthorizationValidator.validateViewPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        val receiving = receivingDataSource.observeReceivings().first()
            .find { it.receivingId == receivingId }
            ?: return DomainResult.Error(message = "Receiving with ID '$receivingId' not found.")
        DomainResult.Success(receiving)
    }

    // ──────────────────────────────────────────────────────────────
    // Receiving Line Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeReceivingLines(receivingId: String): Flow<List<InventoryReceivingLine>> {
        return receivingDataSource.observeReceivingLines().map { list ->
            list.filter { it.receivingId == receivingId }
        }
    }

    override fun observeReceivingLine(receivingLineId: String): Flow<InventoryReceivingLine?> {
        return receivingDataSource.observeReceivingLines().map { list ->
            list.find { it.receivingLineId == receivingLineId }
        }
    }

    override suspend fun getReceivingLine(
        receivingLineId: String,
        callerRole: UserRole?
    ): DomainResult<InventoryReceivingLine> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryReceivingAuthorizationValidator.validateViewPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        val line = receivingDataSource.observeReceivingLines().first()
            .find { it.receivingLineId == receivingLineId }
            ?: return DomainResult.Error(message = "Receiving line with ID '$receivingLineId' not found.")
        DomainResult.Success(line)
    }

    // ──────────────────────────────────────────────────────────────
    // Receiving Mutations
    // ──────────────────────────────────────────────────────────────

    override suspend fun createReceiving(
        receiving: InventoryReceiving,
        callerRole: UserRole?
    ): DomainResult<InventoryReceiving> = repositoryMutex.withLock {
        // 1. RBAC
        val rbac = InventoryReceivingAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        // 2. Structural validation
        val structural = InventoryReceivingValidator.validateReceiving(receiving)
        if (structural is DomainResult.Error) return structural

        // 3. Warehouse validation
        val warehouses = warehouseDataSource.observeWarehouses().first()
        val warehouseResult = InventoryReceivingValidator.validateWarehouse(
            warehouseId = receiving.warehouseId,
            projectId = receiving.projectId,
            allWarehouses = warehouses
        )
        if (warehouseResult is DomainResult.Error) return warehouseResult

        // 4. Reference uniqueness
        val existingReceivings = receivingDataSource.observeReceivings().first()
        val uniquenessResult = InventoryReceivingValidator.validateReceivingReferenceUniqueness(
            reference = receiving.receivingReference,
            receivingId = receiving.receivingId,
            projectId = receiving.projectId,
            existingReceivings = existingReceivings
        )
        if (uniquenessResult is DomainResult.Error) return uniquenessResult

        // 5. Persist
        val insertResult = receivingDataSource.insertReceiving(receiving)
        if (insertResult is DomainResult.Success) {
            emitAudit(
                projectId = receiving.projectId,
                receivingId = receiving.receivingId,
                eventType = InventoryReceivingActivityType.RECEIVING_CREATED,
                actorId = receiving.createdBy,
                description = "Created receiving '${receiving.receivingReference}' for warehouse '${receiving.warehouseId}'.",
                timestamp = receiving.createdAt
            )
        }
        insertResult
    }

    override suspend fun updateReceiving(
        receivingId: String,
        receivingReference: String,
        warehouseId: String,
        receivingDate: String,
        sourceReference: String?,
        sourceType: String?,
        expectedTotalQuantity: Int,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryReceiving> = repositoryMutex.withLock {
        // 1. RBAC
        val rbac = InventoryReceivingAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        // 2. Load existing
        val current = receivingDataSource.observeReceivings().first()
            .find { it.receivingId == receivingId }
            ?: return DomainResult.Error(message = "Receiving with ID '$receivingId' not found.")

        // 3. Terminal guard
        if (current.isTerminal) {
            return DomainResult.Error(message = "Receiving '${receivingId}' is in terminal state '${current.status.defaultLabel}' and cannot be updated.")
        }
        if (current.status == InventoryReceivingStatus.RECEIVING) {
            return DomainResult.Error(message = "Receiving '$receivingId' is actively being received and its header cannot be edited. Complete or cancel first.")
        }

        // 4. Reference uniqueness
        val existingReceivings = receivingDataSource.observeReceivings().first()
        val uniquenessResult = InventoryReceivingValidator.validateReceivingReferenceUniqueness(
            reference = receivingReference,
            receivingId = receivingId,
            projectId = current.projectId,
            existingReceivings = existingReceivings
        )
        if (uniquenessResult is DomainResult.Error) return uniquenessResult

        // 5. Warehouse validation
        val warehouses = warehouseDataSource.observeWarehouses().first()
        val warehouseResult = InventoryReceivingValidator.validateWarehouse(
            warehouseId = warehouseId,
            projectId = current.projectId,
            allWarehouses = warehouses
        )
        if (warehouseResult is DomainResult.Error) return warehouseResult

        val updated = current.copy(
            receivingReference = receivingReference,
            warehouseId = warehouseId,
            receivingDate = receivingDate,
            sourceReference = sourceReference,
            sourceType = sourceType,
            expectedTotalQuantity = expectedTotalQuantity,
            notes = notes,
            updatedAt = timestamp
        )
        val updateResult = receivingDataSource.updateReceiving(updated)
        if (updateResult is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                receivingId = receivingId,
                eventType = InventoryReceivingActivityType.RECEIVING_UPDATED,
                actorId = "SYSTEM",
                description = "Updated receiving '${receivingReference}' header.",
                timestamp = timestamp
            )
        }
        updateResult
    }

    override suspend fun submitReceiving(
        receivingId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryReceiving> = repositoryMutex.withLock {
        val rbac = InventoryReceivingAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = receivingDataSource.observeReceivings().first()
            .find { it.receivingId == receivingId }
            ?: return DomainResult.Error(message = "Receiving with ID '$receivingId' not found.")

        val transition = InventoryReceivingLifecycleValidator.validateReceivingTransition(
            current = current.status,
            target = InventoryReceivingStatus.PENDING
        )
        if (transition is DomainResult.Error) return transition

        if (current.status == InventoryReceivingStatus.PENDING) return DomainResult.Success(current)

        val updated = current.copy(status = InventoryReceivingStatus.PENDING, updatedAt = timestamp)
        val result = receivingDataSource.updateReceiving(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                receivingId = receivingId,
                eventType = InventoryReceivingActivityType.RECEIVING_UPDATED,
                actorId = actorId,
                description = "Receiving '${current.receivingReference}' submitted to PENDING.",
                timestamp = timestamp
            )
        }
        result
    }

    override suspend fun startReceiving(
        receivingId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryReceiving> = repositoryMutex.withLock {
        val rbac = InventoryReceivingAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = receivingDataSource.observeReceivings().first()
            .find { it.receivingId == receivingId }
            ?: return DomainResult.Error(message = "Receiving with ID '$receivingId' not found.")

        val transition = InventoryReceivingLifecycleValidator.validateReceivingTransition(
            current = current.status,
            target = InventoryReceivingStatus.RECEIVING
        )
        if (transition is DomainResult.Error) return transition

        if (current.status == InventoryReceivingStatus.RECEIVING) return DomainResult.Success(current)

        val updated = current.copy(status = InventoryReceivingStatus.RECEIVING, updatedAt = timestamp)
        val result = receivingDataSource.updateReceiving(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                receivingId = receivingId,
                eventType = InventoryReceivingActivityType.RECEIVING_STARTED,
                actorId = actorId,
                description = "Receiving '${current.receivingReference}' started — now actively receiving.",
                timestamp = timestamp
            )
        }
        result
    }

    override suspend fun cancelReceiving(
        receivingId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryReceiving> = repositoryMutex.withLock {
        val rbac = InventoryReceivingAuthorizationValidator.validateCancelPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = receivingDataSource.observeReceivings().first()
            .find { it.receivingId == receivingId }
            ?: return DomainResult.Error(message = "Receiving with ID '$receivingId' not found.")

        val transition = InventoryReceivingLifecycleValidator.validateReceivingTransition(
            current = current.status,
            target = InventoryReceivingStatus.CANCELLED
        )
        if (transition is DomainResult.Error) return transition

        if (current.status == InventoryReceivingStatus.CANCELLED) return DomainResult.Success(current)

        val updated = current.copy(
            status = InventoryReceivingStatus.CANCELLED,
            cancelledAt = timestamp,
            cancelledBy = actorId,
            updatedAt = timestamp
        )
        val result = receivingDataSource.updateReceiving(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                receivingId = receivingId,
                eventType = InventoryReceivingActivityType.RECEIVING_CANCELLED,
                actorId = actorId,
                description = "Receiving '${current.receivingReference}' cancelled by '$actorId'.",
                timestamp = timestamp
            )
        }
        result
    }

    /**
     * Completes a receiving operation atomically.
     *
     * Within one mutex lock:
     *   1. Validates all lines are finalized.
     *   2. Determines aggregate completion status.
     *   3. Creates exactly one StockInRecord per line with acceptedQuantity > 0.
     *   4. Updates the receiving to COMPLETED.
     *   5. Emits audit events for each stock-in and the completion.
     *
     * Idempotent: if already COMPLETED, returns success without duplication.
     */
    override suspend fun completeReceiving(
        receivingId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryReceiving> = repositoryMutex.withLock {
        val rbac = InventoryReceivingAuthorizationValidator.validateCompletePermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = receivingDataSource.observeReceivings().first()
            .find { it.receivingId == receivingId }
            ?: return DomainResult.Error(message = "Receiving with ID '$receivingId' not found.")

        // Idempotency: already completed
        if (current.status == InventoryReceivingStatus.COMPLETED) {
            return DomainResult.Success(current)
        }

        // Terminal guard (CANCELLED, REJECTED)
        if (current.isTerminal) {
            return DomainResult.Error(
                message = "Receiving '${receivingId}' is in terminal state '${current.status.defaultLabel}' and cannot be completed."
            )
        }

        // Must be in a state where completion is allowed
        val lines = receivingDataSource.observeReceivingLines().first()
            .filter { it.receivingId == receivingId }

        if (lines.isEmpty()) {
            return DomainResult.Error(message = "Receiving '$receivingId' has no lines. At least one line is required to complete.")
        }

        // All lines must be finalized
        val nonFinalizedLines = lines.filter { !it.isFullyFinalized }
        if (nonFinalizedLines.isNotEmpty()) {
            return DomainResult.Error(
                message = "Receiving '$receivingId' has ${nonFinalizedLines.size} non-finalized line(s). " +
                    "All lines must be fully verified and reconciled before completion."
            )
        }

        // Collect existing stock-in records for this receiving (idempotency check per line)
        val existingStockIns = receivingDataSource.observeStockInRecords().first()
            .filter { it.receivingId == receivingId }
        val stockedLineIds = existingStockIns.map { it.receivingLineId }.toSet()

        // Compute aggregate totals
        val acceptedTotal = lines.sumOf { it.acceptedQuantity }
        val rejectedTotal = lines.sumOf { it.rejectedQuantity }
        val acceptedLineCount = lines.count { it.acceptedQuantity > 0 }
        val rejectedLineCount = lines.count { it.acceptedQuantity == 0 && it.rejectedQuantity > 0 }

        // Derive final receiving status
        val finalStatus = InventoryReceivingLifecycleValidator.deriveReceivingCompletionStatus(
            totalLines = lines.size,
            acceptedLines = acceptedLineCount,
            rejectedLines = rejectedLineCount
        )

        // Atomically create stock-in records for all lines with accepted quantity > 0
        for (line in lines) {
            if (line.acceptedQuantity > 0 && line.receivingLineId !in stockedLineIds) {
                val stockInRecord = InventoryStockInRecord(
                    stockInId = UUID.randomUUID().toString(),
                    receivingId = receivingId,
                    receivingLineId = line.receivingLineId,
                    projectId = current.projectId,
                    inventoryProductId = line.inventoryProductId,
                    warehouseId = line.warehouseId,
                    locationId = line.locationId,
                    quantity = line.acceptedQuantity,
                    unit = line.unit,
                    createdBy = actorId,
                    createdAt = timestamp,
                    sourceReference = current.receivingReference
                )
                val stockInResult = receivingDataSource.insertStockInRecord(stockInRecord)
                if (stockInResult is DomainResult.Error) {
                    return DomainResult.Error(
                        message = "Failed to create stock-in record for line '${line.receivingLineId}': ${stockInResult.message}"
                    )
                }
                emitAudit(
                    projectId = current.projectId,
                    receivingId = receivingId,
                    receivingLineId = line.receivingLineId,
                    eventType = InventoryReceivingActivityType.STOCK_IN_CREATED,
                    actorId = actorId,
                    description = "Stock-in record created: ${line.acceptedQuantity} ${line.unit.defaultLabel} of product '${line.inventoryProductId}' → location '${line.locationId}'.",
                    timestamp = timestamp
                )
            }
        }

        // Mark receiving as COMPLETED
        val completed = current.copy(
            status = InventoryReceivingStatus.COMPLETED,
            acceptedTotalQuantity = acceptedTotal,
            rejectedTotalQuantity = rejectedTotal,
            completedAt = timestamp,
            completedBy = actorId,
            updatedAt = timestamp
        )
        val updateResult = receivingDataSource.updateReceiving(completed)
        if (updateResult is DomainResult.Error) return updateResult

        emitAudit(
            projectId = current.projectId,
            receivingId = receivingId,
            eventType = InventoryReceivingActivityType.RECEIVING_COMPLETED,
            actorId = actorId,
            description = "Receiving '${current.receivingReference}' completed. " +
                "Accepted: $acceptedTotal, Rejected: $rejectedTotal. Status: ${finalStatus.defaultLabel}.",
            timestamp = timestamp
        )

        DomainResult.Success(completed)
    }

    // ──────────────────────────────────────────────────────────────
    // Receiving Line Mutations
    // ──────────────────────────────────────────────────────────────

    override suspend fun addReceivingLine(
        line: InventoryReceivingLine,
        callerRole: UserRole?
    ): DomainResult<InventoryReceivingLine> = repositoryMutex.withLock {
        // 1. RBAC
        val rbac = InventoryReceivingAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        // 2. Structural validation
        val structural = InventoryReceivingLineValidator.validateLine(line)
        if (structural is DomainResult.Error) return structural

        // 3. Receiving must exist and not be terminal or COMPLETED
        val receiving = receivingDataSource.observeReceivings().first()
            .find { it.receivingId == line.receivingId }
            ?: return DomainResult.Error(message = "Receiving with ID '${line.receivingId}' not found.")
        if (receiving.isTerminal) {
            return DomainResult.Error(message = "Cannot add line to receiving '${line.receivingId}' in terminal state '${receiving.status.defaultLabel}'.")
        }

        // 4. Validate product
        val products = productDataSource.observeProducts().first()
        val productResult = InventoryReceivingValidator.validateProduct(line.inventoryProductId, products)
        if (productResult is DomainResult.Error) return productResult

        // 5. Validate warehouse
        val warehouses = warehouseDataSource.observeWarehouses().first()
        val warehouseResult = InventoryReceivingValidator.validateWarehouse(
            warehouseId = line.warehouseId,
            projectId = line.projectId,
            allWarehouses = warehouses
        )
        if (warehouseResult is DomainResult.Error) return warehouseResult

        // 6. Warehouse must match receiving's warehouse
        if (line.warehouseId != receiving.warehouseId) {
            return DomainResult.Error(
                message = "Line warehouseId '${line.warehouseId}' does not match receiving warehouseId '${receiving.warehouseId}'."
            )
        }

        // 7. Validate location (existence, project, warehouse)
        val locations = locationDataSource.observeLocations().first()
        val locationResult = InventoryReceivingValidator.validateLocation(
            locationId = line.locationId,
            warehouseId = line.warehouseId,
            projectId = line.projectId,
            allLocations = locations
        )
        if (locationResult is DomainResult.Error) return locationResult

        // 8. Project isolation: line projectId == receiving projectId == warehouse projectId == location projectId
        if (line.projectId != receiving.projectId) {
            return DomainResult.Error(
                message = "Line projectId '${line.projectId}' does not match receiving projectId '${receiving.projectId}'."
            )
        }
        val isolationResult = InventoryReceivingValidator.validateProjectIsolation(
            receivingProjectId = line.projectId,
            warehouse = (warehouseResult as DomainResult.Success).data,
            location = (locationResult as DomainResult.Success).data
        )
        if (isolationResult is DomainResult.Error) return isolationResult

        // 9. No duplicate line for same product + location in this receiving
        val existingLines = receivingDataSource.observeReceivingLines().first()
        val duplicateCheck = InventoryReceivingLineValidator.validateNoDuplicateLine(
            receivingId = line.receivingId,
            inventoryProductId = line.inventoryProductId,
            locationId = line.locationId,
            existingLines = existingLines
        )
        if (duplicateCheck is DomainResult.Error) return duplicateCheck

        // 10. Persist
        val insertResult = receivingDataSource.insertReceivingLine(line)
        if (insertResult is DomainResult.Success) {
            emitAudit(
                projectId = line.projectId,
                receivingId = line.receivingId,
                receivingLineId = line.receivingLineId,
                eventType = InventoryReceivingActivityType.LINE_ADDED,
                actorId = "SYSTEM",
                description = "Line added: product '${line.inventoryProductId}', location '${line.locationId}', expected: ${line.expectedQuantity} ${line.unit.defaultLabel}.",
                timestamp = line.createdAt
            )
        }
        insertResult
    }

    override suspend fun updateReceivingLine(
        receivingLineId: String,
        expectedQuantity: Int,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryReceivingLine> = repositoryMutex.withLock {
        val rbac = InventoryReceivingAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val line = receivingDataSource.observeReceivingLines().first()
            .find { it.receivingLineId == receivingLineId }
            ?: return DomainResult.Error(message = "Receiving line with ID '$receivingLineId' not found.")

        if (line.lineStatus.isTerminal) {
            return DomainResult.Error(message = "Receiving line '$receivingLineId' is in terminal status '${line.lineStatus.defaultLabel}' and cannot be updated.")
        }
        if (expectedQuantity < 0) {
            return DomainResult.Error(message = "expectedQuantity cannot be negative.")
        }

        val updated = line.copy(expectedQuantity = expectedQuantity, notes = notes, updatedAt = timestamp)
        val result = receivingDataSource.updateReceivingLine(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = line.projectId,
                receivingId = line.receivingId,
                receivingLineId = receivingLineId,
                eventType = InventoryReceivingActivityType.LINE_UPDATED,
                actorId = "SYSTEM",
                description = "Line '$receivingLineId' updated: expectedQuantity=$expectedQuantity.",
                timestamp = timestamp
            )
        }
        result
    }

    override suspend fun recordReceivedQuantity(
        receivingLineId: String,
        receivedQuantity: Int,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryReceivingLine> = repositoryMutex.withLock {
        val rbac = InventoryReceivingAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val line = receivingDataSource.observeReceivingLines().first()
            .find { it.receivingLineId == receivingLineId }
            ?: return DomainResult.Error(message = "Receiving line with ID '$receivingLineId' not found.")

        if (line.lineStatus != InventoryReceivingLineStatus.PENDING) {
            return DomainResult.Error(message = "Received quantity can only be recorded for PENDING lines. Line '${receivingLineId}' is '${line.lineStatus.defaultLabel}'.")
        }

        val qtyValidation = InventoryReceivingLineValidator.validateReceivedQuantity(
            receivedQuantity = receivedQuantity,
            expectedQuantity = line.expectedQuantity
        )
        if (qtyValidation is DomainResult.Error) return qtyValidation

        val updated = line.copy(receivedQuantity = receivedQuantity, updatedAt = timestamp)
        val result = receivingDataSource.updateReceivingLine(updated)

        // Also ensure the parent receiving is in RECEIVING state
        if (result is DomainResult.Success) {
            val receiving = receivingDataSource.observeReceivings().first()
                .find { it.receivingId == line.receivingId }
            if (receiving != null && receiving.status == InventoryReceivingStatus.PENDING) {
                val updatedReceiving = receiving.copy(
                    status = InventoryReceivingStatus.RECEIVING,
                    updatedAt = timestamp
                )
                receivingDataSource.updateReceiving(updatedReceiving)
                emitAudit(
                    projectId = receiving.projectId,
                    receivingId = receiving.receivingId,
                    eventType = InventoryReceivingActivityType.RECEIVING_STARTED,
                    actorId = actorId,
                    description = "Receiving '${receiving.receivingReference}' auto-transitioned to RECEIVING on first quantity recording.",
                    timestamp = timestamp
                )
            }
            emitAudit(
                projectId = line.projectId,
                receivingId = line.receivingId,
                receivingLineId = receivingLineId,
                eventType = InventoryReceivingActivityType.LINE_UPDATED,
                actorId = actorId,
                description = "Received quantity recorded for line '$receivingLineId': $receivedQuantity ${line.unit.defaultLabel}.",
                timestamp = timestamp
            )
        }
        result
    }

    // ──────────────────────────────────────────────────────────────
    // Verification & Acceptance
    // ──────────────────────────────────────────────────────────────

    override suspend fun verifyReceivingLine(
        receivingLineId: String,
        verifiedBy: String,
        acceptedQuantity: Int,
        rejectedQuantity: Int,
        verificationNotes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryReceiptVerification> = repositoryMutex.withLock {
        val rbac = InventoryReceivingAuthorizationValidator.validateVerifyPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val line = receivingDataSource.observeReceivingLines().first()
            .find { it.receivingLineId == receivingLineId }
            ?: return DomainResult.Error(message = "Receiving line with ID '$receivingLineId' not found.")

        // Line must have received quantity recorded
        if (line.receivedQuantity <= 0) {
            return DomainResult.Error(message = "Line '$receivingLineId' has no received quantity recorded. Record received quantity before verifying.")
        }

        // Line must be in PENDING to transition to VERIFIED
        val lineTransition = InventoryReceivingLifecycleValidator.validateLineTransition(
            current = line.lineStatus,
            target = InventoryReceivingLineStatus.VERIFIED
        )
        if (lineTransition is DomainResult.Error) return lineTransition

        // Validate quantity split with full reconciliation required
        val splitValidation = InventoryReceivingLineValidator.validateQuantitySplit(
            receivedQuantity = line.receivedQuantity,
            acceptedQuantity = acceptedQuantity,
            rejectedQuantity = rejectedQuantity,
            requireFullReconciliation = true
        )
        if (splitValidation is DomainResult.Error) return splitValidation

        // Create verification record
        val verification = InventoryReceiptVerification(
            verificationId = UUID.randomUUID().toString(),
            receivingId = line.receivingId,
            receivingLineId = receivingLineId,
            projectId = line.projectId,
            verifiedBy = verifiedBy,
            verifiedAt = timestamp,
            receivedQuantity = line.receivedQuantity,
            acceptedQuantity = acceptedQuantity,
            rejectedQuantity = rejectedQuantity,
            verificationNotes = verificationNotes
        )

        val verResult = receivingDataSource.insertVerification(verification)
        if (verResult is DomainResult.Error) return verResult

        // Update line to VERIFIED with the accepted/rejected split
        val updatedLine = line.copy(
            acceptedQuantity = acceptedQuantity,
            rejectedQuantity = rejectedQuantity,
            lineStatus = InventoryReceivingLineStatus.VERIFIED,
            updatedAt = timestamp
        )
        val lineResult = receivingDataSource.updateReceivingLine(updatedLine)
        if (lineResult is DomainResult.Error) return lineResult

        emitAudit(
            projectId = line.projectId,
            receivingId = line.receivingId,
            receivingLineId = receivingLineId,
            eventType = InventoryReceivingActivityType.LINE_VERIFIED,
            actorId = verifiedBy,
            description = "Line '$receivingLineId' verified by '$verifiedBy'. Accepted: $acceptedQuantity, Rejected: $rejectedQuantity.",
            timestamp = timestamp
        )

        DomainResult.Success(verification)
    }

    override suspend fun acceptLine(
        receivingLineId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryReceivingLine> = repositoryMutex.withLock {
        val rbac = InventoryReceivingAuthorizationValidator.validateAcceptRejectPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val line = receivingDataSource.observeReceivingLines().first()
            .find { it.receivingLineId == receivingLineId }
            ?: return DomainResult.Error(message = "Receiving line with ID '$receivingLineId' not found.")

        val lineTransition = InventoryReceivingLifecycleValidator.validateLineTransition(
            current = line.lineStatus,
            target = InventoryReceivingLineStatus.ACCEPTED
        )
        if (lineTransition is DomainResult.Error) return lineTransition

        val updated = line.copy(
            lineStatus = InventoryReceivingLineStatus.ACCEPTED,
            updatedAt = timestamp
        )
        val result = receivingDataSource.updateReceivingLine(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = line.projectId,
                receivingId = line.receivingId,
                receivingLineId = receivingLineId,
                eventType = InventoryReceivingActivityType.QUANTITY_ACCEPTED,
                actorId = actorId,
                description = "Line '$receivingLineId' accepted by '$actorId'. Accepted quantity: ${line.acceptedQuantity} ${line.unit.defaultLabel}.",
                timestamp = timestamp
            )
        }
        result
    }

    override suspend fun rejectLine(
        receivingLineId: String,
        rejectionReason: String?,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryReceivingLine> = repositoryMutex.withLock {
        val rbac = InventoryReceivingAuthorizationValidator.validateAcceptRejectPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val line = receivingDataSource.observeReceivingLines().first()
            .find { it.receivingLineId == receivingLineId }
            ?: return DomainResult.Error(message = "Receiving line with ID '$receivingLineId' not found.")

        val lineTransition = InventoryReceivingLifecycleValidator.validateLineTransition(
            current = line.lineStatus,
            target = InventoryReceivingLineStatus.REJECTED
        )
        if (lineTransition is DomainResult.Error) return lineTransition

        val updated = line.copy(
            lineStatus = InventoryReceivingLineStatus.REJECTED,
            rejectionReason = rejectionReason,
            updatedAt = timestamp
        )
        val result = receivingDataSource.updateReceivingLine(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = line.projectId,
                receivingId = line.receivingId,
                receivingLineId = receivingLineId,
                eventType = InventoryReceivingActivityType.QUANTITY_REJECTED,
                actorId = actorId,
                description = "Line '$receivingLineId' rejected by '$actorId'. Reason: ${rejectionReason ?: "Not specified"}.",
                timestamp = timestamp
            )
        }
        result
    }

    // ──────────────────────────────────────────────────────────────
    // Stock-In Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeStockInRecords(projectId: String): Flow<List<InventoryStockInRecord>> {
        return receivingDataSource.observeStockInRecords().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override fun observeStockInRecordsByReceiving(receivingId: String): Flow<List<InventoryStockInRecord>> {
        return receivingDataSource.observeStockInRecords().map { list ->
            list.filter { it.receivingId == receivingId }
        }
    }

    override suspend fun getStockInRecords(
        receivingId: String,
        callerRole: UserRole?
    ): DomainResult<List<InventoryStockInRecord>> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryReceivingAuthorizationValidator.validateViewPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        val records = receivingDataSource.observeStockInRecords().first()
            .filter { it.receivingId == receivingId }
        DomainResult.Success(records)
    }

    // ──────────────────────────────────────────────────────────────
    // Verification Queries
    // ──────────────────────────────────────────────────────────────

    override suspend fun getVerifications(
        receivingId: String,
        callerRole: UserRole?
    ): DomainResult<List<InventoryReceiptVerification>> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryReceivingAuthorizationValidator.validateViewPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        val verifications = receivingDataSource.observeVerifications().first()
            .filter { it.receivingId == receivingId }
        DomainResult.Success(verifications)
    }

    // ──────────────────────────────────────────────────────────────
    // Audit Trail
    // ──────────────────────────────────────────────────────────────

    override fun observeAuditEvents(projectId: String): Flow<List<InventoryReceivingActivityEvent>> {
        return receivingDataSource.observeAuditEvents().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override suspend fun getAuditHistory(
        receivingId: String,
        callerRole: UserRole?
    ): DomainResult<List<InventoryReceivingActivityEvent>> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryReceivingAuthorizationValidator.validateViewPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        val events = receivingDataSource.observeAuditEvents().first()
            .filter { it.receivingId == receivingId }
        DomainResult.Success(events)
    }

    // ──────────────────────────────────────────────────────────────
    // Internal Helpers
    // ──────────────────────────────────────────────────────────────

    private suspend fun emitAudit(
        projectId: String,
        receivingId: String,
        receivingLineId: String? = null,
        eventType: InventoryReceivingActivityType,
        actorId: String,
        description: String,
        timestamp: String,
        metadata: String? = null
    ) {
        val event = InventoryReceivingActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = projectId,
            receivingId = receivingId,
            receivingLineId = receivingLineId,
            eventType = eventType,
            actorId = actorId,
            description = description,
            timestamp = timestamp,
            metadata = metadata
        )
        receivingDataSource.recordAuditEvent(event)
    }
}

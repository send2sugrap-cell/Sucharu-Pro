package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.InventoryLocationDataSource
import com.sucharu.sucharupro.data.datasource.InventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.InventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.InventoryStockAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.InventoryStockOutDataSource
import com.sucharu.sucharupro.data.datasource.InventoryStockTransferDataSource
import com.sucharu.sucharupro.data.datasource.InventoryWarehouseDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryAdjustmentType
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustment
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentActivityType
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentLine
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentRecord
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryStockAdjustmentRepository
import com.sucharu.sucharupro.domain.validation.InventoryStockAdjustmentAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.InventoryStockAdjustmentLifecycleValidator
import com.sucharu.sucharupro.domain.validation.InventoryStockAdjustmentLineValidator
import com.sucharu.sucharupro.domain.validation.InventoryStockAdjustmentValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Thread-safe production-grade repository implementation for Stock Adjustment Management
 * (Module 07 Step 06).
 */
class InventoryStockAdjustmentRepositoryImpl(
    private val adjustmentDataSource: InventoryStockAdjustmentDataSource,
    private val stockOutDataSource: InventoryStockOutDataSource,
    private val transferDataSource: InventoryStockTransferDataSource,
    private val receivingDataSource: InventoryReceivingDataSource,
    private val productDataSource: InventoryProductDataSource,
    private val warehouseDataSource: InventoryWarehouseDataSource,
    private val locationDataSource: InventoryLocationDataSource
) : InventoryStockAdjustmentRepository {

    private val repositoryMutex = Mutex()

    // ──────────────────────────────────────────────────────────────
    // Stock Adjustment Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeStockAdjustments(projectId: String): Flow<List<InventoryStockAdjustment>> {
        return adjustmentDataSource.observeStockAdjustments().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override fun observeStockAdjustment(adjustmentId: String): Flow<InventoryStockAdjustment?> {
        return adjustmentDataSource.observeStockAdjustments().map { list ->
            list.find { it.adjustmentId == adjustmentId }
        }
    }

    override suspend fun getStockAdjustment(
        adjustmentId: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockAdjustment> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryStockAdjustmentAuthorizationValidator.validateViewPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        val adjustment = adjustmentDataSource.observeStockAdjustments().first()
            .find { it.adjustmentId == adjustmentId }
            ?: return DomainResult.Error(message = "Stock Adjustment with ID '$adjustmentId' not found.")
        DomainResult.Success(adjustment)
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Adjustment Line Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeStockAdjustmentLines(adjustmentId: String): Flow<List<InventoryStockAdjustmentLine>> {
        return adjustmentDataSource.observeStockAdjustmentLines().map { list ->
            list.filter { it.adjustmentId == adjustmentId }
        }
    }

    override fun observeStockAdjustmentLine(adjustmentLineId: String): Flow<InventoryStockAdjustmentLine?> {
        return adjustmentDataSource.observeStockAdjustmentLines().map { list ->
            list.find { it.adjustmentLineId == adjustmentLineId }
        }
    }

    override suspend fun getStockAdjustmentLine(
        adjustmentLineId: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockAdjustmentLine> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryStockAdjustmentAuthorizationValidator.validateViewPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        val line = adjustmentDataSource.observeStockAdjustmentLines().first()
            .find { it.adjustmentLineId == adjustmentLineId }
            ?: return DomainResult.Error(message = "Stock Adjustment Line with ID '$adjustmentLineId' not found.")
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

        val adjustmentRecords = adjustmentDataSource.observeStockAdjustmentRecords().first()
            .filter {
                it.projectId == projectId &&
                    it.warehouseId == warehouseId &&
                    it.locationId == locationId &&
                    it.inventoryProductId == productId
            }

        val totalIn = stockInRecords.sumOf { it.quantity }
        val totalOut = stockOutRecords.sumOf { it.quantity }

        val transfersOut = transferRecords.filter { it.fromWarehouseId == warehouseId && it.fromLocationId == locationId }
            .sumOf { it.quantity }
        val transfersIn = transferRecords.filter { it.toWarehouseId == warehouseId && it.toLocationId == locationId }
            .sumOf { it.quantity }

        val adjustmentsIn = adjustmentRecords.filter { it.adjustmentType == InventoryAdjustmentType.INCREASE }
            .sumOf { it.quantity }
        val adjustmentsOut = adjustmentRecords.filter { it.adjustmentType == InventoryAdjustmentType.DECREASE }
            .sumOf { it.quantity }

        return (totalIn - totalOut - transfersOut + transfersIn + adjustmentsIn - adjustmentsOut).coerceAtLeast(0)
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Adjustment Mutations
    // ──────────────────────────────────────────────────────────────

    override suspend fun createStockAdjustment(
        adjustment: InventoryStockAdjustment,
        callerRole: UserRole?
    ): DomainResult<InventoryStockAdjustment> = repositoryMutex.withLock {
        val rbac = InventoryStockAdjustmentAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val structural = InventoryStockAdjustmentValidator.validateAdjustment(adjustment)
        if (structural is DomainResult.Error) return structural

        val warehouse = warehouseDataSource.observeWarehouses().first().find { it.id == adjustment.warehouseId }
        val warehouseResult = InventoryStockAdjustmentValidator.validateWarehouseEligibility(
            warehouseId = adjustment.warehouseId,
            projectId = adjustment.projectId,
            warehouse = warehouse
        )
        if (warehouseResult is DomainResult.Error) return warehouseResult

        val existingAdjustments = adjustmentDataSource.observeStockAdjustments().first()
        val uniquenessResult = InventoryStockAdjustmentValidator.validateAdjustmentReferenceUniqueness(
            reference = adjustment.adjustmentReference,
            adjustmentId = adjustment.adjustmentId,
            projectId = adjustment.projectId,
            existingAdjustments = existingAdjustments
        )
        if (uniquenessResult is DomainResult.Error) return uniquenessResult

        val insertResult = adjustmentDataSource.insertStockAdjustment(adjustment)
        if (insertResult is DomainResult.Success) {
            emitAudit(
                projectId = adjustment.projectId,
                adjustmentId = adjustment.adjustmentId,
                eventType = InventoryStockAdjustmentActivityType.CREATED,
                actorId = adjustment.createdBy,
                description = "Created stock adjustment '${adjustment.adjustmentReference}' for warehouse '${adjustment.warehouseId}'.",
                timestamp = adjustment.createdAt
            )
        }
        insertResult
    }

    override suspend fun updateStockAdjustment(
        adjustmentId: String,
        adjustmentReference: String,
        warehouseId: String,
        adjustmentDate: String,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockAdjustment> = repositoryMutex.withLock {
        val rbac = InventoryStockAdjustmentAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = adjustmentDataSource.observeStockAdjustments().first()
            .find { it.adjustmentId == adjustmentId }
            ?: return DomainResult.Error(message = "Stock Adjustment with ID '$adjustmentId' not found.")

        if (current.isTerminal) {
            return DomainResult.Error(message = "Stock adjustment '$adjustmentId' is in terminal state and cannot be updated.")
        }

        val warehouse = warehouseDataSource.observeWarehouses().first().find { it.id == warehouseId }
        val warehouseResult = InventoryStockAdjustmentValidator.validateWarehouseEligibility(
            warehouseId = warehouseId,
            projectId = current.projectId,
            warehouse = warehouse
        )
        if (warehouseResult is DomainResult.Error) return warehouseResult

        val existingAdjustments = adjustmentDataSource.observeStockAdjustments().first()
        val uniquenessResult = InventoryStockAdjustmentValidator.validateAdjustmentReferenceUniqueness(
            reference = adjustmentReference,
            adjustmentId = adjustmentId,
            projectId = current.projectId,
            existingAdjustments = existingAdjustments
        )
        if (uniquenessResult is DomainResult.Error) return uniquenessResult

        val updated = current.copy(
            adjustmentReference = adjustmentReference,
            warehouseId = warehouseId,
            adjustmentDate = adjustmentDate,
            notes = notes,
            updatedAt = timestamp
        )
        val updateResult = adjustmentDataSource.updateStockAdjustment(updated)
        if (updateResult is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                adjustmentId = adjustmentId,
                eventType = InventoryStockAdjustmentActivityType.UPDATED,
                actorId = "SYSTEM",
                description = "Updated stock adjustment header '${adjustmentReference}'.",
                timestamp = timestamp
            )
        }
        updateResult
    }

    override suspend fun submitStockAdjustment(
        adjustmentId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockAdjustment> = repositoryMutex.withLock {
        val rbac = InventoryStockAdjustmentAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = adjustmentDataSource.observeStockAdjustments().first()
            .find { it.adjustmentId == adjustmentId }
            ?: return DomainResult.Error(message = "Stock Adjustment with ID '$adjustmentId' not found.")

        val transition = InventoryStockAdjustmentLifecycleValidator.validateAdjustmentTransition(
            current = current.status,
            target = InventoryStockAdjustmentStatus.PENDING
        )
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(status = InventoryStockAdjustmentStatus.PENDING, updatedAt = timestamp)
        val result = adjustmentDataSource.updateStockAdjustment(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                adjustmentId = adjustmentId,
                eventType = InventoryStockAdjustmentActivityType.SUBMITTED,
                actorId = actorId,
                description = "Stock adjustment '${current.adjustmentReference}' submitted for approval.",
                timestamp = timestamp
            )
        }
        result
    }

    override suspend fun approveStockAdjustment(
        adjustmentId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockAdjustment> = repositoryMutex.withLock {
        val rbac = InventoryStockAdjustmentAuthorizationValidator.validateApprovePermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = adjustmentDataSource.observeStockAdjustments().first()
            .find { it.adjustmentId == adjustmentId }
            ?: return DomainResult.Error(message = "Stock Adjustment with ID '$adjustmentId' not found.")

        val transition = InventoryStockAdjustmentLifecycleValidator.validateAdjustmentTransition(
            current = current.status,
            target = InventoryStockAdjustmentStatus.APPROVED
        )
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            status = InventoryStockAdjustmentStatus.APPROVED,
            approvedAt = timestamp,
            approvedBy = actorId,
            updatedAt = timestamp
        )
        val result = adjustmentDataSource.updateStockAdjustment(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                adjustmentId = adjustmentId,
                eventType = InventoryStockAdjustmentActivityType.APPROVED,
                actorId = actorId,
                description = "Stock adjustment '${current.adjustmentReference}' approved by '$actorId'.",
                timestamp = timestamp
            )
        }
        result
    }

    override suspend fun startStockAdjustment(
        adjustmentId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockAdjustment> = repositoryMutex.withLock {
        val rbac = InventoryStockAdjustmentAuthorizationValidator.validateProcessPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = adjustmentDataSource.observeStockAdjustments().first()
            .find { it.adjustmentId == adjustmentId }
            ?: return DomainResult.Error(message = "Stock Adjustment with ID '$adjustmentId' not found.")

        val transition = InventoryStockAdjustmentLifecycleValidator.validateAdjustmentTransition(
            current = current.status,
            target = InventoryStockAdjustmentStatus.ADJUSTING
        )
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(status = InventoryStockAdjustmentStatus.ADJUSTING, updatedAt = timestamp)
        val result = adjustmentDataSource.updateStockAdjustment(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                adjustmentId = adjustmentId,
                eventType = InventoryStockAdjustmentActivityType.STARTED,
                actorId = actorId,
                description = "Stock adjustment '${current.adjustmentReference}' started — now ADJUSTING.",
                timestamp = timestamp
            )
        }
        result
    }

    override suspend fun rejectStockAdjustment(
        adjustmentId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockAdjustment> = repositoryMutex.withLock {
        val rbac = InventoryStockAdjustmentAuthorizationValidator.validateCancelPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = adjustmentDataSource.observeStockAdjustments().first()
            .find { it.adjustmentId == adjustmentId }
            ?: return DomainResult.Error(message = "Stock Adjustment with ID '$adjustmentId' not found.")

        val transition = InventoryStockAdjustmentLifecycleValidator.validateAdjustmentTransition(
            current = current.status,
            target = InventoryStockAdjustmentStatus.CANCELLED
        )
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            status = InventoryStockAdjustmentStatus.CANCELLED,
            cancelledAt = timestamp,
            cancelledBy = actorId,
            updatedAt = timestamp
        )
        val result = adjustmentDataSource.updateStockAdjustment(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                adjustmentId = adjustmentId,
                eventType = InventoryStockAdjustmentActivityType.CANCELLED,
                actorId = actorId,
                description = "Stock adjustment '${current.adjustmentReference}' rejected/cancelled by '$actorId'.",
                timestamp = timestamp
            )
        }
        result
    }

    override suspend fun cancelStockAdjustment(
        adjustmentId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockAdjustment> = repositoryMutex.withLock {
        val rbac = InventoryStockAdjustmentAuthorizationValidator.validateCancelPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = adjustmentDataSource.observeStockAdjustments().first()
            .find { it.adjustmentId == adjustmentId }
            ?: return DomainResult.Error(message = "Stock Adjustment with ID '$adjustmentId' not found.")

        val transition = InventoryStockAdjustmentLifecycleValidator.validateAdjustmentTransition(
            current = current.status,
            target = InventoryStockAdjustmentStatus.CANCELLED
        )
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            status = InventoryStockAdjustmentStatus.CANCELLED,
            cancelledAt = timestamp,
            cancelledBy = actorId,
            updatedAt = timestamp
        )
        val result = adjustmentDataSource.updateStockAdjustment(updated)
        if (result is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                adjustmentId = adjustmentId,
                eventType = InventoryStockAdjustmentActivityType.CANCELLED,
                actorId = actorId,
                description = "Stock adjustment '${current.adjustmentReference}' cancelled by '$actorId'.",
                timestamp = timestamp
            )
        }
        result
    }

    override suspend fun completeStockAdjustment(
        adjustmentId: String,
        actorId: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockAdjustment> = repositoryMutex.withLock {
        val rbac = InventoryStockAdjustmentAuthorizationValidator.validateProcessPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = adjustmentDataSource.observeStockAdjustments().first()
            .find { it.adjustmentId == adjustmentId }
            ?: return DomainResult.Error(message = "Stock Adjustment with ID '$adjustmentId' not found.")

        if (current.status == InventoryStockAdjustmentStatus.COMPLETED) return DomainResult.Success(current)

        val transition = InventoryStockAdjustmentLifecycleValidator.validateAdjustmentTransition(
            current = current.status,
            target = InventoryStockAdjustmentStatus.COMPLETED
        )
        if (transition is DomainResult.Error) return transition

        // If in APPROVED, we need to implicitly or explicitly move to ADJUSTING?
        // Let's assume we use the state machine correctly in tests now.
        // But for robust implementation, if we are in APPROVED, we can auto-transition if business permits.
        // However, Step 06 says: DRAFT → PENDING → APPROVED → ADJUSTING → COMPLETED.
        // So we strictly follow it.

        val lines = adjustmentDataSource.observeStockAdjustmentLines().first()
            .filter { it.adjustmentId == adjustmentId }

        val nonEmptyCheck = InventoryStockAdjustmentValidator.validateNonEmptyLines(lines)
        if (nonEmptyCheck is DomainResult.Error) return nonEmptyCheck

        // Validate sufficient stock for all DECREASE lines
        for (line in lines) {
            if (line.adjustmentType == InventoryAdjustmentType.DECREASE) {
                val available = getAvailableQuantity(
                    projectId = current.projectId,
                    warehouseId = current.warehouseId,
                    locationId = line.locationId,
                    productId = line.inventoryProductId
                )
                // Note: currentQuantity in line should be what was observed at creation.
                // We should validate against real-time availability.
                if (available + line.quantityChange < 0) {
                    return DomainResult.Error(
                        message = "Insufficient stock for product '${line.inventoryProductId}' at location '${line.locationId}'. " +
                            "Available: $available, Adjustment: ${line.quantityChange}."
                    )
                }
            }
        }

        // Create Records
        for (line in lines) {
            val record = InventoryStockAdjustmentRecord(
                adjustmentRecordId = UUID.randomUUID().toString(),
                adjustmentId = adjustmentId,
                adjustmentLineId = line.adjustmentLineId,
                projectId = current.projectId,
                inventoryProductId = line.inventoryProductId,
                warehouseId = current.warehouseId,
                locationId = line.locationId,
                adjustmentType = line.adjustmentType,
                adjustmentReason = line.adjustmentReason,
                quantity = Math.abs(line.quantityChange),
                unit = line.unit,
                createdBy = actorId,
                createdAt = timestamp,
                sourceReference = current.adjustmentReference
            )
            val recordResult = adjustmentDataSource.insertStockAdjustmentRecord(record)
            if (recordResult is DomainResult.Error) {
                return DomainResult.Error(message = "Failed to create StockAdjustmentRecord: ${recordResult.message}")
            }

            emitAudit(
                projectId = current.projectId,
                adjustmentId = adjustmentId,
                adjustmentLineId = line.adjustmentLineId,
                eventType = InventoryStockAdjustmentActivityType.STARTED,
                actorId = actorId,
                description = "Stock adjustment record created: ${line.adjustmentType.defaultLabel} ${Math.abs(line.quantityChange)} ${line.unit.defaultLabel}.",
                timestamp = timestamp
            )
        }

        val completed = current.copy(
            status = InventoryStockAdjustmentStatus.COMPLETED,
            completedAt = timestamp,
            completedBy = actorId,
            updatedAt = timestamp,
            totalQuantityChange = lines.sumOf { it.quantityChange }
        )
        val updateResult = adjustmentDataSource.updateStockAdjustment(completed)
        if (updateResult is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                adjustmentId = adjustmentId,
                eventType = InventoryStockAdjustmentActivityType.COMPLETED,
                actorId = actorId,
                description = "Stock adjustment '${current.adjustmentReference}' completed.",
                timestamp = timestamp
            )
        }
        updateResult
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Adjustment Line Mutations
    // ──────────────────────────────────────────────────────────────

    override suspend fun addStockAdjustmentLine(
        line: InventoryStockAdjustmentLine,
        callerRole: UserRole?
    ): DomainResult<InventoryStockAdjustmentLine> = repositoryMutex.withLock {
        val rbac = InventoryStockAdjustmentAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val structural = InventoryStockAdjustmentLineValidator.validateLine(line)
        if (structural is DomainResult.Error) return structural

        val current = adjustmentDataSource.observeStockAdjustments().first()
            .find { it.adjustmentId == line.adjustmentId }
            ?: return DomainResult.Error(message = "Stock Adjustment with ID '${line.adjustmentId}' not found.")

        if (current.isTerminal) {
            return DomainResult.Error(message = "Cannot add line to terminal stock adjustment.")
        }

        val product = productDataSource.observeProducts().first().find { it.id == line.inventoryProductId }
        val productResult = InventoryStockAdjustmentValidator.validateProductEligibility(line.inventoryProductId, product)
        if (productResult is DomainResult.Error) return productResult

        val location = locationDataSource.observeLocations().first().find { it.id == line.locationId }
        val locationResult = InventoryStockAdjustmentValidator.validateLocationEligibility(
            locationId = line.locationId,
            warehouseId = current.warehouseId,
            location = location
        )
        if (locationResult is DomainResult.Error) return locationResult

        val isolationResult = InventoryStockAdjustmentValidator.validateProjectIsolation(current.projectId, line.projectId)
        if (isolationResult is DomainResult.Error) return isolationResult

        val qtyResult = InventoryStockAdjustmentLineValidator.validateAdjustmentQuantity(
            type = line.adjustmentType,
            currentQuantity = line.currentQuantity,
            adjustedQuantity = line.adjustedQuantity,
            quantityChange = line.quantityChange
        )
        if (qtyResult is DomainResult.Error) return qtyResult

        val insertResult = adjustmentDataSource.insertStockAdjustmentLine(line)
        if (insertResult is DomainResult.Success) {
            emitAudit(
                projectId = line.projectId,
                adjustmentId = line.adjustmentId,
                adjustmentLineId = line.adjustmentLineId,
                eventType = InventoryStockAdjustmentActivityType.STARTED,
                actorId = "SYSTEM",
                description = "Line added: ${line.inventoryProductId} at ${line.locationId}, change: ${line.quantityChange} ${line.unit.defaultLabel}.",
                timestamp = line.createdAt
            )
        }
        insertResult
    }

    override suspend fun updateStockAdjustmentLine(
        adjustmentLineId: String,
        adjustedQuantity: Int,
        locationId: String,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<InventoryStockAdjustmentLine> = repositoryMutex.withLock {
        val rbac = InventoryStockAdjustmentAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = adjustmentDataSource.observeStockAdjustmentLines().first()
            .find { it.adjustmentLineId == adjustmentLineId }
            ?: return DomainResult.Error(message = "Stock Adjustment Line with ID '$adjustmentLineId' not found.")

        val parent = adjustmentDataSource.observeStockAdjustments().first()
            .find { it.adjustmentId == current.adjustmentId }
            ?: return DomainResult.Error(message = "Parent Stock Adjustment not found.")

        if (parent.isTerminal) {
            return DomainResult.Error(message = "Cannot update line in terminal stock adjustment.")
        }

        // Validate new location if changed
        if (locationId != current.locationId) {
            val location = locationDataSource.observeLocations().first().find { it.id == locationId }
            val locationResult = InventoryStockAdjustmentValidator.validateLocationEligibility(
                locationId = locationId,
                warehouseId = parent.warehouseId,
                location = location
            )
            if (locationResult is DomainResult.Error) return locationResult
        }

        val newQuantityChange = adjustedQuantity - current.currentQuantity
        val qtyResult = InventoryStockAdjustmentLineValidator.validateAdjustmentQuantity(
            type = current.adjustmentType,
            currentQuantity = current.currentQuantity,
            adjustedQuantity = adjustedQuantity,
            quantityChange = newQuantityChange
        )
        if (qtyResult is DomainResult.Error) return qtyResult

        val updated = current.copy(
            adjustedQuantity = adjustedQuantity,
            quantityChange = newQuantityChange,
            locationId = locationId,
            notes = notes,
            updatedAt = timestamp
        )
        val updateResult = adjustmentDataSource.updateStockAdjustmentLine(updated)
        if (updateResult is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                adjustmentId = current.adjustmentId,
                adjustmentLineId = adjustmentLineId,
                eventType = InventoryStockAdjustmentActivityType.STARTED,
                actorId = "SYSTEM",
                description = "Line updated: adjustedQuantity=$adjustedQuantity, locationId=$locationId.",
                timestamp = timestamp
            )
        }
        updateResult
    }

    override suspend fun removeStockAdjustmentLine(
        adjustmentLineId: String,
        callerRole: UserRole?
    ): DomainResult<Unit> = repositoryMutex.withLock {
        val rbac = InventoryStockAdjustmentAuthorizationValidator.validateCreateEditPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = adjustmentDataSource.observeStockAdjustmentLines().first()
            .find { it.adjustmentLineId == adjustmentLineId }
            ?: return DomainResult.Error(message = "Stock Adjustment Line with ID '$adjustmentLineId' not found.")

        val parent = adjustmentDataSource.observeStockAdjustments().first()
            .find { it.adjustmentId == current.adjustmentId }
            ?: return DomainResult.Error(message = "Parent Stock Adjustment not found.")

        if (parent.isTerminal) {
            return DomainResult.Error(message = "Cannot remove line from terminal stock adjustment.")
        }

        val deleteResult = adjustmentDataSource.deleteStockAdjustmentLine(adjustmentLineId)
        if (deleteResult is DomainResult.Success) {
            emitAudit(
                projectId = current.projectId,
                adjustmentId = current.adjustmentId,
                adjustmentLineId = adjustmentLineId,
                eventType = InventoryStockAdjustmentActivityType.STARTED,
                actorId = "SYSTEM",
                description = "Line removed: $adjustmentLineId.",
                timestamp = "SYSTEM"
            )
        }
        deleteResult
    }

    // ──────────────────────────────────────────────────────────────
    // Stock Adjustment Records & Audit
    // ──────────────────────────────────────────────────────────────

    override fun observeStockAdjustmentRecords(projectId: String): Flow<List<InventoryStockAdjustmentRecord>> {
        return adjustmentDataSource.observeStockAdjustmentRecords().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override fun observeAuditEvents(projectId: String): Flow<List<InventoryStockAdjustmentActivityEvent>> {
        return adjustmentDataSource.observeAuditEvents().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override suspend fun getAuditHistory(
        adjustmentId: String,
        callerRole: UserRole?
    ): DomainResult<List<InventoryStockAdjustmentActivityEvent>> = repositoryMutex.withLock {
        if (callerRole != null) {
            val rbac = InventoryStockAdjustmentAuthorizationValidator.validateViewPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }
        val events = adjustmentDataSource.observeAuditEvents().first()
            .filter { it.adjustmentId == adjustmentId }
        DomainResult.Success(events)
    }

    // ──────────────────────────────────────────────────────────────
    // Internal Helpers
    // ──────────────────────────────────────────────────────────────

    private suspend fun emitAudit(
        projectId: String,
        adjustmentId: String,
        adjustmentLineId: String? = null,
        eventType: InventoryStockAdjustmentActivityType,
        actorId: String,
        description: String,
        timestamp: String,
        metadata: String? = null
    ) {
        val event = InventoryStockAdjustmentActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = projectId,
            adjustmentId = adjustmentId,
            adjustmentLineId = adjustmentLineId,
            eventType = eventType,
            actorId = actorId,
            description = description,
            timestamp = timestamp,
            metadata = metadata
        )
        adjustmentDataSource.recordAuditEvent(event)
    }
}

package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryBatch
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryLot
import com.sucharu.sucharupro.domain.model.inventory.traceability.InventoryTraceabilityStatus

/**
 * Domain validator for Inventory Batch & Lot Traceability operations (Module 07 Step 07).
 */
object InventoryTraceabilityValidator {

    /**
     * Ensures `projectId + batchNo` is unique among existing batches.
     */
    fun validateBatchUniqueness(
        batchNo: String,
        projectId: String,
        currentBatchId: String?,
        existingBatches: List<InventoryBatch>
    ): DomainResult<Unit> {
        val normalized = batchNo.trim().uppercase()
        if (normalized.isBlank()) {
            return DomainResult.Error(message = "Batch number cannot be blank.")
        }
        val match = existingBatches.find {
            it.projectId == projectId &&
                    it.batchNo.trim().uppercase() == normalized &&
                    it.batchId != currentBatchId
        }
        if (match != null) {
            return DomainResult.Error(
                message = "Batch number '$batchNo' already exists for project '$projectId' (Batch ID: '${match.batchId}')."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Ensures `projectId + lotNo` is unique among existing lots.
     */
    fun validateLotUniqueness(
        lotNo: String,
        projectId: String,
        currentLotId: String?,
        existingLots: List<InventoryLot>
    ): DomainResult<Unit> {
        val normalized = lotNo.trim().uppercase()
        if (normalized.isBlank()) {
            return DomainResult.Error(message = "Lot number cannot be blank.")
        }
        val match = existingLots.find {
            it.projectId == projectId &&
                    it.lotNo.trim().uppercase() == normalized &&
                    it.lotId != currentLotId
        }
        if (match != null) {
            return DomainResult.Error(
                message = "Lot number '$lotNo' already exists for project '$projectId' (Lot ID: '${match.lotId}')."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Ensures product, batch, lot, and trace record all belong to the same project.
     *
     * Validates cross-project isolation:
     * - batch.projectId == record.projectId
     * - lot.projectId == record.projectId
     * - batch.productId == record.productId
     * - lot.productId == record.productId
     */
    fun validateProjectIsolation(
        projectId: String,
        productId: String,
        batch: InventoryBatch? = null,
        lot: InventoryLot? = null
    ): DomainResult<Unit> {
        if (batch != null) {
            if (batch.projectId != projectId) {
                return DomainResult.Error(
                    message = "Project isolation violation: record projectId '$projectId' != batch projectId '${batch.projectId}'."
                )
            }
            if (batch.productId != productId) {
                return DomainResult.Error(
                    message = "Project isolation violation: record productId '$productId' != batch productId '${batch.productId}'."
                )
            }
        }
        if (lot != null) {
            if (lot.projectId != projectId) {
                return DomainResult.Error(
                    message = "Project isolation violation: record projectId '$projectId' != lot projectId '${lot.projectId}'."
                )
            }
            if (lot.productId != productId) {
                return DomainResult.Error(
                    message = "Project isolation violation: record productId '$productId' != lot productId '${lot.productId}'."
                )
            }
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Enforces lifecycle rules (e.g., cannot move from terminal states like CLOSED).
     */
    fun validateStatusTransition(
        currentStatus: InventoryTraceabilityStatus,
        newStatus: InventoryTraceabilityStatus
    ): DomainResult<Unit> {
        if (currentStatus.isTerminal) {
            return DomainResult.Error(
                message = "Cannot change status from terminal state '${currentStatus.defaultLabel}' (Current: ${currentStatus.name})."
            )
        }
        return DomainResult.Success(Unit)
    }
}

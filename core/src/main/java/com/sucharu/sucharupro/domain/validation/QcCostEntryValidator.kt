package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostEntry
import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostType

/**
 * Domain validator for individual QC operational cost entries (Module 06 Step 08).
 */
object QcCostEntryValidator {

    /**
     * Validates input parameters required to record a QC cost entry.
     */
    fun validateCreation(
        projectId: String,
        productionJobId: String,
        costType: QcCostType?,
        description: String,
        quantity: Double,
        unitCost: Double,
        currency: String,
        recordedBy: String,
        recordedAt: String
    ): DomainResult<Unit> {
        if (projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (productionJobId.isBlank()) {
            return DomainResult.Error(message = "Production Job ID cannot be blank.")
        }
        if (costType == null) {
            return DomainResult.Error(message = "QC Cost Type must be specified.")
        }
        if (description.isBlank()) {
            return DomainResult.Error(message = "Description cannot be blank.")
        }
        if (quantity <= 0.0) {
            return DomainResult.Error(message = "Quantity must be greater than zero. Provided: $quantity")
        }
        if (unitCost < 0.0) {
            return DomainResult.Error(message = "Unit cost cannot be negative. Provided: $unitCost")
        }
        if (currency.isBlank()) {
            return DomainResult.Error(message = "Currency cannot be blank.")
        }
        if (recordedBy.isBlank()) {
            return DomainResult.Error(message = "RecordedBy user ID cannot be blank.")
        }
        if (recordedAt.isBlank()) {
            return DomainResult.Error(message = "RecordedAt timestamp cannot be blank.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates the internal integrity of a [QcCostEntry].
     */
    fun validateModel(entry: QcCostEntry): DomainResult<Unit> {
        if (entry.id.isBlank()) {
            return DomainResult.Error(message = "Cost entry ID cannot be blank.")
        }
        if (entry.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (entry.productionJobId.isBlank()) {
            return DomainResult.Error(message = "Production Job ID cannot be blank.")
        }
        if (entry.quantity <= 0.0) {
            return DomainResult.Error(message = "Quantity must be greater than zero.")
        }
        if (entry.unitCost < 0.0) {
            return DomainResult.Error(message = "Unit cost cannot be negative.")
        }
        if (entry.totalCost < 0.0) {
            return DomainResult.Error(message = "Total cost cannot be negative.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Enforces status transition rules.
     */
    fun validateStatusTransition(current: QcCostStatus, target: QcCostStatus): DomainResult<Unit> {
        if (!current.canTransitionTo(target)) {
            return DomainResult.Error(message = "Illegal QC Cost status transition from $current to $target.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Enforces immutability on locked or terminal cost entries.
     */
    fun validateImmutability(entry: QcCostEntry): DomainResult<Unit> {
        if (entry.isLocked) {
            return DomainResult.Error(message = "QC Cost entry '${entry.id}' is LOCKED and cannot be modified.")
        }
        if (entry.isTerminal) {
            return DomainResult.Error(message = "QC Cost entry '${entry.id}' is in terminal state '${entry.status}' and cannot be modified.")
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Rejects cross-job reference mismatches.
     */
    fun validateCrossJobIsolation(expectedJobId: String, actualJobId: String): DomainResult<Unit> {
        if (expectedJobId != actualJobId) {
            return DomainResult.Error(
                message = "Cross-job reference violation: Cost entry belongs to Job '$actualJobId' but target Job is '$expectedJobId'."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Rejects cross-project reference mismatches.
     */
    fun validateCrossProjectIsolation(expectedProjectId: String, actualProjectId: String): DomainResult<Unit> {
        if (expectedProjectId != actualProjectId) {
            return DomainResult.Error(
                message = "Cross-project reference violation: Expected Project '$expectedProjectId' but received '$actualProjectId'."
            )
        }
        return DomainResult.Success(Unit)
    }
}

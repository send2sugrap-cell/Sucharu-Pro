package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryStockLevelPolicy

/**
 * Domain validator for Inventory Stock Level Policy management (Module 07 Step 08).
 */
object InventoryStockLevelPolicyValidator {

    /**
     * Validates structural invariants and logical ordering of an [InventoryStockLevelPolicy].
     *
     * Invariants:
     * 1. Magnitudes: All levels >= 0.
     * 2. Logical Ordering: Critical <= Reorder <= Min <= Target <= Max.
     * 3. IDs: Policy, Project, and Product IDs cannot be blank.
     */
    fun validatePolicy(policy: InventoryStockLevelPolicy): DomainResult<Unit> {
        // ID checks
        if (policy.policyId.isBlank()) {
            return DomainResult.Error(message = "Policy ID cannot be blank.")
        }
        if (policy.projectId.isBlank()) {
            return DomainResult.Error(message = "Project ID cannot be blank.")
        }
        if (policy.productId.isBlank()) {
            return DomainResult.Error(message = "Product ID cannot be blank.")
        }

        // Magnitude checks
        if (policy.criticalStockLevel < 0) {
            return DomainResult.Error(message = "Critical stock level cannot be negative.")
        }
        if (policy.reorderPoint < 0) {
            return DomainResult.Error(message = "Reorder point cannot be negative.")
        }
        if (policy.minimumStockLevel < 0) {
            return DomainResult.Error(message = "Minimum stock level cannot be negative.")
        }
        if (policy.targetStockLevel < 0) {
            return DomainResult.Error(message = "Target stock level cannot be negative.")
        }
        if (policy.maximumStockLevel < 0) {
            return DomainResult.Error(message = "Maximum stock level cannot be negative.")
        }

        // Logical Ordering: Critical <= Reorder <= Min <= Target <= Max
        if (policy.reorderPoint < policy.criticalStockLevel) {
            return DomainResult.Error(
                message = "Reorder point (${policy.reorderPoint}) cannot be less than Critical level (${policy.criticalStockLevel})."
            )
        }
        if (policy.minimumStockLevel < policy.reorderPoint) {
            return DomainResult.Error(
                message = "Minimum stock level (${policy.minimumStockLevel}) cannot be less than Reorder point (${policy.reorderPoint})."
            )
        }
        if (policy.targetStockLevel < policy.minimumStockLevel) {
            return DomainResult.Error(
                message = "Target stock level (${policy.targetStockLevel}) cannot be less than Minimum level (${policy.minimumStockLevel})."
            )
        }
        if (policy.maximumStockLevel < policy.targetStockLevel) {
            return DomainResult.Error(
                message = "Maximum stock level (${policy.maximumStockLevel}) cannot be less than Target level (${policy.targetStockLevel})."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates project isolation for a policy operation.
     */
    fun validateProjectIsolation(policy: InventoryStockLevelPolicy, activeProjectId: String): DomainResult<Unit> {
        if (policy.projectId != activeProjectId) {
            return DomainResult.Error(
                message = "Cross-project policy operation detected. Policy belongs to '${policy.projectId}', but active project is '$activeProjectId'."
            )
        }
        return DomainResult.Success(Unit)
    }
}

package com.sucharu.sucharupro.domain.model.inventory.reorder

/**
 * Threshold configuration for automated inventory monitoring (Module 07 Step 08).
 *
 * Defines the stock level boundaries for a specific product at a specific location
 * or globally within a project. These thresholds trigger reorder alerts.
 */
data class InventoryStockLevelPolicy(
    val policyId: String,
    val projectId: String,
    val productId: String,
    val locationId: String? = null, // Optional for global product policy
    val minimumStockLevel: Double = 0.0,
    val reorderPoint: Double = 0.0,
    val criticalStockLevel: Double = 0.0,
    val targetStockLevel: Double = 0.0,
    val maximumStockLevel: Double = 0.0,
    val enabled: Boolean = true
) {
    init {
        require(policyId.isNotBlank()) { "Policy ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        require(minimumStockLevel >= 0) { "minimumStockLevel cannot be negative." }
        require(reorderPoint >= 0) { "reorderPoint cannot be negative." }
        require(criticalStockLevel >= 0) { "criticalStockLevel cannot be negative." }
        require(targetStockLevel >= 0) { "targetStockLevel cannot be negative." }
        require(maximumStockLevel >= 0) { "maximumStockLevel cannot be negative." }
        
        // Logical ordering validation
        require(maximumStockLevel >= targetStockLevel) { "maximumStockLevel cannot be less than targetStockLevel." }
        require(targetStockLevel >= reorderPoint) { "targetStockLevel cannot be less than reorderPoint." }
    }
}

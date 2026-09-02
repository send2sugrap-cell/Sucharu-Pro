package com.sucharu.sucharupro.domain.model.inventory.reorder

/**
 * Represents a detected stock level condition that requires attention (Module 07 Step 08).
 *
 * Alerts are generated automatically when stock levels cross thresholds defined in
 * an [InventoryStockLevelPolicy].
 */
data class InventoryReorderAlert(
    val alertId: String,
    val projectId: String,
    val productId: String,
    val locationId: String,
    val policyId: String,
    val alertType: InventoryReorderAlertType,
    val availableQuantity: Double,
    val thresholdQuantity: Double,
    val status: InventoryReorderAlertStatus = InventoryReorderAlertStatus.OPEN,
    val detectedAt: String,
    val acknowledgedAt: String? = null,
    val acknowledgedBy: String? = null,
    val resolvedAt: String? = null,
    val resolvedBy: String? = null
) {
    init {
        require(alertId.isNotBlank()) { "Alert ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        require(locationId.isNotBlank()) { "Location ID cannot be blank." }
        require(policyId.isNotBlank()) { "Policy ID cannot be blank." }
        require(detectedAt.isNotBlank()) { "DetectedAt timestamp cannot be blank." }

        if (status == InventoryReorderAlertStatus.ACKNOWLEDGED) {
            require(!acknowledgedAt.isNullOrBlank()) { "acknowledgedAt is required for ACKNOWLEDGED status." }
            require(!acknowledgedBy.isNullOrBlank()) { "acknowledgedBy is required for ACKNOWLEDGED status." }
        }
        if (status == InventoryReorderAlertStatus.RESOLVED) {
            require(!resolvedAt.isNullOrBlank()) { "resolvedAt is required for RESOLVED status." }
            require(!resolvedBy.isNullOrBlank()) { "resolvedBy is required for RESOLVED status." }
        }
    }
}

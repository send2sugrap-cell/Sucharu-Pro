package com.sucharu.sucharupro.domain.model.inventory.analytics

/**
 * Main KPI model for Inventory Analytics (Module 07 Step 10).
 */
data class InventoryAnalyticsSummary(
    val projectId: String,
    val totalStockQuantity: Double,
    val totalStockValue: Double?,
    val valuationStatus: ValuationStatus,
    val inboundQuantity: Double,
    val outboundQuantity: Double,
    val lowStockCount: Int,
    val criticalStockCount: Int,
    val outOfStockCount: Int,
    val openExceptionsCount: Int
) {
    enum class ValuationStatus {
        CALCULATED,
        DATA_MISSING
    }

    init {
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        // totalStockQuantity can be negative in governance scenarios (exceptions)
        totalStockValue?.let { require(it >= 0.0) { "Total stock value cannot be negative." } }
        require(inboundQuantity >= 0.0) { "Inbound quantity cannot be negative." }
        require(outboundQuantity >= 0.0) { "Outbound quantity cannot be negative." }
        require(lowStockCount >= 0) { "Low stock count cannot be negative." }
        require(criticalStockCount >= 0) { "Critical stock count cannot be negative." }
        require(outOfStockCount >= 0) { "Out of stock count cannot be negative." }
        require(openExceptionsCount >= 0) { "Open exceptions count cannot be negative." }
    }
}

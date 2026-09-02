package com.sucharu.sucharupro.domain.model.inventory.analytics

/**
 * Data class for tracking stock trends over a specific date (Module 07 Step 10).
 */
data class InventoryAnalyticsTrendPoint(
    val date: String,
    val closingQuantity: Double,
    val inboundQuantity: Double,
    val outboundQuantity: Double,
    val adjustmentQuantity: Double
) {
    init {
        require(date.isNotBlank()) { "Date cannot be blank." }
        require(closingQuantity >= 0.0) { "Closing quantity cannot be negative." }
        require(inboundQuantity >= 0.0) { "Inbound quantity cannot be negative." }
        require(outboundQuantity >= 0.0) { "Outbound quantity cannot be negative." }
    }
}

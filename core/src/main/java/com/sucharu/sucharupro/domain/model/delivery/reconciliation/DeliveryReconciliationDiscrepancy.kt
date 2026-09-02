package com.sucharu.sucharupro.domain.model.delivery.reconciliation

/**
 * Represents a deterministic discrepancy detected during Delivery Reconciliation (Module 08 Step 09).
 */
data class DeliveryReconciliationDiscrepancy(
    val discrepancyId: String,
    val reconciliationId: String,
    val projectId: String,
    val deliveryOrderLineId: String? = null,
    val discrepancyType: DeliveryReconciliationDiscrepancyType,
    val severity: DeliveryReconciliationDiscrepancySeverity = DeliveryReconciliationDiscrepancySeverity.MEDIUM,
    val expectedValue: Double = 0.0,
    val actualValue: Double = 0.0,
    val description: String,
    val isResolved: Boolean = false,
    val resolutionNotes: String? = null,
    val resolvedBy: String? = null,
    val resolvedAt: Long? = null,
    val detectedAt: Long = System.currentTimeMillis()
) {
    init {
        require(discrepancyId.isNotBlank()) { "Discrepancy ID cannot be blank." }
        require(reconciliationId.isNotBlank()) { "Reconciliation ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(description.isNotBlank()) { "Discrepancy description cannot be blank." }
        require(detectedAt > 0) { "Detected timestamp must be positive." }
    }
}

package com.sucharu.sucharupro.domain.model.inventory.analytics

/**
 * Governance record for tracking inventory data anomalies (Module 07 Step 10).
 */
data class InventoryException(
    val exceptionId: String,
    val projectId: String,
    val type: InventoryExceptionType,
    val targetId: String,
    val targetType: TargetType,
    val severity: Severity,
    val status: InventoryExceptionStatus,
    val detectedAt: String,
    val details: String? = null
) {
    enum class TargetType {
        PRODUCT,
        LOCATION
    }

    enum class Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    init {
        require(exceptionId.isNotBlank()) { "Exception ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(targetId.isNotBlank()) { "Target ID cannot be blank." }
        require(detectedAt.isNotBlank()) { "DetectedAt timestamp cannot be blank." }
    }
}

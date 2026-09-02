package com.sucharu.sucharupro.domain.model.qc.governance

/**
 * Configurable KPI target record for project-level quality governance (Module 06 Step 10).
 */
data class QcKpiTarget(
    val targetId: String,
    val projectId: String,
    val kpiType: QcGovernanceKpi,
    val targetValue: Double,
    val minimumAcceptableValue: Double? = null,
    val maximumAcceptableValue: Double? = null,
    val unit: String = kpiType.unit,
    val effectiveFrom: String,
    val effectiveTo: String? = null,
    val configuredBy: String,
    val createdAt: String,
    val updatedAt: String,
    val active: Boolean = true
) {
    init {
        require(targetId.isNotBlank()) { "Target ID cannot be blank" }
        require(projectId.isNotBlank()) { "Project ID cannot be blank" }
        require(configuredBy.isNotBlank()) { "ConfiguredBy cannot be blank" }
        require(effectiveFrom.isNotBlank()) { "EffectiveFrom date cannot be blank" }
        if (minimumAcceptableValue != null && maximumAcceptableValue != null) {
            require(minimumAcceptableValue <= maximumAcceptableValue) {
                "Minimum acceptable value cannot exceed maximum acceptable value"
            }
        }
    }
}

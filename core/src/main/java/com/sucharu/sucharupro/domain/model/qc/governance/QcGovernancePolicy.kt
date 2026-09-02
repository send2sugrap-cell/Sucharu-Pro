package com.sucharu.sucharupro.domain.model.qc.governance

/**
 * Project or Organization-level QC Governance policy configuration.
 */
data class QcGovernancePolicy(
    val policyId: String,
    val projectId: String,
    val name: String,
    val description: String? = null,
    val autoCreateAlertsOnBreach: Boolean = true,
    val requireReviewOnCriticalBreach: Boolean = true,
    val maxOverdueActionDays: Int = 7,
    val reviewCycleDays: Int = 30,
    val effectiveFrom: String,
    val effectiveTo: String? = null,
    val configuredBy: String,
    val createdAt: String,
    val updatedAt: String,
    val active: Boolean = true
) {
    init {
        require(policyId.isNotBlank()) { "Policy ID cannot be blank" }
        require(projectId.isNotBlank()) { "Project ID cannot be blank" }
        require(name.isNotBlank()) { "Policy Name cannot be blank" }
        require(maxOverdueActionDays >= 1) { "Max overdue action days must be at least 1" }
        require(reviewCycleDays >= 1) { "Review cycle days must be at least 1" }
    }
}

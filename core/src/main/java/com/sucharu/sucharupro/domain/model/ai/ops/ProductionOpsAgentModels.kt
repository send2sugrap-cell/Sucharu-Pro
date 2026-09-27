package com.sucharu.sucharupro.domain.model.ai.ops

/**
 * Controlled Operational Exception Categories.
 */
enum class OpsExceptionCategory {
    SLA_RISK,
    PRODUCTION_DELAY,
    QC_REWORK,
    DELIVERY_RISK,
    VENDOR_BLOCKER,
    CUSTOMER_APPROVAL_PENDING,
    FINANCE_BLOCKER,
    RESOURCE_ATTENTION
}

/**
 * Structured Operational Observation with Fact / Interpretation / Recommendation Separation.
 */
data class StructuredOpsObservation(
    val observationId: String,
    val category: OpsExceptionCategory,
    val factSummary: String,         // Fact: What canonical ERP backend reports
    val interpretation: String,      // Interpretation: What facts indicate operationally
    val recommendation: String,      // Recommendation: What human manager may consider
    val targetEntityId: String? = null
)

/**
 * Master Production & Operations Daily Brief Model.
 */
data class ProductionOpsDailyBrief(
    val totalActiveJobsCount: Int,
    val delayedJobsCount: Int,
    val slaRiskJobsCount: Int,
    val readyShipmentsCount: Int,
    val qcExceptionsCount: Int,
    val vendorBlockersCount: Int,
    val observations: List<StructuredOpsObservation> = emptyList(),
    val isCanonicalPipelinePreserved: Boolean = true, // Critical Invariant: Always true (13 stages)!
    val generatedAt: String
)

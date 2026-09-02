package com.sucharu.sucharupro.domain.model.qc.governance

/**
 * Canonical quality KPIs for QC Governance (Module 06 Step 10).
 */
enum class QcGovernanceKpi(
    val defaultLabel: String,
    val unit: String,
    val isHigherBetter: Boolean
) {
    FIRST_PASS_RATE("First-Pass QC Rate", "%", true),
    DEFECT_RATE("Defect Rate", "%", false),
    REWORK_RATE("Rework Rate", "%", false),
    RE_QC_RATE("Re-QC Rate", "%", false),
    FINAL_QC_PASS_RATE("Final QC Pass Rate", "%", true),
    CRITICAL_DEFECT_RATE("Critical Defect Rate", "%", false),
    MAJOR_DEFECT_RATE("Major Defect Rate", "%", false),
    RECURRING_DEFECT_RATE("Recurring Defect Rate", "%", false),
    OPEN_DEFECT_RATE("Open Defect Rate", "%", false),
    DEFECT_CLOSURE_RATE("Defect Closure Rate", "%", true),
    REWORK_COMPLETION_RATE("Rework Completion Rate", "%", true),
    REWORK_TURNAROUND_TIME("Rework Turnaround Time", "min", false),
    RE_QC_FAILURE_RATE("Re-QC Failure Rate", "%", false),
    QC_COST_VARIANCE("QC Cost Variance", "currency", false),
    QC_TIME_VARIANCE("QC Time Variance", "min", false),
    QUALITY_EFFICIENCY_SCORE("Quality Efficiency Score", "pts", true);

    companion object {
        fun fromString(value: String?): QcGovernanceKpi? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}

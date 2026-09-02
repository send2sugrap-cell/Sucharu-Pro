package com.sucharu.sucharupro.domain.model.qc.analytics

/**
 * Supported QC operational metric types for analytics aggregation and filtering.
 */
enum class QcAnalyticsMetricType(val defaultLabel: String) {
    TOTAL_QC_COST("Total QC Cost"),
    TOTAL_QC_TIME("Total QC Time"),
    AVERAGE_QC_COST_PER_JOB("Avg QC Cost / Job"),
    AVERAGE_QC_TIME_PER_JOB("Avg QC Time / Job"),
    TOTAL_DEFECTS("Total Defects"),
    AVERAGE_DEFECTS_PER_JOB("Avg Defects / Job"),
    TOTAL_REWORKS("Total Reworks"),
    TOTAL_RE_QC_CYCLES("Total Re-QC Cycles"),
    FIRST_PASS_QC_RATE("First-Pass QC Rate"),
    REWORK_RATE("Rework Rate"),
    RE_QC_RATE("Re-QC Rate"),
    FINAL_QC_PASS_RATE("Final QC Pass Rate"),
    COST_VARIANCE("Cost Variance"),
    TIME_VARIANCE("Time Variance"),
    OPEN_DEFECT_COUNT("Open Defect Count"),
    ACTIVE_REWORK_COUNT("Active Rework Count"),
    FAILED_RE_QC_COUNT("Failed Re-QC Count"),
    RELEASED_JOB_COUNT("Released Job Count")
}

package com.sucharu.sucharupro.domain.model.production

/**
 * Execution status of a single production stage for a specific job in Sucharu Pro.
 *
 * Used to track per-stage state within a job's production lifecycle.
 * Supports rework cycles: a stage can go from COMPLETED → REWORK → IN_PROGRESS → COMPLETED.
 *
 * Future use: Each job will maintain a list of ProductionStageRecord entries,
 * one per applicable [ProductionStageType], with this status.
 */
enum class ProductionStageStatus(val defaultLabel: String) {
    /** Stage has not been started yet. */
    PENDING("Pending"),

    /** Stage is currently being worked on. */
    IN_PROGRESS("In Progress"),

    /** Stage completed successfully. */
    COMPLETED("Completed"),

    /** Stage was not applicable for this job type and was intentionally skipped. */
    SKIPPED("Skipped"),

    /**
     * Stage failed QC and is being reworked.
     * Only applicable to [ProductionStageType.isQcStage] = true stages,
     * or stages that a QC failure caused rework to return to.
     */
    REWORK("Rework"),

    /** Stage is paused/blocked (e.g. waiting for customer approval). */
    ON_HOLD("On Hold")
}

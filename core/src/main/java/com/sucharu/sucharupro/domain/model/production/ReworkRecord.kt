package com.sucharu.sucharupro.domain.model.production

/**
 * Domain foundation for QC rework cycles in Sucharu Pro.
 *
 * Represents a single rework event: when a job fails QC at a given stage,
 * this records what failed, why, where it needs to return, and when.
 *
 * This is a FOUNDATION model only. Full QC/Rework module implementation
 * is deferred to a future module. This model establishes the data contract
 * so that future features can be built without breaking existing architecture.
 *
 * Rework cycle:
 * QC_STAGE fails → ReworkRecord created → job returns to [returnToStage] → re-runs → QC_STAGE again
 */
data class ReworkRecord(
    /** Unique identifier for this rework event. */
    val reworkId: String,

    /** The job this rework is associated with. */
    val jobId: String,

    /** The QC stage at which the failure was detected. */
    val failedAtStage: ProductionStageType,

    /** The production stage the job must return to for rework. */
    val returnToStage: ProductionStageType,

    /** Description of what failed and what needs to be corrected. */
    val failureReason: String,

    /** Additional notes from the QC inspector. */
    val inspectorNotes: String = "",

    /** ISO 8601 timestamp when the rework was initiated (e.g. "2026-08-15T10:30:00"). */
    val initiatedAt: String,

    /** ISO 8601 timestamp when rework was resolved. Null if still in progress. */
    val resolvedAt: String? = null,

    /** Whether this rework cycle has been closed. */
    val isResolved: Boolean = false,

    /** How many rework cycles this job has undergone in total (for escalation logic). */
    val reworkCycleNumber: Int = 1
)

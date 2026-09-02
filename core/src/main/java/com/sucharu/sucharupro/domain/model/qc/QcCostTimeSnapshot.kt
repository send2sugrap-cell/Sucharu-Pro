package com.sucharu.sucharupro.domain.model.qc

/**
 * Immutable historical snapshot generated upon finalizing and locking QC cost/time reconciliation (Module 06 Step 08).
 *
 * Preserves the exact values, variance metrics, and referenced source IDs permanently.
 */
data class QcCostTimeSnapshot(
    val snapshotId: String,
    val reconciliationId: String,
    val productionJobId: String,
    val projectId: String,
    val plannedCost: Double,
    val actualCost: Double,
    val costVariance: Double,
    val plannedMinutes: Long,
    val actualMinutes: Long,
    val timeVarianceMinutes: Long,
    val costEntryIds: List<String>,
    val timeEntryIds: List<String>,
    val defectIds: List<String> = emptyList(),
    val reworkIds: List<String> = emptyList(),
    val reQcIds: List<String> = emptyList(),
    val finalQcId: String? = null,
    val currency: String = "BDT",
    val lockedBy: String,
    val lockedByName: String? = null,
    val lockedAt: String,
    val notes: String? = null,
    val createdAt: String
)

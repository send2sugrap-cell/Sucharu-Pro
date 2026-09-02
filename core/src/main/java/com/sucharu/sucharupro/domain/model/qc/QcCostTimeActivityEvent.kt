package com.sucharu.sucharupro.domain.model.qc

/**
 * Immutable append-only audit activity event recording QC cost, time, and reconciliation transitions (Module 06 Step 08).
 */
data class QcCostTimeActivityEvent(
    val eventId: String,
    val productionJobId: String,
    val projectId: String,
    val costEntryId: String? = null,
    val timeEntryId: String? = null,
    val reconciliationId: String? = null,
    val snapshotId: String? = null,
    val actorId: String,
    val actorName: String? = null,
    val activityType: QcCostTimeActivityType,
    val notes: String? = null,
    val timestamp: String,
    val metadata: Map<String, String> = emptyMap()
)

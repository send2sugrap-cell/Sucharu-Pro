package com.sucharu.sucharupro.domain.model.qc

import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Immutable audit trail event model for Re-QC inspection lifecycle actions (Module 06 Step 06).
 */
data class ReQcActivityEvent(
    val eventId: String,
    val reQcId: String,
    val cycleNumber: Int,
    val productionJobId: String,
    val projectId: String,
    val relatedDefectId: String? = null,
    val relatedReworkId: String? = null,
    val actorId: String? = null,
    val actorName: String? = null,
    val role: UserRole? = null,
    val activityType: ReQcActivityType,
    val notes: String? = null,
    val timestamp: String
)

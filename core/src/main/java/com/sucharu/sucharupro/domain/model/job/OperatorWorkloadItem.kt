package com.sucharu.sucharupro.domain.model.job

/**
 * Derived item representing the current production workload and active tasks for an operator.
 */
data class OperatorWorkloadItem(
    val operatorId: String,
    val operatorName: String,
    val activeWorkCount: Int = 0,
    val inProgressCount: Int = 0,
    val pendingAssignedCount: Int = 0,
    val completedCount: Int = 0,
    val urgentCount: Int = 0,
    val currentJobSummary: String? = null
)

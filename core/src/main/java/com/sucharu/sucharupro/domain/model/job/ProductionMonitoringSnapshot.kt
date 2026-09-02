package com.sucharu.sucharupro.domain.model.job

/**
 * Immutable snapshot of high-level production metrics and aggregated KPIs across all jobs.
 */
data class ProductionMonitoringSnapshot(
    val totalJobs: Int = 0,
    val activeJobs: Int = 0,
    val draftJobs: Int = 0,
    val readyForProductionJobs: Int = 0,
    val inProgressJobs: Int = 0,
    val onHoldJobs: Int = 0,
    val readyJobs: Int = 0,
    val deliveredJobs: Int = 0,
    val cancelledJobs: Int = 0,
    val activeStageCount: Int = 0,
    val completedStageCount: Int = 0,
    val assignedStageCount: Int = 0,
    val unassignedPendingStageCount: Int = 0,
    val overallProgressFraction: Float = 0f,
    val urgentJobCount: Int = 0,
    val highPriorityJobCount: Int = 0,
    val attentionRequiredCount: Int = 0
)

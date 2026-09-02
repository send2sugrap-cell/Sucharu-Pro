package com.sucharu.sucharupro.domain.model.qc.governance

import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsPeriod

/**
 * Immutable point-in-time snapshot of quality performance and governance metrics (Module 06 Step 10).
 */
data class QcGovernanceSnapshot(
    val snapshotId: String,
    val projectId: String,
    val period: QcAnalyticsPeriod,
    val kpiValues: Map<String, Double> = emptyMap(),
    val kpiTargets: Map<String, Double> = emptyMap(),
    val thresholdStates: Map<String, String> = emptyMap(),
    val totalAlertCount: Int = 0,
    val openCriticalAlertCount: Int = 0,
    val totalDefectCount: Int = 0,
    val recurringDefectCount: Int = 0,
    val reworkCount: Int = 0,
    val reQcCycleCount: Int = 0,
    val costVariance: Double = 0.0,
    val timeVarianceMinutes: Long = 0L,
    val qualityEfficiencyScore: Double = 100.0,
    val generatedAt: String,
    val generatedBy: String
) {
    init {
        require(snapshotId.isNotBlank()) { "Snapshot ID cannot be blank" }
        require(projectId.isNotBlank()) { "Project ID cannot be blank" }
        require(generatedAt.isNotBlank()) { "GeneratedAt timestamp cannot be blank" }
        require(generatedBy.isNotBlank()) { "GeneratedBy cannot be blank" }
    }
}

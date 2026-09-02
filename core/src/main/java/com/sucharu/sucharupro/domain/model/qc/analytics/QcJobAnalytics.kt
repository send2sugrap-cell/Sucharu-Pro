package com.sucharu.sucharupro.domain.model.qc.analytics

import kotlin.math.max
import kotlin.math.min

/**
 * Per-production-job analytical breakdown.
 */
data class QcJobAnalytics(
    val productionJobId: String,
    val projectId: String,
    val totalQcCost: Double = 0.0,
    val totalQcTimeMinutes: Long = 0L,
    val plannedQcCost: Double = 0.0,
    val actualQcCost: Double = 0.0,
    val costVariance: Double = 0.0,
    val plannedQcTimeMinutes: Long = 0L,
    val actualQcTimeMinutes: Long = 0L,
    val timeVarianceMinutes: Long = 0L,
    val defectCount: Int = 0,
    val reworkCount: Int = 0,
    val reQcCycleCount: Int = 0,
    val finalQcPassed: Boolean = false,
    val productionReleased: Boolean = false,
    val firstPassQc: Boolean = true,
    val efficiencyScore: Double = 100.0
) {
    companion object {
        /**
         * Calculates deterministic QC Operational Efficiency Score (0.0 to 100.0).
         *
         * Base: 100.0
         * Deductions:
         * - Cost overrun: up to 25 pts (proportional to % overrun)
         * - Time overrun: up to 25 pts (proportional to % overrun)
         * - Defect penalty: 10 pts per defect (max 20 pts)
         * - Rework penalty: 7.5 pts per rework (max 15 pts)
         * - Re-QC loop penalty: 7.5 pts per cycle (max 15 pts)
         */
        fun calculateEfficiencyScore(
            plannedCost: Double,
            actualCost: Double,
            plannedMinutes: Long,
            actualMinutes: Long,
            defectCount: Int,
            reworkCount: Int,
            reQcCycleCount: Int
        ): Double {
            var score = 100.0

            val costOverrun = actualCost - plannedCost
            if (costOverrun > 0.0) {
                val baseline = if (plannedCost > 0.0) plannedCost else 1.0
                val ratio = costOverrun / baseline
                val costPenalty = min(25.0, ratio * 25.0)
                score -= costPenalty
            }

            val timeOverrun = actualMinutes - plannedMinutes
            if (timeOverrun > 0L) {
                val baseline = if (plannedMinutes > 0L) plannedMinutes.toDouble() else 1.0
                val ratio = timeOverrun.toDouble() / baseline
                val timePenalty = min(25.0, ratio * 25.0)
                score -= timePenalty
            }

            val defectPenalty = min(20.0, defectCount * 10.0)
            score -= defectPenalty

            val reworkPenalty = min(15.0, reworkCount * 7.5)
            score -= reworkPenalty

            val reQcPenalty = min(15.0, reQcCycleCount * 7.5)
            score -= reQcPenalty

            return max(0.0, min(100.0, score))
        }
    }
}
